package de.upb.ds.surnia.queries;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.upb.ds.surnia.preprocessing.Token;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.apache.jena.query.ParameterizedSparqlString;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueryPatternMatcher {

  static final Logger logger = LoggerFactory.getLogger(QueryPatternMatcher.class);

  public static final float QUERY_RANKING_THRESHOlD = 0.5f;

  private List<Query> queries;

  /**
   * Parses all queries from a given file.
   * @param fileName Name of the query file.
   */
  public QueryPatternMatcher(String fileName) {
    queries = new LinkedList<>();
    try {
      // Read all prepared queries from the JSON file.
      String file = getClass().getClassLoader().getResource(fileName).getFile();
      BufferedReader reader = new BufferedReader(new FileReader(file));
      String line;
      String jsonString = "";
      while ((line = reader.readLine()) != null) {
        jsonString += line;
      }
      if (jsonString.length() > 0) {
        ObjectMapper mapper = new ObjectMapper();
        queries = mapper.readValue(jsonString, new TypeReference<ArrayList<Query>>() {
        });
      }
      reader.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Find all queries that were rated above the threshold for the given question.
   *
   * @param questionTokens Tokens of the question with the analysis of the preprocessing pipeline.
   * @return A list with all parameterized SPARQL queries with a good rating.
   */
  public List<ParameterizedSparqlString> findMatchingQueries(List<Token> questionTokens) {
    QuestionProperties questionProperties = new QuestionProperties(questionTokens);
    logger.info(questionProperties.toString());
    LinkedList<ParameterizedSparqlString> possibleQueries = new LinkedList<>();
    for (Query query : queries) {
      String bestExampleQuestion = rateQuery(questionProperties, query);
      if (bestExampleQuestion != null) {
        QueryParameterReplacer queryParameterReplacer;
        queryParameterReplacer = new QueryParameterReplacer(questionTokens, bestExampleQuestion, query);
        possibleQueries.addAll(queryParameterReplacer.getQueriesWithReplacedParameters());
      }
    }
    logger.info("Query amount: " + possibleQueries.size());
    return possibleQueries;
  }

  /**
   * Rate a query according to the properties of the given question.
   *
   * @param questionProperties Analyzed properties of the input question.
   * @param query              A query from the prepared query set.
   * @return A ranking for the query regarding the question between 0 and 1.
   */
  private String rateQuery(QuestionProperties questionProperties, Query query) {
    String questionStartWord = questionProperties.questionStart.toUpperCase();
    if (!Arrays.asList(query.questionStartWord).contains(questionStartWord)) {
      logger.info("Wrong question word");
      return null;
    }
    if (query.cotainsSuperlative && !questionProperties.containsSuperlative) {
      logger.info("Inconsistent superlative");
      return null;
    }
    if (query.resourceAmount > questionProperties.resourceAmount) {
      logger.info("Not enough properties");
      return null;
    }
    if (query.ontologyAmount > questionProperties.ontologyAmount) {
      logger.info("Not enough ontologies");
      return null;
    }
    double max = 0.0f;
    String question = "";
    for (String exampleQuestions : query.exampleQuestions) {
      String s1 = exampleQuestions;
      String s2 = questionProperties.representationForm;
      double similarity = stringSimilarity(s1, s2);
      if (similarity > max) {
        max = similarity;
        question = s1;
      }
    }
    if (max >= QUERY_RANKING_THRESHOlD) {
      logger.info(question + " - " + questionProperties.representationForm + ": " + max);
      return question;
    } else {
      logger.info("Similarity too low");
      return null;
    }
  }

  private double stringSimilarity(String s1, String s2) {
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

    return 1.0d - (v0[s2.length()] / len);
  }

}
