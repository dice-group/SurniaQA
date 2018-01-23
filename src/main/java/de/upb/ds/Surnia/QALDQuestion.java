package de.upb.ds.Surnia;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class QALDQuestion {
    @JsonProperty("id")
    public int id;

    public String questionString;
    @JsonProperty("question")
    private void findEnglishQuestion(Map<String, Object>[] questions) {
        for (Map<String, Object> question : questions) {
            if (question.containsKey("language") && question.get("language").equals("en")) {
                if (question.containsKey("string")) {
                    questionString = (String)question.get("string");
                }
            }
        }
    }
}
