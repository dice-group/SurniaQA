package de.upb.ds.Surnia;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.upb.ds.Surnia.preprocessing.ProcessingPipeline;
import de.upb.ds.Surnia.preprocessing.Token;
import de.upb.ds.Surnia.queries.QueryPatternMatcher;
import org.apache.jena.query.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class SurniaQA {

    final static Logger logger = LoggerFactory.getLogger(SurniaQA.class);

    public static void main (String[] args) {
        List<QALDQuestion> questions = readQALDQuestions("qald-7-train-multilingual.json");
        ProcessingPipeline preprocessingPipeline = new ProcessingPipeline();
        QueryPatternMatcher queryPatternMatcher = new QueryPatternMatcher("Queries.json");
        try {
            for (QALDQuestion question : questions) {
                logger.info("Question " + question.id + ": " + question.questionString);
                // Analyze question with CoreNLP, FOX and DBOIndex
                List<Token> tokens = preprocessingPipeline.processQuestion(question.questionString);
                // Get a list with all queries that were rated above a given threshold for the question and query DBpedia
                List<ParameterizedSparqlString> queries = queryPatternMatcher.findMatchingQueries(tokens);
                if (queries.size() > 0) {
                    logger.info("DBpedia results: ");
                    for (ParameterizedSparqlString query : queries) {
                        if (queryDBpedia(query)) {
                            break;
                        } else {
                            logger.info("Query returned no result");
                        }
                    }
                } else {
                    logger.info("No query with a rating above the threshold found.");
                }
                logger.info("*********************************************************************");
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

    static List<QALDQuestion> readQALDQuestions (String fileName) {
        List<QALDQuestion> questions = new LinkedList<>();
        try {
            // Read all prepared queries from the JSON file.
            BufferedReader reader = new BufferedReader(new FileReader(SurniaQA.class.getClassLoader().getResource(fileName).getFile()));
            String line;
            String jsonString = "";
            while((line = reader.readLine()) != null) {
                jsonString += line;
            }
            if (jsonString.length() > 0) {
                ObjectNode node = new ObjectMapper().readValue(jsonString, ObjectNode.class);
                if (node.has("questions")) {
                    ObjectMapper mapper = new ObjectMapper();
                    questions = mapper.readValue(node.get("questions").toString(), new TypeReference<ArrayList<QALDQuestion>>() {});
                }
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return questions;
    }

    /**
     * Run a SPARQL query against the DBpedia endpoint and print the results.
     * @param queryString SPARQL query with set parameters.
     */
    static boolean queryDBpedia (ParameterizedSparqlString queryString) {
        String queryStringRepresentation = queryString.toString();
        logger.info("Query DBpedia with: " + queryStringRepresentation);
        QueryExecution execution = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", queryString.asQuery());
        if (queryStringRepresentation.contains("SELECT")) {
            ResultSet resultSet = execution.execSelect();
            boolean successful = false;
            while (resultSet.hasNext()) {
                QuerySolution solution = resultSet.nextSolution();
                if (solution.toString().trim() != "") {
                    successful = true;
                    logger.info("Result: " + solution.toString());
                }
            }
            return successful;
        } else if (queryStringRepresentation.contains("ASK")) {
            boolean result = execution.execAsk();
            logger.info("Result: " + result);
            return true;
        } else {
            return false;
        }
    }

}
