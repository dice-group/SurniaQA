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
    // Create your answer container
    AnswerContainer answers = new AnswerContainer();
    answers.setType(AnswerType.RESOURCE);

    // Create your answers
    Set<String> answerSet = new HashSet<String>();

    // Analyze question with CoreNLP, FOX and OntologyIndex
    List<Token> tokens = null;
    try {
      tokens = preprocessingPipeline.processQuestion(question);
    } catch (IOException e) {
      logger.error("Error while processing question", e);
    }

    // Get a list with all queries rated above the threshold for the question and query DBpedia
    List<ParameterizedSparqlString> queries = queryPatternMatcher.findMatchingQueries(tokens);
    if (queries.size() > 0) {
      logger.info("DBpedia results: ");
      for (ParameterizedSparqlString query : queries) {
        String queryStringRepresentation = query.toString();
        if (queryStringRepresentation.contains("SELECT")) {
          Set<RDFNode> results = selectQueryDBpedia(query);
          if (results != null) {
            RDFNode node = results.toArray(new RDFNode[results.size()])[0];
            if (node.isResource()) {
              answers.setType(AnswerType.RESOURCE);
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
                  answers.setType(AnswerType.NUMBER);
                  break;
                case "http://www.w3.org/2001/XMLSchema#date":
                  answers.setType(AnswerType.DATE);
                  break;
                case "http://www.w3.org/2001/XMLSchema#string":
                  answers.setType(AnswerType.STRING);
                  break;
                case "http://www.w3.org/2001/XMLSchema#boolean":
                  answers.setType(AnswerType.BOOLEAN);
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
            answers.setSparqlQuery(query.toString());
            break;
          } else {
            logger.info("Query returned no result");
          }
        } else if (queryStringRepresentation.contains("ASK")) {
          answers.setSparqlQuery(query.toString());
          answers.setType(AnswerType.BOOLEAN);
          answerSet.add(String.valueOf(askQueryDBpedia(query)));
          break;
        }
      }
    } else {
      logger.info("No query with a rating above the threshold found.");
    }

    answers.setAnswers(answerSet);
    return answers;
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
      // if there is some projection variable
      projectionVar = queryString.asQuery().getProjectVars().get(0).getName();
    } else {
      // if there is a aggregation, e.g. 'SELECT COUNT(?uri)...'
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
