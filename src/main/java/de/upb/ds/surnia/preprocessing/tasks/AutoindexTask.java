package de.upb.ds.surnia.preprocessing.tasks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.upb.ds.surnia.preprocessing.TokenMerger;
import de.upb.ds.surnia.preprocessing.model.NGramEntryPosition;
import de.upb.ds.surnia.preprocessing.model.NGramHierarchy;
import de.upb.ds.surnia.preprocessing.model.Token;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * This Task-implementation builds onto previous tasks and tries to find URIs for the given
 * questions. For this, the question is given to an Autoindex-endpoint. The returned URIs are
 * processed into a n-gram hierarchy, giving a mapping of n-grams to the found URIs. This mapping
 * influences the token building, which considers information of the given tokens from previous
 * tokens.
 */
public class AutoindexTask implements TaskInterface {

  /**
   * The default URL for the Autoindex-endpoint.
   */
  public static final String DEFAULT_URL = "http://localhost:9091/search";
  private static final Logger log = LoggerFactory.getLogger(AutoindexTask.class);

  private UriComponentsBuilder builder;
  private HttpMethod httpMethod;

  /**
   * Constructs this task with the default URL.
   */
  public AutoindexTask() {
    this(DEFAULT_URL);
  }

  /**
   * Constructs this task with the given URL.
   */
  public AutoindexTask(String url) {
    this.builder = UriComponentsBuilder.fromHttpUrl(url);
    this.httpMethod = HttpMethod.POST;
  }

  /**
   * Produces tokens via using Autoindex to retrieve URIs. These tokens are merged with the given
   * tokens. The result will be returned.
   *
   * @param question question asked by the user
   * @param tokens tokens from other tasks, which will be merged with the tokens produces by this
   * class
   * @return merged tokens with URL-information produces by Autoindex
   */
  @Override
  public List<Token> processTokens(String question, List<Token> tokens) {
    List<Token> autoindexToken = produceTokens(question);
    log.debug("Candidate Mapping Produced: {}", autoindexToken);
    TokenMerger tokenMerger = new TokenMerger();
    List<Token> finalTokens = new ArrayList<>(tokens);
    for (Token token : autoindexToken) {
      finalTokens = tokenMerger.integrateToken(finalTokens, token);
    }
    return finalTokens;
  }

  /**
   * Given a question, provides the candidates for all n-grams. In this process, the children will
   * be pruned if their parents have any URIs.
   *
   * @param question question for which the candidates should be found
   */
  private List<Token> produceTokens(String question) {
    Map<NGramEntryPosition, Set<String>> candidateMap = new HashMap<>();
    NGramHierarchy nGramHierarchy = new NGramHierarchy(question);
    HashMap<String, Set<String>> answerMap = getAnswerMapFromAutoindex(question);

    // first iteration: only add to candidateMap
    for (String nGram : answerMap.keySet()) {
      NGramEntryPosition entryPosition = nGramHierarchy.getPosition(nGram);
      if (entryPosition != null) {
        candidateMap.put(entryPosition, answerMap.get(nGram));
      }
    }

    // second iteration: delete children whose parents have URIs
    Map<NGramEntryPosition, Set<String>> prunedCandidateMap = new HashMap<>(candidateMap);
    for (NGramEntryPosition parent : candidateMap.keySet()) {
      for (NGramEntryPosition child : parent.getAllDescendants()) {
        log.debug("{} has parent {}, deleting child", child, parent);
        prunedCandidateMap.remove(child);
      }
    }

    // Map n-gram hierarchy into list of tokens
    List<Token> finalTokens = new ArrayList<>();
    List<NGramEntryPosition> tmpKeyList = new ArrayList<>(prunedCandidateMap.keySet());
    tmpKeyList.sort(Comparator.comparing(NGramEntryPosition::getPosition));
    for (NGramEntryPosition entry : tmpKeyList) {
      String nGram = nGramHierarchy.getNGram(entry);
      Token nGramToken = new Token(nGram);
      nGramToken.addUris(candidateMap.get(entry));
      finalTokens.add(nGramToken);
    }
    return finalTokens;
  }

  /**
   * Given a question, it requests the Autoindex-endpoint for a mapping. The response is parsed into
   * a HashMap.
   *
   * @return mapping of n-grams to a set of URIs
   */
  protected HashMap<String, Set<String>> getAnswerMapFromAutoindex(String question) {
    HttpEntity<String> response = getRestResponse(question);
    ObjectMapper mapper = new ObjectMapper();
    HashMap<String, Set<String>> answerMap = new HashMap<>();
    try {
      JsonNode root = mapper.readTree(response.getBody());
      JsonNode answerArray = root.path("results").path("bindings");
      for (JsonNode objNode : answerArray) {
        String label = objNode.path("label").path("value").toString().replace("\"", "");
        String uri = objNode.path("uri").path("value").toString().replace("\"", "");
        if (!answerMap.containsKey(label)) {
          answerMap.put(label, new HashSet<>());
        }
        Set<String> uris = answerMap.get(label);
        uris.add(uri);
        answerMap.put(label, uris);
      }
    } catch (IOException ioE) {
      log.error("Answer of request is not JSON-formatted!");
    }
    log.info("{}", answerMap);
    return answerMap;
  }

  private HttpEntity<String> getRestResponse(String request) {
    RestTemplate restTemplate = new RestTemplate();
    HttpHeaders headers = setHeaders();
    headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
    String body = buildJSON(request);
    HttpEntity<?> entity = new HttpEntity<>(body, headers);
    return restTemplate.exchange(
      builder.toUriString(), //URL
      httpMethod,
      entity,
      String.class);
  }

  private HttpHeaders setHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
    return headers;
  }

  private String buildJSON(String request) {
    JSONObject json = new JSONObject();
    json.put("type", "LABEL");
    json.put("category", "ENTITY"); //change when autoindex is updated!!!
    json.put("query", request);
    return json.toString();
  }
}
