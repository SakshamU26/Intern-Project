package com.example.demo.pojo;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "version_data")
@Data
@RequiredArgsConstructor
public class VersionData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "record_data_id", nullable = false)
    @JsonBackReference
    private RecordData recordDataId;

    @JsonProperty("version")
    private int version;

    @JsonProperty("friendly_column_name_version")
    private String friendlyColumnNameVersion;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

}
