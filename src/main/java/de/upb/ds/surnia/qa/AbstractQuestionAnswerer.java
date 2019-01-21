package de.upb.ds.surnia.qa;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import de.upb.ds.surnia.gerbil.GerbilFinalResponse;
import de.upb.ds.surnia.qa.AnswerContainer.AnswerType;
import org.aksw.qa.commons.datastructure.Question;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.Set;

/**
 * An abstract layer for the qa system.
 */
public abstract class AbstractQuestionAnswerer {

  @SuppressWarnings("unchecked")
  private JSONObject getAnswersAsQald(Set<String> answers, AnswerType answerType)
    throws IOException, ParseException {

    String varName = answerType.toString().toLowerCase();
//    String varName = "resource";
    JSONObject answerJson = new JSONObject();
    JSONObject head = new JSONObject();
    JSONArray varArr = new JSONArray();
    if (answerType.equals(AnswerType.BOOLEAN)) {
      // boolean answers are handled different
      answerJson.put("head", head);
      answerJson.put("boolean", Boolean.valueOf(answers.iterator().next()));
      return answerJson;
    }
    varArr.add(varName);
    head.put("vars", varArr);

    JSONObject results = new JSONObject();
    JSONArray bindings = new JSONArray();
    for (String answer : answers) {
      JSONObject binding = new JSONObject();
      JSONObject var = new JSONObject();
      var.put("type", "literal");
      switch (answerType) {
        case RESOURCE:
          var.put("type", answerType.toString().toLowerCase());
          var.put("value", answer);
          break;
        default:
          // is literal
          processLiteral(answer, var);
          break;
      }

      binding.put(varName, var);
      bindings.add(binding);
    }
    results.put("bindings", bindings);
    answerJson.put("head", head);
    answerJson.put("results", results);

    return answerJson;
  }

  @SuppressWarnings("unchecked")
  private void processLiteral(String answer, JSONObject var) {
    // regular expression to check if answer got a lang tag
    boolean hasLangTag = answer.matches(".*@\\w+");
    // matches either a ^^<whatever> or ^^xml:tag datatype
    boolean hasDatatypeTag = answer.matches(".*\\^\\^(<[^<>]+>|xml:\\w+)");
    int offset = answer.length();
    if (hasLangTag) {
      int langStartIndex = answer.lastIndexOf("@");
      String lang = answer.substring(langStartIndex + 1);
      var.put("xml:lang", lang);
      offset = langStartIndex;
    } else if (hasDatatypeTag) {
      int datatypeStartIndex = answer.lastIndexOf("^^");
      String datatype = answer.substring(datatypeStartIndex + 2);
      if (datatype.startsWith("<")) {
        datatype = datatype.substring(1, datatype.length() - 1);
      }
      var.put("datatype", datatype);
      offset = datatypeStartIndex;
    }
    int beginIndex = 0;
    if (answer.startsWith("'''")) {
      beginIndex = 3;
      offset -= 3;
    }
    if (answer.startsWith("\"") || answer.startsWith("'")) {
      beginIndex = 1;
      offset -= 1;
    }
    var.put("value", answer.substring(beginIndex, offset));
  }

  /**
   * Create a JSON Object from the question answers.
   *
   * @param q Question object with the answers.
   * @param lang Language of the answered question.
   * @return JSON Object representing the answers.
   */
  public JSONObject getAnswersToQuestion(final Question q, String lang) {
    // gets the prefered question
    String question = q.getLanguageToQuestion().get(lang);

    // retrieve the answers from the system
    AnswerContainer answers = retrieveAnswers(question, lang);
    // sets the answers
    q.setGoldenAnswers(lang, answers.getAnswers());
    // sets the answertype as lower case (e.g. resource)
    q.setAnswerType(answers.getType().toString().toLowerCase());
    //
    q.setSparqlQuery(lang, answers.getSparqlQuery());
    try {
      q.setAnswerAsQALDJSON(getAnswersAsQald(answers.getAnswers(), answers.getType()));

      // return the answer as a valid qaldJSON string
      GerbilFinalResponse resp = new GerbilFinalResponse();

      resp.setQuestions(q, lang);
      ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
      String json;

      json = ow.writeValueAsString(resp);
      return (JSONObject) new JSONParser().parse(json);
    } catch (ParseException | IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }

  /**
   * Retrieves the Answers from the System for a particular question and its language. <br/> It has
   * to set the answers as a set, the answer types and the sparql query used
   *
   * @param question String form of the question.
   * @param lang Language of the question.
   * @return Retrieved answer for the question.
   */
  public abstract AnswerContainer retrieveAnswers(String question, String lang);
}
