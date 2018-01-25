package de.upb.ds.surnia;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.upb.ds.surnia.preprocessing.ProcessingPipeline;
import de.upb.ds.surnia.preprocessing.Token;
import de.upb.ds.surnia.queries.QueryPatternMatcher;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SurniaQuestionAnswering {

  static final Logger logger = LoggerFactory.getLogger(SurniaQuestionAnswering.class);

  /**
   * Starts SurniaQA and test it with QALD-7 questions.
   * @param args Commandline Parameters.
   */
  public static void main(String[] args) {
    List<Question> questions = readQuestions("qald-7-train-multilingual.json");
    ProcessingPipeline preprocessingPipeline = new ProcessingPipeline();
    QueryPatternMatcher queryPatternMatcher = new QueryPatternMatcher("Queries.json");
    try {
      for (Question question : questions) {
        logger.info("Question " + question.id + ": " + question.questionString);
        // Analyze question with CoreNLP, FOX and OntologyIndex
        List<Token> tokens = preprocessingPipeline.processQuestion(question.questionString);
        // Get a list with all queries rated above the threshold for the question and query DBpedia
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

  /**
   * Parses questions from a QALD-7 question set.
   * @param fileName Name of the file with the questions.
   * @return List with all parsed questions.
   */
  static List<Question> readQuestions(String fileName) {
    List<Question> questions = new LinkedList<>();
    try {
      // Read all prepared queries from the JSON file.
      String file = SurniaQuestionAnswering.class.getClassLoader().getResource(fileName).getFile();
      BufferedReader reader = new BufferedReader(new FileReader(file));
      String line;
      String jsonString = "";
      while ((line = reader.readLine()) != null) {
        jsonString += line;
      }
      if (jsonString.length() > 0) {
        ObjectNode node = new ObjectMapper().readValue(jsonString, ObjectNode.class);
        if (node.has("questions")) {
          ObjectMapper mapper = new ObjectMapper();
          String questionNodeContent = node.get("questions").toString();
          questions = mapper.readValue(
                  questionNodeContent,
                  new TypeReference<ArrayList<Question>>() {}
          );
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
   *
   * @param queryString SPARQL query with set parameters.
   */
  static boolean queryDBpedia(ParameterizedSparqlString queryString) {
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
