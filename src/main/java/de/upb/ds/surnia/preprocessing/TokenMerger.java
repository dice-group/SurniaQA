package de.upb.ds.surnia.preprocessing;

import de.upb.ds.surnia.preprocessing.model.Token;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TokenMerger {

  /**
   * Combine tokens that can be linked to a DBpedia resource.
   *
   * @param oldTokens List of tokens.
   * @param newToken String appearance of the found resource.
   * @return List of tokens where the resource tokens are combined and linked.
   */
  public List<Token> linkUri(List<Token> oldTokens, Token newToken) {
    List<Token> linkedTokens = new ArrayList<>();
    HashMap<String, Integer> posTags = new HashMap<>();
    boolean appearanceFound = false;
    boolean entityAdded = false;
    for (Token token : oldTokens) {
      if (newToken.getText().contains(token.getText()) && token.getUris().isEmpty()) {
        if (!appearanceFound) {
          appearanceFound = true;
        }
        if (posTags.containsKey(token.getType())) {
          posTags.put(token.getType(), posTags.get(token.getType()) + 1);
        } else {
          posTags.put(token.getType(), 1);
        }
      } else {
        if (!appearanceFound || entityAdded) {
          linkedTokens.add(token);
        } else {
          if (appearanceFound && !entityAdded) {
            entityAdded = true;
            // if new token already has a POS-tag, we can assume that it is better
            String posTag = newToken.getType() != null ?
              newToken.getType() :
              choosePosTag(posTags);
            linkedTokens.add(new Token(newToken.getText(), posTag, newToken.getUris()));
            linkedTokens.add(token);
          }
        }
      }
    }
    return linkedTokens;
  }

  private String choosePosTag(Map<String, Integer> posTags) {
    String posTag = "NNP";
    int max = 0;
    for (String tag : posTags.keySet()) {
      if (posTags.get(tag) > max) {
        max = posTags.get(tag);
        posTag = tag;
      }
    }
    return posTag;
  }

}
