package de.upb.ds.surnia.preprocessing;

import de.upb.ds.surnia.preprocessing.model.Token;
import de.upb.ds.surnia.preprocessing.tasks.AutoindexTask;
import de.upb.ds.surnia.preprocessing.tasks.StanfordNERTask;
import de.upb.ds.surnia.preprocessing.tasks.TaskInterface;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProcessingPipeline {

  final Logger logger = LoggerFactory.getLogger(ProcessingPipeline.class);

  private List<TaskInterface> taskPipeline;


  public ProcessingPipeline() {
    this.taskPipeline = new ArrayList<>();
    taskPipeline.add(new StanfordNERTask());
    taskPipeline.add(new AutoindexTask());// TODO: 18/11/2018 <S> for now don't use AutoIndex , uncomment later
  }

  /**
   * Create pipeline with given tasks. The first task has to split the text up into its components!
   */
  public ProcessingPipeline(List<TaskInterface> taskPipeline) {
    this.taskPipeline = new ArrayList<>(taskPipeline);
  }


  /**
   * Processes a question into a List of tokens.
   *
   * @param question Input question.
   * @return List of tokens extracted from the input question.
   * @throws IOException Error while performing Named Entity Recognition.
   */
  public List<Token> processQuestion(String question) {
    List<Token> tokens = new ArrayList<>();
    for (TaskInterface task : taskPipeline) {
      tokens = task.processTokens(question, tokens);
    }
    return tokens;
  }

}
