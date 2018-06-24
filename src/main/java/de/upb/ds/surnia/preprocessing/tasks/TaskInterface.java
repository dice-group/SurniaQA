package de.upb.ds.surnia.preprocessing.tasks;

import de.upb.ds.surnia.preprocessing.model.Token;
import java.util.List;

/**
 * A task-object that processes the given question and the given tokens from previous tasks and
 * produces its own list of tokens, depending on the implementation. Implementations should consider
 * the previous token list as much as possible.
 */
public interface TaskInterface {

  /**
   * Given the question and tokens from previous tasks, this method returns its own set of tokens.
   * Implementations can ignore either the given question or the tokens. Ignoring the tokens means
   * that this task can be considered as a starting point For the processing pipeline.
   *
   * @param question question asked by the use
   * @param tokens tokens from previous tasks
   */
  List<Token> processTokens(String question, List<Token> tokens);

}
