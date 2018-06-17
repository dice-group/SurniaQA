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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public class AutoindexTask implements TaskInterface {

  private static final String DEFAULT_URL = "http://localhost:9091/search";
  private static final Logger log = LoggerFactory.getLogger(AutoindexTask.class);

  private UriComponentsBuilder builder;
  private HttpMethod httpMethod;

  public AutoindexTask() {
    this(DEFAULT_URL);
  }

  public AutoindexTask(String url) {
    this.builder = UriComponentsBuilder.fromHttpUrl(url);
    this.httpMethod = HttpMethod.POST;
  }

  @Override
  public List<Token> processTokens(String question, List<Token> tokens) {
    NGramHierarchy hierarchy = new NGramHierarchy(question);
    List<Token> autoindexToken = getCandidateMapping(hierarchy);
    log.debug("Candidate Mapping Produced: {}", autoindexToken);
    TokenMerger tokenMerger = new TokenMerger();
    List<Token> finalTokens = new ArrayList<>(tokens);
    for (Token token : autoindexToken) {
      finalTokens = tokenMerger.linkUri(finalTokens, token);
    }
    return finalTokens;
  }

  /**
   * Given a n-gram hierarchy, provides the candidates for all n-grams. In this process, the
   * children will also be pruned of candidates which already present in their parents.
   *
   * @param nGramHierarchy n-gram hierarchy, for which the candidates should be found
   */
  private List<Token> getCandidateMapping(
    NGramHierarchy nGramHierarchy) {
    Map<NGramEntryPosition, Set<String>> candidateMap = new HashMap<>();

    // first iteration: only add to candidateMap
    for (NGramEntryPosition nGramEntry : nGramHierarchy.getAllPositions()) {
      Set<String> nGramMappings;
      String nGram = nGramHierarchy.getNGram(nGramEntry);
      nGramMappings = askAutoindex(nGram);
      if (!nGramMappings.isEmpty()) {
        log.debug("Got '{}' for n-gram '{}'", nGramMappings, nGram);
      }
      if (!nGramMappings.isEmpty()) {
        candidateMap.put(nGramEntry, nGramMappings);
      }
    }

    // second iteration: delete children whose parents have URIs
    for (NGramEntryPosition parent : candidateMap.keySet()) {
      for (NGramEntryPosition child : parent.getAllDescendants()) {
        log.debug("{} has parent {}, deleting child", child, parent);
        candidateMap.remove(child);
      }
    }
    List<Token> finalTokens = new ArrayList<>();
    List<NGramEntryPosition> tmpKeyList = new ArrayList<>(candidateMap.keySet());
    tmpKeyList.sort(Comparator.comparing(NGramEntryPosition::getPosition));
    for (NGramEntryPosition entry : tmpKeyList) {
      String nGram = nGramHierarchy.getNGram(entry);
      Token nGramToken = new Token(nGram, null);
      nGramToken.addUris(candidateMap.get(entry));
      finalTokens.add(nGramToken);
    }
    return finalTokens;
  }

  protected Set<String> askAutoindex(String nGram) {
    HttpEntity<String> response = getRestResponse(nGram);
    ObjectMapper mapper = new ObjectMapper();
    Set<String> uris = new HashSet<>();
    try {
      JsonNode root = mapper.readTree(response.getBody());
      JsonNode answerArray = root.path("results").path("bindings");
      for (JsonNode objNode : answerArray) {
        uris.add(objNode.path("uri").path("value").toString());
      }
    } catch (IOException ioE) {
      log.error("Answer of request is not JSON-formatted!");
    }
    return uris;
  }

  private HttpEntity<String> getRestResponse(String request) {
    RestTemplate restTemplate = new RestTemplate();
    UriComponentsBuilder queryBuilder = addParameters(request, builder);
    HttpHeaders headers = setHeaders();
    headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
    HttpEntity<?> entity = new HttpEntity<>(headers);
    HttpEntity<String> response = restTemplate.exchange(
      queryBuilder.toUriString(),
      HttpMethod.POST,
      entity,
      String.class);
    return response;
  }

  private UriComponentsBuilder addParameters(String request, UriComponentsBuilder builder) {
    return builder.queryParam("type", "LABEL")
      .queryParam("category", "ALL")
      .queryParam("query", request);
  }

  private HttpHeaders setHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Content-Type", MediaType.APPLICATION_JSON_VALUE);
    return headers;
  }
}