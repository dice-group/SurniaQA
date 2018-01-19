package de.upb.ds.Surnia;

import de.upb.ds.Surnia.preprocessing.ProcessingPipeline;
import de.upb.ds.Surnia.preprocessing.Token;
import de.upb.ds.Surnia.queries.QueryPatternMatcher;
import org.apache.jena.query.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class SurniaQA {

    final static Logger logger = LoggerFactory.getLogger(SurniaQA.class);

    static final String question = "Who is married to John F. Kennedy?";

    public static void main (String[] args) {
        ProcessingPipeline preprocessingPipeline = new ProcessingPipeline();
        QueryPatternMatcher queryPatternMatcher = new QueryPatternMatcher("Queries.json");
        try {
            // Analyze question with CoreNLP, FOX and DBOIndex
            List<Token> tokens = preprocessingPipeline.processQuestion(question);
            logger.info("Question preprocessing: ");
            for (Token token : tokens) {
                logger.info(token.toString());
            }
            // Get a list with all queries that were rated above a given threshold for the question and query DBpedia
            logger.info("*********************************************************************");
            List<ParameterizedSparqlString> queries = queryPatternMatcher.findMatchingQueries(tokens);
            if (queries.size() > 0) {
                logger.info("DBpedia results: ");
                for (ParameterizedSparqlString query : queries) {
                    if (queryDBpedia(query)) {
                        break;
                    }
                }
            } else {
                logger.info("No query with a rating above the threshold found.");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            preprocessingPipeline.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Run a SPARQL query against the DBpedia endpoint and print the results.
     * @param queryString SPARQL query with set parameters.
     */
    static boolean queryDBpedia (ParameterizedSparqlString queryString) {
        logger.info("Query DBpedia with: " + queryString.toString());
        QueryExecution execution = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", queryString.asQuery());
        ResultSet resultSet = execution.execSelect();
        boolean successful = resultSet.hasNext();
        while (resultSet.hasNext()) {
            logger.info(resultSet.nextSolution().toString());
        }
        return successful;
    }

}
