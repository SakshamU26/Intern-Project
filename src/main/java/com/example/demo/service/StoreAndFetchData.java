package com.example.demo.service;

import com.example.demo.dao.RecordDataDAO;
import com.example.demo.dao.SampleDataDAO;
import com.example.demo.dao.VersionDataDAO;
import com.example.demo.pojo.RecordData;
import com.example.demo.pojo.SampleData;
import com.example.demo.pojo.VersionData;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class StoreAndFetchData {

    @Autowired
    private RecordDataDAO recordDataDAO;

    @Autowired
    private VersionDataDAO versionDataDAO;

    @Autowired
    private SampleDataDAO sampleDataDAO;

    @Autowired
    private JavaMailSender mailSender;

    @Transactional
    public void saveToRecordDataAndVersionData(Map<String,String> columnsToFriendlyColumns, String tableName) {
        for(Map.Entry<String,String> entry : columnsToFriendlyColumns.entrySet()) {
            String column = entry.getKey();
            String friendlyColumn = entry.getValue();

            Optional<RecordData> existingRecord = recordDataDAO.findByTableNameAndColumnName(
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

                versionDataDAO.save(version);
                recordDataDAO.save(prev);
            }
            else {
                RecordData recordData = new RecordData();
                recordData.setColumnName(column);
                recordData.setFriendlyColumnName(friendlyColumn);
                recordData.setTableName(tableName);

                recordDataDAO.save(recordData);
            }
        }
    }

    public Map<String, List<String>> getPreviousSamples(String tableName) {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(1);
        List<SampleData> previousSamples = sampleDataDAO.findRecentSamples(tableName,cutoff);

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
            log.info("Email Sent");
        } catch (Exception e) {
            log.error("Failed to send email: {}", e.getMessage(), e);
        }
    }

}
