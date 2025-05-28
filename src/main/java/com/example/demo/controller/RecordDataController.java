package com.example.demo.controller;

import com.example.demo.pojo.*;
import com.example.demo.service.OllamaService;
import com.example.demo.service.RequestDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/data")
public class RecordDataController {

    public static final String INVALID_INPUT_MSG = "Invalid table_name and/or column_name. Ensure both are valid";
    public static final String NOT_FOUND_MSG = "The specific table or column doesn't exist";
    public static final String RECORD_NOT_FOUND_MSG = "Record not found for the provided table and column names.";
    public static final String UPDATE_SUCCESS_MSG = "Column name %s having table name %s has been changed to %s";
    public static final String UPDATE_ERROR_MSG = "Failed to update data due to an internal error.";
    public static final String DB_RETRIEVE_ERROR = "An unexpected error occurred while retrieving data from the database";
    public static final String RAW_DATA_EMPTY_MSG = "Sample records are empty";
    public static final String DB_CONNECTION_ERROR_MSG = "Database connection error";
    public static final String DATA_STORED_SUCCESS_MSG = "Data stored successfully!";
    public static final String FRIENDLY_NAME_SUCCESS_MSG = "Friendly column name for the column name %s has been generated and stored in database";
    public static final String VERSIONS_NOT_FOUND_MSG = "Versions for %s column with table name %s not found";

    @Autowired
    private RequestDataService requestDataService;

    @Autowired
    private OllamaService ollamaService;

    @GetMapping
    public ResponseEntity<?> getAllData() {
        try {
            return ResponseEntity.ok(requestDataService.getAllData());
        } catch (Exception e) {
            return ResponseEntity.status(500).body(DB_RETRIEVE_ERROR);
        }
    }

    @GetMapping("/{id}")
    public Optional<RecordData> getDataById(@PathVariable Long id) {
        return requestDataService.getDataById(id);
    }

    @GetMapping("/search")
    public ResponseEntity<?> getDataByTableNameAndColumnName(@RequestParam String table_name,
                                                                @RequestParam String column_name) {
        if (table_name == null || table_name.trim().isEmpty() ||
                column_name == null || column_name.trim().isEmpty()) {
            return ResponseEntity.status(400).body(INVALID_INPUT_MSG);
        }

        Optional<RecordData> data = requestDataService.getDataByTableNameAndColumnName(table_name,column_name);
        return ResponseEntity.ok(data.get());
    }

    @PostMapping
    public void addData(@RequestBody RecordData tableData) {
        requestDataService.addData(tableData);
    }


    @PutMapping("/search")
    public ResponseEntity<String> updateDataByTableNameAndColumnName(@RequestParam String table_name,
                                                     @RequestParam String column_name,
                                                     @RequestBody RecordData tableData) {
        if (table_name == null || table_name.trim().isEmpty() ||
                column_name == null || column_name.trim().isEmpty()) {
            return ResponseEntity.status(400).body(INVALID_INPUT_MSG);
        }
        try {
            requestDataService.updateDataByTableNameAndColumnName(table_name, column_name, tableData);
            return ResponseEntity.ok(String.format(UPDATE_SUCCESS_MSG, column_name, table_name, tableData.getColumnName()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(UPDATE_ERROR_MSG);
        }
    }


    @DeleteMapping("/search")
    public ResponseEntity<String> deleteDataByTableNameAndColumnName(@RequestParam String table_name,
                                                     @RequestParam String column_name) {
        if (table_name == null || table_name.trim().isEmpty() ||
                column_name == null || column_name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(INVALID_INPUT_MSG);
        }
        boolean exists = requestDataService.getDataByTableNameAndColumnName(table_name,column_name).isPresent();

        if(!exists) {
            return ResponseEntity.status(404).body(NOT_FOUND_MSG);
        }

        requestDataService.deleteDataByTableNameAndColumnName(table_name,column_name);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/raw")
    public ResponseEntity<String> inputRawData(@RequestBody RequestDataDTO input) {
        if (input.getSampleRecords() == null || input.getSampleRecords().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, RAW_DATA_EMPTY_MSG);
        }
        try {
            requestDataService.saveInput(input);
            return ResponseEntity.status(HttpStatus.CREATED).body(DATA_STORED_SUCCESS_MSG);
        } catch (DataAccessException e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, DB_CONNECTION_ERROR_MSG);
        }
    }


    @PutMapping("/generate")
    public ResponseEntity<String> generateFriendlyColumnNameByTableNameAndColumnName(@RequestParam String table_name,
                                                                                     @RequestParam String column_name) {

        if (table_name == null || table_name.trim().isEmpty() ||
                column_name == null || column_name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(INVALID_INPUT_MSG);
        }

        Optional<RecordData> rec = requestDataService.getDataByTableNameAndColumnName(table_name, column_name);
        if (!rec.isPresent()) {
            return ResponseEntity.status(404).body(RECORD_NOT_FOUND_MSG);
        }

        String response = requestDataService.generateFriendlyColumnNameByTableNameAndColumnName(table_name,column_name);
        requestDataService.updateFriendlyColumnNameByTableNameAndColumnName(table_name,column_name,response);

        return ResponseEntity.ok(String.format(FRIENDLY_NAME_SUCCESS_MSG, column_name));
    }

    @GetMapping("/versions")
    public ResponseEntity<?> retrieveVersionsByTableNameAndColumnName(@RequestParam String table_name,
                                                                      @RequestParam String column_name) {
        List<VersionData> versions = requestDataService.retrieveVersionsByTableNameAndColumnName(table_name,column_name);
        if(versions.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(String.format(VERSIONS_NOT_FOUND_MSG, column_name, table_name));
        }
        return ResponseEntity.ok(versions);
    }
}
