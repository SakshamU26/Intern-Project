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

    @Autowired
    private RequestDataService requestDataService;

    @Autowired
    private OllamaService ollamaService;

    @GetMapping
    public ResponseEntity<?> getAllData() {
        try {
            return ResponseEntity.ok(requestDataService.getAllData());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("An unexpected error occurred while retrieving data from the database");
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
            return ResponseEntity.status(400).body("Invalid table_name and/or column_name. Ensure both are valid");
        }

        Optional<RecordData> data = requestDataService.getDataByTableNameAndColumnName(table_name,column_name);
        return ResponseEntity.ok(data.get());
    }

    @PostMapping
    public void addData(@RequestBody RecordData tableData) {
        requestDataService.addData(tableData);
    }

//    @PutMapping("/{id}")
//    public String updateData(@PathVariable Long id, @RequestBody RecordData tableData) {
//        return recordDataService.updateData(id,tableData);
//    }

    @PutMapping("/search")
    public ResponseEntity<String> updateDataByTableNameAndColumnName(@RequestParam String table_name,
                                                     @RequestParam String column_name,
                                                     @RequestBody RecordData tableData) {
        if (table_name == null || table_name.trim().isEmpty() ||
                column_name == null || column_name.trim().isEmpty()) {
            return ResponseEntity.status(400).body("Invalid table_name and/or column_name. Ensure both are valid");
        }
        try {
            requestDataService.updateDataByTableNameAndColumnName(table_name, column_name, tableData);
            return ResponseEntity.ok("Column name " + column_name +
                                            " having table name " + table_name +
                                            " has been changed to " + tableData.getColumnName());
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to update data due to an internal error.");
        }
    }

//    @DeleteMapping("/{id}")
//    public String deleteData(@PathVariable Long id) {
//        return recordDataService.deleteData(id);
//    }

    @DeleteMapping("/search")
    public ResponseEntity<String> deleteDataByTableNameAndColumnName(@RequestParam String table_name,
                                                     @RequestParam String column_name) {
        if (table_name == null || table_name.trim().isEmpty() ||
                column_name == null || column_name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid table_name and/or column_name. Ensure both are valid");
        }
        boolean exists = requestDataService.getDataByTableNameAndColumnName(table_name,column_name).isPresent();

        if(!exists) {
            return ResponseEntity.status(404).body("The specific table or column doesn't exist");
        }

        requestDataService.deleteDataByTableNameAndColumnName(table_name,column_name);
        return ResponseEntity.noContent().build();
    }

//    @PutMapping("/{id}/generate")
//    public void updateFriendlyFieldName(@PathVariable Long id, @RequestBody OllamaRequest request) {
//        String resp =  recordDataService.generateFriendlyColumnName(id,request);
//        recordDataService.updateFriendlyColumnName(id,resp);
//    }
@PostMapping("/raw")
public ResponseEntity<String> inputRawData(@RequestBody RequestDataDTO input) {
    if (input.getSample_records() == null || input.getSample_records().isEmpty()) {
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sample records are empty");
    }
    try {
        requestDataService.saveInput(input);
        return ResponseEntity.status(HttpStatus.CREATED).body("Data stored successfully!");
    } catch (DataAccessException e) {
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Database connection error");
    }
}


    @PutMapping("/generate")
    public ResponseEntity<String> generateFriendlyColumnNameByTableNameAndColumnName(@RequestParam String table_name,
                                                                                     @RequestParam String column_name) {

        if (table_name == null || table_name.trim().isEmpty() ||
                column_name == null || column_name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Invalid table_name and/or column_name. Ensure both are valid");
        }

        Optional<RecordData> rec = requestDataService.getDataByTableNameAndColumnName(table_name, column_name);
        if (!rec.isPresent()) {
            return ResponseEntity.status(404).body("Record not found for the provided table and column names.");
        }

        String response = requestDataService.generateFriendlyColumnNameByTableNameAndColumnName(table_name,column_name);
        requestDataService.updateFriendlyColumnNameByTableNameAndColumnName(table_name,column_name,response);

        return ResponseEntity.ok("Friendly column name for the column name "
                + column_name + " has been generated and stored in database");
    }

    @GetMapping("/versions")
    public ResponseEntity<?> retrieveVersionsByTableNameAndColumnName(@RequestParam String table_name,
                                                                      @RequestParam String column_name) {
        List<VersionData> versions = requestDataService.retrieveVersionsByTableNameAndColumnName(table_name,column_name);
        if(versions.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Versions for " + column_name
                    + " column with table name " + table_name + " not found");
        }
        return ResponseEntity.ok(versions);
    }
}
