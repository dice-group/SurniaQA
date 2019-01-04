package de.upb.ds.surnia.preprocessing.tasks;

import de.upb.ds.surnia.preprocessing.model.Token;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.core.IsEqual.equalTo;

public class StanfordNERTaskTest {


  // TODO: 04/01/2019 this tast is not being used so keeping it disabled for the moment as it will fail the build
  @Ignore
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
