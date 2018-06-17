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

public class StanfordNERTask implements TaskInterface {

  final Logger logger = LoggerFactory.getLogger(StanfordNERTask.class);

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
