package com.example.demo.service;

import com.example.demo.pojo.OllamaRequest;
import com.example.demo.pojo.OllamaResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class OllamaService {

    private final StoreAndFetchData storeAndFetchData;
    private final String apiUrl;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public OllamaService(StoreAndFetchData storeAndFetchData,
                         @Value("${ollama.api.url:http://localhost:11434/api/generate}") String apiUrl,
                         RestTemplate restTemplate,
                         ObjectMapper objectMapper) {
        this.storeAndFetchData = storeAndFetchData;
        this.apiUrl = apiUrl;
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public OllamaResponse communicateWithOllama(OllamaRequest request) {
        try {
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

        } catch (Exception ex) {
            System.err.println("Unexpected error: " + ex.getMessage());
            throw new RuntimeException("Failed to process Ollama API response", ex);
       }
    }

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

    private String createPrompt(String tableName, String description,
                               Map<String,List<String>> columnValues) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are an expert in the Dutch language, Dutch data systems, and column naming conventions in databases.\n")
                .append("I am giving you some column names that are short forms of Dutch words, along with their example values.\n")
                .append("These columns come from the table named: ").append(tableName).append(", ")
                .append("which is described as: ").append(description).append("\n\n")
                .append("Your task is to deduce the full Dutch word or phrase each column name represents, ")
                .append("based on both the column name itself and the context provided by its sample values, ")
                .append("and then translate that Dutch meaning into a clear and friendly English column name.\n")
                .append("Use your expertise and knowledge of Dutch terminology and abbreviations commonly used in data systems.\n")
                .append("Make sure the English column name clearly explains the intended meaning of the original column.\n")
                .append("Context is crucial — use the full context of table name, table description, and sample values.\n")
                .append("Also note that no 2 columns with different column names should have same translated column name.\n\n")
                .append("You must return exactly one English name for each column, formatted as:\n")
                .append("column_name : translated_column_name\n")
                .append("List each column mapping on a new line. Do not add any explanation, commentary, or formatting other than the mapping.\n\n")
                .append("Here are the column names with their sample values:\n\n");

        for (Map.Entry<String, List<String>> entry : columnValues.entrySet()) {
            String column = entry.getKey();
            List<String> examples = entry.getValue();
            prompt.append("Column: ").append(column).append("\n");
            prompt.append("Example Values:\n");
            for (String example : examples) {
                prompt.append(" - ").append(example).append("\n");
            }
            prompt.append("\n");
        }

        Map<String, List<String>> sampleContext = storeAndFetchData.getPreviousSamples(tableName);

        if (!sampleContext.isEmpty()) {
            prompt.append("Here are additional past sample values for more context:\n\n");
            prompt.append(addSampleData(sampleContext));
        }

        prompt.append("\nPlease return only the column mappings. Do not include explanations or anything other than the mapping.\n")
                .append("You are confident and precise, and you excel at interpreting Dutch abbreviations in technical contexts.\n");

        log.info(prompt.toString());
        return prompt.toString();

    }

    public Map<String,String> callToOllama(String tableName, String description,
                                           Map<String, List<String>> columnValues) {
        String prompt = createPrompt(tableName, description, columnValues);
        OllamaRequest request = new OllamaRequest("gemma3:4b",prompt,false);
        OllamaResponse response = selectBestSuggestionWithOllama(request);

        log.info("\n\n" + response.getResponse());

        return parseResponse(response.getResponse());
    }

    private Map<String,String> parseResponse(String response) {
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

    private StringBuilder addSampleData(Map<String,List<String>> sampleContext) {
        StringBuilder samplePrompt = new StringBuilder();
        samplePrompt.append("Some more context about the table and its columns are:- \n");

        for(Map.Entry<String,List<String>> entry : sampleContext.entrySet()) {
            String column = entry.getKey();;
            List<String> values = entry.getValue();
            samplePrompt.append("Column: ").append(column).append("\n");
            samplePrompt.append("Sample Data:-\n");
            for(String val : values) {
                samplePrompt.append(" - ").append(val).append("\n");
            }
            samplePrompt.append("\n");
        }
        return samplePrompt;
    }

}
