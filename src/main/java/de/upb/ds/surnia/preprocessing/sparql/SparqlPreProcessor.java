package de.upb.ds.surnia.preprocessing.sparql;

import de.upb.ds.surnia.preprocessing.tasks.StanfordNERTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The purpose of this class is to create SPARQL queries from parsed standfor
 * tokens. The class will make use of SPARQL templates to decided the best suited
 * query for a particular question.
 */
@Component
public class SparqlPreProcessor {

    private final StanfordNERTask stanfordNERTask;

    @Autowired
    public SparqlPreProcessor(StanfordNERTask stanfordNERTask) {
        this.stanfordNERTask = stanfordNERTask;
    }


}
