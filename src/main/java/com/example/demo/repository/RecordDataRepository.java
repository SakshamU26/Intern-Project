package com.example.demo.repository;

import com.example.demo.pojo.RecordData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;

public interface RecordDataRepository extends JpaRepository<RecordData,Long> {
    @Query(value = "SELECT * FROM record_data WHERE table_name = :tableName AND column_name = :columnName", nativeQuery = true)
    Optional<RecordData> findByTableNameAndColumnName(@Param("tableName") String tableName, @Param("columnName") String columnName);

    @Modifying
    @Transactional
    @Query(value = "DELETE FROM record_data WHERE table_name = :tableName AND column_name = :columnName", nativeQuery = true)
    void deleteByTableNameAndColumnName(@Param("tableName") String tableName, @Param("columnName") String columnName);
}
