package de.upb.ds.surnia.queries;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QueryTemplate {

  @JsonProperty("sparqlTemplate")
  private String sparqlTemplate;

  @JsonProperty("sparqlParams")
  private String[] sparqlParams;

  @JsonProperty("questionStartWord")
  private String[] questionStartWord;

  @JsonProperty("resourceAmount")
  private int resourceAmount;

  @JsonProperty("ontologyAmount")
  private int ontologyAmount;

  @JsonProperty("containsSuperlative")
  private boolean containsSuperlative;

  @JsonProperty("exampleQuestions")
  private String[] exampleQuestions;

  public String getSparqlTemplate() {
    return sparqlTemplate;
  }

  public void setSparqlTemplate(String sparqlTemplate) {
    this.sparqlTemplate = sparqlTemplate;
  }

  public String[] getSparqlParams() {
    return sparqlParams;
  }

  public void setSparqlParams(String[] sparqlParams) {
    this.sparqlParams = sparqlParams;
  }

  public String[] getQuestionStartWord() {
    return questionStartWord;
  }

  public void setQuestionStartWord(String[] questionStartWord) {
    this.questionStartWord = questionStartWord;
  }

  public int getResourceAmount() {
    return resourceAmount;
  }

  public void setResourceAmount(int resourceAmount) {
    this.resourceAmount = resourceAmount;
  }

  public int getOntologyAmount() {
    return ontologyAmount;
  }

  public void setOntologyAmount(int ontologyAmount) {
    this.ontologyAmount = ontologyAmount;
  }

  public boolean containsSuperlative() {
    return containsSuperlative;
  }

  public void setContainsSuperlative(boolean containsSuperlative) {
    this.containsSuperlative = containsSuperlative;
  }

  public String[] getExampleQuestions() {
    return exampleQuestions;
  }

  public void setExampleQuestions(String[] exampleQuestions) {
    this.exampleQuestions = exampleQuestions;
  }
}
