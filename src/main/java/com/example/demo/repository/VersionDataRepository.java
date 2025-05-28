package com.example.demo.repository;

import com.example.demo.pojo.RecordData;
import com.example.demo.pojo.VersionData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VersionDataRepository extends JpaRepository<VersionData,Long> {

    List<VersionData> findByRecordDataId(RecordData recordData);
    void deleteByRecordDataId(RecordData recordData);

}
