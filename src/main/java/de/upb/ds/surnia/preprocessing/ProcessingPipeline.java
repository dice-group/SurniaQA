package de.upb.ds.surnia.preprocessing;

import de.upb.ds.surnia.preprocessing.model.Token;
import de.upb.ds.surnia.preprocessing.tasks.StanfordNERTask;
import de.upb.ds.surnia.preprocessing.tasks.TaskInterface;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProcessingPipeline {

  // Configuration variables for the connection to CoreNLP and FOX
  private static final String FOX_URL = "http://fox.cs.uni-paderborn.de:4444/fox";
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
  final Logger logger = LoggerFactory.getLogger(ProcessingPipeline.class);

  private List<TaskInterface> taskPipeline;


  public ProcessingPipeline(){
    this.taskPipeline = new ArrayList<>();
    taskPipeline.add(new StanfordNERTask());
  }

  /**
   * Create pipeline with given tasks. The first task has to split the text up into its components!
   */
  public ProcessingPipeline(List<TaskInterface> taskPipeline) {
    this.taskPipeline = new ArrayList<>(taskPipeline);
  }


  /**
   * Processes a question into a List of tokens.
   *
   * @param question Input question.
   * @return List of tokens extracted from the input question.
   * @throws IOException Error while performing Named Entity Recognition.
   */
  public List<Token> processQuestion(String question) {
    List<Token> tokens = new ArrayList<>();
    for(TaskInterface task : taskPipeline){
      tokens = task.processTokens(question, tokens);
    }
    return tokens;
  }

//  private List<Token> namedEntityExtraction(String input, List<Token> tokens) throws IOException {
//    String query = FOX_REQUEST_PAYLOAD.replace("$INPUT$", input);
//    foxPost.setEntity(new StringEntity(query, ContentType.APPLICATION_JSON));
//    HttpResponse response = httpClient.execute(foxPost);
//    Model model = ModelFactory.createDefaultModel();
//    model.read(response.getEntity().getContent(), null, "TURTLE");
//    try (QueryExecution qexec = QueryExecutionFactory.create(FOX_RESULT_SPARQL_QUERY, model)) {
//      ResultSet results = qexec.execSelect();
//      while (results.hasNext()) {
//        QuerySolution solution = results.nextSolution();
//        String appearance = solution.get("appearance").asLiteral().getString();
//        String resource = solution.get("entity").asResource().getURI();
//        List<String> uri = new LinkedList<>();
//        uri.add(resource);
//        tokens = linkUri(tokens, appearance, uri);
//      }
//    }
//    return tokens;
//  }

  /**
   * Combine tokens that can be linked to a DBpedia resource.
   *
   * @param tokens List of tokens.
   * @param appearance String appearance of the found resource.
   * @param uris DBpedia URI of the found resource.
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
