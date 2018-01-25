package de.upb.ds.surnia.queries;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.ResourceFactory;

public class QueryParameterReplacer {

  private List<List<String>> phR;
  private List<List<String>> phO;
  private String[] params;
  private String queryString;

  /**
   * Create a replacer for all combinations of the query with the question.
   * @param questionProperties Questions with all possible uris.
   * @param query Query with the parameters to be replaced.
   */
  public QueryParameterReplacer(QuestionProperties questionProperties, Query query) {
    queryString = query.sparqlTemplate;
    params = query.sparqlParams;
    phR = questionProperties.resources;
    phO = questionProperties.ontologies;
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
    int[] counter = new int[phO.size() + phR.size()];
    for (int i = 0; i < counter.length; i++) {
      counter[i] = 0;
    }
    while (!isCounterFinished(counter)) {
      HashMap<String, String> combination = new HashMap<>();
      for (String param : params) {
        char type = param.charAt(0);
        int index = Integer.parseInt(param.substring(1)) - 1;
        String replacement;
        if (type == 'R') {
          replacement = phR.get(index).get(counter[phO.size() + index]);
        } else {
          replacement = phO.get(index).get(counter[index]);
        }
        combination.put(param, replacement);
      }
      if (!combinations.contains(combination)) {
        combinations.add(combination);
      }
      increaseCounterArray(counter, 0);
    }
    return combinations;
  }

  private void increaseCounterArray(int[] counter, int i) {
    int l = i < phO.size() ? phO.get(i).size() : phR.get(i - phO.size()).size();
    if (counter[i] < l - 1) {
      counter[i]++;
    } else if (i < counter.length - 1) {
      counter[i] = 0;
      increaseCounterArray(counter, i + 1);
    }
  }

  private boolean isCounterFinished(int[] counter) {
    boolean finished = true;
    for (int i = 0; i < counter.length; i++) {
      int l = i < phO.size() ? phO.get(i).size() : phR.get(i - phO.size()).size();
      if (counter[i] < l - 1) {
        finished = false;
        break;
      }
    }
    return finished;
  }
}
