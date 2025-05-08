package com.example.demo.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OllamaRequest {
    @JsonProperty("model")
    private String model;

    @JsonProperty("prompt")
    private String prompt;

    @JsonProperty("stream")
    private boolean stream = false;

//    @JsonProperty("context")
//    private List<Integer> context;
//
//    @JsonProperty("options")
//    private Map<String, Object> options;
//
//    @JsonProperty("format")
//    private String format;
//
//    @JsonProperty("raw")
//    private boolean raw = false;
}
