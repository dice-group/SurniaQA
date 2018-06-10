package de.upb.ds.surnia.preprocessing.tasks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.upb.ds.surnia.preprocessing.Token;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.util.UriComponentsBuilder;

public class AutoindexTask extends AbstractRestTask {

  private static final String DEFAULT_URL = "localhost:9091/search";
  private static final Logger log = LoggerFactory.getLogger(AutoindexTask.class);

  public AutoindexTask() {
    super(DEFAULT_URL, HttpMethod.POST);
  }

  public AutoindexTask(String url) {
    super(url, HttpMethod.POST);
  }

  @Override
  protected UriComponentsBuilder addParameters(String request, UriComponentsBuilder builder) {
    return builder.queryParam("type", "LABEL")
      .queryParam("category", "ALL")
      .queryParam("query", request);
  }

  @Override
  protected HttpHeaders setHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
    return headers;
  }

  @Override
  public List<Token> processTokens(String question, List<Token> tokens) {
    for(Token token : tokens) {
      HttpEntity<String> response =  getRestReponse(token.getLemma());
      ObjectMapper mapper = new ObjectMapper();
      List<String> uris = new ArrayList<>();
      try{
        JsonNode root = mapper.readTree(response.getBody());
        JsonNode answerArray = root.path("results").path("bindings");
        for(JsonNode objNode : answerArray) {
          uris.add(objNode.path("uri").path("value").toString());
        }
      } catch(IOException ioE) {
        log.error("Answer of request is not JSON-formatted!");
      }
      token.addUris(uris);
    }
    return tokens;
  }
}
