package com.aisearch.service;

import com.aisearch.entity.KGCommunity;
import com.aisearch.exception.ResourceNotFoundException;
import com.aisearch.repository.KGCommunityRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class KGCommunityService {
    @Autowired
    private KGCommunityRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PersistenceContext
    private EntityManager entityManager;


    public Page<KGCommunity> findAll(String schema, Pageable pageable) {
        jdbcTemplate.execute("USE " + schema);
        return repository.findAll(pageable);
    }

    public Optional<KGCommunity> findById(String schema,Long id) {

        jdbcTemplate.execute("USE " + schema);
        return repository.findById(id);
    }

    public KGCommunity save(String schema,KGCommunity community) {

        jdbcTemplate.execute("USE " + schema);
        return repository.save(community);
    }
    @Transactional
    public void deleteById(String schema,Long id) {
        String sql = String.format("DELETE FROM %s.kgcommunity WHERE id = ?", schema);
        int rows = jdbcTemplate.update(sql, id);

        if (rows == 0) {
            throw new ResourceNotFoundException("Community with ID " + id + " not found");
        }
    }
}