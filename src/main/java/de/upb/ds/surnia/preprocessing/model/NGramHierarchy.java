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
 * This class represents the n-gram hierarchy. For a given n-gram it represents a tree. The root of
 * the tree is the n-gram itself. The children of each node are the different 'n-1-grams'. E.g. the
 * children of the trigram "bill gates wife" are bigrams "bill gates" and "gates wife".
 */
public class NGramHierarchy {

  private List<String> nGrams;

  /**
   * Initializes with already split n-gram. The split has to be between the words. E.g. the orginal
   * n-gram "birthplace bill gates" has to be given as '["birthplace", "bill", "gates"]'.
   *
   * @param nGrams already split n-gram
   */
  public NGramHierarchy(String[] nGrams) {
      this.nGrams = new ArrayList();
      for (int i = 0; i < nGrams.length; i++) {
          nGrams[i] = nGrams[i].replaceAll("\\p{P}", "").toLowerCase();
      }
      Collections.addAll(this.nGrams, nGrams);
  }

  /**
   * Initializes with n-gram sentences in normal String-representation. Unigram has to be split by
   * one single space.
   *
   * @param nGrams n-gram in String-representation
   */
  public NGramHierarchy(String nGrams) {
    this(nGrams.split(" "));
  }

  /**
   * Returns the n-gram for which the positional data was given.
   *
   * @param pos the n-gram entry position
   * @return n-gram at the given position
   */
  public String getNGram(NGramEntryPosition pos) {
    return getNGram(pos.getLength(), pos.getPosition());
  }

  /**
   * Returns the n-gram for which the positional data was given.
   *
   * @param length represents the length of the n-gram, e.g. length=2 is a bigram
   * @param index represents the position within the n-gram of given length
   * @return n-gram at the given position
   */
  public String getNGram(int length, int index) {
    if (length == 1) {
      return nGrams.get(index);
    } else {
      StringBuilder sb = new StringBuilder();
      for (int i = index; i < length + index - 1; i++) {
        sb.append(nGrams.get(i) + " ");
      }
      sb.append(nGrams.get(length + index - 1));
      return sb.toString();
    }
  }


  /**
   * Returns the parents of given n-gram.
   *
   * @param length represents the length of the n-gram, e.g. length=2 is a bigram
   * @param index represents the position within the n-gram of given length
   * @return parents of given n-gram
   */
  public String[] getParents(int length, int index) {
    String parents[];
    if (index == 0) {
      if (index + length == nGrams.size()) {
        return null;
      } else {
        String parent = getNGram(length + 1, index);
        parents = new String[1];
        parents[0] = parent;
      }
    } else if (index + length == nGrams.size()) {
      String parent = getNGram(length + 1, index - 1);
      parents = new String[1];
      parents[0] = parent;
    } else {
      String parent1 = getNGram(length + 1, index - 1);
      String parent2 = getNGram(length + 1, index);
      parents = new String[2];
      parents[0] = parent1;
      parents[1] = parent2;
    }
    return parents;
  }


  /**
   * Given the position and length for a n-gram, returns direct children, i.e. the children directly
   * connected to the n-gram within the n-gram hierarchy.
   *
   * @param length represents the length of the n-gram, e.g. length=2 is a bigram
   * @param index represents the position within the n-gram of given length
   * @return directly connected children
   */
  public String[] getDirectChildren(int length, int index) {
    if (length == 1) {
      return null;
    } else {
      String[] children = new String[2];
      children[0] = getNGram(length - 1, index);
      children[1] = getNGram(length - 1, index + 1);
      return children;
    }
  }

  /**
   * Returns the whole n-gram hierarchy sorted by length, then by position. E.g. "birthplace bill
   * gates" would return ["birthplace bill gates", "birthplace bill", "bill gates", "birthplace",
   * "bill", "gates"]
   *
   * @return n-gram hierarchy represented as array
   */
  public String[] toStringArray() {
    String[] hierarchy = new String[(nGrams.size() * (nGrams.size() + 1)) / 2];
    int hierarchyIndex = 0;
    for (int l = nGrams.size(); l > 0; l--) {
      for (int i = 0; i + l <= nGrams.size(); i++) {
        hierarchy[hierarchyIndex] = getNGram(l, i);
        hierarchyIndex++;
      }
    }
    return hierarchy;
  }

  /**
   * Generates all possible NGramEntryPositions for this n-gram hierarchy. I.e. if the n-gram has a
   * length of 3, it would generate the NGramEntryPositions for the trigram, for the 2 possible
   * bigrams and for the 3 unigrams.
   *
   * @return all possible NgramEntryPositions as set
   * @see NGramEntryPosition
   */
  public Set<NGramEntryPosition> getAllPositions() {
    Set<NGramEntryPosition> positions = new HashSet<>();
    for (int l = nGrams.size(); l > 0; l--) {
      for (int i = 0; i + l <= nGrams.size(); i++) {
        positions.add(new NGramEntryPosition(l, i));
      }
    }
    return positions;
  }

  /**
   * Returns length of initial n-gram, i.e. how many words it has.
   *
   * @return number of words within the initial n-gram
   */
  public int getNGramLength() {
    return nGrams.size();
  }

  /**
   * Extends the hierarchy by adding additional keywords (as array) at the end.
   *
   * @param extension array of strings which should be added
   */
  public void extendHierarchy(String[] extension) {
    this.nGrams.addAll(Arrays.asList(extension));
  }

  /**
   * Extends the hierarchy by adding additional keywords at the end.
   *
   * @param extension array of strings which should be added
   */
  public void extendHierarchy(String extension) {
    extendHierarchy(extension.split(" "));
  }

  /**
   * Returns the position of the given n-gram in the n-gram hierarchy.
   *
   * @param nGram n-gram for which the position should be given back
   * @return position of the given n-gram
   */
  public NGramEntryPosition getPosition(String nGram) {
    return getPosition(nGram.split(" "));
  }

  /**
   * Returns the position of the given n-gram (in array-format) in the n-gram hierarchy.
   *
   * @param forNGram n-gram for which the position should be given back
   * @return position of the given n-gram
   */
  public NGramEntryPosition getPosition(String[] forNGram) {
    String forEachNGram = forNGram[0].replaceAll("\\\\","").toLowerCase(); // TODO: 13/12/2018 this is because autoindex sends unwanted characters along with nGrams
    int pos = -1;
    for (int i = 0; i < nGrams.size(); i++) {
      if (nGrams.get(i).equals(forEachNGram)) {
        boolean foundWholeNGram = true;
        for (int j = 1; j < forNGram.length; j++) {
          if (i + j >= nGrams.size() || !nGrams.get(i + j).equals(forNGram[j])) {
            foundWholeNGram = false;
          }
        }
        if (foundWholeNGram) {
          return new NGramEntryPosition(forNGram.length, i);
        } else {
          return null;
        }
      }
    }
    return null;
  }

  /**
   * Returns all keywords in their representative order as string (with space as delimiter).
   *
   * @return all keywords in their representative order as string (with space as delimiter).
   */
  @Override
  public String toString() {
    return String.join(" ", nGrams);
  }


}
