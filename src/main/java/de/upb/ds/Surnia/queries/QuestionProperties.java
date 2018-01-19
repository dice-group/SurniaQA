package de.upb.ds.Surnia.queries;

import de.upb.ds.Surnia.preprocessing.Token;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class QuestionProperties {

    public boolean containsSuperlative = false;
    public String questionStart;
    public int resourceAmount = 0;
    public int ontologyAmount = 0;
    public String[] resources;
    public String[] ontologies;
    public String representationForm;

    public QuestionProperties(List<Token> questionTokens) {
        // Set all properties for the question according to the question tokens
        if (questionTokens.size() > 0) {
            questionStart = questionTokens.get(0).getText();
            resources = new String[questionTokens.size()];
            ontologies = new String[questionTokens.size()];
            LinkedList<String> representationFormElements = new LinkedList<>();
            for (Token token : questionTokens) {
                if (token.getType().equals("JJS") || token.getType().equals("RBS")) {
                    containsSuperlative = true;
                }
                if (token.getURIs() != null) {
                    for (String uri : token.getURIs()) {
                        if (uri.contains("http://dbpedia.org/resource/")) {
                            resources[resourceAmount++] = uri;
                            representationFormElements.add("R" + resourceAmount);
                            break;
                        }
                        if (uri.contains("http://dbpedia.org/ontology/")) {
                            ontologies[ontologyAmount++] = uri;
                            representationFormElements.add("O" + ontologyAmount);
                            break;
                        }
                    }
                } else {
                    representationFormElements.add(token.getType());
                }
            }
            representationForm = String.join(" ", representationFormElements);
        }
    }

    @Override
    public String toString() {
        return "QuestionProperties{" +
                "containsSuperlative=" + containsSuperlative +
                ", questionStart='" + questionStart + '\'' +
                ", resourceAmount=" + resourceAmount +
                ", ontologyAmount=" + ontologyAmount +
                ", resources=" + Arrays.toString(resources) +
                ", ontologies=" + Arrays.toString(ontologies) +
                ", representationForm='" + representationForm + '\'' +
                '}';
    }
}
