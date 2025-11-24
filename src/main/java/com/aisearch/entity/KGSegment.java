package com.aisearch.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Data
public class KGSegment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String segment;

    @Column(name = "file_name")
    private String fileName;

    public KGSegment() {
    }

    public KGSegment(String segment, String fileName) {
        this.segment = segment;
        this.fileName = fileName;
    }

    @Override
    public String toString() {
        return "KGSegment{" +
                "segment='" + segment + '\'' +
                ", fileName='" + fileName + '\'' +
                '}';
    }
}
