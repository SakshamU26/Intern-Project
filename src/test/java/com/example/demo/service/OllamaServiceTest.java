package com.example.demo.service;

import com.example.demo.pojo.OllamaRequest;
import com.example.demo.pojo.OllamaResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OllamaServiceTest {

    private static final String API_URL = "http://localhost:11434/api/generate";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private StoreAndFetchData storeAndFetchData;

    @InjectMocks
    private OllamaService ollamaService;

    @BeforeEach
    void setUp() {
        ollamaService = new OllamaService(storeAndFetchData, API_URL, restTemplate, objectMapper);
    }

    @Test
    void testSelectBestSuggestionWithOllama_Success() throws Exception {
        OllamaRequest request = new OllamaRequest("modelName", "prompt", false);
        String responseJson = "{\"response\":\"best suggestion\"}";
        OllamaResponse expectedResponse = new OllamaResponse();
        expectedResponse.setResponse("best suggestion");

        when(restTemplate.exchange(eq(API_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(responseJson, HttpStatus.OK));

        when(objectMapper.readValue(responseJson, OllamaResponse.class))
                .thenReturn(expectedResponse);

        OllamaResponse actualResponse = ollamaService.selectBestSuggestionWithOllama(request);

        assertEquals("best suggestion", actualResponse.getResponse());
        verify(restTemplate).exchange(eq(API_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
        verify(objectMapper).readValue(responseJson, OllamaResponse.class);
    }

    @Test
    void testCallToOllama_ReturnsExpectedMap() throws Exception {
        String tableName = "TestTable";
        String description = "Sample table description";

        Map<String, List<String>> columnValues = new HashMap<>();
        columnValues.put("kolom1", Arrays.asList("value1", "value2"));
        columnValues.put("kolom2", Arrays.asList("value3", "value4"));

        Map<String, String> expected = new HashMap<>();
        expected.put("kolom1", "column1");
        expected.put("kolom2", "column2");

        String mockResponseString = "kolom1 : column1\nkolom2 : column2";
        String rawJson = "{\"response\":\"kolom1 : column1\\nkolom2 : column2\"}";

        when(storeAndFetchData.getPreviousSamples(tableName)).thenReturn(Collections.emptyMap());

        OllamaResponse mockResponse = new OllamaResponse();
        mockResponse.setResponse(mockResponseString);

        when(restTemplate.exchange(eq(API_URL), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(new ResponseEntity<>(rawJson, HttpStatus.OK));

        when(objectMapper.readValue(rawJson, OllamaResponse.class)).thenReturn(mockResponse);

        Map<String, String> result = ollamaService.callToOllama(tableName, description, columnValues);

        assertEquals(expected, result);
    }
}
