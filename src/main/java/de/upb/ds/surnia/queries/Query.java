package de.upb.ds.surnia.queries;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Query {

  @JsonProperty("sparqlTemplate")
  public String sparqlTemplate;

  @JsonProperty("sparqlParams")
  public String[] sparqlParams;

  @JsonProperty("questionStartWord")
  public String[] questionStartWord;

  @JsonProperty("resourceAmount")
  public int resourceAmount;

  @JsonProperty("ontologyAmount")
  public int ontologyAmount;

  @JsonProperty("containsSuperlative")
  public boolean cotainsSuperlative;

  @JsonProperty("exampleQuestions")
  public String[] exampleQuestions;
}
