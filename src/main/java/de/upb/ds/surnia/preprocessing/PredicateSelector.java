package de.upb.ds.surnia.preprocessing;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import org.aksw.hawk.index.DBOIndex;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PredicateSelector {

  // POS tags for that OntologyIndex should try to find a matching ontology property
  private static final List<String> DBO_INDEX_TAGS = Arrays.asList(new String[]{
    "FW", "JJ", "JJR", "JJS", "NN", "NNS", "RB", "RBR", "RBS", "VB", "VBN", "VBD", "VBG"
  });
  private static final int MAX_PREDICATE_AMOUNT = 5;
  private static final String PREDICATE_FREQUENCY_QUERY =
    "SELECT (COUNT(DISTINCT ?x) AS ?n) WHERE {?x ?p ?y . }";
  final Logger logger = LoggerFactory.getLogger(PredicateSelector.class);
  private DBOIndex dboIndex;
  private HashMap<String, Integer> predicateFrequency;


  public PredicateSelector() {
    dboIndex = new DBOIndex();
    predicateFrequency = new HashMap<>();
  }

  /**
   * Adds the best predicate look up results from the DBpedia Ontology.
   *
   * @param tokens Token to do the look up for.
   * @return Token with added predicate URIs if appropriate.
   */
  public List<Token> addPredicates(List<Token> tokens) {
    for (int i = 0; i < tokens.size(); i++) {
      if (tokens.get(i).getUris() == null && DBO_INDEX_TAGS.contains(tokens.get(i).getType())) {
        tokens.set(i, predicatesLookUp(tokens.get(i)));
      }
    }
    return tokens;
  }

  private Token predicatesLookUp(Token token) {
    List<String> uris = dboIndex.search(token.getText());
    if (uris.size() > 0) {
      uris = filterPredicates(uris, token.getText());
      logger.info(token.getText() + " predicates filtered: " + uris);
      Token t = new Token(token.getText(), token.getType(), token.getLemma(), uris);
      return t;
    } else {
      uris = dboIndex.search(token.getLemma());
      if (uris.size() > 0) {
        uris = filterPredicates(uris, token.getLemma());
        logger.info(token.getText() + " predicates of lemma filtered: " + uris);
        Token t = new Token(token.getText(), token.getType(), token.getLemma(), uris);
        return t;
      } else {
        return token;
      }
    }
  }

  private List<String> filterPredicates(List<String> predicates, String tokenText) {
    predicates.sort(new Comparator<String>() {
      @Override
      public int compare(String p1, String p2) {
        boolean c1 = p1.toLowerCase().contains(tokenText.toLowerCase());
        boolean c2 = p2.toLowerCase().contains(tokenText.toLowerCase());
        if (c1 & !c2) {
          return -1;
        }
        if (!c1 & c2) {
          return 1;
        }
        int f1 = getPredicateFrequency(p1);
        int f2 = getPredicateFrequency(p2);
        if (f1 == f2) {
          return 0;
        } else if (f1 > f2) {
          return -1;
        } else {
          return 1;
        }
      }
    });
    if (predicates.size() > MAX_PREDICATE_AMOUNT) {
      return predicates.subList(0, MAX_PREDICATE_AMOUNT);
    } else {
      return predicates;
    }
  }

  private int getPredicateFrequency(String uri) {
    int f;
    if (predicateFrequency.containsKey(uri)) {
      f = predicateFrequency.get(uri);
    } else {
      ParameterizedSparqlString query = new ParameterizedSparqlString(PREDICATE_FREQUENCY_QUERY);
      query.setParam("?p", ResourceFactory.createResource(uri));
      QueryExecution execution = QueryExecutionFactory
        .sparqlService("http://dbpedia.org/sparql", query.asQuery());
      QuerySolution solution = execution.execSelect().next();
      f = solution.getLiteral("?n").getInt();
      predicateFrequency.put(uri, f);
    }
    return f;
  }

}
