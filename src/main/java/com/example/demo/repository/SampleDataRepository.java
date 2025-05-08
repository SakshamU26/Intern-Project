package com.example.demo.repository;

import com.example.demo.pojo.SampleData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SampleDataRepository extends JpaRepository<SampleData,Long> {
    List<SampleData> findByTableName(String tableName);
    boolean existsByTableNameAndSampleRow(String tableName, String sampleRow);
    boolean existsByTableNameAndColumnName(String tableName, String columnName);
    boolean existsByTableNameAndColumnNameAndSampleRow(String tableName, String columnName, String sampleRow);
    List<SampleData> findByTableNameAndColumnName(String tableName, String columnName);
}
