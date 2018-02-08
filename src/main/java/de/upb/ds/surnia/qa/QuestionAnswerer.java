package de.upb.ds.surnia.qa;

import de.upb.ds.surnia.preprocessing.ProcessingPipeline;
import de.upb.ds.surnia.preprocessing.Token;
import de.upb.ds.surnia.qa.AnswerContainer.AnswerType;
import de.upb.ds.surnia.queries.QueryPatternMatcher;
import java.io.IOException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.aksw.qa.commons.datastructure.Question;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.core.Var;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuestionAnswerer extends AbstractQuestionAnswerer {

  static final Logger logger = LoggerFactory.getLogger(QuestionAnswerer.class);

  private ProcessingPipeline preprocessingPipeline;
  private QueryPatternMatcher queryPatternMatcher;

  public QuestionAnswerer() {
    preprocessingPipeline = new ProcessingPipeline();
    queryPatternMatcher = new QueryPatternMatcher("Queries.json");
  }

  @Override
  public AnswerContainer retrieveAnswers(String question, String lang) {
    // Analyze question with CoreNLP, FOX and OntologyIndex
    List<Token> tokens = null;
    try {
      tokens = preprocessingPipeline.processQuestion(question);
    } catch (IOException e) {
      logger.error("Error while processing question", e);
    }

    // Get a list with all queries rated above the threshold for the question and query DBpedia
    List<ParameterizedSparqlString> queries = queryPatternMatcher.findMatchingQueries(tokens);
    AnswerContainer answer = null;
    if (queries.size() > 0) {
      for (ParameterizedSparqlString query : queries) {
        logger.info("Query: " + query.toString());
      }
      for (ParameterizedSparqlString query : queries) {
        answer = getAnswerForQuery(query);
        if (answer != null) {
          break;
        }
      }
    } else {
      logger.info("No query with a rating above the threshold found.");
    }

    if (answer == null) {
      answer = new AnswerContainer();
      answer.setType(AnswerType.BOOLEAN);
      Set<String> answerSet = new HashSet<String>();
      answerSet.add("false");
      answer.setAnswers(answerSet);
    }

    return answer;
  }

  private AnswerContainer getAnswerForQuery(ParameterizedSparqlString query) {
    String queryStringRepresentation = query.toString();
    if (queryStringRepresentation.contains("SELECT")) {
      Set<RDFNode> results = selectQueryDBpedia(query);
      if (results != null) {
        AnswerContainer result = new AnswerContainer();
        Set<String> answerSet = new HashSet<String>();
        RDFNode node = results.toArray(new RDFNode[results.size()])[0];
        if (node.isResource()) {
          result.setType(AnswerType.RESOURCE);
          logger.info("Resource Result.");
        } else if (node.isLiteral()) {
          String type = node.asNode().getLiteralDatatypeURI();
          logger.info(type + " Result. ");
          switch (type) {
            case "http://www.w3.org/2001/XMLSchema#nonNegativeInteger":
            case "http://www.w3.org/2001/XMLSchema#decimal":
            case "http://www.w3.org/2001/XMLSchema#double":
            case "http://www.w3.org/2001/XMLSchema#float":
            case "http://www.w3.org/2001/XMLSchema#int":
            case "http://www.w3.org/2001/XMLSchema#long":
            case "http://www.w3.org/2001/XMLSchema#negativeInteger":
            case "http://www.w3.org/2001/XMLSchema#nonPositiveInteger":
            case "http://www.w3.org/2001/XMLSchema#positiveInteger":
            case "http://www.w3.org/2001/XMLSchema#integer":
            case "http://www.w3.org/2001/XMLSchema#short":
            case "http://www.w3.org/2001/XMLSchema#unsignedByte":
            case "http://www.w3.org/2001/XMLSchema#unsignedInt":
            case "http://www.w3.org/2001/XMLSchema#unsignedLong":
            case "http://www.w3.org/2001/XMLSchema#unsignedShort":
            case "http://www.w3.org/2001/XMLSchema#gYear":
              result.setType(AnswerType.NUMBER);
              break;
            case "http://www.w3.org/2001/XMLSchema#date":
              result.setType(AnswerType.DATE);
              break;
            case "http://www.w3.org/2001/XMLSchema#string":
              result.setType(AnswerType.STRING);
              break;
            case "http://www.w3.org/2001/XMLSchema#boolean":
              result.setType(AnswerType.BOOLEAN);
              break;
            default:
              logger.info("Unknown datatype " + type);
          }
        }
        for (RDFNode n : results) {
          if (n.isLiteral()) {
            answerSet.add(n.asLiteral().getValue().toString());
          } else {
            answerSet.add(n.asNode().getURI());
          }
        }
        result.setSparqlQuery(query.toString());
        result.setAnswers(answerSet);
        return result;
      } else {
        logger.info("Query returned no result");
      }
    } else if (queryStringRepresentation.contains("ASK")) {
      AnswerContainer result = new AnswerContainer();
      result.setSparqlQuery(query.toString());
      result.setType(AnswerType.BOOLEAN);
      Set<String> answerSet = new HashSet<String>();
      answerSet.add(String.valueOf(askQueryDBpedia(query)));
      result.setAnswers(answerSet);
      return result;
    }
    return null;
  }

  /**
   * Run a SPARQL select query against the DBpedia endpoint.
   *
   * @param queryString SPARQL query with set parameters.
   */
  private Set<RDFNode> selectQueryDBpedia(ParameterizedSparqlString queryString) {
    String queryStringRepresentation = queryString.toString();
    logger.info("Query DBpedia with: " + queryStringRepresentation);
    QueryExecution execution = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", queryString.asQuery());
    ResultSet resultSet = execution.execSelect();
    List<Var> projectVars = queryString.asQuery().getProjectVars();
    String projectionVar;
    if (!projectVars.isEmpty() && queryString.asQuery().getAggregators().isEmpty()) {
      projectionVar = queryString.asQuery().getProjectVars().get(0).getName();
    } else {
      projectionVar = resultSet.getResultVars().get(0);
    }
    Set<RDFNode> nodes = new HashSet<>();
    QuerySolution qs;
    while (resultSet.hasNext()) {
      qs = resultSet.next();
      RDFNode rdfNode = qs.get(projectionVar);
      nodes.add(rdfNode);
    }

    if (nodes.size() > 0) {
      return nodes;
    } else {
      return null;
    }
  }

  private boolean askQueryDBpedia(ParameterizedSparqlString queryString) {
    String queryStringRepresentation = queryString.toString();
    logger.info("Query DBpedia with: " + queryStringRepresentation);
    QueryExecution execution = QueryExecutionFactory.sparqlService("http://dbpedia.org/sparql", queryString.asQuery());
    boolean result = execution.execAsk();
    logger.info("Result: " + result);
    return result;
  }

}
