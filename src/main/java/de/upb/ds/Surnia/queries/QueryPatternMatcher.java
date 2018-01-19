package de.upb.ds.Surnia.queries;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.upb.ds.Surnia.preprocessing.Token;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.ResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class QueryPatternMatcher {

    final static Logger logger = LoggerFactory.getLogger(QueryPatternMatcher.class);

    public static final float QUERY_RANKING_THRESHOld = 0.5f;

    private List<Query> queries;

    public QueryPatternMatcher (String fileName) {
        queries = new LinkedList<>();
        try {
            // Read all prepared queries from the JSON file.
            BufferedReader reader = new BufferedReader(new FileReader(getClass().getClassLoader().getResource(fileName).getFile()));
            String line;
            String jsonString = "";
            while((line = reader.readLine()) != null) {
                jsonString += line;
            }
            if (jsonString.length() > 0) {
                ObjectMapper mapper = new ObjectMapper();
                queries = mapper.readValue(jsonString, new TypeReference<ArrayList<Query>>() {});
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Rate a query according to the properties of the given question.
     * @param questionProperties Analyzed properties of the input question.
     * @param query A query from the prepared query set.
     * @return A ranking for the query regarding the question between 0 and 1.
     */
    public double rateQuery (QuestionProperties questionProperties, Query query) {
        if (!Arrays.asList(query.questionStartWord).contains(questionProperties.questionStart.toUpperCase())) {
            logger.info("Wrong question word");
            return 0.0f;
        }
        if (query.cotainsSuperlative && !questionProperties.containsSuperlative) {
            logger.info("Inconsistent superlative");
            return 0.0f;
        }
        if (query.resourceAmount > questionProperties.resourceAmount) {
            logger.info("Not enough properties");
            return 0.0f;
        }
        if (query.ontologyAmount > questionProperties.ontologyAmount) {
            logger.info("Not enough ontologies");
            return 0.0f;
        }
        double max = 0.0f;
        String question = "";
        for (String exampleQuestions : query.exampleQuestions) {
            String s1 = exampleQuestions;
            String s2 = questionProperties.representationForm;
            double len = Math.max(s1.length(), s2.length());
            int[] v0 = new int[s2.length() + 1];
            int[] v1 = new int[s2.length() + 1];
            int[] vtemp;
            for (int i = 0; i < v0.length; i++) {
                v0[i] = i;
            }

            for (int i = 0; i < s1.length(); i++) {
                v1[0] = i + 1;
                for (int j = 0; j < s2.length(); j++) {
                    int cost = 1;
                    if (s1.charAt(i) == s2.charAt(j)) {
                        cost = 0;
                    }
                    v1[j + 1] = Math.min(
                            v1[j] + 1,
                            Math.min(v0[j + 1] + 1, v0[j] + cost));
                }
                vtemp = v0;
                v0 = v1;
                v1 = vtemp;

            }

            double similarity = 1.0d - (v0[s2.length()] / len);
            if (similarity > max) {
                max = similarity;
                question = s1;
            }
        }
        logger.info(question + " - " + questionProperties.representationForm + ": " + String.valueOf(max));
        return max;
    }

    /**
     * Find all queries that were rated above the threshold for the given question.
     * @param questionTokens The tokens of the question with the analysis result of the preprocessing pipeline.
     * @return A list with all parameterized SPARQL queries with a good rating.
     */
    public List<ParameterizedSparqlString> findMatchingQueries (List<Token> questionTokens) {
        QuestionProperties questionProperties = new QuestionProperties(questionTokens);
        logger.info(questionProperties.toString());
        LinkedList<ParameterizedSparqlString> possibleQueries = new LinkedList<>();
        for (Query query: queries) {
            if (rateQuery(questionProperties, query) >= QUERY_RANKING_THRESHOld) {
                ParameterizedSparqlString sparqlString = new ParameterizedSparqlString(query.sparqlTemplate);
                // Replace parameters of the query
                for (String param : query.sparqlParams) {
                    if (param.charAt(0) == 'R') {
                        logger.info("Replace: " + param + " with " + questionProperties.resources[Integer.valueOf(param.substring(1)) - 1]);
                        sparqlString.setParam(param, ResourceFactory.createResource(questionProperties.resources[Integer.valueOf(param.substring(1)) - 1]));
                    } else if (param.charAt(0) == 'O') {
                        logger.info("Replace: " + param + " with " + questionProperties.ontologies[Integer.valueOf(param.substring(1)) - 1]);
                        sparqlString.setParam(param, ResourceFactory.createResource(questionProperties.ontologies[Integer.valueOf(param.substring(1)) - 1]));
                    }
                }
                possibleQueries.add(sparqlString);
            }
        }
        return possibleQueries;
    }

}
