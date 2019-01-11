package de.upb.ds.surnia.queries;

import de.upb.ds.surnia.preprocessing.model.Token;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.ResourceFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

public class QueryParameterReplacer {

  private String[] params;
  private List<Token> tokens;
  private List<Token> usedTokens;
  private String queryString;
  private String bestQuestionTemplate;
  private HashMap<String, List<String>> possibleParamInputs;

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
    if (null != combinations) {
      for (HashMap<String, String> parameterReplacement : combinations) {
        ParameterizedSparqlString query = new ParameterizedSparqlString(queryString);
        for (String param : params) {
          query.setParam(param, ResourceFactory.createResource(parameterReplacement.get(param)));
        }
        queries.add(query);
      }
    }
    return queries;
  }

  /**
   * Produces all possible combinations of parameter replacements. E.g. for 3 parameter with 2 URIs
   * each, it will produce 8 combinations. Each list element contains 1 combination.
   */
  private List<HashMap<String, String>> generateParameterReplacementCombinations() {
    List<HashMap<String, String>> combinations = new LinkedList<>();
    possibleParamInputs = new HashMap<>();
    for (String param : params) {
      List<String> uris = getUrisFromClosestToken(param);
      if (null != uris)
        possibleParamInputs.put(param, uris);
      else
        return null;
    }

    /* Produce a array which holds the information which combination we should use now.
     * I.e. for three parameters A,B,C (this order) and the counter-array [2,0,4],
     * it would specify that the 3rd URI for A, the 1st URI for B and the 5th URI for C should be
     * used for this combination.
     */
    int[] counter = new int[params.length];
    for (int i = 0; i < counter.length; i++) {
      counter[i] = 0;
    }
    while (!allCombinationsProduced(counter)) {
      combinations.add(createCombination(counter));
      increaseCounterArray(counter, 0);
    }
    combinations.add(createCombination(counter));
    return combinations;
  }

  private void increaseCounterArray(int[] counter, int i) {
    int length = possibleParamInputs.get(params[i]).size() - 1;
    if (counter[i] < length) {
      counter[i]++;
    } else if (i < counter.length - 1) {
      counter[i] = 0;
      increaseCounterArray(counter, i + 1);
    }
  }

  private boolean allCombinationsProduced(int[] counter) {
    for (int i = 0; i < counter.length; i++) {
      if (null != possibleParamInputs.get(params[i])) {
        if (counter[i] < (possibleParamInputs.get(params[i]).size() - 1)) {
          return false;
        }
      }
    }
    return true;
  }

  private HashMap<String, String> createCombination(int[] counter) {
    HashMap<String, String> combination = new HashMap<>();
    for (int i = 0; i < params.length; i++) {
      String param = params[i];
      String uri = possibleParamInputs.get(param).get(counter[i]);
      combination.put(param, uri);
    }
    return combination;
  }

  /**
   * Searches for URIs in tokens close to the given POS-Tag and returns them. The given parameter is
   * the parameter for the SPARQL-query.
   *
   * @param param POS-Tag that indicates where we should start the search in the question-template
   * @return List of URIs in a token that is close to the token we wanted it for
   */
  private List<String> getUrisFromClosestToken(String param) {
    boolean resourceWanted = param.charAt(0) == 'R';

    // Get position of param in question template
    int pos = 0;
    for (String posTag : bestQuestionTemplate.split(" ")) {
      if (posTag.equalsIgnoreCase(param)) {
        break;
      }
      pos++;
    }

    // Iterate over the tokens in both directions until you find a token with URIs
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

  /**
   * Returns a list of URIs for the given token. For this, it is considered if we want a resource or
   * not.
   *
   * @param token token to be inspected for URIs
   * @param resourceWanted true if we want a Ressource as URI
   * @return list of URIs extracted from the given token
   */
  private List<String> getUrisForToken(Token token, boolean resourceWanted) {
    if (!usedTokens.contains(token)) {
      if (!token.getUris().isEmpty()) {
        if (token.getUris().iterator().next().contains("resource") && resourceWanted) {
          usedTokens.add(token);
          return new ArrayList<>(token.getUris());
        } else if (token.getUris().iterator().next().contains("ontology")) {
          usedTokens.add(token);
          return new ArrayList<>(token.getUris());
        } else if (token.getUris().iterator().next().contains("property")) {
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
