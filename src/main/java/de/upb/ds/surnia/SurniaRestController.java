package de.upb.ds.surnia;

import com.github.jsonldjava.utils.JsonUtils;
import de.upb.ds.surnia.preprocessing.model.Token;
import de.upb.ds.surnia.preprocessing.tasks.AutoindexTask;
import de.upb.ds.surnia.qa.QuestionAnswerer;

import java.util.*;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;

import org.aksw.qa.commons.datastructure.Question;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/ask")
public class SurniaRestController {

  static Logger logger = LoggerFactory.getLogger(SurniaRestController.class);

  private QuestionAnswerer qa = new QuestionAnswerer();

  private final AutoindexTask autoindexTask;

  public SurniaRestController(AutoindexTask autoindexTask) {
    this.autoindexTask = autoindexTask;
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

//  @RequestMapping(value = "/ask", method = RequestMethod.GET)
  @GetMapping("/{query}")
  public String askQuestions(@Valid @PathVariable String query, HttpServletResponse response) {

//    String question = params.get("query");
    String question = query;
    List<Token> autoIndextokens = autoindexTask.processTokens(question, produceStandfordTestTokens());
    Set<String> uri = null;
    for (Token t : autoIndextokens
      ) {
      uri = t.getUris();
      if (uri.size()>0)
        break;
    }
    String possibleURL = "";
    for(Iterator<String> it = uri.iterator(); ((Iterator) it).hasNext();)
    {
      possibleURL = it.next();
    }
    return possibleURL;
  }

  public List<Token> produceStandfordTestTokens() {
    List<Token> stanfordTokensTest = new ArrayList<>();
    stanfordTokensTest.add(new Token("What", "PWAV"));
    stanfordTokensTest.add(new Token("is", "VAFIN"));
    stanfordTokensTest.add(new Token("Soccer", "NE"));
    stanfordTokensTest.add(new Token("?", "$."));
    return stanfordTokensTest;
  }

}
