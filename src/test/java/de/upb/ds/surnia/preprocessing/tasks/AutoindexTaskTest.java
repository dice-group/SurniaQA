package de.upb.ds.surnia.preprocessing.tasks;

import de.upb.ds.surnia.preprocessing.model.Token;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.core.IsEqual.equalTo;


public class AutoindexTaskTest {

    private final String QUESTION = "Who is relative of Jenny McCarthy";

    private AutoindexTask autoindexTask;

    private List<Token> stanfordTokens;


    @Before
    public void init() {
        // Set up fake autoindex via hashmap
        // produce a stanford output

        autoindexTask = new AutoindexTask();

        stanfordTokens = new ArrayList<>();

        stanfordTokens.add(new Token("Who", "WP"));
        stanfordTokens.add(new Token("is", "VBZ"));
        stanfordTokens.add(new Token("relative", "NN"));
        stanfordTokens.add(new Token("of", "IN"));
        stanfordTokens.add(new Token("Jenny", "NNP"));
        stanfordTokens.add(new Token("McCarthy", "NNP"));
    }

    //This test will run only when Autoindex is running , enable this only for development
    @Ignore
    @Test
    public void testProcessTokens_Soccer() {

        List<Token> actualTokens = new ArrayList<>();

        actualTokens.add(new Token("Who", "WP"));
        actualTokens.add(new Token("is", "VBZ"));
        actualTokens.add(new Token("relative", "NN", new HashSet<String>(Arrays.asList("http://dbpedia.org/ontology/relative"))));
        actualTokens.add(new Token("of", "IN"));
        actualTokens.add(new Token("jenny mccarthy", "NNP", new HashSet<String>(Arrays.asList("http://dbpedia.org/resource/Jenny_McCarthy"))));

        List<Token> autoIndexTokens = autoindexTask.processTokens(QUESTION, stanfordTokens);
        Assert.assertThat(autoIndexTokens, equalTo(actualTokens));
    }
}
