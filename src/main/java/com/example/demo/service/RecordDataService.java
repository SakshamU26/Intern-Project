package com.example.demo.service;

import com.example.demo.pojo.*;
import com.example.demo.repository.RecordDataRepository;
import com.example.demo.repository.SampleDataRepository;
import com.example.demo.repository.VersionDataRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.var;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class RecordDataService {

    private static final Logger logger = LoggerFactory.getLogger(RecordDataService.class);

    @Autowired
    private RecordDataRepository recordDataRepository;

    @Autowired
    private VersionDataRepository versionDataRepository;

    @Autowired
    private SampleDataRepository sampleDataRepository;

    @Autowired
    private OllamaService ollamaService;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private ObjectMapper objectMapper;

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
            updated.setColumnName(tableData.getColumnName());
            updated.setFriendlyColumnName(null);
            updated.setVersionCount(0);
            updated.setVersionSelected(-1);

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
        temp.setData(tableData.getData());
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

    public void saveInput(List<InputPayLoad> payLoadList) {
        List<RecordData> entries = new ArrayList<>();
        List<SampleData> sampleEntries = new ArrayList<>();

        for (InputPayLoad payLoad : payLoadList) {
            String tableName = payLoad.getTable_name();

            for (ColumnData col : payLoad.getColumns()) {
                RecordData record = new RecordData();
                record.setTableName(tableName);
                record.setColumnName(col.getColumn_name());
                record.setDescription(col.getDescription());
                record.setData(col.getData());
                record.setFriendlyColumnName(null);
                record.setVersionCount(0);
                entries.add(record);
            }

            if (payLoad.getSample_data() != null) {
                for (Map<String, String> sampleRow : payLoad.getSample_data()) {
                    for (Map.Entry<String, String> entry : sampleRow.entrySet()) {
                        String columnName = entry.getKey();
                        String sampleValue = entry.getValue();

                        boolean existingSample = sampleDataRepository.existsByTableNameAndColumnNameAndSampleRow
                                (tableName, columnName, sampleValue);

                        if (!existingSample) {
                            SampleData sampleData = new SampleData();
                            sampleData.setTableName(tableName);
                            sampleData.setColumnName(columnName);
                            sampleData.setSampleRow(sampleValue);

                            sampleEntries.add(sampleData);
                        }
                    }
                }
            }
        }

        recordDataRepository.saveAll(entries);
        logger.info("Saved {} record_data entries", entries.size());

        sampleDataRepository.saveAll(sampleEntries);
        logger.info("Saved {} sample_data entries", sampleEntries.size());
    }



    public String generateFriendlyColumnNameByTableNameAndColumnName(String tableName,
                                                                   String columnName) {
        Optional<RecordData> curr = recordDataRepository.findByTableNameAndColumnName(tableName,columnName);
        if(curr.isPresent()) {
            RecordData recordData = curr.get();
            String cname = recordData.getColumnName();
            OllamaRequest request = new OllamaRequest("llama3.2",cname,false);

            OllamaResponse response = ollamaService.communicateWithOllama(request);
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

    public void sendEmail(List<ProcessedColumn> processedColumns) {
        try {
            Map<String, List<String>> tableToColumns = new LinkedHashMap<>();

            for (ProcessedColumn col : processedColumns) {
                tableToColumns
                        .computeIfAbsent(col.getTableName(), k -> new ArrayList<>())
                        .add(col.getColumnName());
            }

            StringBuilder message = new StringBuilder("The following column names have been translated:\n\n");

            for (Map.Entry<String, List<String>> entry : tableToColumns.entrySet()) {
                message.append("🔹 Table: ").append(entry.getKey()).append("\n");
                for (String column : entry.getValue()) {
                    message.append("    - ").append(column).append("\n");
                }
                message.append("\n");
            }

            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo("nolola5447@bocapies.com");
            msg.setSubject("Job Complete!");
            msg.setText(message.toString());

            mailSender.send(msg);
        } catch (Exception e) {
            logger.error("Failed to send email: {}", e.getMessage(), e);
        }
    }

    public List<VersionData> retrieveVersionsByTableNameAndColumnName(String table_name, String column_name) {
        Optional<RecordData> curr = recordDataRepository.findByTableNameAndColumnName(table_name, column_name);
        if (!curr.isPresent()) {
            return Collections.emptyList();
        }
        return versionDataRepository.findByRecordDataId(curr.get());
    }

//    public String inputRawData(String rawData) {
//        String prompt = String.format(
//                "You are given tab-separated raw data. Each line represents one record.\n" +
//                "Convert each line into a separate JSON object with the following keys: "  +
//                "table_name, column_name, data, and friendly_column_name, where friendly_column_name" +
//                "is null initially, field_name is in Dutch language, and data should be NA\n\n" + rawData +
//                "\nOnly give me the output JSON format and nothing else");
//
//        OllamaRequest request = new OllamaRequest("llama3.2",prompt,false);
//        OllamaResponse response = ollamaService.translateRawDataWithOllama(request);
//
//        List<RecordData> jsonData = parseResponse(response.getResponse());
//        recordDataRepository.saveAll(jsonData);
//
//        return "Data stored in table successfully!";
//    }
//
//    public List<RecordData> parseResponse(String responseText) {
//        ObjectMapper objectMapper = new ObjectMapper();
//        try {
//            return objectMapper.readValue(responseText, new TypeReference<List<RecordData>>() {});
//        } catch (Exception e) {
//            throw new RuntimeException("Error parsing Ollama response", e);
//        }
//    }
}
