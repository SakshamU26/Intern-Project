package com.example.demo.repository;

import com.example.demo.pojo.RecordData;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RecordDataRepository extends JpaRepository<RecordData,Long> {
    List<RecordData> findByFriendlyColumnNameIsNull();
    List<RecordData> findByFriendlyColumnNameIsNullAndVersionCountLessThan(int versionCount);
    Optional<RecordData> findByTableNameAndColumnName(String tableName, String columnName);
    void deleteByTableNameAndColumnName(String tableName, String columnName);
}
