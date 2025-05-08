package com.example.demo.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OllamaResponse {
    @JsonProperty("model")
    private String model;

    @JsonProperty("created_at")
    private String createdAt;

    @JsonProperty("response")
    private String response;

    @JsonProperty("done")
    private boolean done;

//    @JsonProperty("done_reason")
//    private String doneReason;
//
//    @JsonProperty("context")
//    private List<Integer> context;
//
//    @JsonProperty("load_duration")
//    private long loadDuration;
//
//    @JsonProperty("prompt_eval_count")
//    private int promptEvalCount;
//
//    @JsonProperty("prompt_eval_duration")
//    private long promptEvalDuration;
//
//    @JsonProperty("eval_count")
//    private int evalCount;
//
//    @JsonProperty("eval_duration")
//    private long evalDuration;
//
//    @JsonProperty("total_duration")
//    private long totalDuration;
}
