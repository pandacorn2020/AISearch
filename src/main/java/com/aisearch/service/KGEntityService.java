package com.aisearch.service;

import com.aisearch.entity.KGEntity;
import com.aisearch.exception.ResourceNotFoundException;
import com.aisearch.repository.KGEntityRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class KGEntityService {
    @Autowired
    private KGEntityRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;


    public Page<KGEntity> findAll(String schema, Pageable pageable) {
        jdbcTemplate.execute("USE " + schema);
        return repository.findAll(pageable);
    }

    public Optional<KGEntity> findById(String schema, String name) {
        jdbcTemplate.execute("USE " + schema);
        return repository.findById(name);
    }

    public KGEntity save(String schema, KGEntity entity) {
        jdbcTemplate.execute("USE " + schema);
        return repository.save(entity);
    }

    public void deleteById(String schema, String name) {
        String sql = String.format("DELETE FROM %s.kgentity WHERE name = ?", schema);
        int rows = jdbcTemplate.update(sql, name);

        if (rows == 0) {
            throw new ResourceNotFoundException("Entity with name " + name + " not found");
        }
    }
}