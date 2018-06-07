package de.upb.ds.surnia.gerbil;

import org.aksw.qa.commons.datastructure.Question;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class GerbilFinalResponse {
  private Logger log = LoggerFactory.getLogger(GerbilFinalResponse.class);
  private List<GerbilResponseBuilder> questions;

  public GerbilFinalResponse() {
    questions = new ArrayList<>();
  }

  /**
   * Add a question.
   * @param answer Answered question.
   * @param lang Language of the question.
   * @return Answered question as a GerbilFinalResponse.
   */
  public GerbilFinalResponse setQuestions(Question answer, String lang) {
    GerbilResponseBuilder responseToGerbil = new GerbilResponseBuilder();
    responseToGerbil.setId(answer.getId());
    responseToGerbil.setAnswertype(answer.getAnswerType());
    responseToGerbil.setQuery(answer.getSparqlQuery(lang));
    log.info("query: " + responseToGerbil.getQuery());
    responseToGerbil.setQuestion(answer, lang);
    responseToGerbil.setAnswerVec(answer);
    this.questions.add(responseToGerbil);
    log.info("GerbilQA object: " + this.toString());
    return this;
  }

  /**
   * Getter for all Questions.
   * @return List of questions.
   */
  public List<GerbilResponseBuilder> getQuestions() {
    return questions;
  }

  @Override
  public String toString() {
    return "\n    questions: " + Objects.toString(questions);
  }
}