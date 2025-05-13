package com.example.demo.repository;

import com.example.demo.pojo.RecordData;
import com.example.demo.pojo.VersionData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface VersionDataRepository extends JpaRepository<VersionData,Long> {

    List<VersionData> findByRecordDataId(RecordData recordData);
    void deleteByRecordDataId(RecordData recordData);

    @Query("SELECT v FROM VersionData v " +
            "WHERE v.recordDataId.friendlyColumnName IS NULL " +
            "AND v.recordDataId.id IN (" +
            "    SELECT v2.recordDataId.id " +
            "    FROM VersionData v2 " +
            "    GROUP BY v2.recordDataId.id " +
            "    HAVING COUNT(v2) = 3" +
            ")")
    List<VersionData> findEntriesWithNullFriendlyNameAndExactly3Versions();
}
