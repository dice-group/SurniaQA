package de.upb.ds.surnia;

import com.github.jsonldjava.utils.JsonUtils;
import de.upb.ds.surnia.qa.QuestionAnswerer;
import org.aksw.qa.commons.datastructure.Question;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
public class SurniaRestController {

  private static Logger logger = LoggerFactory.getLogger(SurniaRestController.class);

  private final QuestionAnswerer qa;

  @Autowired
  public SurniaRestController(QuestionAnswerer qa) {
    this.qa = qa;
  }

  /**
   * Endpoint for answering a question with a QALD JSON.
   *
   * @param params   Params containing the question and the language.
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

    JSONObject answer = qa.getAnswersToQuestion(q, language);

    try {
      logger.info("Got: " + JsonUtils.toPrettyString(answer));
      return JsonUtils.toPrettyString(answer);
    } catch (Exception e) {
      logger.error("Error in JSON answer.", e);
      return "JSON Error";
    }
  }
}
