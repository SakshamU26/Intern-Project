package com.example.demo.repository;

import com.example.demo.pojo.SampleData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SampleDataRepository extends JpaRepository<SampleData, Long> {

    @Query("SELECT s FROM SampleData s WHERE s.tableName = :tableName AND s.storedAt >= :cutoff")
    List<SampleData> findSamplesFromLastHour(@Param("tableName") String tableName, @Param("cutoff") LocalDateTime cutoff);

    boolean existsByTableNameAndColumnNameAndExampleData(String tableName, String columnName, String exampleData);
}
