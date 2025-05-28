package com.example.demo.pojo;

import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "request_data")
@Data
@RequiredArgsConstructor
public class RequestData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long Id;

    @Column(name = "request_id")
    private Long requestId;

    @Column(name = "table_name")
    private String tableName;

    @Column(name = "column_name")
    private String columnName;

    @Column(name = "value")
    private String value;

    @Column(name = "description")
    private String description;
}
