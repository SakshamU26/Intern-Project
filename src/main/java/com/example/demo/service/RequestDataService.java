package com.example.demo.service;

import com.example.demo.dao.RecordDataDAO;
import com.example.demo.dao.RequestDataDAO;
import com.example.demo.dao.SampleDataDAO;
import com.example.demo.dao.VersionDataDAO;
import com.example.demo.pojo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class RequestDataService {

    @Autowired
    private RecordDataDAO recordDataDAO;

    @Autowired
    private VersionDataDAO versionDataDAO;

    @Autowired
    private SampleDataDAO sampleDataDAO;

    @Autowired
    private RequestDataDAO requestDataDAO;

    @Autowired
    private OllamaService ollamaService;

    @Autowired
    private AsyncProcessingService asyncProcessingService;


    public List<RecordData> getAllData() {
        log.info("Fetching all record data...");
        return recordDataDAO.findAll();
    }

    public Optional<RecordData> getDataById(Long id) {
        log.debug("Fetching data by ID: {}", id);
        return recordDataDAO.findById(id);
    }

    public Optional<RecordData> getDataByTableNameAndColumnName(String tableName, String columnName) {
        log.debug("Fetching data for table '{}' and column '{}'", tableName, columnName);
        return recordDataDAO.findByTableNameAndColumnName(tableName,columnName);
    }

    public void updateDataByTableNameAndColumnName(String tableName, String columnName, RecordData tableData) {
        Optional<RecordData> curr = recordDataDAO.findByTableNameAndColumnName(tableName,columnName);

        curr.ifPresent(updated -> {

            versionDataDAO.deleteByRecordDataId(curr.get());

            updated.setColumnName(tableData.getColumnName());
            updated.setFriendlyColumnName(null);
            updated.setVersionCount(0);

            recordDataDAO.save(updated);
            log.info("Update successful for column '{}' in table '{}'", columnName, tableName);
        });
    }

    public void addData(RecordData tableData) {
        recordDataDAO.save(tableData);
    }

    public String updateData(Long id, RecordData tableData) {
        RecordData temp = recordDataDAO.getReferenceById(id);
        temp.setColumnName(tableData.getColumnName());
        temp.setTableName(tableData.getTableName());
        temp.setFriendlyColumnName("Updated");
        recordDataDAO.save(temp);
        return "Table Data with id " + id + " has been updated and saved successfully!";
    }

    public String deleteData(Long id) {
        recordDataDAO.deleteById(id);
        return "Table Data with id " + id + " has been deleted successfully!";
    }

    @Transactional
    public void deleteDataByTableNameAndColumnName(String tableName, String columnName) {
        log.warn("Deleting data for table '{}' and column '{}'", tableName, columnName);
        recordDataDAO.deleteByTableNameAndColumnName(tableName,columnName);

    }
    public String generateFriendlyColumnName(Long id, OllamaRequest request) {
        RecordData temp = recordDataDAO.getReferenceById(id);
        String fn = temp.getColumnName();
        request.setPrompt(fn);
        OllamaResponse response = ollamaService.selectBestSuggestion(request);
        return response.getResponse();
    }

    public void updateFriendlyColumnName(Long id, String response) {
        RecordData temp = recordDataDAO.getReferenceById(id);
        temp.setFriendlyColumnName(response);
        recordDataDAO.save(temp);
    }

    @Transactional
    public void saveInput(RequestDataDTO inputList) {
        log.info("Thread executing saveInput(): {}", Thread.currentThread().getName());
        if (inputList == null || inputList.getSampleRecords().isEmpty()) return;

        Long requestId = inputList.getRequestId();
        List<SampleRecordDTO> sampleRecords = inputList.getSampleRecords();
        List<RequestData> requestDataList = new ArrayList<>();
        List<SampleData> sampleDataList = new ArrayList<>();

        for (SampleRecordDTO record : sampleRecords) {
            String tableName = record.getTableName();
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

                    boolean exists = sampleDataDAO.sampleExists(
                            tableName,
                            entry.getKey(),
                            entry.getValue()
                    );

                    if(!exists) {
                        SampleData sampleData = new SampleData();
                        sampleData.setRequestId(requestId);
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
            requestDataDAO.saveAll(requestDataList);
        }

        if (!sampleDataList.isEmpty()) {
            sampleDataDAO.saveAll(sampleDataList);
        }

        log.info("Data has been stored in request_data and sample_data tables");

        asyncProcessingService.processDataAsync(inputList);

        log.info("Processing has finished");
    }


    public String generateFriendlyColumnNameByTableNameAndColumnName(String tableName,
                                                                   String columnName) {
        Optional<RecordData> curr = recordDataDAO.findByTableNameAndColumnName(tableName,columnName);
        if(curr.isPresent()) {
            RecordData recordData = curr.get();

            String prompt = "You are an expert in understanding Dutch Column names provided a specific context\n" +
                    "I want you to convert the below column name into an english Column name, taking help" +
                    "from the extra information provided to you:-\n" +
                    "Column Name: " + recordData.getColumnName() +"\n" +
                    "You need to give a 1 word answer containing the appropriate English Column Name";

            log.info("The prompt generated for column {} is as follows:-\n", prompt);
            OllamaRequest request = new OllamaRequest("gemma3:4b",prompt,false);

            OllamaResponse response = ollamaService.selectBestSuggestion(request);
            return response.getResponse();
        }
        else return "Column Name not present";
    }

    public void updateFriendlyColumnNameByTableNameAndColumnName(String tableName,
                                                                   String columnName,
                                                                   String response) {
        Optional<RecordData> curr = recordDataDAO.findByTableNameAndColumnName(tableName,columnName);
        if(curr.isPresent()) {
            RecordData recordData = curr.get();
            recordData.setFriendlyColumnName(response);
            recordDataDAO.save(recordData);
        }
    }
    public List<VersionData> retrieveVersionsByTableNameAndColumnName(String table_name, String column_name) {
        Optional<RecordData> curr = recordDataDAO.findByTableNameAndColumnName(table_name, column_name);
        if (!curr.isPresent()) {
            return Collections.emptyList();
        }
        return versionDataDAO.findByRecordDataId(curr.get());
    }
}
