package de.upb.ds.surnia.preprocessing.tasks;

import static org.hamcrest.core.IsEqual.equalTo;

import de.upb.ds.surnia.preprocessing.model.Token;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

public class AutoindexTaskTest {

  private final String QUESTION = "Wie alt ist Angela Merkel?";
  private AutoindexTask autoindexTask;
  private List<Token> stanfordTokens;


  @Before
  public void init() {
    // Set up fake autoindex via hashmap
    autoindexTask = new AutoindexTask() {
      @Override
      protected HashMap<String, Set<String>> getAnswerMapFromAutoindex(String question) {
        HashMap<String, Set<String>> fakeAutoindexEndpoint = new HashMap<>();
        fakeAutoindexEndpoint.put("alt", new HashSet<String>(Arrays.asList("foaf:age")));
        fakeAutoindexEndpoint
          .put("Angela Merkel", new HashSet<String>(Arrays.asList("dbr:Angela_Merkel")));
        fakeAutoindexEndpoint.put("Angela", new HashSet<String>(Arrays.asList("fake:Angela")));
        return fakeAutoindexEndpoint;
      }
    };

    // produce a stanford output
    stanfordTokens = new ArrayList<>();
    stanfordTokens.add(new Token("Wie", "PWAV"));
    stanfordTokens.add(new Token("alt", "ADJD"));
    stanfordTokens.add(new Token("ist", "VAFIN"));
    stanfordTokens.add(new Token("Angela", "NE"));
    stanfordTokens.add(new Token("Merkel", "NE"));
    stanfordTokens.add(new Token("?", "$."));
  }

  @Test
  public void testProcessTokens_AngelaMerkel() {
    List<Token> actualTokens = new ArrayList<>();
    actualTokens.add(new Token("Wie", "PWAV"));
    actualTokens.add(new Token("alt", "ADJD", new HashSet<String>(Arrays.asList("foaf:age"))));
    actualTokens.add(new Token("ist", "VAFIN"));
    actualTokens.add(new Token("Angela Merkel", "NE", new HashSet<String>(Arrays.asList("dbr:Angela_Merkel"))));
    actualTokens.add(new Token("?", "$."));

    List<Token> autoindexTokens = autoindexTask.processTokens(QUESTION, stanfordTokens);
    Assert.assertThat(autoindexTokens, equalTo(actualTokens));
  }

  /**
   * This test only works when there is an Autoindex-Endpoint with the below mentioned stuff.
   */
  @Ignore
  @Test
  public void testProcessTokens_AngelaMerkel_withActualAutoindex() {
    autoindexTask = new AutoindexTask();
    List<Token> actualTokens = new ArrayList<>();
    actualTokens.add(new Token("Wie", "PWAV"));
    actualTokens.add(new Token("alt", "ADJD", new HashSet<String>(Arrays.asList("foaf:age"))));
    actualTokens.add(new Token("ist", "VAFIN"));
    actualTokens.add(new Token("Angela Merkel", "NE", new HashSet<String>(Arrays.asList("dbr:Angela_Merkel"))));
    actualTokens.add(new Token("?", "$."));

    List<Token> autoindexTokens = autoindexTask.processTokens(QUESTION, stanfordTokens);
    Assert.assertThat(autoindexTokens, equalTo(actualTokens));
  }

}
