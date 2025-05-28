package com.example.demo.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SampleRecordDTO {

    @JsonProperty("table_name")
    private String tableName;

    @JsonProperty("description")
    private String description;

    @JsonProperty("data")
    private List<Map<String, String>> data;
}
