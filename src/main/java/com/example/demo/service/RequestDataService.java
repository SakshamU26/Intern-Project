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

//    @Async
//    public void processDataAsync(RequestDataDTO requestData) {
//        logger.info("Thread executing processDataAsync(): {}", Thread.currentThread().getName());
//        Long requestId = requestData.getRequest_id();
//        List<SampleRecordDTO> sampleRecords = requestData.getSample_records();
//        List<RecordData> processedRecords = new ArrayList<>();
//
//        for(SampleRecordDTO record : sampleRecords) {
//            String tableName = record.getTable_name();
//            String description = record.getDescription();
//
//            Map<String, List<String>> columnValues = new HashMap<>();
//            for (Map<String, String> row : record.getData()) {
//                for (Map.Entry<String, String> entry : row.entrySet()) {
//                    columnValues
//                            .computeIfAbsent(entry.getKey(), k -> new ArrayList<>())
//                            .add(entry.getValue());
//                }
//            }
//            Map<String,String> columnsToFriendlyColumns = callToOllama(tableName, description, columnValues);
//            saveToRecordDataAndVersionData(columnsToFriendlyColumns, tableName);
//        }
//        recordDataRepository.saveAll(processedRecords);
//        sendEmail(requestId);
//    }

//    public Map<String,String> callToOllama(String tableName, String description,
//                                           Map<String,List<String>> columnValues) {
//        String prompt = createPrompt(tableName, description, columnValues); // map of column name and value
//        OllamaRequest request = new OllamaRequest("llama3.2",prompt,false);
//        OllamaResponse response = ollamaService.selectBestSuggestionWithOllama(request);
//
//        logger.info("\n\n" + response.getResponse());
//
//        return parseResponse(response.getResponse());
//    }

//    public String createPrompt(String tableName, String description,
//                               Map<String,List<String>> columnValues) {
//        StringBuilder prompt = new StringBuilder();
//
//        prompt.append("I am giving you some column names along with their possible example data ")
//                .append("from the table named: ").append(tableName)
//                .append(" and the description of the table being: ").append(description).append("\n")
//                .append(" The column names are short forms of Dutch words.")
//                .append(" You may be able to understand the meanings from table name and description.")
//                .append(" Return a mapping of each column name to an English Column name")
//                .append(" of what you think best describes the column name according to the given context.")
//                .append(" Use the context of all sample values along with table name and ")
//                .append("description of the table, for each column to determine the meaning.")
//                .append(" Only return one friendly name per column ")
//                .append("in the format 'column name : translated column name' ")
//                .append("for each unique column name in new line and nothing else.\n\n")
//                .append("The column names with their example values are:- \n\n");
//
//        for (Map.Entry<String, List<String>> entry : columnValues.entrySet()) {
//            String column = entry.getKey();
//            List<String> examples = entry.getValue();
//            prompt.append("Column: ").append(column).append("\n");
//            prompt.append("Example Values:-\n");
//            for (String example : examples) {
//                prompt.append(" - ").append(example).append("\n");
//            }
//            prompt.append("\n");
//        }
//
//        Map<String,List<String>> sampleContext = getPreviousSamples(tableName);
//
//        if(!sampleContext.isEmpty()) {
//            prompt.append("Some more context about the table and its columns are:- \n");
//
//            for(Map.Entry<String,List<String>> entry : sampleContext.entrySet()) {
//                String column = entry.getKey();;
//                List<String> values = entry.getValue();
//                prompt.append("Column: ").append(column).append("\n");
//                prompt.append("Sample Data:-\n");
//                for(String val : values) {
//                    prompt.append(" - ").append(val).append("\n");
//                }
//                prompt.append("\n");
//            }
//        }
//
//        prompt.append("Take reference from the table name and its description ")
//                .append("and you do not need to explain anything else, ")
//                .append("just the list of column names with their translated column names\n");
//
//        logger.info(prompt.toString());
//        return prompt.toString();
//    }

//    public Map<String, List<String>> getPreviousSamples(String tableName) {
//        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
//        List<SampleData> previousSamples = sampleDataRepository.findSamplesFromLastHour(tableName,cutoff);
//
//        Map<String, List<String>> previousColumnValues = new HashMap<>();
//
//        for (SampleData sample : previousSamples) {
//            previousColumnValues
//                    .computeIfAbsent(sample.getColumnName(), k -> new ArrayList<>())
//                    .add(sample.getExampleData());
//        }
//
//        return previousColumnValues;
//    }

//    public Map<String,String> parseResponse(String response) {
//        Map<String,String> translationMap = new HashMap<>();
//
//        String[] lines = response.split("\\r?\\n");
//
//        for (String line : lines) {
//            if (line.trim().isEmpty()) continue;
//
//            String[] parts = line.split(":", 2);
//            if (parts.length == 2) {
//                String key = parts[0].trim();
//                String value = parts[1].trim();
//                translationMap.put(key, value);
//            }
//        }
//
//        return translationMap;
//    }

//    @Transactional
//    public void saveToRecordDataAndVersionData(Map<String,String> columnsToFriendlyColumns, String tableName) {
//        for(Map.Entry<String,String> entry : columnsToFriendlyColumns.entrySet()) {
//            String column = entry.getKey();
//            String friendlyColumn = entry.getValue();
//
//            Optional<RecordData> existingRecord = recordDataRepository.findByTableNameAndColumnName(
//                    tableName,column);
//
//            if(existingRecord.isPresent()) {
//                RecordData prev = existingRecord.get();
//
//                VersionData version = new VersionData();
//                int currVersion = prev.getVersionCount();
//                version.setVersion(currVersion+1);
//                version.setRecordDataId(prev);
//                version.setFriendlyColumnNameVersion(prev.getFriendlyColumnName());
//
//                prev.setVersionCount(version.getVersion());
//                prev.setFriendlyColumnName(friendlyColumn);
//
//                versionDataRepository.save(version);
//                recordDataRepository.save(prev);
//            }
//            else {
//                RecordData recordData = new RecordData();
//                recordData.setColumnName(column);
//                recordData.setFriendlyColumnName(friendlyColumn);
//                recordData.setTableName(tableName);
//
//                recordDataRepository.save(recordData);
//            }
//        }
//    }

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

//    public void sendEmail(Long request_id) {
//        try {
//            String message = "The request with request ID " + request_id + " has been processed and " +
//                    "stored in the database successfully!";
//
//            SimpleMailMessage msg = new SimpleMailMessage();
//            msg.setTo("nolola5447@bocapies.com");
//            msg.setSubject("Job Complete!");
//            msg.setText(message);
//
//            mailSender.send(msg);
//        } catch (Exception e) {
//            logger.error("Failed to send email: {}", e.getMessage(), e);
//        }
//    }

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
