package com.example.demo.dao;

import com.example.demo.pojo.SampleData;
import com.example.demo.repository.SampleDataRepository;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class SampleDataDAO {

    private final SampleDataRepository repository;

    public SampleDataDAO(SampleDataRepository repository) {
        this.repository = repository;
    }

    public boolean sampleExists(String tableName, String columnName, String exampleData) {
        return repository.existsByTableNameAndColumnNameAndExampleData(tableName, columnName, exampleData);
    }

    public List<SampleData> findRecentSamples(String tableName, LocalDateTime cutoff) {
        return repository.findSamplesFromLastHour(tableName, cutoff);
    }

    public void saveAll(List<SampleData> sampleDataList) {
        repository.saveAll(sampleDataList);
    }
}


