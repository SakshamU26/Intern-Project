package com.example.demo.pojo;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "record_data")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class RecordData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonProperty("table_name")
    private String tableName;

    @JsonProperty("column_name")
    private String columnName;


    @JsonProperty("friendly_column_name")
    private String friendlyColumnName;

    @Column(name = "version_count")
    private int versionCount;

    @OneToMany(mappedBy = "recordDataId", cascade = CascadeType.ALL, orphanRemoval = true)
    @JsonManagedReference
    private List<VersionData> versions = new ArrayList<>();
}
