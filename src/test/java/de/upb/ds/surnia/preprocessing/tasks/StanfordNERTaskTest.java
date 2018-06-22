package de.upb.ds.surnia.preprocessing.tasks;

import static org.hamcrest.core.IsEqual.equalTo;

import de.upb.ds.surnia.preprocessing.model.Token;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class StanfordNERTaskTest {


  @Test
  public void testProcessTokens_AngelaMerkel(){
    String question = "Wie alt ist Angela Merkel?";
    TaskInterface stanfordNer = new StanfordNERTask();
    List<Token> tokens = stanfordNer.processTokens(question, new ArrayList<>());

    List<Token> actualTokens = new ArrayList<>();
    actualTokens.add(new Token("Wie", "PWAV"));
    actualTokens.add(new Token("alt", "ADJD"));
    actualTokens.add(new Token("ist", "VAFIN"));
    actualTokens.add(new Token("Angela", "NE"));
    actualTokens.add(new Token("Merkel", "NE"));
    actualTokens.add(new Token("?", "$."));
    Assert.assertThat(tokens, equalTo(actualTokens));
  }

}
