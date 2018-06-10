package de.upb.ds.surnia.preprocessing;

import java.util.List;

public class Token {

  private String tokenText;
  private String tokenType;
  private String tokenLemma;
  private List<String> possibleTokenUris;


  public Token(String text, String type) {
    this(text, type, null, null);
  }

  public Token(String text, String type, String lemma) {
    this(text, type, lemma, null);
  }

  public Token(String text, String type, List<String> uris) {
    this(text, type, null, uris);
  }

  /**
   * Constructor with all properties.
   *
   * @param text Surface string of the token.
   * @param type POS type of the token.
   * @param lemma Lemma of the token.
   * @param uris Possible ontology uris of the token.
   */
  public Token(String text, String type, String lemma, List<String> uris) {
    tokenText = text;
    tokenType = type;
    tokenLemma = lemma;
    possibleTokenUris = uris;
  }

  public String getText() {
    return tokenText;
  }

  public String getType() {
    return tokenType;
  }

  public String getLemma() {
    return tokenLemma;
  }

  public void addUris(List<String> uris) {
    for(String uri : uris) {
      addUri(uri);
    }
  }

  public void addUri(String uri){
    if(!possibleTokenUris.contains(uri)) {
      possibleTokenUris.add(uri);
    }
  }

  public List<String> getUris() {
    return possibleTokenUris;
  }

  @Override
  public String toString() {
    String uris = (possibleTokenUris != null ? (" - " + possibleTokenUris) : "");
    return tokenText + "(" + tokenLemma + " - " + tokenType + uris + ")";
  }
}
