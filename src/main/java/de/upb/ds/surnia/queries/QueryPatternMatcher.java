package de.upb.ds.surnia.queries;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.upb.ds.surnia.preprocessing.model.Token;
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

  public static final float QUERY_RANKING_THRESHOlD = 0.5f;
  static final Logger logger = LoggerFactory.getLogger(QueryPatternMatcher.class);
  private List<QueryTemplate> queryTemplates;

  /**
   * Parses all queryTemplates from a given file.
   *
   * @param queryTemplatesFileName Name of the query file.
   */
  public QueryPatternMatcher(String queryTemplatesFileName) {
    queryTemplates = new LinkedList<>();
    try {
      // Read all prepared queryTemplates from the JSON file.
      String queryTemplatesFile = getClass().getClassLoader().getResource(queryTemplatesFileName)
        .getFile();
      BufferedReader queryTemplateFileReader = new BufferedReader(
        new FileReader(queryTemplatesFile));
      String line;
      StringBuilder jsonStringBuilder = new StringBuilder();
      while ((line = queryTemplateFileReader.readLine()) != null) {
        jsonStringBuilder.append(line);
      }
      if (jsonStringBuilder.length() > 0) {
        ObjectMapper mapper = new ObjectMapper();
        queryTemplates = mapper
          .readValue(jsonStringBuilder.toString(), new TypeReference<ArrayList<QueryTemplate>>() {
          });
      }
      queryTemplateFileReader.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Find all queryTemplates that were rated above the threshold for the given question.
   *
   * @param questionTokens Tokens of the question with the analysis of the pre-processing pipeline.
   * @return A list with all parameterized SPARQL queryTemplates with a good rating.
   */
  public List<ParameterizedSparqlString> findMatchingQueries(List<Token> questionTokens) {
    QuestionProperties questionProperties = new QuestionProperties(questionTokens);
    logger.debug("{}", questionProperties);
    LinkedList<ParameterizedSparqlString> possibleQueries = new LinkedList<>();
    for (QueryTemplate queryTemplate : queryTemplates) {
      String bestQuestionTemplate = rateQuery(questionProperties, queryTemplate);
      if (bestQuestionTemplate != null) {
        QueryParameterReplacer queryParameterReplacer = new QueryParameterReplacer(questionTokens,
          bestQuestionTemplate,
          queryTemplate);
        possibleQueries.addAll(queryParameterReplacer.getQueriesWithReplacedParameters());
      }
    }
    logger.debug("QueryTemplate amount: {}", possibleQueries.size());
    return possibleQueries;
  }

  /**
   * Rate a query according to the properties of the given question.
   *
   * @param questionProperties Analyzed properties of the input question.
   * @param queryTemplate A query template from the prepared query set.
   * @return A ranking for the query regarding the question between 0 and 1.
   */
  private String rateQuery(QuestionProperties questionProperties, QueryTemplate queryTemplate) {
    String questionStartWord = questionProperties.getQuestionStart();
    if (!Arrays.asList(queryTemplate.getQuestionStartWords()).contains(questionStartWord)) {
      logger.debug("Wrong question word");
      return null;
    }
    if (queryTemplate.containsSuperlative() && !questionProperties.containsSuperlative()) {
      logger.debug("Inconsistent superlative");
      return null;
    }
//    if (query.resourceAmount > questionProperties.resourceAmount) {
//      logger.info("Not enough properties");
//      return null;
//    }
//    if (query.ontologyAmount > questionProperties.ontologyAmount) {
//      logger.info("Not enough ontologies");
//      return null;
//    }

    double max = 0.0f;
    String bestFitQuestion = "";
    for (String questionTemplate : queryTemplate.getExampleQuestions()) {
      double similarity = stringSimilarity(questionTemplate,
        questionProperties.getRepresentationForm());
      if (similarity > max) {
        max = similarity;
        bestFitQuestion = questionTemplate;
      }
    }
    if (max >= QUERY_RANKING_THRESHOlD) {
      logger.info("{} - {}: {}", bestFitQuestion, questionProperties.getRepresentationForm(), max);
      return bestFitQuestion;
    } else {
      logger.warn("Similarity too low. Maximum is only {}", max);
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
