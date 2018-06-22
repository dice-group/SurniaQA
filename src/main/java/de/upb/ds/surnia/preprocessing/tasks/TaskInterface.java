package de.upb.ds.surnia.preprocessing.tasks;

import de.upb.ds.surnia.preprocessing.model.Token;
import java.util.List;

/**
 * A task-object that processes the given question and the given tokens from previous tasks and
 * produces its own list of tokens, depending on the implementation. Implementations should consider
 * the previous token list as much as possible.
 */
public interface TaskInterface {

  List<Token> processTokens(String question, List<Token> tokens);

}
