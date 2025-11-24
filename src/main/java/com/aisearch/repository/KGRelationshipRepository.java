package com.aisearch.repository;

import java.util.List;

import com.aisearch.entity.KGRelationship;
import com.aisearch.entity.KGRelationshipKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KGRelationshipRepository extends JpaRepository<KGRelationship, KGRelationshipKey> {
    List<KGRelationship> findByIdSourceOrIdTarget(String source, String target);
}