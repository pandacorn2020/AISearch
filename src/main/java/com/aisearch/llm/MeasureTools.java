package com.aisearch.llm;

import com.aisearch.service.GraphSearch;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.apache.logging.log4j.Logger;

public class MeasureTools {
    private GraphSearch graphSearch;

    private SessionData sessionData;
    private static Logger logger = org.apache.logging.log4j.LogManager.getLogger(MeasureTools.class);

    public MeasureTools(SessionData sessionData, GraphSearch graphSearch) {
        this.sessionData = sessionData;
        this.graphSearch = graphSearch;
    }


    @Tool("""
          Use this tool to query backend data system for information with a text as query parameter.  
            """)
    public String searchForInfo(@P("input") String arg0) {
        logger.info("MeasureTools.searchForInfo: {}", arg0);
        String inputText = arg0;
        RagQuery ragQuery = RagQuery.valueOf(arg0);
        String result = graphSearch.search(ragQuery);
        logger.info("result: {}", result);
        if (result == null || result.isEmpty()) {
            return "没有找到相关信息";
        }
        return String.format(MESSAGE_PATTERN, inputText, result);
    }

    static String MESSAGE_PATTERN = """
            用户的提问：%s
            下面是搜索得到的相关信息：
            %s
            请用上面的数据来组织语言来生成回答用户提问的报告，并在报告的最后附上数据来源信息。
            """;

}
