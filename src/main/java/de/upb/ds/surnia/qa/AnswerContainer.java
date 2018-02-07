package de.upb.ds.surnia.qa;

import java.util.Set;

/**
 * Simple Answer Container which contains the answers as a Set and tells
 * the answer type (resource, boolean, date, number) as well as the used SPARQL Query.
 *
 */
public class AnswerContainer {

  private Set<String> answers;
  private AnswerType type;
  private String sparqlQuery;

  /**
   * AnswerTypes declaration.
   */
  public enum AnswerType {
    /**
     * Answers are a bulk of uris.
     */
    RESOURCE,
    /**
     * Answer is a boolean.
     */
    BOOLEAN,
    /**
     * Answer is a date.
     */
    DATE,
    /**
     * Answer is a number.
     */
    NUMBER,
    /**
     * Answer is a string.
     */
    STRING
  }

  /**
   * Getter for answer set.
   * @return the answers
   */
  public Set<String> getAnswers() {
    return answers;
  }

  /**
   * Setter fpr answer set.
   * @param answers the answers to set
   */
  public void setAnswers(Set<String> answers) {
    this.answers = answers;
  }

  /**
   * Getter for answer type.
   * @return the type
   */
  public AnswerType getType() {
    return type;
  }

  /**
   * Setter for answer type.
   * @param type the type to set
   */
  public void setType(AnswerType type) {
    this.type = type;
  }

  /**
   * Getter for SPARQL query.
   * @return the sparqlQuery
   */
  public String getSparqlQuery() {
    return sparqlQuery;
  }

  /**
   * Setter for SPARQL query.
   * @param sparqlQuery the sparqlQuery to set
   */
  public void setSparqlQuery(String sparqlQuery) {
    this.sparqlQuery = sparqlQuery;
  }

}
