package de.upb.ds.surnia.gerbil;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Vector;
import org.aksw.qa.commons.datastructure.Question;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GerbilResponseBuilder {

  private Logger log = LoggerFactory.getLogger(GerbilResponseBuilder.class);
  private String id;
  private String answertype;
  private Vector<Map<String, String>> question;
  private Map<String, String> query;
  private Vector<JSONObject> answers;

  /**
   * Default constructor for an empty response builder.
   */
  public GerbilResponseBuilder() {
    this.id = new String();
    this.answertype = new String();
    this.question = new Vector<>();
    this.query = new HashMap<>();
    this.answers = new Vector<>();
  }

  public GerbilResponseBuilder setId(final String ques) {
    this.id = "1";
    return this;
  }

  /**
   * Setter to construct add a question with a given language.
   * @param q Question object.
   * @param lang Language of the the question.
   * @return Updated response builder.
   */
  public GerbilResponseBuilder setQuestion(final Question q, String lang) {
    Map<String, String> gerbilQuestion = new HashMap<>();
    gerbilQuestion.put("language", q.getLanguageToQuestion().keySet().toArray()[0].toString());
    gerbilQuestion.put("string", q.getLanguageToQuestion().get(lang));
    this.question.add(gerbilQuestion);
    return this;
  }

  public GerbilResponseBuilder setQuery(final String query) {
    this.query.put("sparql", query);
    return this;
  }

  /**
   * Setter for the answer vector.
   * @param q Question object.
   */
  public void setAnswerVec(final Question q) {
    if (q.getAnswerAsQALDJSON() != null && !q.getAnswerAsQALDJSON().isEmpty()) {
      JSONObject json = q.getAnswerAsQALDJSON();
      this.answers.add(json);
    }
  }

  public void setAnswertype(final String answertype) {
    this.answertype = answertype;
  }

  public String getId() {
    return this.id;
  }

  public String getAnswertype() {
    return this.answertype;
  }

  public Map<String, String> getQuery() {
    return this.query;
  }

  public Vector<Map<String, String>> getQuestion() {
    return this.question;
  }

  public Vector<JSONObject> getAnswers() {
    return this.answers;
  }

  @Override
  public String toString() {
    return "\n ID :" + id + "\n Answertype: " + answertype + "\n Query: " + Objects.toString(query)
        + "\n Question: " + Objects.toString(question) + "\n Answers: " + Objects.toString(answers);
  }

}
