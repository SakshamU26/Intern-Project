package com.example.demo.dao;

import com.example.demo.pojo.RecordData;
import com.example.demo.pojo.VersionData;
import com.example.demo.repository.VersionDataRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VersionDataDAO {
    private final VersionDataRepository versionDataRepository;

    public VersionDataDAO(VersionDataRepository versionDataRepository) {
        this.versionDataRepository = versionDataRepository;
    }

    public List<VersionData> findByRecordDataId(RecordData recordData) {
        return versionDataRepository.findByRecordDataId(recordData);
    }

    public void deleteByRecordDataId(RecordData recordData) {
        versionDataRepository.deleteByRecordDataId(recordData);
    }

    public void save(VersionData versionData) {
        versionDataRepository.save(versionData);
    }
}
