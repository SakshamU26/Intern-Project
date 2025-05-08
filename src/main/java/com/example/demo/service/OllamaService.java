package com.example.demo.service;

import com.example.demo.pojo.OllamaRequest;
import com.example.demo.pojo.OllamaResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class OllamaService {

    @Value("${ollama.api.url:http://localhost:11434/api/generate}")
    private String apiUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OllamaService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public OllamaResponse communicateWithOllama(OllamaRequest request) {
        try {
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//            headers.setAccept(Arrays.asList(MediaType.APPLICATION_JSON));
            String prePrompt = "You are an expert in understanding Dutch Column names provided a specific context\n" +
                    "I want you to convert the below column name into an english Column name ";
            String postPrompt = "You need to make use of all the given context to create an " +
                    "appropriate English column name. Focus more on the description than just the data." +
                    "You only need to give a 1 word response containing the actual translated " +
                    "Column name which is very descriptive and you don't need to explain anything else";
            String dynamicPrompt = request.getPrompt();
            request.setPrompt(prePrompt + dynamicPrompt + postPrompt);
            HttpEntity<OllamaRequest> entity = new HttpEntity<>(request);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            String jsonResponse = response.getBody();
            return objectMapper.readValue(jsonResponse, OllamaResponse.class);

//            if (response.getStatusCode() != HttpStatus.OK) {
//                throw new RestClientException("Ollama API returned status: " + response.getStatusCode());
//            }
//

//            if (jsonResponse == null || jsonResponse.isEmpty()) {
//                throw new RestClientException("Empty response from Ollama API");
//            }

//        } catch (RestClientException ex) {
//            System.err.println("Error communicating with Ollama API: " + ex.getMessage());
//            throw ex;
        } catch (Exception ex) {
            System.err.println("Unexpected error: " + ex.getMessage());
            throw new RuntimeException("Failed to process Ollama API response", ex);
       }
    }

//    public OllamaResponse translateRawDataWithOllama(OllamaRequest request) {
//        try {
//            HttpEntity<OllamaRequest> entity = new HttpEntity<>(request);
//            ResponseEntity<String> response = restTemplate.exchange(
//                    apiUrl,
//                    HttpMethod.POST,
//                    entity,
//                    String.class
//            );
//            String jsonResponse = response.getBody();
//            return objectMapper.readValue(jsonResponse, OllamaResponse.class);
//        } catch (Exception ex) {
//            System.err.println("Unexpected error: " + ex.getMessage());
//            throw new RuntimeException("Failed to process Ollama API response", ex);
//        }
//    }

    public OllamaResponse selectBestSuggestionWithOllama(OllamaRequest request) {
        try {
            HttpEntity<OllamaRequest> entity = new HttpEntity<>(request);

            ResponseEntity<String> response = restTemplate.exchange(
                    apiUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );
            String jsonResponse = response.getBody();
            return objectMapper.readValue(jsonResponse, OllamaResponse.class);
        } catch (Exception ex) {
            System.err.println("Unexpected error: " + ex.getMessage());
            throw new RuntimeException("Failed to process Ollama API response", ex);
        }
    }
}
