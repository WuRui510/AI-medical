package org.example.tools;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 网页搜索工具
 */
@Component
@Slf4j
public class WebSearchTool {

    // SearchAPI 的搜索接口地址
    private static final String SEARCH_API_URL = "https://serpapi.com/search.json";

    private static final String GOOGLE_SEARCH_API_KEY = System.getenv("GOOGLE_SEARCH_API_KEY");

    private static final int SEARCH_ITEM_NUM = 5;

    @Tool(name = "联网搜素", value = "使用谷歌搜索引擎实现联网搜索")
    public String searchWeb(@P(value = "搜索的关键词", required = true) String query) {

        // 构建请求参数
        Map<String, Object> paramMap = new HashMap<>();
        paramMap.put("q", query);
        paramMap.put("api_key", GOOGLE_SEARCH_API_KEY);
        paramMap.put("engine", "google");
        // 搜索条数
        paramMap.put("num", SEARCH_ITEM_NUM);
        // 语言
        paramMap.put("hl", "zh-cn");

        String resultJson = null;
        try {
            resultJson = HttpUtil.get(SEARCH_API_URL, paramMap);
        } catch (Exception e) {
            log.error("searchWeb error, e", e);
        }

        // 解析JSON响应
        String result = parseResult(resultJson);
        log.info("调用searchWeb成功, result:\n {}", result);

        return result;
    }

    private String parseResult(String json) {
        JSONObject jsonObject = JSONUtil.parseObj(json);
        // 提取 organic_results 部分
        JSONArray organicResults = jsonObject.getJSONArray("organic_results");
        // 取出返回结果的前 5 条
        List<Object> objects = organicResults.subList(0, Math.min(SEARCH_ITEM_NUM, organicResults.size()));
        // 拼接搜索结果为字符串
        String result = objects.stream().map(obj -> {
            JSONObject item = (JSONObject) obj;
            // 提取摘要
            return (String)item.get("snippet");
        }).collect(Collectors.joining("\n"));

        return result;
    }
}
