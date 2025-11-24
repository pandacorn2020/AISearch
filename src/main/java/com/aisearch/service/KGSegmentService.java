package com.aisearch.service;

import com.aisearch.entity.KGSegment;
import com.aisearch.exception.ResourceNotFoundException;
import com.aisearch.repository.KGSegmentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class KGSegmentService {
    @Autowired
    private KGSegmentRepository repository;


    @Autowired
    private JdbcTemplate jdbcTemplate;


    public Page<KGSegment> findAll(String schema,Pageable pageable) {
        jdbcTemplate.execute("USE " + schema);
        return repository.findAll(pageable);
    }

    public Optional<KGSegment> findById(String schema,Long id) {
        jdbcTemplate.execute("USE " + schema);
        return repository.findById(id);
    }

    public KGSegment save(String schema,KGSegment segment) {

        jdbcTemplate.execute("USE " + schema);
        return repository.save(segment);
    }

    @Transactional
    public void deleteById(String schema,Long id) {


        String sql = String.format("DELETE FROM %s.kgsegment WHERE id = ?", schema);
        int rows = jdbcTemplate.update(sql, id);

        if (rows == 0) {
            throw new ResourceNotFoundException("Segment with ID " + id + " not found");
        }
    }
}