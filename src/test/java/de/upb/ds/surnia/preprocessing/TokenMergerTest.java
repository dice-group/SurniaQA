package de.upb.ds.surnia.preprocessing;

import de.upb.ds.surnia.preprocessing.model.Token;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItem;

public class TokenMergerTest {

  private List<Token> stanfordTokens;

  @Before
  public void init() {
    stanfordTokens = new ArrayList<>();
    stanfordTokens.add(new Token("Wie", "PWAV"));
    stanfordTokens.add(new Token("alt", "ADJD"));
    stanfordTokens.add(new Token("ist", "VAFIN"));
    stanfordTokens.add(new Token("Angela", "NE"));
    stanfordTokens.add(new Token("Merkel", "NE"));
    stanfordTokens.add(new Token("?", "$."));
  }

  @Ignore
  @Test
  public void testLinkUri_Merge2Tokens() {
    TokenMerger tm = new TokenMerger();
    List<Token> newTokens = tm.integrateToken(stanfordTokens,
      new Token("Angela Merkel",
        null,
        new HashSet<>(Arrays.asList("dbr:Angela_Merkel"))
      )
    );
    Assert.assertThat(newTokens, hasSize(stanfordTokens.size() - 1));
    Token angelaMerkel = new Token(
      "Angela Merkel",
      "NE",
      "angela merkel",
      new HashSet<>(Arrays.asList("dbr:Angela_Merkel"))
    );
    Assert.assertThat(newTokens, hasItem(angelaMerkel));
  }

  @Ignore
  @Test
  public void testLinkUri_JustMergeTokenInformation_WithNoType() {
    TokenMerger tm = new TokenMerger();
    List<Token> newTokens = tm.integrateToken(stanfordTokens,
      new Token("alt",
        null,
        new HashSet<>(Arrays.asList("foaf:age"))
      )
    );
    Token mergedToken = new Token(
      "alt",
      "ADJD",
      "alt",
      new HashSet<>(Arrays.asList("foaf:age"))
    );
    Assert.assertThat(newTokens, hasSize(stanfordTokens.size()));
    Assert.assertThat(newTokens, hasItem(mergedToken));
  }

  @Ignore
  @Test
  public void testLinkUri_JustMergeTokenInformation_WithType() {
    TokenMerger tm = new TokenMerger();
    List<Token> newTokens = tm.integrateToken(stanfordTokens,
      new Token("alt",
        "Test",
        new HashSet<>(Arrays.asList("foaf:age"))
      )
    );
    Token mergedToken = new Token(
      "alt",
      "Test",
      "alt",
      new HashSet<>(Arrays.asList("foaf:age"))
    );
    Assert.assertThat(newTokens, hasSize(stanfordTokens.size()));
    Assert.assertThat(newTokens, hasItem(mergedToken));
  }
}