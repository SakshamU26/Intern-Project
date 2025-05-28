package com.example.demo.service;

import com.example.demo.dao.RecordDataDAO;
import com.example.demo.dao.VersionDataDAO;
import com.example.demo.pojo.RecordData;
import com.example.demo.pojo.VersionData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StoreAndFetchDataTest {
    @InjectMocks
    private StoreAndFetchData storeAndFetchData;

    @Mock
    private RecordDataDAO recordDataDAO;

    @Mock
    private VersionDataDAO versionDataDAO;

    @Test
    void testSaveToRecordDataAndVersionData_InsertNewRecord() {
        Map<String, String> columns = new HashMap<>();
        columns.put("col1","Column One");

        when(recordDataDAO.findByTableNameAndColumnName("table1", "col1"))
                .thenReturn(Optional.empty());

        storeAndFetchData.saveToRecordDataAndVersionData(columns,"table1");

        ArgumentCaptor<RecordData> recordCaptor = ArgumentCaptor.forClass(RecordData.class);
        verify(recordDataDAO).save(recordCaptor.capture());

        RecordData saved = recordCaptor.getValue();
        assertEquals("col1", saved.getColumnName());
        assertEquals("Column One", saved.getFriendlyColumnName());
        assertEquals("table1", saved.getTableName());
    }

    @Test
    void testSaveToRecordDataAndVersionData_UpdateExistingRecord() {
        RecordData existing = new RecordData();
        existing.setVersionCount(2);
        existing.setFriendlyColumnName("Old Name");
        existing.setColumnName("col1");
        existing.setTableName("table1");

        when(recordDataDAO.findByTableNameAndColumnName("table1", "col1"))
                .thenReturn(Optional.of(existing));

        Map<String, String> columns = new HashMap<>();
        columns.put("col1", "New Friendly");

        storeAndFetchData.saveToRecordDataAndVersionData(columns, "table1");

        ArgumentCaptor<VersionData> versionCaptor = ArgumentCaptor.forClass(VersionData.class);
        verify(versionDataDAO).save(versionCaptor.capture());

        VersionData savedVersion = versionCaptor.getValue();
        assertEquals("Old Name", savedVersion.getFriendlyColumnNameVersion());
        assertEquals(3, savedVersion.getVersion());
        assertEquals(existing, savedVersion.getRecordDataId());

        verify(recordDataDAO).save(existing);
        assertEquals("New Friendly", existing.getFriendlyColumnName());
        assertEquals(3, existing.getVersionCount());
    }
}
