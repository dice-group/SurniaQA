package de.upb.ds.surnia.preprocessing.model;

import java.util.ArrayList;
import java.util.List;

public class Token {

  private String text;
  private String type;
  private String lemma;
  private List<String> possibleTokenUris;


  public Token(String text, String type) {
    this(text, type, null, new ArrayList<>());
  }

  public Token(String text, String type, String lemma) {
    this(text, type, lemma, new ArrayList<>());
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

  public void setText(String text) {
    this.text = text;
  }

  public String getText() {
    return text;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getType() {
    return type;
  }

  public void setLemma(String lemma) {
    this.lemma = lemma;
  }

  public String getLemma() {
    return lemma;
  }

  public void addUri(String uri){
    if(!possibleTokenUris.contains(uri)) {
      possibleTokenUris.add(uri);
    }
  }

  public void addUris(List<String> uris) {
    for(String uri : uris) {
      addUri(uri);
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
