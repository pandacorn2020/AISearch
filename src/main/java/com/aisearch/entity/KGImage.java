package com.aisearch.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Entity
@Data
public class KGImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private byte[] content;

    private String description;

    public KGImage() {
    }

    public KGImage(byte[] content, String description) {
        this.content = content;
        this.description = description;
    }

    public Long getId() {
        return id;
    }

    public byte[] getContent() {
        return content;
    }

    public String getDescription() {
        return description;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String toString() {
        return "KGImage{" +
                "content.length" + content.length +
                ", description='" + description + '\'' +
                '}';
    }

}
