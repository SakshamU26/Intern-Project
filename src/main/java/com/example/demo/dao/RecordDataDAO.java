package com.example.demo.dao;


import com.example.demo.pojo.RecordData;
import com.example.demo.repository.RecordDataRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class RecordDataDAO {

    private final RecordDataRepository recordDataRepository;

    public RecordDataDAO(RecordDataRepository recordDataRepository) {
        this.recordDataRepository = recordDataRepository;
    }

    public List<RecordData> findAll() {
        return recordDataRepository.findAll();
    }

    public Optional<RecordData> findById(Long id) {
        return recordDataRepository.findById(id);
    }

    public Optional<RecordData> findByTableNameAndColumnName(String tableName, String columnName) {
        return recordDataRepository.findByTableNameAndColumnName(tableName, columnName);
    }

    public RecordData getReferenceById(Long id) {
        return recordDataRepository.getReferenceById(id);
    }

    public void deleteById(Long id) {
        recordDataRepository.deleteById(id);
    }

    public void deleteByTableNameAndColumnName(String tableName, String columnName) {
        recordDataRepository.deleteByTableNameAndColumnName(tableName, columnName);
    }

    public void save(RecordData recordData) {
        recordDataRepository.save(recordData);
    }
}