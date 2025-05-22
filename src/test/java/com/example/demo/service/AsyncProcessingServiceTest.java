package com.example.demo.service;

import com.example.demo.pojo.RequestDataDTO;
import com.example.demo.pojo.SampleRecordDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
public class AsyncProcessingServiceTest {

    @Mock
    private OllamaService ollamaService;

    @Mock
    private StoreAndFetchData storeAndFetchData;


    @InjectMocks
    private AsyncProcessingService asyncProcessingService;

    @Test
    void processDataAsyncShouldCallOllamaAndSaveToRecordAndVersionData() {
        RequestDataDTO dto = new RequestDataDTO();
        dto.setRequest_id(1L);

        SampleRecordDTO sample = new SampleRecordDTO();
        sample.setTable_name("lars.dummy-booking");
        sample.setDescription("dummy booking table on LARS");

        Map<String,String> mp = new HashMap<>();
        mp.put("klnt-voorlv","Jens");
        mp.put("klnt-naam","Dominiak");
        sample.setData(Collections.singletonList(mp));

        dto.setSample_records(Collections.singletonList(sample));

        Map<String,String> mockedResponse = new HashMap<>();
        mockedResponse.put("klnt-voorlv", "Customer Surname");
        mockedResponse.put("klnt-naam", "Customer Name");

        Mockito.when(ollamaService.callToOllama(
                Mockito.anyString(),
                Mockito.anyString(),
                Mockito.anyMap())).thenReturn(mockedResponse);

        asyncProcessingService.processDataAsync(dto);

        Mockito.verify(ollamaService).callToOllama(
                Mockito.eq("lars.dummy-booking"),
                Mockito.eq("dummy booking table on LARS"),
                Mockito.anyMap());

        Mockito.verify(storeAndFetchData).saveToRecordDataAndVersionData(mockedResponse, "lars.dummy-booking");
        Mockito.verify(storeAndFetchData).sendEmail(1L);
    }
}
