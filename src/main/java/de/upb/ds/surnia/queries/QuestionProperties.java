package de.upb.ds.surnia.queries;

import de.upb.ds.surnia.preprocessing.Token;

import java.util.LinkedList;
import java.util.List;

public class QuestionProperties {

  public boolean containsSuperlative = false;
  public String questionStart;
  public int resourceAmount = 0;
  public int ontologyAmount = 0;
  public List<List<String>> resources;
  public List<List<String>> ontologies;
  public String representationForm;

  /**
   * Extract properties from a question.
   * @param questionTokens All tokens of the given question.
   */
  public QuestionProperties(List<Token> questionTokens) {
    // Set all properties for the question according to the question tokens
    if (questionTokens.size() > 0) {
      questionStart = questionTokens.get(0).getText();
      resources = new LinkedList<>();
      ontologies = new LinkedList<>();
      LinkedList<String> representationFormElements = new LinkedList<>();
      for (Token token : questionTokens) {
        if (token.getType().equals("JJS") || token.getType().equals("RBS")) {
          containsSuperlative = true;
        }
        if (token.getUris() != null) {
          if (token.getUris().get(0).contains("http://dbpedia.org/resource/")) {
            resources.add(token.getUris());
            resourceAmount++;
            representationFormElements.add("R" + resourceAmount);
          } else if (token.getUris().get(0).contains("http://dbpedia.org/ontology/")) {
            ontologies.add(token.getUris());
            ontologyAmount++;
            representationFormElements.add("O" + ontologyAmount);
          }
        } else {
          representationFormElements.add(token.getType());
        }
      }
      representationForm = String.join(" ", representationFormElements);
    }
  }

  @Override
  public String toString() {
    return "QuestionProperties{"
            + "containsSuperlative=" + containsSuperlative
            + ", questionStart='" + questionStart + '\''
            + ", resourceAmount=" + resourceAmount
            + ", ontologyAmount=" + ontologyAmount
            + ", resources=" + resources
            + ", ontologies=" + ontologies
            + ", representationForm='" + representationForm + '\''
            + '}';
  }
}
