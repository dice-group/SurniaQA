package de.upb.ds.Surnia.queries;

import java.util.*;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.ResourceFactory;

public class QueryParameterReplacer {

    private List<List<String>> placeholderR, placeholderO;
    private String[] params;
    private String queryString;

    public QueryParameterReplacer(QuestionProperties questionProperties, Query query) {
        queryString = query.sparqlTemplate;
        params = query.sparqlParams;
        placeholderR = questionProperties.resources;
        placeholderO = questionProperties.ontologies;
    }

    public List<ParameterizedSparqlString> getQueriesWithReplacedParameters() {
        LinkedList<ParameterizedSparqlString> queries = new LinkedList<>();
        List<HashMap<String, String>> parameterReplacementCombinations = generateParameterReplacementCombinations();
        for (HashMap<String, String> parameterReplacement : parameterReplacementCombinations) {
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
        int[] counter = new int[placeholderO.size() + placeholderR.size()];
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
                    replacement = placeholderR.get(index).get(counter[placeholderO.size() + index]);
                } else {
                    replacement = placeholderO.get(index).get(counter[index]);
                }
                combination.put(param, replacement);
            }
            if (!combinations.contains(combination)){
                combinations.add(combination);
            }
            increaseCounterArray(counter, 0);
        }
        return combinations;
    }

    private void increaseCounterArray (int[] counter, int i) {
        if (counter[i] < (i < placeholderO.size() ? placeholderO.get(i).size() : placeholderR.get(i - placeholderO.size()).size()) - 1) {
            counter[i]++;
        } else if (i < counter.length - 1){
            counter[i] = 0;
            increaseCounterArray(counter, i + 1);
        }
    }

    private boolean isCounterFinished (int[] counter) {
        boolean finished = true;
        for (int i = 0; i < counter.length; i++) {
            if (counter[i] < ((i < placeholderO.size() ? placeholderO.get(i).size() : placeholderR.get(i - placeholderO.size()).size()) - 1)) {
                finished = false;
                break;
            }
        }
        return finished;
    }
}
