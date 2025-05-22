package com.example.demo.service;

import com.example.demo.pojo.RequestDataDTO;
import com.example.demo.pojo.SampleRecordDTO;
import com.example.demo.repository.RequestDataRepository;
import com.example.demo.repository.SampleDataRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

@ExtendWith(MockitoExtension.class)
class RequestDataServiceTest {

    @Mock
    private RequestDataRepository requestDataRepository;

    @Mock
    private SampleDataRepository sampleDataRepository;

    @Mock
    private AsyncProcessingService asyncProcessingService;

    @InjectMocks
    RequestDataService requestDataService;

    private RequestDataDTO dto;
    private SampleRecordDTO sample1;
    private SampleRecordDTO sample2;

    @BeforeEach
    void inputSetUp() {
        dto = new RequestDataDTO();
        dto.setRequest_id(1L);

        sample1 = new SampleRecordDTO();
        sample1.setTable_name("lars.huisotaconfig");
        sample1.setDescription("Housecode and ota mapping");

        Map<String, String> mp1 = new HashMap<>();
        mp1.put("huov-code", "1DYZEWBX");
        mp1.put("klnt-code", "4983420");
        sample1.setData(Collections.singletonList(mp1));

        sample2 = new SampleRecordDTO();
        sample2.setTable_name("lars.airp");
        sample2.setDescription("Airports");

        Map<String, String> mp2 = new HashMap<>();
        mp2.put("land-code", "US");
        mp2.put("airp-latitude", "37,14170074");
        sample2.setData(Collections.singletonList(mp2));
    }

    @Test
    void saveInputShouldSaveInRequestDataAndSampleDataAndCallAsyncProcess() {

        dto.setSample_records(Arrays.asList(sample1,sample2));

        Mockito.when(sampleDataRepository.existsByTableNameAndColumnNameAndExampleData(
                        Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(false);

        requestDataService.saveInput(dto);

        Mockito.verify(requestDataRepository, Mockito.times(1)).saveAll(Mockito.anyList());
        Mockito.verify(sampleDataRepository, Mockito.times(1)).saveAll(Mockito.anyList());

        Mockito.verify(asyncProcessingService, Mockito.times(1)).processDataAsync(Mockito.eq(dto));
    }

    @Test
    void saveInputShouldDoNothingWhenInputIsNull() {
        requestDataService.saveInput(null);

        Mockito.verifyNoInteractions(requestDataRepository);
        Mockito.verifyNoInteractions(sampleDataRepository);
        Mockito.verifyNoInteractions(asyncProcessingService);
    }

    @Test
    void saveInputShouldDoNothingWhenSampleRecordsIsEmpty() {
        RequestDataDTO dto = new RequestDataDTO();
        dto.setRequest_id(1L);
        dto.setSample_records(Collections.emptyList());

        requestDataService.saveInput(dto);

        Mockito.verifyNoInteractions(requestDataRepository);
        Mockito.verifyNoInteractions(sampleDataRepository);
        Mockito.verifyNoInteractions(asyncProcessingService);
    }
}