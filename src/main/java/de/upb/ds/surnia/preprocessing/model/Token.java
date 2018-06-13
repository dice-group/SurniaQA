package de.upb.ds.surnia.preprocessing.model;

import java.util.List;

public class Token {

  private String text;
  private String type;
  private String lemma;
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
    this.text = text;
    this.type = type;
    this.lemma = lemma;
    possibleTokenUris = uris;
  }

  public String getText() {
    return text;
  }

  public String getType() {
    return type;
  }

  public String getLemma() {
  }

  public void addUris(List<String> uris) {
    for(String uri : uris) {
      addUri(uri);
    }
    return lemma;
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
    return "Token{" +
      "text='" + text + '\'' +
      ", type='" + type + '\'' +
      ", lemma='" + lemma + '\'' +
      ", possibleTokenUris=" + possibleTokenUris +
      '}';
  }
}
