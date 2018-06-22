package de.upb.ds.surnia.preprocessing.tasks;

import de.upb.ds.surnia.preprocessing.model.Token;
import edu.stanford.nlp.ling.CoreAnnotations.LemmaAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.PartOfSpeechAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TextAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations.TokensAnnotation;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.StringUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This Task-implementation acts as a start task for the processing. It ignores the given tokens and
 * instead processes the question with tools of StanfordNER.
 */
public class StanfordNERTask implements TaskInterface {

  private final Logger logger = LoggerFactory.getLogger(StanfordNERTask.class);

  /**
   * Returns a list of tokens with POS-tags and other NER-relevant information provided by the
   * StanfordNER. It ignores the given token-list but instead build its own list by only considering
   * the given question.
   *
   * @param question question which will be processed and transformed into tokens
   * @param tokens given tokens are ignored
   * @return a list of tokens with POS-tags and other NER-relevant information
   */
  @Override
  public List<Token> processTokens(String question, List<Token> tokens) {
    Properties props = StringUtils.argsToProperties("-props", "StanfordCoreNLP-german.properties");
    props.setProperty("annotators", "tokenize, ssplit, pos, lemma"); //lemma not supported in german
    Annotation annotation = new Annotation(question);
    StanfordCoreNLP nlp = new StanfordCoreNLP(props);
    nlp.annotate(annotation);
    List<CoreMap> sentences = annotation.get(SentencesAnnotation.class);
    List<Token> stanfordTokens = new ArrayList<>();
    if (sentences.size() != 1) {
      logger.error("Input contains more than one sentence!");
    } else {
      for (CoreMap sentence : sentences) {
        for (CoreLabel token : sentence.get(TokensAnnotation.class)) {
          String text = token.get(TextAnnotation.class);
          String pos = token.get(PartOfSpeechAnnotation.class);
          String lemma = token.get(LemmaAnnotation.class);
          stanfordTokens.add(new Token(text, pos, lemma));
        }
      }
    }
    return stanfordTokens;
  }
}
