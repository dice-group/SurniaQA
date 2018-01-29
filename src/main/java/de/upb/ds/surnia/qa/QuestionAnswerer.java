package de.upb.ds.surnia.qa;

import de.upb.ds.surnia.preprocessing.ProcessingPipeline;
import de.upb.ds.surnia.preprocessing.Token;
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

public class QuestionAnswerer {

  static final Logger logger = LoggerFactory.getLogger(QuestionAnswerer.class);

  private ProcessingPipeline preprocessingPipeline;
  private QueryPatternMatcher queryPatternMatcher;

  public QuestionAnswerer() {
    preprocessingPipeline = new ProcessingPipeline();
    queryPatternMatcher = new QueryPatternMatcher("Queries.json");
  }

  /**
   * Try to find a answer for the question.
   * @param question Question object.
   * @return Answer to the question in QALD JSON.
   */
  public JSONObject answerQuestion(Question question) {
    String questionString = question.getLanguageToQuestion().get("en");
    // Analyze question with CoreNLP, FOX and OntologyIndex
    List<Token> tokens = null;
    try {
      tokens = preprocessingPipeline.processQuestion(questionString);
    } catch (IOException e) {
      logger.error("Error while processing question", e);
    }
    String resultingQuery = "";
    question.setAnswerType("");
    JSONArray answerArray = new JSONArray();
    JSONObject answerObject = new JSONObject();
    answerObject.put("head", new JSONObject());
    answerObject.put("results", new JSONObject());
    answerArray.add(answerObject);
    question.setAnswerAsQALDJSON(answerObject);
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
              question.setAnswerType("resource");
            } else if (node.isLiteral()) {
              switch (node.asNode().getLiteralDatatypeURI()) {
                case "http://www.w3.org/2001/XMLSchema#nonNegativeInteger":
                case "http://www.w3.org/2001/XMLSchema#decimal":
                case "http://www.w3.org/2001/XMLSchema#double":
                case "http://www.w3.org/2001/XMLSchema#float":
                case "http://www.w3.org/2001/XMLSchema#int":
                case "http://www.w3.org/2001/XMLSchema#long":
                case "http://www.w3.org/2001/XMLSchema#negativeInteger":
                case "http://www.w3.org/2001/XMLSchema#nonPositiveInteger":
                case "http://www.w3.org/2001/XMLSchema#positiveInteger":
                case "http://www.w3.org/2001/XMLSchema#short":
                case "http://www.w3.org/2001/XMLSchema#unsignedByte":
                case "http://www.w3.org/2001/XMLSchema#unsignedInt":
                case "http://www.w3.org/2001/XMLSchema#unsignedLong":
                case "http://www.w3.org/2001/XMLSchema#unsignedShort":
                  question.setAnswerType("number");
                  break;
                case "http://www.w3.org/2001/XMLSchema#date":
                  question.setAnswerType("date");
                  break;
                case "http://www.w3.org/2001/XMLSchema#string":
                  question.setAnswerType("string");
                  break;
                case "http://www.w3.org/2001/XMLSchema#boolean":
                  question.setAnswerType("boolean");
                  break;
                default:
                  String type = node.asNode().getLiteralDatatypeURI();
                  logger.info("Unknown datatype " + type + ". Setting type to list.");
                  question.setAnswerType("list");
              }
            }
            String type;
            if (node.isLiteral()) {
              type = "literal";
            } else {
              type = "uri";
            }
            JSONObject head = new JSONObject();
            JSONArray headVars = new JSONArray();
            headVars.add("x");
            head.put("vars", headVars);
            JSONObject answer = new JSONObject();
            answer.put("head", head);
            JSONObject resultsObject = new JSONObject();
            JSONArray bindings = new JSONArray();
            for (RDFNode n : results) {
              JSONObject bindingElement = new JSONObject();
              JSONObject binding = new JSONObject();
              binding.put("type", type);
              if (n.isLiteral()) {
                binding.put("value", n.asLiteral().getValue().toString());
              } else {
                binding.put("value", n.asNode().getURI());
              }
              bindingElement.put("x", binding);
              bindings.add(bindingElement);
            }
            resultsObject.put("bindings", bindings);
            answer.put("results", resultsObject);
            answerArray = new JSONArray();
            answer = new JSONObject();
            answer.put("answer", answerArray);
            question.setAnswerAsQALDJSON(answer);
            resultingQuery = query.toString();
            break;
          } else {
            logger.info("Query returned no result");
          }
        } else if (queryStringRepresentation.contains("ASK")) {
          question.setAnswerType("boolean");
          resultingQuery = query.toString();
          JSONObject answer = new JSONObject ();
          answer.put("head", new JSONObject());
          answer.put("results", new JSONObject());
          answer.put("boolean", askQueryDBpedia(query));
          answerArray = new JSONArray();
          answerArray.add(answer);
          answer = new JSONObject();
          answer.put("answer", answerArray);
          question.setAnswerAsQALDJSON(answer);
          break;
        }
      }
    } else {
      logger.info("No query with a rating above the threshold found.");
    }
    question.setSparqlQuery("en", resultingQuery);
    return question.getAnswerAsQALDJSON();
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
