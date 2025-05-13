package com.example.demo.service;

import com.example.demo.pojo.OllamaRequest;
import com.example.demo.pojo.OllamaResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OllamaService {

    private static final Logger logger = LoggerFactory.getLogger(RequestDataService.class);

    @Autowired StoreAndFetchData storeAndFetchData;

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

    public String createPrompt(String tableName, String description,
                               Map<String,List<String>> columnValues) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("I am giving you some column names along with their possible example data ")
                .append("from the table named: ").append(tableName)
                .append(" and the description of the table being: ").append(description).append("\n")
                .append(" The column names are short forms of Dutch words.")
                .append(" You may be able to understand the meanings from table name and description.")
                .append(" Return a mapping of each column name to an English Column name")
                .append(" of what you think best describes the column name according to the given context.")
                .append(" Use the context of all sample values along with table name and ")
                .append("description of the table, for each column to determine the meaning.")
                .append(" Only return one friendly name per column ")
                .append("in the format 'column name : translated column name' ")
                .append("for each unique column name in new line and nothing else.\n\n")
                .append("The column names with their example values are:- \n\n");

        for (Map.Entry<String, List<String>> entry : columnValues.entrySet()) {
            String column = entry.getKey();
            List<String> examples = entry.getValue();
            prompt.append("Column: ").append(column).append("\n");
            prompt.append("Example Values:-\n");
            for (String example : examples) {
                prompt.append(" - ").append(example).append("\n");
            }
            prompt.append("\n");
        }

        Map<String,List<String>> sampleContext = storeAndFetchData.getPreviousSamples(tableName);

        if(!sampleContext.isEmpty()) {
            prompt.append("Some more context about the table and its columns are:- \n");

            for(Map.Entry<String,List<String>> entry : sampleContext.entrySet()) {
                String column = entry.getKey();;
                List<String> values = entry.getValue();
                prompt.append("Column: ").append(column).append("\n");
                prompt.append("Sample Data:-\n");
                for(String val : values) {
                    prompt.append(" - ").append(val).append("\n");
                }
                prompt.append("\n");
            }
        }

        prompt.append("Take reference from the table name and its description ")
                .append("and you do not need to explain anything else, ")
                .append("just the list of column names with their translated column names\n");

        logger.info(prompt.toString());
        return prompt.toString();
    }

    public Map<String,String> callToOllama(String tableName, String description,
                                           Map<String, List<String>> columnValues) {
        String prompt = createPrompt(tableName, description, columnValues);
        OllamaRequest request = new OllamaRequest("llama3.2",prompt,false);
        OllamaResponse response = selectBestSuggestionWithOllama(request);

        logger.info("\n\n" + response.getResponse());

        return parseResponse(response.getResponse());
    }
    public Map<String,String> parseResponse(String response) {
        Map<String,String> translationMap = new HashMap<>();

        String[] lines = response.split("\\r?\\n");

        for (String line : lines) {
            if (line.trim().isEmpty()) continue;

            String[] parts = line.split(":", 2);
            if (parts.length == 2) {
                String key = parts[0].trim();
                String value = parts[1].trim();
                translationMap.put(key, value);
            }
        }

        return translationMap;
    }
}
