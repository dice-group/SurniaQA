package de.upb.ds.surnia.queries;

import de.upb.ds.surnia.preprocessing.model.Token;
import java.util.ArrayList;
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
  private String bestQuestionTemplate;
  private HashMap<String, List<String>> possibleReplacements;

  /**
   * Create a replacer for all combinations of the query with the question.
   *
   * @param questionTokens Pre-processing result of the question.
   * @param bestQuestionTemplate the best fitting template for the given question
   * @param queryTemplate QueryTemplate with the parameters to be replaced.
   */
  public QueryParameterReplacer(List<Token> questionTokens, String bestQuestionTemplate,
    QueryTemplate queryTemplate) {
    queryString = queryTemplate.getSparqlTemplate();
    tokens = questionTokens;
    params = queryTemplate.getSparqlParams();
    usedTokens = new LinkedList<>();
    this.bestQuestionTemplate = bestQuestionTemplate;
  }

  /**
   * Generate all replacement combinations.
   *
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

  private HashMap<String, String> createCombination(int[] counter) {
    HashMap<String, String> combination = new HashMap<>();
    for (int i = 0; i < params.length; i++) {
      String param = params[i];
      String uri = possibleReplacements.get(param).get(counter[i]);
      combination.put(param, uri);
    }
    return combination;
  }

  private List<String> getUrisFromClosestToken(String param) {
    boolean resourceWanted = param.charAt(0) == 'R';

    int pos = 0;
    for (String posTag : bestQuestionTemplate.split(" ")) {
      if (posTag.equalsIgnoreCase(param)) {
        break;
      }
      pos++;
    }

    for (int distance = 0; distance < tokens.size(); distance++) {
      int leftPos = pos - distance;
      int rightPos = pos + distance;
      if (leftPos >= 0 && leftPos < tokens.size()) {
        List<String> uris = getUrisForToken(tokens.get(leftPos), resourceWanted);
        if (uris != null) {
          return uris;
        }
      }
      if (rightPos < tokens.size()) {
        List<String> uris = getUrisForToken(tokens.get(rightPos), resourceWanted);
        if (uris != null) {
          return uris;
        }
      }
    }
    return null;
  }

  private List<String> getUrisForToken(Token token, boolean resourceWanted) {
    if (!usedTokens.contains(token)) {
      if (!token.getUris().isEmpty()) {
        if (token.getUris().iterator().next().contains("resource") && resourceWanted) {
          usedTokens.add(token);
          return new ArrayList<>(token.getUris());
        } else if (token.getUris().iterator().next().contains("ontology") && !resourceWanted) {
          usedTokens.add(token);
          return new ArrayList<>(token.getUris());
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
