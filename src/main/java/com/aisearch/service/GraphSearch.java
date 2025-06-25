package com.aisearch.service;

import com.aisearch.entity.*;
import com.aisearch.llm.LLMModel;
import com.aisearch.llm.RagQuery;
import com.aisearch.llm.SessionData;
import com.aisearch.repository.*;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GraphSearch {
    public static final int MAX_COUNT = 32;
    private static final Logger logger = LoggerFactory.getLogger(GraphSearch.class.getSimpleName());

    @Autowired
    private JdbcRepository jdbcRepository;

    @Autowired
    private LLMModel llmModel;

    public static final int RELATIONSHIP_MAX_SIZE = 128;
    public static final int DOC_SEGMENT_MAX_SIZE = 6;

    public static final int ENTITY_MAX_SIZE = 64;

    private Map<String, KGGraph> graphMap = new HashMap<>();

    private SessionData sessionData;


    public GraphSearch() {

    }

    public void setSessionData(SessionData sessionData) {
        this.sessionData = sessionData;
    }

    @PostConstruct
    public void initialize() {
        loadGraphs();
    }

    public void loadGraphs() {
        try {
            String[] schemas = Schemas.SCHEMAS;
            for (String schema : schemas) {
                KGGraph graph = new KGGraph(schema);
                graph.load(this);
                graphMap.put(schema, graph);
            }
        } catch (Throwable t) {
            logger.error("Error loading graphs", t);
        }
    }

    public int getMaxRelationshipSize(String schema) {
        return RELATIONSHIP_MAX_SIZE;
    }

    public int getMaxEntitySize(String schema) {
        return ENTITY_MAX_SIZE;
    }

    public int getMaxSegmentSize(String schema) {
        return DOC_SEGMENT_MAX_SIZE;
    }

    public String search(RagQuery query) {
        StringJoiner joiner = new StringJoiner("\n\n");
        String input = query.getQuery();
        String[] entities = query.getEntities();
        for (String schema : Schemas.SCHEMAS) {
            String result = search(schema, input, entities);
            joiner.add(result);
        }
        return joiner.toString();
    }

    private String search(String schema, String input, String[] entities) {
        return searchComplete(schema, input, entities);
    }

    private String searchComplete(String schema, String input, String[] entities) {
        StringJoiner joiner = initializeSchemaJoiner(schema);

        addSegmentsToJoiner(schema, input, entities, joiner);
        addCommunitiesToJoiner(schema, input, entities, joiner);

        List<KGEntity> entityList = searchEntities(schema, entities);
        if (!entityList.isEmpty()) {
            joiner.add(buildEntitiesContent(entityList));
            addRelationshipsToJoiner(schema, entityList, joiner);
        }
        return joiner.toString();
    }

    private StringJoiner initializeSchemaJoiner(String schema) {
        String schemaDescription = Schemas.getSchemaDescription(schema);
        return new StringJoiner("\n",
                "\n" + schemaDescription + ":\n" + String.format("<%s>\n", schemaDescription),
                String.format("\n</%s>\n", schemaDescription));
    }

    private void addSegmentsToJoiner(String schema, String input, String[] entities, StringJoiner joiner) {
        searchSegments(schema, input, entities).forEach(segment -> joiner.add(segment.getSegment()));
    }

    private void addCommunitiesToJoiner(String schema, String input, String[] entities, StringJoiner joiner) {
        searchCommunities(schema, input, entities).forEach(community -> joiner.add(community.getSummary()));
    }

    private void addRelationshipsToJoiner(String schema, List<KGEntity> entityList, StringJoiner joiner) {
        List<KGRelationship> relationships = searchRelationships(schema, entityList);
        for (KGRelationship relationship : relationships) {
            joiner.add(relationship.getDescription());
        }
    }

    private List<KGSegment> searchSegments(String schema, String input, String[] entities) {
        int maxSize = getMaxSegmentSize(schema);
        List<KGSegment> segments = jdbcRepository.semanticSearchForSegments(schema, input,
                maxSize);
        return segments;
    }
    private List<KGCommunity> searchCommunities(String schema, String input, String[] entities) {
        List<KGCommunity> communities = jdbcRepository.semanticSearchForCommunities(
                schema, input, DOC_SEGMENT_MAX_SIZE);
        return communities;
    }

    private List<KGEntity> searchEntities(String schema, String[] entities) {
        List<KGEntity> entityList = jdbcRepository.getEntities(schema, entities, 6);
        int maxSize = getMaxEntitySize(schema);
        if (entityList.size() > maxSize) {
            return entityList.subList(0, maxSize);
        }
        return entityList;
    }

    private List<KGRelationship> searchRelationships(String schema,  List<KGEntity> entityList) {
        String[] entities = new String[entityList.size()];
        for (int i = 0; i < entityList.size(); i++) {
            entities[i] = entityList.get(i).getName();
        }
        KGGraph graph = graphMap.get(schema);
        List<KGRelationship> relationships = new ArrayList<>();
        for (String entity : entities) {
            List<KGRelationship> list = graph.getRelationships(entity);
            if (list != null && !list.isEmpty()) {
                relationships.addAll(list);
            }
        }
        int maxSize = getMaxRelationshipSize(schema);
        if (relationships.size() > maxSize) {
            return relationships.subList(0, maxSize);
        }
        return relationships;
    }

    private String buildSegmentsContent(List<KGSegment> segments) {
        StringJoiner joiner = new StringJoiner("\n");
        for (KGSegment segment : segments) {
            joiner.add(segment.getSegment());
        }
        return joiner.toString();
    }

    private String buildCommunitiesContent(List<KGCommunity> communities) {
        StringJoiner joiner = new StringJoiner("\n");
        for (KGCommunity community : communities) {
            joiner.add(community.getSummary());
        }
        return joiner.toString();
    }

    private String buildEntitiesContent(List<KGEntity> entities) {
        StringJoiner joiner = new StringJoiner("\n");
        for (KGEntity entity : entities) {
            joiner.add(entity.toString());
        }
        return joiner.toString();
    }

    private String buildRelationshipsContent(List<KGRelationship> relationships) {
        StringJoiner joiner = new StringJoiner("\n");
        for (KGRelationship relationship : relationships) {
            joiner.add(relationship.toString());
        }
        return joiner.toString();
    }


    public JdbcRepository getJdbcRepository() {
        return jdbcRepository;
    }
}
