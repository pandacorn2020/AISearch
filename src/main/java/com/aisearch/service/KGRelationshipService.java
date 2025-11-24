package com.aisearch.service;

import com.aisearch.entity.KGRelationship;
import com.aisearch.entity.KGRelationshipKey;
import com.aisearch.exception.ResourceNotFoundException;
import com.aisearch.repository.KGRelationshipRepository;
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
public class KGRelationshipService {
    @Autowired
    private KGRelationshipRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;



    public Page<KGRelationship> findAll(String schema, Pageable pageable) {
        jdbcTemplate.execute("USE " + schema);
        return repository.findAll(pageable);
    }

    public Optional<KGRelationship> findById(String schema,KGRelationshipKey id) {

        jdbcTemplate.execute("USE " + schema);
        return repository.findById(id);
    }

    public KGRelationship save(String schema,KGRelationship relationship) {

        jdbcTemplate.execute("USE " + schema);
        return repository.save(relationship);
    }

    @Transactional
    public void deleteById(String schema,KGRelationshipKey key) {

        String sql = String.format("DELETE FROM %s.kgrelationship WHERE source = ? and target = ? and relation = ?", schema);
        int rows = jdbcTemplate.update(sql, key.getSource(), key.getTarget(),key.getRelation());

        if (rows == 0) {
            throw new ResourceNotFoundException(
                "Relationship not found: " + key.getSource() + " / " + key.getRelation() + " / " + key.getTarget()
            );
        }

    }

    public List<KGRelationship> findBySourceOrTarget(String schema,String entityName) {
        jdbcTemplate.execute("USE " + schema);
        return repository.findByIdSourceOrIdTarget(entityName, entityName);
    }
}