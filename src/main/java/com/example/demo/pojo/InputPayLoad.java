package com.example.demo.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InputPayLoad {
    private String table_name;
    private List<ColumnData> columns;
    private List<Map<String,String>> sample_data;
}
