package de.upb.ds.surnia.queries;

import de.upb.ds.surnia.preprocessing.model.Token;
import java.util.LinkedList;
import java.util.List;

public class QuestionProperties {

  public boolean containsSuperlative = false;
  public String questionStart;
  public int resourceAmount = 0;
  public int ontologyAmount = 0;
  public List<Token> tokens;
  public String representationForm;

  /**
   * Extract properties from a question.
   *
   * @param questionTokens All tokens of the given question.
   */
  public QuestionProperties(List<Token> questionTokens) {
    // Set all properties for the question according to the question tokens
    if (questionTokens.size() > 0) {
      questionStart = questionTokens.get(0).getText();
      tokens = questionTokens;
      LinkedList<String> representationFormElements = new LinkedList<>();
      for (Token token : questionTokens) {
        if (token.getType().equals("JJS") || token.getType().equals("RBS")) {
          containsSuperlative = true;
        }
        if (token.getUris() != null) {
          if (token.getUris().get(0).contains("http://dbpedia.org/resource/")) {
            resourceAmount++;
            representationFormElements.add("R");
          } else if (token.getUris().get(0).contains("http://dbpedia.org/ontology/")) {
            ontologyAmount++;
            representationFormElements.add(token.getType());
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
    return "QuestionProperties{\n"
      + "\tcontainsSuperlative=" + containsSuperlative + '\n'
      + "\tquestionStart='" + questionStart + '\'' + '\n'
      + "\tresourceAmount=" + resourceAmount + '\n'
      + "\tontologyAmount=" + ontologyAmount + '\n'
      + "\ttokens=" + tokens + '\n'
      + "\trepresentationForm='" + representationForm + '\'' + '\n'
      + '}';
  }
}
