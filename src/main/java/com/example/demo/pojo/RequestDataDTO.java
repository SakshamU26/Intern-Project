package com.example.demo.pojo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

@Data
@RequiredArgsConstructor
public class RequestDataDTO {

    @JsonProperty("request_id")
    private Long requestId;

    @JsonProperty("sample_records")
    private List<SampleRecordDTO> sampleRecords;
}

