package com.example.demo.pojo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "sample_data")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SampleData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "table_name", nullable = false)
    private String tableName;

    @Column(name = "example_data")
    private String exampleData;

    @Column(name = "column_name")
    private String columnName;

    @Column(name = "request_id")
    private Long request_id;

    @Column(name = "description")
    private String description;

    @Column(name = "stored_at")
    private LocalDateTime storedAt;
}
