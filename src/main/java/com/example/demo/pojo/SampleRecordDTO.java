package com.example.demo.pojo;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class SampleRecordDTO {
    private String table_name;
    private String description;
    private List<Map<String, String>> data;
}
