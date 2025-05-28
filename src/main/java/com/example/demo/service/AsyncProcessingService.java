package com.example.demo.service;

import com.example.demo.pojo.RequestDataDTO;
import com.example.demo.pojo.SampleRecordDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class AsyncProcessingService {

    @Autowired
    private OllamaService ollamaService;

    @Autowired
    private StoreAndFetchData storeAndFetchData;

    @Async
    public void processDataAsync(RequestDataDTO requestData) {
        log.info("Thread executing processDataAsync(): {}", Thread.currentThread().getName());
        Long requestId = requestData.getRequestId();
        List<SampleRecordDTO> sampleRecords = requestData.getSampleRecords();

        for(SampleRecordDTO record : sampleRecords) {
            String tableName = record.getTableName();
            String description = record.getDescription();

            Map<String, List<String>> columnValues = new HashMap<>();
            for (Map<String, String> row : record.getData()) {
                for (Map.Entry<String, String> entry : row.entrySet()) {
                    columnValues
                            .computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                            .add(entry.getValue());
                }
            }
            Map<String,String> columnsToFriendlyColumns = ollamaService.generateFriendlyColumnNameUsingGenAi(tableName, description, columnValues);
            storeAndFetchData.saveToRecordDataAndVersionData(columnsToFriendlyColumns, tableName);
        }
        storeAndFetchData.sendEmail(requestId);
    }
}
