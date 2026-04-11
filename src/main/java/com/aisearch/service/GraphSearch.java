package com.aisearch.service;

import com.aisearch.config.CloudwaveProperties;
import com.aisearch.config.DataSourceProperties;
import com.aisearch.config.QueryProperties;
import com.aisearch.config.WebserverProperties;
import com.aisearch.entity.*;
import com.aisearch.llm.LLMModel;
import com.aisearch.llm.RagQuery;
import com.aisearch.llm.SessionData;
import com.aisearch.repository.*;
import com.wisdomdata.jdbc.CloudConnection;
import com.wisdomdata.jdbc.CloudDatabaseMetaData;
import com.wisdomdata.jdbc.CloudResultSet;
import com.wisdomdata.tools.dbclient.DBConnectionHelper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.*;

@Service
public class GraphSearch {
    public static final int MAX_COUNT = 32;
    private static final Logger logger = LoggerFactory.getLogger(GraphSearch.class.getSimpleName());
    private static final String APP_PREFIX = "aiask";

    @Autowired
    private JdbcRepository jdbcRepository;

    @Autowired
    private LLMModel llmModel;

    @Autowired
    private WebserverProperties webServerProperties;

    @Autowired
    private CloudwaveProperties cloudwaveProperties;

    public static final int RELATIONSHIP_MAX_SIZE = 128;
    public static final int DOC_SEGMENT_MAX_SIZE = 6;

    public static final int ENTITY_MAX_SIZE = 64;

    private Map<String, KGGraph> graphMap = new HashMap<>();

    private SessionData sessionData;

    public static class SearchItem {
        private String category;
        private String text;
        private List<String> sources;

        public SearchItem() {
        }

        public SearchItem(String category, String text, List<String> sources) {
            this.category = category;
            this.text = text;
            this.sources = sources;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public List<String> getSources() {
            return sources;
        }

        public void setSources(List<String> sources) {
            this.sources = sources;
        }
    }

    public static class SearchV4Result {
        private List<SearchItem> communities = new ArrayList<>();
        private List<SearchItem> entities = new ArrayList<>();
        private List<SearchItem> segments = new ArrayList<>();
        private List<SearchItem> relationships = new ArrayList<>();
        private List<String> sourceList = new ArrayList<>();

        public List<SearchItem> getCommunities() {
            return communities;
        }

        public void setCommunities(List<SearchItem> communities) {
            this.communities = communities;
        }

        public List<SearchItem> getEntities() {
            return entities;
        }

        public void setEntities(List<SearchItem> entities) {
            this.entities = entities;
        }

        public List<SearchItem> getSegments() {
            return segments;
        }

        public void setSegments(List<SearchItem> segments) {
            this.segments = segments;
        }

        public List<SearchItem> getRelationships() {
            return relationships;
        }

        public void setRelationships(List<SearchItem> relationships) {
            this.relationships = relationships;
        }

        public List<String> getSourceList() {
            return sourceList;
        }

        public void setSourceList(List<String> sourceList) {
            this.sourceList = sourceList;
        }

        public int getTotalCount() {
            return communities.size() + entities.size() + segments.size() + relationships.size();
        }
    }


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

            // 从配置文件spring.datasource.url读取数据地址



            CloudConnection connection = (CloudConnection) DBConnectionHelper.connect(cloudwaveProperties.getServer(), cloudwaveProperties.getUser(),cloudwaveProperties.getPassword());
            connection.setAutoCommit(false);
            CloudDatabaseMetaData meta = (CloudDatabaseMetaData) connection.getMetaData();
            CloudResultSet result = (CloudResultSet) meta.getSchemas();
            while (result.next()) {
                String schemaName = result.getString("TABLE_SCHEM");
                if (schemaName.startsWith(APP_PREFIX)) {
                    // 过滤出以aiask_开头的schema
                    KGGraph graph = new KGGraph(schemaName);
                    graph.load(this);
                    graphMap.put(schemaName, graph);

                }
                System.out.println(schemaName);
            }
//
//            String[] schemas = Schemas.SCHEMAS;
//            for (String schema : schemas) {
//                KGGraph graph = new KGGraph(schema);
//                graph.load(this);
//                graphMap.put(schema, graph);
//            }
        } catch (Throwable t) {
            logger.error("Error loading graphs", t);
        }
    }

    public void loadGraph(String schema) {
        try {

            // 过滤出以aiask_开头的schema
            KGGraph graph = new KGGraph(schema);
            graph.load(this);
            graphMap.put(schema, graph);

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

    public String search(String schema, RagQuery query) {
        StringJoiner joiner = new StringJoiner("\n\n");
        String input = query.getQuery();
        String[] entities = normalizeEntities(query.getEntities());
        String result = search(schema, input, entities);
        joiner.add(result);
        List<KGImage> images = jdbcRepository.findKGImagesByDescriptionSimilarity(
                schema, input, entities);
        if (!images.isEmpty()) {
            StringJoiner imageJoiner = new StringJoiner("\n", "\n请在报告的合适位置根据图片描述加上如下markdown文本（加上之后，可以精简图片描述。）：\n", "");
            images.forEach(image -> {
                imageJoiner.add(String.format("![%s](%s%s)", image.getDescription().replaceAll("[\\r\\n]","_").replaceAll("[\\[\\]]","_"), webServerProperties.getImageUrlPrefix(),image.getId()));
            });
            joiner.add(imageJoiner.toString());
        }

        return joiner.toString();
    }

    public SearchV4Result searchV4(String schema, RagQuery query, Set<String> categories,
                                   int maxCommunityCount, int maxEntityCount,
                                   int maxSegmentCount, int maxRelationshipCount) {
        String input = query.getQuery();
        String[] entities = normalizeEntities(query.getEntities());

        Set<String> selectedCategories = normalizeCategories(categories);
        int communityLimit = Math.max(0, maxCommunityCount);
        int entityLimit = Math.max(0, maxEntityCount);
        int segmentLimit = Math.max(0, maxSegmentCount);
        int relationshipLimit = Math.max(0, maxRelationshipCount);

        SearchV4Result result = new SearchV4Result();
        LinkedHashSet<String> sourceSet = new LinkedHashSet<>();

        if (selectedCategories.contains("community")) {
            List<KGCommunity> communities = searchCommunities(schema, input, entities, communityLimit);
            for (KGCommunity community : communities) {
                List<String> sources = new ArrayList<>();
                if (StringUtils.hasText(community.getName())) {
                    sources.add("community:" + community.getName());
                }
                sourceSet.addAll(sources);
                result.getCommunities().add(new SearchItem("community", community.getSummary(), sources));
            }
        }

        if (selectedCategories.contains("entity")) {
            List<KGEntity> entitiesResult = searchEntities(schema, entities, entityLimit);
            for (KGEntity entity : entitiesResult) {
                List<String> sources = buildFileSources(entity.getFileName());
                sourceSet.addAll(sources);
                result.getEntities().add(new SearchItem("entity", entity.toString(), sources));
            }
        }

        if (selectedCategories.contains("segment")) {
            List<KGSegment> segments = searchSegments(schema, input, entities, segmentLimit);
            for (KGSegment segment : segments) {
                List<String> sources = buildFileSources(segment.getFileName());
                sourceSet.addAll(sources);
                result.getSegments().add(new SearchItem("segment", segment.getSegment(), sources));
            }
        }

        if (selectedCategories.contains("relationship") && relationshipLimit > 0) {
            int relationshipEntityTopCount = entityLimit > 0 ? entityLimit : 6;
            List<KGEntity> relationshipEntities = searchEntities(schema, entities, relationshipEntityTopCount);
            List<KGRelationship> relationships = searchRelationships(schema, relationshipEntities, relationshipLimit);
            for (KGRelationship relationship : relationships) {
                List<String> sources = buildFileSources(relationship.getFileName());
                sourceSet.addAll(sources);
                result.getRelationships().add(new SearchItem("relationship", relationship.toString(), sources));
            }
        }

        result.setSourceList(new ArrayList<>(sourceSet));
        return result;
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
        return searchSegments(schema, input, entities, maxSize);
    }

    private List<KGSegment> searchSegments(String schema, String input, String[] entities, int maxSize) {
        if (maxSize <= 0) {
            return Collections.emptyList();
        }
        List<KGSegment> segments = jdbcRepository.semanticSearchForSegments(schema, input,
                maxSize);
        return segments;
    }

    private List<KGCommunity> searchCommunities(String schema, String input, String[] entities) {
        return searchCommunities(schema, input, entities, DOC_SEGMENT_MAX_SIZE);
    }

    private List<KGCommunity> searchCommunities(String schema, String input, String[] entities, int maxSize) {
        if (maxSize <= 0) {
            return Collections.emptyList();
        }
        List<KGCommunity> communities = jdbcRepository.semanticSearchForCommunities(
                schema, input, maxSize);
        return communities;
    }

    private List<KGEntity> searchEntities(String schema, String[] entities) {
        return searchEntities(schema, entities, 6);
    }

    private List<KGEntity> searchEntities(String schema, String[] entities, int topCount) {
        if (topCount <= 0 || entities == null || entities.length == 0) {
            return Collections.emptyList();
        }
        List<KGEntity> entityList = jdbcRepository.getEntities(schema, entities, topCount);
        int maxSize = getMaxEntitySize(schema);
        if (entityList.size() > maxSize) {
            return entityList.subList(0, maxSize);
        }
        return entityList;
    }

    private List<KGRelationship> searchRelationships(String schema,  List<KGEntity> entityList) {
        return searchRelationships(schema, entityList, getMaxRelationshipSize(schema));
    }

    private List<KGRelationship> searchRelationships(String schema, List<KGEntity> entityList, int maxSizeLimit) {
        if (maxSizeLimit <= 0 || entityList == null || entityList.isEmpty()) {
            return Collections.emptyList();
        }
        String[] entities = new String[entityList.size()];
        for (int i = 0; i < entityList.size(); i++) {
            entities[i] = entityList.get(i).getName();
        }
        KGGraph graph = graphMap.get(schema);
        if (graph == null) {
            return Collections.emptyList();
        }
        List<KGRelationship> relationships = new ArrayList<>();
        for (String entity : entities) {
            List<KGRelationship> list = graph.getRelationships(entity);
            if (list != null && !list.isEmpty()) {
                relationships.addAll(list);
            }
        }
        int maxSize = Math.min(getMaxRelationshipSize(schema), maxSizeLimit);
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

    private String[] normalizeEntities(String[] entities) {
        if (entities == null || entities.length == 0) {
            return new String[0];
        }
        List<String> list = new ArrayList<>();
        for (String entity : entities) {
            if (StringUtils.hasText(entity)) {
                list.add(entity.trim());
            }
        }
        return list.toArray(new String[0]);
    }

    private Set<String> normalizeCategories(Set<String> categories) {
        if (categories == null || categories.isEmpty()) {
            return new HashSet<>(Arrays.asList("community", "entity", "segment", "relationship"));
        }
        Set<String> normalized = new HashSet<>();
        for (String category : categories) {
            if ("community".equals(category) || "entity".equals(category)
                || "segment".equals(category) || "relationship".equals(category)) {
                normalized.add(category);
            }
        }
        if (normalized.isEmpty()) {
            normalized.addAll(Arrays.asList("community", "entity", "segment", "relationship"));
        }
        return normalized;
    }

    private List<String> buildFileSources(String sourceField) {
        if (!StringUtils.hasText(sourceField)) {
            return Collections.emptyList();
        }
        String[] parts = sourceField.split(",");
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (String part : parts) {
            if (StringUtils.hasText(part)) {
                set.add(part.trim());
            }
        }
        return new ArrayList<>(set);
    }
}
