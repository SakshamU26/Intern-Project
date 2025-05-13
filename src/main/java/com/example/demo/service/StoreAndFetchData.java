package com.example.demo.service;

import com.example.demo.pojo.RecordData;
import com.example.demo.pojo.SampleData;
import com.example.demo.pojo.VersionData;
import com.example.demo.repository.RecordDataRepository;
import com.example.demo.repository.SampleDataRepository;
import com.example.demo.repository.VersionDataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class StoreAndFetchData {

    private static final Logger logger = LoggerFactory.getLogger(StoreAndFetchData.class);

    @Autowired
    private RecordDataRepository recordDataRepository;

    @Autowired
    private VersionDataRepository versionDataRepository;

    @Autowired
    private SampleDataRepository sampleDataRepository;

    @Autowired
    private JavaMailSender mailSender;

    @Transactional
    public void saveToRecordDataAndVersionData(Map<String,String> columnsToFriendlyColumns, String tableName) {
        for(Map.Entry<String,String> entry : columnsToFriendlyColumns.entrySet()) {
            String column = entry.getKey();
            String friendlyColumn = entry.getValue();

            Optional<RecordData> existingRecord = recordDataRepository.findByTableNameAndColumnName(
                    tableName,column);

            if(existingRecord.isPresent()) {
                RecordData prev = existingRecord.get();

                VersionData version = new VersionData();
                int currVersion = prev.getVersionCount();
                version.setVersion(currVersion+1);
                version.setRecordDataId(prev);
                version.setFriendlyColumnNameVersion(prev.getFriendlyColumnName());

                prev.setVersionCount(version.getVersion());
                prev.setFriendlyColumnName(friendlyColumn);

                versionDataRepository.save(version);
                recordDataRepository.save(prev);
            }
            else {
                RecordData recordData = new RecordData();
                recordData.setColumnName(column);
                recordData.setFriendlyColumnName(friendlyColumn);
                recordData.setTableName(tableName);

                recordDataRepository.save(recordData);
            }
        }
    }

    public Map<String, List<String>> getPreviousSamples(String tableName) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        List<SampleData> previousSamples = sampleDataRepository.findSamplesFromLastHour(tableName,cutoff);

        Map<String, List<String>> previousColumnValues = new HashMap<>();

        for (SampleData sample : previousSamples) {
            previousColumnValues
                    .computeIfAbsent(sample.getColumnName(), k -> new ArrayList<>())
                    .add(sample.getExampleData());
        }

        return previousColumnValues;
    }

    public void sendEmail(Long request_id) {
        try {
            String message = "The request with request ID " + request_id + " has been processed and " +
                    "stored in the database successfully!";

            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo("nolola5447@bocapies.com");
            msg.setSubject("Job Complete!");
            msg.setText(message);

            mailSender.send(msg);
        } catch (Exception e) {
            logger.error("Failed to send email: {}", e.getMessage(), e);
        }
    }

}
