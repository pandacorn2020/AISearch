package com.aisearch.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.Statement;
import java.util.StringJoiner;

@Service
public class SchemaService {

    @Autowired
    private DataSource dataSource;
    private static final Logger logger = LoggerFactory.getLogger(SchemaService.class.getSimpleName());

    public void initializeSchemas(String[] schemas) {
        try (Connection connection = dataSource.getConnection()) {
            for (String schema : schemas) {
                createSchema(connection, schema);
                executeSqlFile(connection, schema);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize schemas", e);
        }
    }

    private void createSchema(Connection connection, String schema) throws Exception {
        try (Statement statement = connection.createStatement()) {
            try {
                statement.execute("CREATE SCHEMA " + schema);
            } catch (Exception e) {
                if (isAlreadyExistsError(e)) {
                    logger.info("Schema already exists, skip create: {}", schema);
                } else {
                    throw e;
                }
            }
            statement.execute("USE " + schema);
        }
    }

    private void executeSqlFile(Connection connection, String schema) throws Exception {
        ClassPathResource resource = new ClassPathResource("tables.sql");
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(resource.getInputStream()))) {
            String line;
            StringJoiner joiner = new StringJoiner("\n");
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty()) {
                    joiner.add(line);
                    if (line.endsWith(";")) {
                        try (Statement statement = connection.createStatement()) {
                            String sql = joiner.toString();
                            logger.info("Executing SQL: {}", sql);
                            statement.execute("use " + schema);
                            try {
                                statement.execute(sql);
                            } catch (Exception e) {
                                if (isAlreadyExistsError(e)) {
                                    logger.info("Skip existing object in schema {}: {}", schema, sql);
                                } else {
                                    throw e;
                                }
                            }
                        }
                        joiner = new StringJoiner("\n");
                    }
                }
            }
        }
    }

    private boolean isAlreadyExistsError(Exception e) {
        String msg = e.getMessage();
        return msg != null && msg.toLowerCase().contains("already exists");
    }
}