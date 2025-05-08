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

    @Column(name = "sample_row")
    private String sampleRow;

    @Column(name = "column_name")
    private String columnName;
}
