package de.upb.ds.surnia.preprocessing.model;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

public class Token {

  private String text;
  private String type;
  private String lemma;
  private Set<String> possibleTokenUris;

  public Token(String text) {
    this(text, null);
  }

  public Token(String text, String type) {
    this(text, type, text.toLowerCase());
  }

  public Token(String text, String type, String lemma) {
    this(text, type, lemma, new HashSet<>());
  }

  public Token(String text, String type, Set<String> uris) {
    this(text, type, text.toLowerCase(), uris);
  }

  /**
   * Constructor with all properties.
   *
   * @param text Surface string of the token.
   * @param type POS type of the token.
   * @param lemma Lemma of the token.
   * @param uris Possible ontology uris of the token.
   */
  public Token(String text, String type, String lemma, Set<String> uris) {
    this.text = text;
    this.type = type;
    this.lemma = lemma;
    possibleTokenUris = uris;
  }

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public String getLemma() {
    return lemma;
  }

  public void setLemma(String lemma) {
    this.lemma = lemma;
  }

  public void addUri(String uri) {
    possibleTokenUris.add(uri);
  }

  public void addUris(Set<String> uris) {
    possibleTokenUris.addAll(uris);
  }


  public Set<String> getUris() {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof Token)) {
      return false;
    }
    Token token = (Token) o;
    return Objects.equals(text, token.text) &&
      Objects.equals(type, token.type) &&
      Objects.equals(lemma, token.lemma) &&
      Objects.equals(possibleTokenUris, token.possibleTokenUris);
  }

  @Override
  public int hashCode() {

    return Objects.hash(text, type, lemma, possibleTokenUris);
  }
}
