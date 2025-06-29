package com.aisearch.service;

import com.google.common.base.Stopwatch;
import com.aisearch.entity.KGGraph;
import com.aisearch.entity.KGSegment;
import com.aisearch.llm.KgTask;
import com.aisearch.llm.LLMModel;
import com.aisearch.repository.*;
import com.aisearch.util.FileConverter;
import com.aisearch.util.JsonArrayToStringArray;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatLanguageModel;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;


@Service
public class GraphBuilder {
    private static final Logger logger = LoggerFactory.getLogger(GraphBuilder.class.getSimpleName());

    private final LLMModel llmModel;
    private final DocumentLoader documentLoader;
    private final JdbcRepository jdbcRepository;

    private ExecutorService executorService;
    private String ragKgSystemPrompt;
    private String ragKgUserPrompt;
    private ChatLanguageModel model;

    public static final int BATCH_SIZE = 4;

    @Autowired
    public GraphBuilder(LLMModel llmModel, DocumentLoader documentLoader,
                        JdbcRepository jdbcRepository) {
        this.llmModel = llmModel;
        this.documentLoader = documentLoader;
        this.jdbcRepository = jdbcRepository;
        this.executorService = Executors.newFixedThreadPool(BATCH_SIZE);
        this.model = llmModel.buildKgModel();
        initGraphs();
        try {
            this.ragKgSystemPrompt = documentLoader.readKgSystemPrompt();
            this.ragKgUserPrompt = documentLoader.readKgUserPrompt();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public LLMModel getLlmModel() {
        return llmModel;
    }

    public ChatLanguageModel getModel() {
        return model;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    private void initGraphs() {
    }

    public void buildAllGraphs() {
        for (String schema : Schemas.allSchemas()) {
            buildGraph(schema);
        }
    }

    @Transactional
    public void buildGraph(String schema) {
        logger.info("Building knowledge graph for schema: {}", schema);
        String directoryName = Schemas.getSchemaDir(schema);
        buildGraph(schema, directoryName);
    }



    public String getCommunityTemplate() {
        try {
            return documentLoader.readKgCommunityPrompt();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }


    private void buildGraph(String schemaName, List<String> slices, String description) {
        logger.info("Building knowledge graph for schema: {}, description: {}", schemaName, description);
        KGGraph graph = new KGGraph(schemaName);
        // add slices to segments
        for (String slice : slices) {
            KGSegment segment = new KGSegment(slice, description);
            graph.addSegment(segment);
        }
        List<KgTask> taskList = new ArrayList<>();
        int batchSize = BATCH_SIZE;
        int size = 0;
        for (String slice : slices) {
            taskList.add(new KgTask(SystemMessage.systemMessage(ragKgSystemPrompt),
                    ragKgSystemPrompt, ragKgUserPrompt,
                    TextSegment.from(slice), model));
            if (taskList.size() >= batchSize) {
                size += taskList.size();
                logger.info("Processing batch of size: {}, {}/{}",
                        taskList.size(), size, slices.size());
                List<String> textList = batchGetKgText(taskList);
                for (String text : textList) {
                    updateKnowledgeGraph(graph, text, description);
                }

                taskList.clear();
            }
        }
        if (!taskList.isEmpty()) {
            logger.info("Processing batch of size: {}, {}/{}",
                    taskList.size(), size, slices.size());
            List<String> textList = batchGetKgText(taskList);
            for (String text : textList) {
                updateKnowledgeGraph(graph, text, description);
            }
        }
        logger.info("Building communities for schema: {}, size: {}",
                schemaName, graph.entitySize());
        graph.buildCommunities(this);
        graph.save(this);
        jdbcRepository.saveKGFile(schemaName, description);
        logger.info("Finished building knowledge graph for: {}/{}",
                schemaName, description);
    }

    public void buildEBMGraph() {
        String directoryName = "docs";
        buildGraph(Schemas.DOCS, directoryName);
    }

    private void buildGraph(String schemaName, String directoryName) {
        try {
            Path path = FileConverter.getResourcePath(directoryName);
            buildGraph(schemaName, path);
        } catch (Throwable t) {
            logger.error("Failed to build graph for " + schemaName, t);
        }
    }

    public void buildGraph(String schema, Path path) {
        if (Files.isDirectory(path)) {
            try {
                Files.list(path).forEach(p -> buildGraph(schema, p));
            } catch (Throwable t) {
                logger.error("Failed to build graph", t);
            }
        } else {
            try {
                String description = path.getFileName().toString();
                String existFile = jdbcRepository.findKGFileByName(schema, description);
                if (existFile != null) {
                    logger.info("File already ingested: {}", description);
                    return;
                }
                logger.info("Processing file: {}", description);
                String text = FileConverter.convertFileToText(Files.newInputStream(path),
                        description);
                boolean direct = description.startsWith("slice") && description.endsWith(".txt")
                        || description.endsWith(".json");
                if (direct) {
                    logger.info("Directly ingesting file: {}", description);
                    ingestSlices(schema, Collections.singletonList(text), description);
                } else {
                    // slip into chunks with overlap
                    String[] texts = documentLoader.splitText(text);
                    logger.info("Splitting file into {} chunks, file size: {}",
                            texts.length, text.length());
                    ingestSlices(schema, Arrays.asList(texts), description);
                }

            } catch (Throwable t) {
                logger.error("Failed to build graph", t);
            }
        }
    }


    public void ingestSlices(String schemaName, List<String> slices, String description) {
        List<String> textList = new ArrayList<>();
        if (!description.endsWith("json")) {
            for (String slice : slices) {
                textList.add(slice);
            }
        } else {
            logger.error("Ingesting json file: {}", description);
            for (String slice : slices) {
                String[] jsonElements = JsonArrayToStringArray.convert(slice);
                for (String s : jsonElements) {
                    textList.add(s);
                }
            }
        }
        buildGraph(schemaName, textList, description);
    }


    private static final String ENTITIES_START = "<entities>";
    private static final String ENTITIES_END = "</entities";
    private static final String RELATIONSHIP_START = "<relationships>";
    private static final String RELATIONSHIP_END = "</relationships";
    private void updateKnowledgeGraph(KGGraph graph, String text, String fileName) {
        try {
            if (text == null) {
                return;
            }
            logger.info("Updating knowledge graph with text: {}", text);
            List<String[]> entities = getRecords(text, ENTITIES_START, ENTITIES_END);
            if (entities != null && !entities.isEmpty()) {
                logger.info("Found {} entities", entities.size());
                graph.addEntities(entities, fileName);
            }
            List<String[]> relationships = getRecords(text, RELATIONSHIP_START, RELATIONSHIP_END);
            if (relationships != null && !relationships.isEmpty()) {
                logger.info("Found {} relationships", relationships.size());
                graph.addRelationships(relationships, fileName);
            }
        } catch (Throwable t) {
            logger.error("Failed to update knowledge graph", t);
            t.printStackTrace();
        }
    }

    public static List<String[]> getRecords(String text, String startTag, String endTag) {
        try {
            int startIndex = text.indexOf(startTag);
            if (startIndex < 0) {
                return Collections.EMPTY_LIST;
            }
            startIndex = startIndex + startTag.length();
            int endIndex = text.indexOf(endTag);
            if (endIndex < 0) {
                endIndex = text.indexOf("```", startIndex);
            }
            if (endIndex < 0) {
                return Collections.EMPTY_LIST;
            }
            text = text.substring(startIndex, endIndex);
            BufferedReader br = new BufferedReader(new StringReader(text));
            String line = br.readLine();
            List<String[]> list = new ArrayList<>();
            while (line != null) {
                if (line.contains("|")) {
                    list.add(line.split("\\|"));
                }
                line = br.readLine();
            }
            return list;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }
    private List<String> batchGetKgText(List<KgTask> taskList) {
        try {
            Stopwatch watch = Stopwatch.createStarted();
            List<String> textList = new ArrayList<>();
            List<Future<String>> futureList = new ArrayList<>();
            for (KgTask task : taskList) {
                futureList.add(executorService.submit(task));
            }
            for (Future<String> future : futureList) {
                textList.add(future.get());
            }
            logger.info("Batch get kg text, task size: {}, time: {}ms",
                    taskList.size(), watch.elapsed(TimeUnit.MILLISECONDS));
            return textList;
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public void batchExecuteTasks(List<Callable> taskList) {
        try {
            int batchSize = BATCH_SIZE;
            List<Future<String>> futureList = new ArrayList<>();
            int index = 0;
            for (Callable task : taskList) {
                futureList.add(executorService.submit(task));
                if (futureList.size() >= batchSize) {
                    Stopwatch watch = Stopwatch.createStarted();
                    for (Future<String> future : futureList) {
                        future.get();
                    }
                    index += futureList.size();
                    logger.info("Batch execute tasks, task size: {}/{}, time: {}ms",
                            index, taskList.size(), watch.elapsed(TimeUnit.MILLISECONDS));
                    futureList.clear();
                }
            }
            if (!futureList.isEmpty()) {
                Stopwatch watch = Stopwatch.createStarted();
                for (Future<String> future : futureList) {
                    future.get();
                }
                index += futureList.size();
                logger.info("Batch execute tasks, task size: {}/{}, time: {}ms",
                        index, taskList.size(), watch.elapsed(TimeUnit.MILLISECONDS));
            }
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    public DocumentLoader getDocumentLoader() {
        return documentLoader;
    }
    public JdbcRepository getJdbcRepository() {
        return jdbcRepository;
    }

}
