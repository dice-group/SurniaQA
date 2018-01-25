package de.upb.ds.surnia.preprocessing;

import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLPClient;
import edu.stanford.nlp.util.CoreMap;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProcessingPipeline {

  final Logger logger = LoggerFactory.getLogger(ProcessingPipeline.class);

  // Configuration variables for the connection to CoreNLP and FOX
  private static final String CORE_NLP_URL = "http://139.18.2.39";
  private static final int CORE_NLP_PORT = 9000;
  private static final String FOX_URL = "http://fox.cs.uni-paderborn.de:4444/fox";

  // Interfaces for the interaction with the preprocessors
  private StanfordCoreNLPClient nlpClient;
  private HttpClient httpClient;
  private HttpPost foxPost;
  private OntologyIndex dboIndex;

  // JSON template for a FOX request
  private static final String FOX_REQUEST_PAYLOAD = "{\n"
          + "\"input\" : \"$INPUT$\",\n"
          + "\"type\": \"text\",\n"
          + "\"task\": \"ner\",\n"
          + "\"output\": \"turtle\",\n"
          + "\"lang\": \"en\",\n"
          + "\"foxlight\":\"org.aksw.fox.tools.ner.en.IllinoisExtendedEN\"\n"
          + "}";

  // SPARQL query to get the NER results from a FOX request
  private static final String FOX_RESULT_SPARQL_QUERY = "SELECT ?entity ?appearance "
          + "WHERE {?s <http://www.w3.org/2005/11/its/rdf#taIdentRef>  ?entity . "
          + " ?s <http://persistence.uni-leipzig.org/nlp2rdf/ontologies/nif-core#anchorOf> ?appearance}";

  // POS tags for that OntologyIndex should try to find a matching ontology property
  private static final List<String> DBO_INDEX_TAGS = Arrays.asList(new String[]{
      "FW", "JJ", "JJR", "JJS", "NN", "NNS", "RB", "RBR", "RBS", "VB", "VBN", "VBD"
  });

  /**
   * Create pipeline with CoreNLP, FOX and OntologyIndex.
   */
  public ProcessingPipeline() {
    // Init CoreNLP client
    Properties nlpProperties = new Properties();
    nlpProperties.setProperty("annotators", "tokenize, ssplit, pos, lemma");
    nlpClient = new StanfordCoreNLPClient(nlpProperties, CORE_NLP_URL, CORE_NLP_PORT, 1);

    // Init FOX client
    httpClient = HttpClientBuilder.create().build();
    foxPost = new HttpPost(FOX_URL);

    // Init OntologyIndex
    dboIndex = new OntologyIndex();
  }

  /**
   * Terminate all clients.
   * @throws InterruptedException Interruption while closing CoreNLP client.
   */
  public void close() throws InterruptedException {
    nlpClient.shutdown();
    foxPost.abort();
    dboIndex.close();
  }

  /**
   * Processes a question into a List of tokens.
   *
   * @param question Input question.
   * @return List of tokens extracted from the input question.
   */
  public List<Token> processQuestion(String question) throws IOException {
    List<Token> tokens = nlp(question);
    tokens = namedEntityExtraction(question, tokens);
    for (int i = 0; i < tokens.size(); i++) {
      if (tokens.get(i).getUris() == null && DBO_INDEX_TAGS.contains(tokens.get(i).getType())) {
        List<String> uris = dboIndex.search(tokens.get(i).getText());
        if (uris.size() > 0) {
          Token oldToken = tokens.get(i);
          Token t = new Token(oldToken.getText(), oldToken.getType(), oldToken.getLemma(), uris);
          tokens.set(i, t);
        } else {
          uris = dboIndex.search(tokens.get(i).getLemma());
          if (uris.size() > 0) {
            Token oldToken = tokens.get(i);
            Token t = new Token(oldToken.getText(), oldToken.getType(), oldToken.getLemma(), uris);
            tokens.set(i, t);
          }
        }
      }
    }
    return tokens;
  }

  /**
   * Performs POS tagging on text when the text contains only one sentence.
   *
   * @param input Input text.
   * @return List of tokens from the input sentence.
   */
  private List<Token> nlp(String input) {
    LinkedList<Token> tokens = new LinkedList<>();
    Annotation annotations = new Annotation(input);
    nlpClient.annotate(annotations);
    List<CoreMap> sentences = annotations.get(SentencesAnnotation.class);
    if (sentences.size() != 1) {
      logger.error("Input contains more than one sentence!");
    } else {
      for (CoreMap sentence : sentences) {
        for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
          String text = token.get(TextAnnotation.class);
          String pos = token.get(PartOfSpeechAnnotation.class);
          String lemma = token.get(LemmaAnnotation.class);
          tokens.add(new Token(text, pos, lemma));
        }
      }
    }
    return tokens;
  }

  private List<Token> namedEntityExtraction(String input, List<Token> tokens) throws IOException {
    String query = FOX_REQUEST_PAYLOAD.replace("$INPUT$", input);
    foxPost.setEntity(new StringEntity(query, ContentType.APPLICATION_JSON));
    HttpResponse response = httpClient.execute(foxPost);
    Model model = ModelFactory.createDefaultModel();
    model.read(response.getEntity().getContent(), null, "TURTLE");
    try (QueryExecution qexec = QueryExecutionFactory.create(FOX_RESULT_SPARQL_QUERY, model)) {
      ResultSet results = qexec.execSelect();
      while (results.hasNext()) {
        QuerySolution solution = results.nextSolution();
        String appearance = solution.get("appearance").asLiteral().getString();
        String resource = solution.get("entity").asResource().getURI();
        List<String> uri = new LinkedList<>();
        uri.add(resource);
        tokens = linkUri(tokens, appearance, uri);
      }
    }
    return tokens;
  }

  /**
   * Combine tokens that can be linked to a DBpedia resource.
   *
   * @param tokens     List of tokens.
   * @param appearance String appearance of the found resource.
   * @param uris       DBpedia URI of the found resource.
   * @return List of tokens where the resource tokens are combined and linked.
   */
  private List<Token> linkUri(List<Token> tokens, String appearance, List<String> uris) {
    List<Token> linkedTokens = new LinkedList<>();
    HashMap<String, Integer> posTags = new HashMap<>();
    boolean appearanceFound = false;
    boolean entityAdded = false;
    for (Token token : tokens) {
      if (appearance.contains(token.getText()) && token.getUris() == null) {
        if (!appearanceFound) {
          appearanceFound = true;
        }
        if (posTags.containsKey(token.getType())) {
          posTags.put(token.getType(), posTags.get(token.getType()) + 1);
        } else {
          posTags.put(token.getType(), 1);
        }
      } else {
        if (!appearanceFound || entityAdded) {
          linkedTokens.add(token);
        } else {
          if (appearanceFound && !entityAdded) {
            entityAdded = true;
            String posTag = "NNP";
            int max = 0;
            for (String tag : posTags.keySet()) {
              if (posTags.get(tag) > max) {
                max = posTags.get(tag);
                posTag = tag;
              }
            }
            linkedTokens.add(new Token(appearance, posTag, uris));
            linkedTokens.add(token);
          }
        }
      }
    }
    return linkedTokens;
  }
}
