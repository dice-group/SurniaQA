package de.upb.ds.surnia.preprocessing.model;

import de.upb.ds.surnia.util.SurniaUtil;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;
import org.atteo.evo.inflector.English;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * @author sajjadpervaiz
 */
public class NGrams {

  private static Logger logger = LoggerFactory.getLogger(NGrams.class);

  private Set<String> nGrams;

  private Map<String, Set<String>> tokensFromAutoIndex;

  /**
   * Produces Shingles/Ngrams from a given string.
   * e.g For a string "Who is relative of Jenny McCarthy?" this will generate
   * McCarthy, Jenny, Jenny McCarthy, Who, relative
   *
   * @param sentence
   * @param tokensFromAutoIndex
   */
  public NGrams(String sentence, Map<String, Set<String>> tokensFromAutoIndex) {
    this.tokensFromAutoIndex = tokensFromAutoIndex;
    this.nGrams = new HashSet<>();
    Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_46);
    StringReader reader = new StringReader(sentence);
    try {
      TokenStream tokenStream = analyzer.tokenStream("sentence", reader);
      ShingleFilter shingleFilter = new ShingleFilter(tokenStream);
      shingleFilter.setOutputUnigrams(true);
      CharTermAttribute charTermAttribute = shingleFilter.addAttribute(CharTermAttribute.class);
      shingleFilter.reset();
      while (shingleFilter.incrementToken()) {
        this.nGrams.add(charTermAttribute.toString()
          .replaceAll("[^\\\\dA-Za-z ]", "")
          .trim()
          .toLowerCase());
      }
      for (String nGram : this.nGrams) {
        logger.info("Ngram: "+nGram);
      }
      shingleFilter.end();
      shingleFilter.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  /**
   * Gets list of token to URI mappings from AutoIndex and compare NL query tokens,
   * If the match is found then that token is kept and rest are discarded.
   * This will also handle any tokens that are in plural forms
   * e.g If a URI exists against token Employer and NL Token is Employers, it will still keep these tokens and map their URIs
   *
   * @return List<Token>
   */
  public List<Token> produceTokens() {
    List<Token> tokenList = new ArrayList<>();
    for (String nGram : this.nGrams) {
      this.tokensFromAutoIndex.keySet().forEach(tokenFromAutoIndex -> {
        String autoIndexToken = tokenFromAutoIndex.replaceAll("\\\\", "").toLowerCase();
        if (nGram.equals(autoIndexToken) || English.plural(autoIndexToken).equals(nGram) || SurniaUtil.levenshtein(nGram, autoIndexToken) <=2) {
          // TODO: 07/01/2019 check if we need to use equals or contains
          // TODO: 11/01/2019 String similarity would be a good thing to implement here
          // TODO: 11/01/2019 Also try generating all possible combinations of a string that contains more than one label e.g "total population" and "population total"
          Token token = new Token(nGram);
          token.addUris(tokensFromAutoIndex.get(tokenFromAutoIndex));
          tokenList.add(token);
        }
      });
    }
    return tokenList;
  }
}
