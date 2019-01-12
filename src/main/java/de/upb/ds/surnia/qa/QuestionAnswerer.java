package de.upb.ds.surnia.qa;

import de.upb.ds.surnia.preprocessing.ProcessingPipeline;
import de.upb.ds.surnia.preprocessing.model.Token;
import de.upb.ds.surnia.qa.AnswerContainer.AnswerType;
import de.upb.ds.surnia.queries.QueryPatternMatcher;
import org.apache.jena.query.*;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.sparql.core.Var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class QuestionAnswerer extends AbstractQuestionAnswerer {

  private static final Logger logger = LoggerFactory.getLogger(QuestionAnswerer.class);

  private ProcessingPipeline preprocessingPipeline;
  private QueryPatternMatcher queryPatternMatcher;

  private final Environment env;

  @Autowired
  public QuestionAnswerer(Environment env) {
    preprocessingPipeline = new ProcessingPipeline();
    queryPatternMatcher = new QueryPatternMatcher("Queries.json");
    this.env = env;
  }

  @Override
  public AnswerContainer retrieveAnswers(String question, String lang) {
    // Analyze question with all the Tasks in the PreprocessingPipeline
    List<Token> tokens = preprocessingPipeline.processQuestion(question);

    // Get a list with all queries rated above the threshold for the question
    Map<Float,List<ParameterizedSparqlString>> queries = queryPatternMatcher.findMatchingQueries(tokens);
    AnswerContainer answer = null;
    if (queries.size() > 0) {
      for (Float bestQueryInxex : queries.keySet()) {
        logger.info("QueryTemplate: " + queries.get(bestQueryInxex).toString());
        answer = getAnswerForQuery(queries.get(bestQueryInxex).get(0));
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
      Set<RDFNode> results = querySPARQLService(query);
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
      answerSet.add(String.valueOf(queryServer(query)));
      result.setAnswers(answerSet);
      return result;
    }
    return null;
  }

  /**
   * Run a SPARQL select query against the FUSEKI endpoint.
   *
   * @param queryString SPARQL query with set parameters.
   */
  private Set<RDFNode> querySPARQLService(ParameterizedSparqlString queryString) {
    String queryStringRepresentation = queryString.toString();
    logger.info("Querying SPARQL endpoint with: " + queryStringRepresentation);
    QueryExecution execution = QueryExecutionFactory
      .sparqlService(env.getProperty("sparql.endpoint"), queryString.asQuery());
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

  private boolean queryServer(ParameterizedSparqlString queryString) {
    String queryStringRepresentation = queryString.toString();
    logger.info("Query SPARQL endpoint with: " + queryStringRepresentation);
    QueryExecution execution = QueryExecutionFactory
      .sparqlService(env.getProperty("sparql.endpoint"), queryString.asQuery());
    boolean result = execution.execAsk();
    logger.info("Result: " + result);
    return result;
  }

}
