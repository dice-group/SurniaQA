package de.upb.ds.surnia;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import de.upb.ds.surnia.gerbil.GerbilFinalResponse;
import de.upb.ds.surnia.qa.QuestionAnswerer;
import java.util.Map;
import javax.servlet.http.HttpServletResponse;
import org.aksw.qa.commons.datastructure.Question;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SurniaRestController {

  static Logger logger = LoggerFactory.getLogger(SurniaRestController.class);

  private QuestionAnswerer qa = new QuestionAnswerer();

  /**
   * Endpoint for answering a question with a QALD JSON.
   * @param params Params containing the question and the language.
   * @param response Response object.
   * @return Answer to the question as a QALD JSON.
   */
  @RequestMapping(value = "/ask-gerbil", method = RequestMethod.POST)
  public String askGerbil(@RequestParam Map<String, String> params, HttpServletResponse response) {
    String question = params.get("query");
    String language = params.get("lang");

    logger.info("Received question(" + language + "): " + question);

    Question q = new Question();
    q.getLanguageToQuestion().put(language, question);
    JSONObject answer = qa.answerQuestion(q);

    GerbilFinalResponse resp = new GerbilFinalResponse();
    resp.setQuestions(q, language);
    ObjectWriter ow = new ObjectMapper().writer().withDefaultPrettyPrinter();
    String json = null;
    try {
      json = ow.writeValueAsString(resp);
    } catch (JsonProcessingException e) {
      logger.error("Error in /ask-gerbil endpoint", e);
    }

    logger.info("\n\n JSON object: \n\n" + json);

    return json;
  }

}
