package de.upb.ds.surnia.preprocessing.model;

import java.util.HashSet;
import java.util.Set;

/**
 * This class represents a position in the n-gram hierarchy. A node with e.g. length of 2 and  third
 * position (position=2) this object would represent the third bi-gram. If the whole n-gram is
 * "birthplace bill gates wife", the object would represent "gates wife". Because this class is also
 * used to describe which nodes are related in the graph, a NGramEntryPosition can also be seen als
 * a color of a node.
 *
 * @author Simon Bordewisch
 */
public class NGramEntryPosition {

  /**
   * Defines the length of the n-gram.
   */
  private int length;

  /**
   * Defines the position in it's "row". All n-grams with the same length are in the same row.
   */
  private int position;


  /**
   * Initializes object with length and position in the "row". I.e. if e.g. length=2, position=2,
   * this object would represent the third bi-gram. If the whole n-gram is "birthplace bill gates
   * wife", the object would represent "gates wife".
   *
   * @param length length of the n-gram
   * @param position position in the "row"
   */
  public NGramEntryPosition(int length, int position) {
    this.position = position;
    this.length = length;
  }

  /**
   * Returns position within the n-gram hierarchy with the same length.
   *
   * @return position within the n-gram hierarchy with the same length
   */
  public int getPosition() {
    return position;
  }

  /**
   * Returns a set of all positional information of descendants of this n-gram entry.
   *
   * @return all positional informtion of descendats of this entry
   */
  public Set<NGramEntryPosition> getAllDescendants() {
    return getAllDescendants(this);
  }

  /**
   * This method is used for recursive calls to get all decendants for this n-gram position.
   *
   * @param pos position of the ngram entry for which the decendants should be searched for
   * @return all descendants of the given node
   * @see #getAllDescendants()
   */
  private Set<NGramEntryPosition> getAllDescendants(NGramEntryPosition pos) {
    if (pos.getLength() == 1) {
      return new HashSet<>();
    } else {
      Set<NGramEntryPosition> descendants = new HashSet<>();
      NGramEntryPosition child1 = new NGramEntryPosition(pos.getLength() - 1, pos.getPosition());
      NGramEntryPosition child2 = new NGramEntryPosition(pos.getLength() - 1,
        pos.getPosition() + 1);
      descendants.add(child1);
      descendants.add(child2);
      descendants.addAll(getAllDescendants(child1));
      descendants.addAll(getAllDescendants(child2));
      return descendants;
    }
  }

  /**
   * Returns length of this entry. I.e. an entry for a bigram would have a length of 2.
   *
   * @return length of this entry
   */
  public int getLength() {
    return length;
  }

  /**
   * Returns string representation of this n-gram position entry. It has the following scheme (#var
   * represent the values of the variable): Entry( position: #position, length: #length)
   */
  @Override
  public String toString() {
    return "Entry( length: " + length + ", position: " + position + ")";
  }

  /**
   * Override for hashcode to get a good and easy hash for the entries. As long as the n-gram does
   * not contain more than 10000 words, it should be unique.
   *
   * @return hash representation of the n-gram position
   */
  @Override
  public int hashCode() {
    return position * 10000 + length;
  }

  /**
   * Compares the specified object with this object for equality. Returns true if the given object
   * is either the same or is of the same class, and has the same length and position.
   *
   * @param other object to be compared for equality with this object
   * @return true if the given object is of the same class and has the same length and position,
   * else false
   */
  @Override
  public boolean equals(Object other) {
    if (this == other) {
      return true;
    }
    if (other.getClass() != this.getClass()) {
      return false;
    }
    if (((NGramEntryPosition) other).getLength() != this.getLength()) {
      return false;
    }
    if (((NGramEntryPosition) other).getPosition() != this.getPosition()) {
      return false;
    }
    return true;

  }


  /**
   * Returns true if the given color is related to this color, i.e. if it is an ancestor or if the
   * other color is an ancestor of this color.
   *
   * @param otherColor other color that should be checked for relation
   * @return true if the given color is related to this color, false otherwise
   * @see #isAncestorOf(NGramEntryPosition)
   */
  public boolean isRelatedTo(NGramEntryPosition otherColor) {
    if (this.equals(otherColor)) {
      return true;
    }

    if (this.isAncestorOf(otherColor)) {
      return true;
    }

    if (otherColor.isAncestorOf(this)) {
      return true;
    }

    return false;
  }


  /**
   * Returns true if this color is an ancestor of the given color. A color is an ancestor of another
   * node, if the node covers all words of the other node and is longer. For example, the color for
   * "gates wife" would be an ancestor to the colors for "gates" and "wife".
   *
   * @param otherColor other color that should be checked if it is an descendant
   * @return true if this color is an ancestor of the given color, false otherwise
   */
  public boolean isAncestorOf(NGramEntryPosition otherColor) {
    return this.getAllDescendants().contains(otherColor);
  }

  /**
   * Returns true if the represented positions of this color are overlapping with the represented
   * positions of the given color. E.g. for the n-gram "birthplace bill gates wife", the color
   * representing "birthplace bill" would overlap with the color representing "bill gates", because
   * both cover the word "bill".
   *
   * @param otherColor color that should be compared with this color for overlaps
   * @return true if the represented positions of this color are overlapping, false otherwise
   */
  public boolean isOverlappingWith(NGramEntryPosition otherColor) {
    if (this.getPosition() <= otherColor.getPosition() &&
      otherColor.getPosition() <= this.getPosition() + this.getLength() - 1) {
      return true;
    }

    if (otherColor.getPosition() <= this.getPosition() &&
      this.getPosition() <= otherColor.getPosition() + otherColor.getLength() - 1) {
      return true;
    }
    return false;
  }

  /**
   * Returns true if the given set of colors is mergeable with this color. Also see {@link
   * #isMergeable(NGramEntryPosition)}
   *
   * @param otherColors set of colors that should be checked for mergeability
   * @return true if the given set of colors is mergeable with this color, false otherwise
   * @see #isMergeable(NGramEntryPosition)
   */
  public boolean isMergeable(Set<NGramEntryPosition> otherColors) {
    for (NGramEntryPosition otherColor : otherColors) {
      if (!isMergeable(otherColor)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns true if the given set of colors is mergeable with this color. They are mergeable if
   * they are related or don't overlap.
   *
   * @param otherColor set of colors that should be checked for mergeability
   * @return true if the given color is mergeable with this color, false otherwise
   */
  public boolean isMergeable(NGramEntryPosition otherColor) {
    if (this.isRelatedTo(otherColor)) {
      return true;
    }
    return !this.isOverlappingWith(otherColor);
  }
}
