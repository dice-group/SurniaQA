package de.upb.ds.surnia.queries;

import de.upb.ds.surnia.preprocessing.Token;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.ResourceFactory;

public class QueryParameterReplacer {

  private String[] params;
  private List<Token> tokens;
  private List<Token> usedTokens;
  private String queryString;
  private String exampleQuestion;
  private HashMap<String, List<String>> possibleReplacements;

  /**
   * Create a replacer for all combinations of the query with the question.
   * @param questionTokens Preprocessing result of the question.
   * @param query Query with the parameters to be replaced.
   */
  public QueryParameterReplacer(List<Token> questionTokens, String bestExampleQuestion, Query query) {
    queryString = query.sparqlTemplate;
    tokens = questionTokens;
    params = query.sparqlParams;
    usedTokens = new LinkedList<>();
    exampleQuestion = bestExampleQuestion;
  }

  /**
   * Generate all replacement combinations.
   * @return All possible query replacements.
   */
  public List<ParameterizedSparqlString> getQueriesWithReplacedParameters() {
    LinkedList<ParameterizedSparqlString> queries = new LinkedList<>();
    List<HashMap<String, String>> combinations = generateParameterReplacementCombinations();
    for (HashMap<String, String> parameterReplacement : combinations) {
      ParameterizedSparqlString query = new ParameterizedSparqlString(queryString);
      for (String param : params) {
        query.setParam(param, ResourceFactory.createResource(parameterReplacement.get(param)));
      }
      queries.add(query);
    }
    return queries;
  }

  private List<HashMap<String, String>> generateParameterReplacementCombinations() {
    List<HashMap<String, String>> combinations = new LinkedList<>();
    possibleReplacements = new HashMap<>();
    for (String param : params) {
      List<String> uris = getUrisFromClosestToken(param);
      possibleReplacements.put(param, uris);
    }
    int[] counter = new int[params.length];
    for (int i = 0; i < counter.length; i++) {
      counter[i] = 0;
    }
    while (!isCounterFinished(counter)) {
      combinations.add(createCombination(counter));
      increaseCounterArray(counter, 0);
    }
    combinations.add(createCombination(counter));
    return combinations;
  }

  private void increaseCounterArray(int[] counter, int i) {
    int l = possibleReplacements.get(params[i]).size() - 1;
    if (counter[i] < l) {
      counter[i]++;
    } else if (i < counter.length - 1) {
      counter[i] = 0;
      increaseCounterArray(counter, i + 1);
    }
  }

  private boolean isCounterFinished(int[] counter) {
    for (int i = 0; i < counter.length; i++) {
      if (counter[i] < (possibleReplacements.get(params[i]).size() - 1)) {
        return false;
      }
    }
    return true;
  }

  private HashMap<String, String> createCombination (int[] counter) {
    HashMap<String, String> combination = new HashMap<>();
    for (int i = 0; i < params.length; i++) {
      String param = params[i];
      String uri = possibleReplacements.get(param).get(counter[i]);
      combination.put(param, uri);
    }
    return combination;
  }

  private List<String> getUrisFromClosestToken(String param) {
    int p = 0;
    boolean resourceWanted = param.charAt(0) == 'R';
    for (String s : exampleQuestion.split(" ")) {
      p++;
      if (s.equalsIgnoreCase(param)) {
        break;
      }
    }
    for (int d = 0; d < tokens.size(); d++) {
      int iL = p - d;
      int iR = p + d;
      if (iL >= 0 && iL < tokens.size()) {
        List<String> uris = checkToken(tokens.get(iL), resourceWanted);
        if (uris != null) {
          return uris;
        }
      }
      if (iR < tokens.size()) {
        List<String> uris = checkToken(tokens.get(iR), resourceWanted);
        if (uris != null) {
          return uris;
        }
      }
    }
    return null;
  }

  private List<String> checkToken (Token token, boolean resourceWanted) {
    if (!usedTokens.contains(token)) {
      if (token.getUris() != null) {
        if (token.getUris().get(0).contains("resource") && resourceWanted) {
          usedTokens.add(token);
          return token.getUris();
        } else if (token.getUris().get(0).contains("ontology") && !resourceWanted) {
          usedTokens.add(token);
          return token.getUris();
        } else {
          return null;
        }
      } else {
        return null;
      }
    } else {
      return null;
    }
  }
}
