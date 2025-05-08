package com.example.demo.pojo;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "version_data")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
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

    @JsonProperty("suggestions")
    private String suggestions;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

}
