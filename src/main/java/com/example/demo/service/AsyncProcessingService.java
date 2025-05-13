package com.example.demo.service;

import com.example.demo.pojo.RecordData;
import com.example.demo.pojo.RequestDataDTO;
import com.example.demo.pojo.SampleRecordDTO;
import com.example.demo.repository.RecordDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class AsyncProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(AsyncProcessingService.class);

    @Autowired
    private OllamaService ollamaService;

    @Autowired
    private StoreAndFetchData storeAndFetchData;

    @Autowired
    private RecordDataRepository recordDataRepository;

    @Async
    public void processDataAsync(RequestDataDTO requestData) {
        logger.info("Thread executing processDataAsync(): {}", Thread.currentThread().getName());
        Long requestId = requestData.getRequest_id();
        List<SampleRecordDTO> sampleRecords = requestData.getSample_records();
        List<RecordData> processedRecords = new ArrayList<>();

        for(SampleRecordDTO record : sampleRecords) {
            String tableName = record.getTable_name();
            String description = record.getDescription();

            Map<String, List<String>> columnValues = new HashMap<>();
            for (Map<String, String> row : record.getData()) {
                for (Map.Entry<String, String> entry : row.entrySet()) {
                    columnValues
                            .computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
                            .add(entry.getValue());
                }
            }
            Map<String,String> columnsToFriendlyColumns = ollamaService.callToOllama(tableName, description, columnValues);
            storeAndFetchData.saveToRecordDataAndVersionData(columnsToFriendlyColumns, tableName);
        }
        recordDataRepository.saveAll(processedRecords);
        storeAndFetchData.sendEmail(requestId);
    }
}
