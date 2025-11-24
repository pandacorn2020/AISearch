package com.aisearch.repository;

import com.aisearch.entity.KGEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KGEntityRepository extends JpaRepository<KGEntity, String> {
}