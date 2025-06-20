package com.aisearch.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class KGSegment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String segment;

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
