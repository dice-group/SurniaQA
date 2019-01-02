package de.upb.ds.surnia.preprocessing.model;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.shingle.ShingleFilter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.util.Version;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * @author sajjadpervaiz
 */
public class NGrams {

    private Set<String> nGrams;

    private Map<String, Set<String>> tokensFromAutoIndex;

    public NGrams(String sentence, Map<String, Set<String>> tokensFromAutoIndex) {
        this.tokensFromAutoIndex = tokensFromAutoIndex;
        this.nGrams = new HashSet();
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
            this.nGrams.stream().forEach(System.out::println);
            shingleFilter.end();
            shingleFilter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<Token> produceTokens() {
        List<Token> tokenList = new ArrayList<>();
        this.nGrams.stream().forEach(nGram -> {
            this.tokensFromAutoIndex.keySet().stream().forEach(tokenFromAutoIndex -> {
                // TODO: 02/01/2019 check if type of NGrams is plural then instead of equals use contains since autoIndex will return against singular label
                if (nGram.equals(tokenFromAutoIndex.replaceAll("\\\\", "").toLowerCase())) {
                    Token token = new Token(nGram);
                    token.addUris(tokensFromAutoIndex.get(tokenFromAutoIndex));
                    tokenList.add(token);
                }
            });
        });
        return tokenList;
    }
}
