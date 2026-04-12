package com.aisearch.controller;

import com.aisearch.config.KgProperties;
import com.aisearch.dto.RequestCommon;
import com.aisearch.dto.RequestContentKnowledgeBuildStatus;
import com.aisearch.dto.RequestContentKnowledgeReset;
import com.aisearch.dto.RequestContentKnowledgeSearch;
import com.aisearch.dto.RequestContentKnowledgeSearchV4;
import com.aisearch.dto.ResponseCommon;
import com.aisearch.entity.KGGraph;
import com.aisearch.llm.RagQuery;
import com.aisearch.repository.JdbcRepository;
import com.aisearch.service.BuildTaskService;
import com.aisearch.service.GraphBuilder;
import com.aisearch.service.GraphSearch;
import com.aisearch.service.SchemaService;
import com.aisearch.service.Schemas;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

@CrossOrigin(origins = "*")
@RestController
@SessionAttributes(value = {"statement", "connection"})
@RequestMapping("/aisearch/knowledge")
public class KnowledgeController {


    private static final Logger logger = java.util.logging.Logger.getLogger(KnowledgeController.class.getSimpleName());

    @Autowired
    private KgProperties kgProperties;

    @Autowired
    private SchemaService schemaService;


    @Autowired
    private GraphBuilder graphBuilder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private GraphSearch graphSearch;

    @Autowired
    private BuildTaskService buildTaskService;

    @Autowired
    private JdbcRepository jdbcRepository;


    // 知识库文件上传接口
    @PostMapping("/v3/upload")
    public ResponseCommon<String[]> uploadV3(@RequestParam("file") MultipartFile file,@RequestParam("schema") String schema) {

        ResponseCommon.Message msg = new ResponseCommon.Message("","");
        ResponseCommon<String[]> resp = new ResponseCommon<>();

        String normalizedSchema = normalizeSchema(schema);
        if (!StringUtils.hasText(normalizedSchema) || !isSchemaNameValid(normalizedSchema)) {
            msg.setFail("schema 非法，仅支持字母数字下划线");
            resp.setMsg(msg);
            resp.setCode(400);
            resp.setContent(new String[]{});
            resp.setCount(0);
            return resp;
        }

        logger.info("schema:" + normalizedSchema);

        try {
            byte[] bytes = file.getBytes();
            // 将文件上传到 application.properties 中配置的 kg.inputDir 目录




            String inputDir = kgProperties.getInputDir();

            // 检查是否有schema目录，没有的话自动创建
            if (!Files.exists(Paths.get(inputDir, normalizedSchema))) {
                Files.createDirectory(Paths.get(inputDir, normalizedSchema));
            }

            System.out.println("原始文件名：" + file.getOriginalFilename());

            String safeName = file.getOriginalFilename().replaceAll("[\\\\/:*?\"<>|]", "_");
            System.out.println("安全文件名：" + safeName);



            Path path = Paths.get(inputDir, normalizedSchema, safeName);

            // 确保目录存在
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);

            msg.setSuccess("文件上传成功（该v3接口已弃用，请迁移至v4接口(/v4/upload)）");
            resp.setMsg(msg);
            resp.setCode(200);
            resp.setContent(new String[]{});
            resp.setCount(0);
            logger.info("即将启动知识库构建线程（该v3接口已弃用，请迁移至v4接口(/v4/upload)）");
            //异步执行
            new Thread(() -> {
                graphBuilder.buildGraph(normalizedSchema, Path.of(inputDir + "/" + normalizedSchema));
                graphSearch.loadGraph(normalizedSchema);

            }).start();
            logger.info("已返回文件上传状态（该v3接口已弃用，请迁移至v4接口(/v4/upload)），" + resp);
            return resp;
        } catch (IOException e) {
            e.printStackTrace();
            msg.setFail("文件上传失败（该v3接口已弃用，请迁移至v4接口(/v4/upload)）");
            resp.setMsg(msg);
            resp.setCode(500);
            resp.setContent(new String[]{});
            resp.setCount(0);
            return resp;
        }
    }
    @PostMapping("/v4/upload")
    public ResponseCommon<String[]> uploadV4(@RequestParam("file") MultipartFile file,@RequestParam("schema") String schema) {

        ResponseCommon.Message msg = new ResponseCommon.Message("","");
        ResponseCommon<String[]> resp = new ResponseCommon<>();

        String normalizedSchema = normalizeSchema(schema);

        logger.info("schema:" + normalizedSchema);

        try {
            if (!StringUtils.hasText(normalizedSchema) || !isSchemaNameValid(normalizedSchema)) {
                msg.setFail("schema 非法，仅支持字母数字下划线");
                resp.setMsg(msg);
                resp.setCode(400);
                resp.setContent(new String[]{});
                resp.setCount(0);
                return resp;
            }

            if (file == null || file.isEmpty() || !StringUtils.hasText(file.getOriginalFilename())) {
                msg.setFail("文件为空");
                resp.setMsg(msg);
                resp.setCode(400);
                resp.setContent(new String[]{});
                resp.setCount(0);
                return resp;
            }

            byte[] bytes = file.getBytes();
            // 将文件上传到 application.properties 中配置的 kg.inputDir 目录




            String inputDir = kgProperties.getInputDir();

            // 检查是否有schema目录，没有的话自动创建
            if (!Files.exists(Paths.get(inputDir, normalizedSchema))) {
                Files.createDirectory(Paths.get(inputDir, normalizedSchema));
            }

            System.out.println("原始文件名：" + file.getOriginalFilename());

            String safeName = file.getOriginalFilename().replaceAll("[\\\\/:*?\"<>|]", "_");
            System.out.println("安全文件名：" + safeName);

            if (buildTaskService.existsBySchemaAndFileName(normalizedSchema, safeName)) {
                msg.setFail("重复构建任务：schema=" + normalizedSchema + ", file_name=" + safeName);
                resp.setMsg(msg);
                resp.setCode(500);
                resp.setContent(new String[]{});
                resp.setCount(0);
                return resp;
            }



            Path path = Paths.get(inputDir, normalizedSchema, safeName);

            // 确保目录存在
            Files.createDirectories(path.getParent());
            Files.write(path, bytes);

            String taskId = buildTaskService.createTask(
                normalizedSchema,
                safeName,
                "UPLOAD",
                "upload-api",
                path.toString(),
                "single-file upload build"
            );

            msg.setSuccess("文件上传成功");
            resp.setMsg(msg);
            resp.setCode(200);
            resp.setContent(new String[]{});
            resp.setCount(0);
            logger.info("即将启动知识库构建线程,当前构建文件:" + path.toString());
            //异步执行
            new Thread(() -> {
                executeBuildTaskWithHeartbeat(taskId, normalizedSchema, safeName, path);
                try {
                    graphSearch.loadGraph(normalizedSchema);
                } catch (Exception ex) {
                    logger.warning("v4 upload loadGraph failed, schema=" + normalizedSchema + ", error=" + ex.getMessage());
                }

            }).start();
            logger.info("已返回文件上传状态，" + resp.toString());
            return resp;
        } catch (IllegalArgumentException e) {
            msg.setFail(e.getMessage());
            resp.setMsg(msg);
            resp.setCode(500);
            resp.setContent(new String[]{});
            resp.setCount(0);
            return resp;
        } catch (Exception e) {
            e.printStackTrace();
            msg.setFail("文件上传失败: " + e.getMessage());
            resp.setMsg(msg);
            resp.setCode(500);
            resp.setContent(new String[]{});
            resp.setCount(0);
            return resp;
        }
    }
    // 知识库文件上传接口
    @PostMapping("/v4/build")
    public ResponseCommon<Object[]> buildv4(@RequestBody String body) {

        ResponseCommon.Message msg = new ResponseCommon.Message("", "");
        ResponseCommon<Object[]> resp = new ResponseCommon<>();

        // 解析 JSON 为 RequestCommon<RequestContentKnowledgeReset>
        RequestCommon<RequestContentKnowledgeReset> request;
        try {
            request = JSON.parseObject(
                body,
                new TypeReference<RequestCommon<RequestContentKnowledgeReset>>() {}
            );
        } catch (Exception ex) {
            msg.setFail("请求体不是合法的 JSON 或解析失败: " + ex.getMessage());
            resp.setMsg(msg);
            resp.setCode(400);
            resp.setContent(new Object[]{});
            resp.setCount(0);
            return resp;
        }

        // 打印（调试）
        System.out.println("收到请求: " + request);

        boolean requestBodyValid = isRequestBodyValid(request);
        if (!requestBodyValid) {
            msg.setFail("fromId,fromNickname,content 验证失败");
            resp.setMsg(msg);
            resp.setCode(400);
            resp.setContent(new Object[]{});
            resp.setCount(0);
            return resp;
        }

        // 根据你实际的 RequestContentKnowledgeReset 字段进行更细粒度校验，这里以 schema 字段为例
        String schema = normalizeSchema(request.getContent().getSchema());
        if (!StringUtils.hasText(schema) || !isSchemaNameValid(schema)) {
            msg.setFail("content.schema 非法，仅支持字母数字下划线");
            resp.setMsg(msg);
            resp.setCode(400);
            resp.setContent(new Object[]{});
            resp.setCount(0);
            return resp;
        }

        // 打印/处理请求（示例）
        System.out.println("fromId = " + request.getFromId());
        System.out.println("fromNickname = " + request.getFromNickname());
        System.out.println("content.schema = " + schema);

        try {
            String inputDir = kgProperties.getInputDir();
            Path schemaPath = Path.of(inputDir + "/" + schema);

            if (!Files.exists(schemaPath) || !Files.isDirectory(schemaPath)) {
                msg.setFail("schema目录不存在: " + schemaPath);
                resp.setMsg(msg);
                resp.setCode(400);
                resp.setContent(new String[]{});
                resp.setCount(0);
                return resp;
            }

            List<Path> files = Files.walk(schemaPath)
                .filter(Files::isRegularFile)
                .toList();

            if (files.isEmpty()) {
                msg.setFail("schema目录下没有可构建文件");
                resp.setMsg(msg);
                resp.setCode(400);
                resp.setContent(new String[]{});
                resp.setCount(0);
                return resp;
            }

            Map<Path, String> taskMap = new LinkedHashMap<>();
            List<String> runningFiles = new ArrayList<>();
            List<String> skippedFiles = new ArrayList<>();
            for (Path filePath : files) {
                String fileName = filePath.getFileName().toString();
                String taskId = buildTaskService.prepareTaskForBuild(
                    schema,
                    fileName,
                    "BUILD",
                    request.getFromId(),
                    filePath.toString(),
                    "schema build"
                );
                if (!StringUtils.hasText(taskId)) {
                    String latestStatus = buildTaskService.getLatestStatusBySchemaAndFile(schema, fileName);
                    if ("RUNNING".equalsIgnoreCase(latestStatus)) {
                        runningFiles.add(fileName);
                    } else {
                        skippedFiles.add(fileName);
                    }
                    continue;
                }
                taskMap.put(filePath, taskId);
            }

            if (taskMap.isEmpty() && !runningFiles.isEmpty()) {
                String failMessage = "构建任务运行中，无法重复触发: " + String.join(",", runningFiles);
                if (!skippedFiles.isEmpty()) {
                    failMessage = failMessage + "；非运行状态未触发: " + String.join(",", skippedFiles);
                }
                msg.setFail(failMessage);
                resp.setMsg(msg);
                resp.setCode(500);
                resp.setContent(new String[]{});
                resp.setCount(0);
                return resp;
            }

            if (taskMap.isEmpty() && runningFiles.isEmpty() && !skippedFiles.isEmpty()) {
                msg.setSuccess("没有可重新触发的任务，非运行状态未触发: " + String.join(",", skippedFiles));
                resp.setMsg(msg);
                resp.setCode(200);
                resp.setContent(new String[]{});
                resp.setCount(0);
                return resp;
            }


            logger.info("即将启动知识库构建线程");
            //异步执行
            new Thread(() -> {
                for (Map.Entry<Path, String> entry : taskMap.entrySet()) {
                    Path filePath = entry.getKey();
                    String taskId = entry.getValue();
                    String fileName = filePath.getFileName().toString();
                    executeBuildTaskWithHeartbeat(taskId, schema, fileName, filePath);
                }
                try {
                    graphSearch.loadGraph(schema);
                } catch (Exception ex) {
                    logger.warning("v4 build loadGraph failed, schema=" + schema + ", error=" + ex.getMessage());
                }

            }).start();
            StringBuilder successMessage = new StringBuilder();
            successMessage.append("正在构建：").append(schema).append("，已触发任务数：").append(taskMap.size());
            if (!runningFiles.isEmpty()) {
                successMessage.append("，运行中未重复触发：").append(String.join(",", runningFiles));
            }
            if (!skippedFiles.isEmpty()) {
                successMessage.append("，非运行状态未触发：").append(String.join(",", skippedFiles));
            }
            msg.setSuccess(successMessage.toString());
            resp.setMsg(msg);
            resp.setCode(200);
            resp.setContent(new String[]{});
            resp.setCount(0);

            return resp;
        } catch (IllegalArgumentException e) {
            msg.setFail(e.getMessage());
            resp.setMsg(msg);
            resp.setCode(500);
            resp.setContent(new String[]{});
            resp.setCount(0);
            return resp;
        } catch (Exception e) {
            e.printStackTrace();
            msg.setFail("接口调用失败: " + e.getMessage());
            resp.setMsg(msg);
            resp.setCode(500);
            resp.setContent(new String[]{});
            resp.setCount(0);
            return resp;
        }
    }



    // 知识库构建状态查询接口
    @PostMapping("/v3/buildStatus")
    public ResponseCommon<Object[]> buildStatusV3(@RequestBody String body) throws IOException {
        return queryBuildStatus(body, false);

    }

    @PostMapping("/v4/buildStatus")
    public ResponseCommon<Object[]> buildStatusV4(@RequestBody String body) throws IOException {
        return queryBuildStatus(body, true);

    }

    // 知识库检索接口
    @ResponseBody
    @PostMapping("/v3/kgsearch")
    public ResponseCommon<Object[]> kgSearchV3(@RequestBody String body) {
        ResponseCommon.Message msg = new ResponseCommon.Message("", "");
        ResponseCommon<Object[]> resp = new ResponseCommon<>();

        // 解析 JSON 为 RequestCommon<RequestContentKnowledgeReset>
        RequestCommon<RequestContentKnowledgeSearch> request;
        try {
            request = JSON.parseObject(
                body,
                new TypeReference<RequestCommon<RequestContentKnowledgeSearch>>() {}
            );
        } catch (Exception ex) {
            msg.setFail("请求体不是合法的 JSON 或解析失败: " + ex.getMessage());
            resp.setMsg(msg);
            resp.setCode(400);
            resp.setContent(new Object[]{});
            resp.setCount(0);
            return resp;
        }

        // 打印（调试）
        System.out.println("收到请求: " + request);

        boolean requestBodyValid = isRequestBodyValid(request);
        if (!requestBodyValid) {
            msg.setFail("fromId,fromNickname,content 验证失败");
            resp.setMsg(msg);
            resp.setCode(400);
            resp.setContent(new Object[]{});
            resp.setCount(0);
            return resp;
        }


        // 根据你实际的 RequestContentKnowledgeReset 字段进行更细粒度校验，这里以 schema 字段为例
        String query = request.getContent().getQuery();
        if (!StringUtils.hasText(query)) {
            msg.setFail("content.query 为空");
            resp.setMsg(msg);
            resp.setCode(400);
            resp.setContent(new Object[]{});
            resp.setCount(0);
            return resp;
        }
        // 校验 schema 字段
        String schema = normalizeSchema(request.getContent().getSchema());
        if (!StringUtils.hasText(schema) || !isSchemaNameValid(schema)) {
            msg.setFail("content.schema 非法，仅支持字母数字下划线");
            resp.setMsg(msg);
            resp.setCode(400);
            resp.setContent(new Object[]{});
            resp.setCount(0);
            return resp;
        }


        logger.info("MeasureTools.searchForInfo: " + query);
        RagQuery ragQuery = RagQuery.valueOf(query);
        try {
            String result = graphSearch.search(schema,ragQuery);
            logger.info("result: " + result);
            if (result == null || result.isEmpty()) {
                msg.setFail("没有找到相关信息");
                resp.setMsg(msg);
                resp.setCode(500);
                resp.setContent(new Object[]{});
                resp.setCount(0);
                return resp;
            }
            msg.setSuccess("操作成功");
            resp.setMsg(msg);
            resp.setCode(200);
            resp.setContent(new Object[]{result});
            resp.setCount(1);
            return resp;
        } catch (Exception e) {
            e.printStackTrace();
            msg.setFail(e.getMessage());
            resp.setMsg(msg);
            resp.setCode(500);
            resp.setContent(new Object[]{});
            resp.setCount(0);
            return resp;
        }
    }

    // 知识库检索接口（v4）
    @ResponseBody
    @PostMapping("/v4/kgsearch")
    public ResponseCommon<Object[]> kgSearchV4(@RequestBody String body) {
        ResponseCommon.Message msg = new ResponseCommon.Message("", "");
        ResponseCommon<Object[]> resp = new ResponseCommon<>();

        RequestCommon<RequestContentKnowledgeSearchV4> request;
        try {
            request = JSON.parseObject(
                body,
                new TypeReference<RequestCommon<RequestContentKnowledgeSearchV4>>() {}
            );
        } catch (Exception ex) {
            msg.setFail("请求体不是合法的 JSON 或解析失败: " + ex.getMessage());
            resp.setMsg(msg);
            resp.setCode(400);
            resp.setContent(new Object[]{});
            resp.setCount(0);
            return resp;
        }

        if (!isRequestBodyValid(request)) {
            msg.setFail("fromId,fromNickname,content 验证失败");
            resp.setMsg(msg);
            resp.setCode(400);
            resp.setContent(new Object[]{});
            resp.setCount(0);
            return resp;
        }

        String query = request.getContent().getQuery();
        if (!StringUtils.hasText(query)) {
            msg.setFail("content.query 为空");
            resp.setMsg(msg);
            resp.setCode(400);
            resp.setContent(new Object[]{});
            resp.setCount(0);
            return resp;
        }

        String schema = normalizeSchema(request.getContent().getSchema());
        if (!StringUtils.hasText(schema) || !isSchemaNameValid(schema)) {
            msg.setFail("content.schema 非法，仅支持字母数字下划线");
            resp.setMsg(msg);
            resp.setCode(400);
            resp.setContent(new Object[]{});
            resp.setCount(0);
            return resp;
        }

        Set<String> categories = normalizeCategories(request.getContent().getCategories());
        int maxCommunityCount = normalizeLimit(
            request.getContent().getMaxCommunityCount(),
            graphSearch.getMaxSegmentSize(schema)
        );
        int maxEntityCount = normalizeLimit(
            request.getContent().getMaxEntityCount(),
            graphSearch.getMaxEntitySize(schema)
        );
        int maxSegmentCount = normalizeLimit(
            request.getContent().getMaxSegmentCount(),
            graphSearch.getMaxSegmentSize(schema)
        );
        int maxRelationshipCount = normalizeLimit(
            request.getContent().getMaxRelationshipCount(),
            graphSearch.getMaxRelationshipSize(schema)
        );

        try {
            RagQuery ragQuery = RagQuery.valueOf(query);
            GraphSearch.SearchV4Result result = graphSearch.searchV4(
                schema,
                ragQuery,
                categories,
                maxCommunityCount,
                maxEntityCount,
                maxSegmentCount,
                maxRelationshipCount
            );

            if (result.getTotalCount() == 0) {
                msg.setFail("没有找到相关信息");
                resp.setMsg(msg);
                resp.setCode(500);
                resp.setContent(new Object[]{});
                resp.setCount(0);
                return resp;
            }

            msg.setSuccess("操作成功");
            resp.setMsg(msg);
            resp.setCode(200);
            resp.setContent(new Object[]{result});
            resp.setCount(result.getTotalCount());
            return resp;
        } catch (Exception e) {
            e.printStackTrace();
            msg.setFail(e.getMessage());
            resp.setMsg(msg);
            resp.setCode(500);
            resp.setContent(new Object[]{});
            resp.setCount(0);
            return resp;
        }
    }

    // 重置知识库接口
    @PostMapping("/v3/reset")
    public ResponseCommon<Object[]> resetKnowledgeV3(@RequestBody String body) {
        ResponseCommon.Message msg = new ResponseCommon.Message("", "");
        ResponseCommon<Object[]> resp = new ResponseCommon<>();

        // 解析 JSON 为 RequestCommon<RequestContentKnowledgeReset>
        RequestCommon<RequestContentKnowledgeReset> request;
        try {
            request = JSON.parseObject(
                body,
                new TypeReference<RequestCommon<RequestContentKnowledgeReset>>() {}
            );
        } catch (Exception ex) {
            msg.setFail("请求体不是合法的 JSON 或解析失败: " + ex.getMessage());
            resp.setMsg(msg);
            resp.setCode(400);
            resp.setContent(new Object[]{});
            resp.setCount(0);
            return resp;
        }

        // 打印（调试）
        System.out.println("收到请求: " + request);

        boolean requestBodyValid = isRequestBodyValid(request);
        if (!requestBodyValid) {
            msg.setFail("fromId,fromNickname,content 验证失败");
            resp.setMsg(msg);
            resp.setCode(400);
            resp.setContent(new Object[]{});
            resp.setCount(0);
            return resp;
        }

        // 根据你实际的 RequestContentKnowledgeReset 字段进行更细粒度校验，这里以 schema 字段为例
        String schema = normalizeSchema(request.getContent().getSchema());
        if (!StringUtils.hasText(schema) || !isSchemaNameValid(schema)) {
            msg.setFail("content.schema 非法，仅支持字母数字下划线");
            resp.setMsg(msg);
            resp.setCode(400);
            resp.setContent(new Object[]{});
            resp.setCount(0);
            return resp;
        }

        // 打印/处理请求（示例）
        System.out.println("fromId = " + request.getFromId());
        System.out.println("fromNickname = " + request.getFromNickname());
        System.out.println("content.schema = " + schema);

        // 删除并重建 对应的 schema
        String sql = "DROP SCHEMA IF EXISTS " + schema;
        jdbcTemplate.execute(sql);
        // 重建 对应的 schema
        schemaService.initializeSchemas(new String[]{schema});

        // 删除对应的目录下的全部文件
        // 清空 kg.inputDir 目录

        // 删除 kg.inputDir/schema 目录，如果目录里有文件，也会删除
        try {
            Files.walk(Paths.get(kgProperties.getInputDir() + "/" + schema))
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        Files.delete(path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
            // 删除 kg.inputDir/schema 目录，如果存在的话
            // 如果目录不存在，就不会抛出异常
            Files.deleteIfExists(Paths.get(kgProperties.getInputDir() + "/" + schema));
        } catch (IOException e) {
//            e.printStackTrace();
//            msg.setFail(e.getMessage());
//
//            resp.setMsg(msg);
//            resp.setCode(500);
//            resp.setContent(new Object[]{});
//            resp.setCount(0);
//            return resp;
        }

        // 业务处理成功响应
        msg.setSuccess("操作成功");
        resp.setMsg(msg);
        resp.setCode(200);
        resp.setContent(new Object[]{});
        resp.setCount(0);

        return resp;
    }

    private ResponseCommon<Object[]> queryBuildStatus(String body, boolean includeTaskDetails) {
        ResponseCommon.Message msg = new ResponseCommon.Message("", "");
        ResponseCommon<Object[]> resp = new ResponseCommon<>();

        RequestCommon<RequestContentKnowledgeBuildStatus> request;
        try {
            request = JSON.parseObject(
                body,
                new TypeReference<RequestCommon<RequestContentKnowledgeBuildStatus>>() {}
            );
        } catch (Exception ex) {
            msg.setFail("请求体不是合法的 JSON 或解析失败: " + ex.getMessage());
            resp.setMsg(msg);
            resp.setCode(400);
            resp.setContent(new Object[]{});
            resp.setCount(0);
            return resp;
        }

        if (!isRequestBodyValid(request)) {
            msg.setFail("fromId,fromNickname,content 验证失败");
            resp.setMsg(msg);
            resp.setCode(400);
            resp.setContent(new Object[]{});
            resp.setCount(0);
            return resp;
        }

        String schema = request.getContent().getSchema();
        if (!StringUtils.hasText(schema)) {
            msg.setFail("content.schema 为空");
            resp.setMsg(msg);
            resp.setCode(400);
            resp.setContent(new Object[]{});
            resp.setCount(0);
            return resp;
        }

        String fileName = normalizeFileName(request.getContent().getFileName());
        List<Map<String, Object>> tasks;
        if (StringUtils.hasText(fileName)) {
            tasks = buildTaskService.findBySchemaAndFile(schema, fileName);
        } else {
            tasks = buildTaskService.findBySchema(schema);
        }

        if (tasks == null || tasks.isEmpty()) {
            msg.setFail("未找到构建任务");
            resp.setMsg(msg);
            resp.setCode(404);
            resp.setContent(new Object[]{});
            resp.setCount(0);
            return resp;
        }

        Map<String, Object> summary = buildTaskService.summarizeStatus(tasks);
        String overallStatus = String.valueOf(summary.get("overallStatus"));
        if ("BUILDING".equalsIgnoreCase(overallStatus)) {
            msg.setFail("构建中");
            resp.setCode(500);
        } else if ("FAILED".equalsIgnoreCase(overallStatus)) {
            msg.setFail("构建失败");
            resp.setCode(500);
        } else if ("SUCCESS".equalsIgnoreCase(overallStatus)) {
            msg.setSuccess("构建完成");
            resp.setCode(200);
        } else {
            msg.setFail("构建状态未知");
            resp.setCode(500);
        }

        resp.setMsg(msg);
        if (includeTaskDetails) {
            List<Map<String, Object>> maskedTasks = new ArrayList<>();
            for (Map<String, Object> task : tasks) {
                Map<String, Object> masked = new HashMap<>(task);
                masked.remove("input_path");
                maskedTasks.add(masked);
            }
            Map<String, Object> content = new HashMap<>();
            content.put("summary", summary);
            content.put("tasks", maskedTasks);
            resp.setContent(new Object[]{content});
        } else {
            resp.setContent(new Object[]{});
            msg.setFail(msg.getFail() + "v3/buildStatus接口即将弃用，请将接口切换到v4/buildStatus以获取构建详情");
            msg.setSuccess(msg.getSuccess() + "v3/buildStatus接口即将弃用，请将接口切换到v4/buildStatus以获取构建详情");
        }
        resp.setCount(tasks.size());
        return resp;
    }

    public static boolean isRequestBodyValid(RequestCommon request) {

        // 校验 fromId
        String fromId = request.getFromId();
        if (!StringUtils.hasText(fromId)) {
            return false;
        }

        // 校验 fromNickname
        String fromNickname = request.getFromNickname();
        if (!StringUtils.hasText(fromNickname)) {
            return false;
        }

        // 校验 content
        Object content = request.getContent();
        if (content == null) {
            return false;
        }
        return true;
    }

    private Set<String> normalizeCategories(List<String> categories) {
        Set<String> all = new HashSet<>(Arrays.asList("community", "entity", "segment", "relationship"));
        if (categories == null || categories.isEmpty()) {
            return all;
        }
        Set<String> normalized = new HashSet<>();
        for (String category : categories) {
            if (!StringUtils.hasText(category)) {
                continue;
            }
            String key = category.trim().toLowerCase(Locale.ROOT);
            if ("community".equals(key) || "communities".equals(key)) {
                normalized.add("community");
            } else if ("entity".equals(key) || "entities".equals(key)) {
                normalized.add("entity");
            } else if ("segment".equals(key) || "segments".equals(key)) {
                normalized.add("segment");
            } else if ("relationship".equals(key) || "relationships".equals(key)
                || "relation".equals(key) || "relations".equals(key)) {
                normalized.add("relationship");
            }
        }
        if (normalized.isEmpty()) {
            return all;
        }
        return normalized;
    }

    private int normalizeLimit(Integer input, int defaultValue) {
        if (input == null) {
            return defaultValue;
        }
        if (input < 0) {
            return 0;
        }
        return input;
    }

    private String normalizeSchema(String schema) {
        if (schema == null) {
            return null;
        }
        return schema.trim();
    }

    private String normalizeFileName(String fileName) {
        if (fileName == null) {
            return null;
        }
        return fileName.trim();
    }

    private boolean isSchemaNameValid(String schema) {
        return schema != null && schema.matches("^[A-Za-z0-9_]+$");
    }

    private void executeBuildTaskWithHeartbeat(String taskId, String schema, String fileName, Path filePath) {
        final AtomicBoolean running = new AtomicBoolean(true);
        final int heartbeatIntervalMs = kgProperties.getTaskHeartbeatIntervalMs();
        Thread heartbeatThread = new Thread(() -> {
            while (running.get()) {
                try {
                    buildTaskService.markHeartbeat(taskId);
                    Thread.sleep(heartbeatIntervalMs);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                } catch (Exception ex) {
                    logger.warning("heartbeat update failed, taskId=" + taskId + ", error=" + ex.getMessage());
                }
            }
        });
        heartbeatThread.setName("build-heartbeat-" + taskId);
        heartbeatThread.setDaemon(true);

        try {
            buildTaskService.markRunning(taskId);
            heartbeatThread.start();

            graphBuilder.buildGraph(schema, filePath);
            String ingestedFile = jdbcRepository.findKGFileByName(schema, fileName);
            if (ingestedFile != null) {
                buildTaskService.markSuccess(taskId);
            } else {
                buildTaskService.markFailed(taskId, "构建未完成，未写入kgfile");
            }
        } catch (Exception ex) {
            buildTaskService.markFailed(taskId, ex.getMessage());
            logger.warning("build failed, taskId=" + taskId + ", error=" + ex.getMessage());
        } finally {
            running.set(false);
            heartbeatThread.interrupt();
        }
    }

}
