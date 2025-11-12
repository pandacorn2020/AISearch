package com.aisearch.llm;

import com.aisearch.service.GraphSearch;
import com.aisearch.service.Schemas;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.apache.logging.log4j.Logger;

public class MeasureTools {
    private GraphSearch graphSearch;

    private SessionData sessionData;

    private String schema;
    private static Logger logger = org.apache.logging.log4j.LogManager.getLogger(MeasureTools.class);

    public MeasureTools(SessionData sessionData, GraphSearch graphSearch, String schema) {
        this.sessionData = sessionData;
        this.graphSearch = graphSearch;
        this.schema = schema;
    }
    public MeasureTools(SessionData sessionData, GraphSearch graphSearch) {
        this.sessionData = sessionData;
        this.graphSearch = graphSearch;
        this.schema = Schemas.DOCS;
    }


    @Tool("""
          Use this tool to query backend data system for information with a text as query parameter.  
            """)
    public String searchForInfo(@P("input") String arg0) {
        logger.info("MeasureTools.searchForInfo: {}", arg0);
        String inputText = arg0;
        RagQuery ragQuery = RagQuery.valueOf(arg0);
        String result = graphSearch.search(this.schema,ragQuery);
        logger.info("result: {}", result);
        if (result == null || result.isEmpty()) {
            return "没有找到相关信息";
        }
        return result;
    }

}
