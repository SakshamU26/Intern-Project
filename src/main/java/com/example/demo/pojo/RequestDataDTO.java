package com.example.demo.pojo;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@RequiredArgsConstructor
public class RequestDataDTO {
    private Long request_id;
    private List<SampleRecordDTO> sample_records;
}

