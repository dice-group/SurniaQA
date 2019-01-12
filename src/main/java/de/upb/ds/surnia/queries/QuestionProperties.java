package de.upb.ds.surnia.queries;

import de.upb.ds.surnia.preprocessing.model.Token;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class QuestionProperties {

  private boolean containsSuperlative;
  private String questionStart;
  private int resourceAmount;
  private int ontologyAmount;
  private int propertyAmount;
  public List<Token> tokens;
  private String representationForm;

  /**
   * Extract properties from a question.
   *
   * @param questionTokens All tokens of the given question.
   */
  // TODO: 18/11/2018 <S> Implementation will be changed and decoupbled from DBPedia
  public QuestionProperties(List<Token> questionTokens) {
    // Set all properties for the question according to the question tokens
    if (questionTokens.size() > 0) {
      questionStart = questionTokens.get(0).getText().toUpperCase();

      tokens = new ArrayList<>(questionTokens);

      LinkedList<String> representationFormElements = new LinkedList<>();
      resourceAmount = 0;
      ontologyAmount = 0;
      propertyAmount = 0;
      for (Token token : questionTokens) {
        if (!containsSuperlative) containsSuperlative = token.getType().equals("JJS") || token.getType().equals("RBS");
        if (token.getUris() != null && token.getUris().size() > 0) {
          String nextToken = token.getUris().iterator().next();
          if (nextToken.contains("http://dbpedia.org/resource/") || nextToken.contains("http://aksw.org/notInWiki/")) {
            resourceAmount++;
            representationFormElements.add("R");
          } else if (nextToken.contains("http://dbpedia.org/ontology/")) {
            ontologyAmount++;
            representationFormElements.add("O");
          }
          else if (nextToken.contains("http://dbpedia.org/property/")) {
            propertyAmount++;
            representationFormElements.add("P");
          }
        } else {
          representationFormElements.add(token.getType());
        }
      }
      representationForm = String.join(" ", representationFormElements);
    }
  }

  public boolean containsSuperlative() {
    return containsSuperlative;
  }

  public String getQuestionStart() {
    return questionStart;
  }

  public int getResourceAmount() {
    return resourceAmount;
  }

  public int getOntologyAmount() {
    return ontologyAmount;
  }

  public List<Token> getTokens() {
    return tokens;
  }

  public String getRepresentationForm() {
    return representationForm;
  }

  @Override
  public String toString() {
    return "QuestionProperties{\n"
      + "\tcontainsSuperlative=" + containsSuperlative + '\n'
      + "\tquestionStart='" + questionStart + '\'' + '\n'
      + "\tresourceAmount=" + resourceAmount + '\n'
      + "\tontologyAmount=" + ontologyAmount + '\n'
      + "\tpropertyAmount=" + propertyAmount + '\n'
      + "\ttokens=" + tokens + '\n'
      + "\trepresentationForm='" + representationForm + '\'' + '\n'
      + '}';
  }
}
