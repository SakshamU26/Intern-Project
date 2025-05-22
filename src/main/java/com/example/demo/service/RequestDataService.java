package com.example.demo.service;

import com.example.demo.pojo.*;
import com.example.demo.repository.RecordDataRepository;
import com.example.demo.repository.RequestDataRepository;
import com.example.demo.repository.SampleDataRepository;
import com.example.demo.repository.VersionDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RequestDataService {

    private static final Logger logger = LoggerFactory.getLogger(RequestDataService.class);

    @Autowired
    private RecordDataRepository recordDataRepository;

    @Autowired
    private VersionDataRepository versionDataRepository;

    @Autowired
    private SampleDataRepository sampleDataRepository;

    @Autowired
    private RequestDataRepository requestDataRepository;

    @Autowired
    private OllamaService ollamaService;

    @Autowired
    private AsyncProcessingService asyncProcessingService;


    public List<RecordData> getAllData() {
        logger.info("Fetching all record data...");
        return recordDataRepository.findAll();
    }

    public Optional<RecordData> getDataById(Long id) {
        logger.debug("Fetching data by ID: {}", id);
        return recordDataRepository.findById(id);
    }

    public Optional<RecordData> getDataByTableNameAndColumnName(String tableName, String columnName) {
        logger.debug("Fetching data for table '{}' and column '{}'", tableName, columnName);
        return recordDataRepository.findByTableNameAndColumnName(tableName,columnName);
    }

    public void updateDataByTableNameAndColumnName(String tableName, String columnName, RecordData tableData) {
        Optional<RecordData> curr = recordDataRepository.findByTableNameAndColumnName(tableName,columnName);

        curr.ifPresent(updated -> {

            versionDataRepository.deleteByRecordDataId(curr.get());

            updated.setColumnName(tableData.getColumnName());
            updated.setFriendlyColumnName(null);
            updated.setVersionCount(0);

            recordDataRepository.save(updated);
            logger.info("Update successful for column '{}' in table '{}'", columnName, tableName);
        });
    }

    public void addData(RecordData tableData) {
        recordDataRepository.save(tableData);
    }

    public String updateData(Long id, RecordData tableData) {
        RecordData temp = recordDataRepository.getReferenceById(id);
        temp.setColumnName(tableData.getColumnName());
        temp.setTableName(tableData.getTableName());
        temp.setFriendlyColumnName("Updated");
        recordDataRepository.save(temp);
        return "Table Data with id " + id + " has been updated and saved successfully!";
    }

    public String deleteData(Long id) {
        recordDataRepository.deleteById(id);
        return "Table Data with id " + id + " has been deleted successfully!";
    }

    @Transactional
    public void deleteDataByTableNameAndColumnName(String tableName, String columnName) {
        logger.warn("Deleting data for table '{}' and column '{}'", tableName, columnName);
        recordDataRepository.deleteByTableNameAndColumnName(tableName,columnName);

    }
    public String generateFriendlyColumnName(Long id, OllamaRequest request) {
        RecordData temp = recordDataRepository.getReferenceById(id);
        String fn = temp.getColumnName();
        request.setPrompt(fn);
        OllamaResponse response = ollamaService.communicateWithOllama(request);
        return response.getResponse();
    }

    public void updateFriendlyColumnName(Long id, String response) {
        RecordData temp = recordDataRepository.getReferenceById(id);
        temp.setFriendlyColumnName(response);
        recordDataRepository.save(temp);
    }

    @Transactional
    public void saveInput(RequestDataDTO inputList) {
        logger.info("Thread executing saveInput(): {}", Thread.currentThread().getName());
        if (inputList == null || inputList.getSample_records().isEmpty()) return;

        Long requestId = inputList.getRequest_id();
        List<SampleRecordDTO> sampleRecords = inputList.getSample_records();
        List<RequestData> requestDataList = new ArrayList<>();
        List<SampleData> sampleDataList = new ArrayList<>();

        for (SampleRecordDTO record : sampleRecords) {
            String tableName = record.getTable_name();
            String description = record.getDescription();

            for (Map<String, String> row : record.getData()) {
                for (Map.Entry<String, String> entry : row.entrySet()) {
                    RequestData requestData = new RequestData();
                    requestData.setRequestId(requestId);
                    requestData.setTableName(tableName);
                    requestData.setColumnName(entry.getKey());
                    requestData.setValue(entry.getValue());
                    requestData.setDescription(description);

                    requestDataList.add(requestData);

                    boolean exists = sampleDataRepository.existsByTableNameAndColumnNameAndExampleData(
                            tableName,
                            entry.getKey(),
                            entry.getValue()
                    );

                    if(!exists) {
                        SampleData sampleData = new SampleData();
                        sampleData.setRequest_id(requestId);
                        sampleData.setDescription(description);
                        sampleData.setTableName(tableName);
                        sampleData.setColumnName(entry.getKey());
                        sampleData.setExampleData(entry.getValue());
                        sampleData.setStoredAt(LocalDateTime.now());

                        sampleDataList.add(sampleData);
                    }
                }
            }
        }

        if (!requestDataList.isEmpty()) {
            requestDataRepository.saveAll(requestDataList);
        }

        if (!sampleDataList.isEmpty()) {
            sampleDataRepository.saveAll(sampleDataList);
        }

        logger.info("Data has been stored in request_data and sample_data tables");

        asyncProcessingService.processDataAsync(inputList);

        logger.info("Processing has finished");
    }


    public String generateFriendlyColumnNameByTableNameAndColumnName(String tableName,
                                                                   String columnName) {
        Optional<RecordData> curr = recordDataRepository.findByTableNameAndColumnName(tableName,columnName);
        if(curr.isPresent()) {
            RecordData recordData = curr.get();

            String prompt = "You are an expert in understanding Dutch Column names provided a specific context\n" +
                    "I want you to convert the below column name into an english Column name, taking help" +
                    "from the extra information provided to you:-\n" +
                    "Column Name: " + recordData.getColumnName() +"\n" +
                    "You need to give a 1 word answer containing the appropriate English Column Name";

            logger.info("The prompt generated for column {} is as follows:-\n", prompt);
            OllamaRequest request = new OllamaRequest("llama3.2",prompt,false);

            OllamaResponse response = ollamaService.selectBestSuggestionWithOllama(request);
            return response.getResponse();
        }
        else return "Column Name not present";
    }

    public void updateFriendlyColumnNameByTableNameAndColumnName(String tableName,
                                                                   String columnName,
                                                                   String response) {
        Optional<RecordData> curr = recordDataRepository.findByTableNameAndColumnName(tableName,columnName);
        if(curr.isPresent()) {
            RecordData recordData = curr.get();
            recordData.setFriendlyColumnName(response);
            recordDataRepository.save(recordData);
        }
    }
    public List<VersionData> retrieveVersionsByTableNameAndColumnName(String table_name, String column_name) {
        Optional<RecordData> curr = recordDataRepository.findByTableNameAndColumnName(table_name, column_name);
        if (!curr.isPresent()) {
            return Collections.emptyList();
        }
        return versionDataRepository.findByRecordDataId(curr.get());
    }
}
