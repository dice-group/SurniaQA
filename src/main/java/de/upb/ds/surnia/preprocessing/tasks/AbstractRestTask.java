package de.upb.ds.surnia.preprocessing.tasks;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

public abstract class AbstractRestTask implements TaskInterface {

  private UriComponentsBuilder builder;
  private HttpMethod httpMethod;

  protected AbstractRestTask(String url, HttpMethod httpMethod){
    this.builder = UriComponentsBuilder.fromHttpUrl(url);
    this.httpMethod = httpMethod;

  }

  protected HttpEntity<String> getRestReponse(String request){
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

  protected abstract UriComponentsBuilder addParameters(String request, UriComponentsBuilder builder);

  protected abstract HttpHeaders setHeaders();

}
