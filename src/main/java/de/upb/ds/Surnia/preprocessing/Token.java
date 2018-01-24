package de.upb.ds.Surnia.preprocessing;

import java.util.List;

public class Token {

    private String tokenText;
    private String tokenType;
    private String tokenLemma;
    private List<String> possibleTokenURIs;

    public Token (String text, String type) {
        this(text, type, null, null);
    }

    public Token (String text, String type, String lemma) {
        this(text, type, lemma,  null);
    }

    public Token (String text, String type, List<String> URIs) {
        this(text, type, null, URIs);
    }

    public Token (String text, String type, String lemma, List<String> URIs) {
        tokenText = text;
        tokenType = type;
        tokenLemma = lemma;
        possibleTokenURIs = URIs;
    }

    public String getText () {
        return tokenText;
    }

    public String getType () {
        return tokenType;
    }

    public String getLemma () {
        return tokenLemma;
    }

    public List<String> getURIs () {
        return possibleTokenURIs;
    }

    @Override
    public String toString()  {
        return tokenText + "(" + tokenLemma + " - " + tokenType + (possibleTokenURIs != null? (" - " + possibleTokenURIs) : "") + ")";
    }
}
