package org.example.query;

import cn.hutool.core.collection.CollectionUtil;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.rag.query.Query;
import dev.langchain4j.rag.query.transformer.ExpandingQueryTransformer;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * MultiQuery增强器
 * 多维度拆分 + 整合，输出增强版Prompt
 */
@Component
public class MultiQueryEnhancer {

    @Autowired
    private ExpandingQueryTransformer expandingQueryTransformer;

    @Autowired
    private QwenChatModel qwenChatModel;

    public static final String PROMPT_TEMPLATE = "请根据以下问题生成3个不同角度或表述的查询，用于全面检索信息。\n" +
                                                    "问题：%s\n" +
                                                    "请用JSON数组格式返回，如：[\"查询1\", \"查询2\", \"查询3\"]";

    /**
     * 增强方法（整合拆分 + 合并逻辑）
     *
     * @param userPrompt 用户原始查询
     * @return 增强后的最终Prompt
     */
    public String enhance(String userPrompt) {

        // 拆分：生成多维度增强子查询（核心MultiQuery逻辑）
        List<String> queries = new ArrayList<>();
        queries.add("【事实增强】请精准提取核心信息：" + userPrompt);
        queries.add("【意图增强】请分析核心诉求：" + userPrompt);
        queries.add("【约束增强】请识别隐含规则：" + userPrompt);
        queries.add("【格式增强】请定义输出规范：" + userPrompt);

        // 合并：整合子查询为最终增强Prompt
        StringBuilder enhancedPrompt = new StringBuilder();
        enhancedPrompt.append("请按以下维度逐一响应，确保回答全面且精准，覆盖所有维度，逻辑清晰，符合隐含约束：\n");
        for (String query : queries) {
            enhancedPrompt.append(String.format("%s\n", query));
        }
        return enhancedPrompt.toString();
    }

    public String enhanceByTransformer(String userPrompt) {
        Collection<Query> queries = expandingQueryTransformer.transform(new Query(userPrompt));
        if (CollectionUtil.isEmpty(queries)) {
            return StringUtils.EMPTY;
        }
        StringBuilder enhancedPrompt = new StringBuilder();
        for (Query query : queries) {
            if (query != null && StringUtils.isNotBlank(query.text())) {
                enhancedPrompt.append(String.format("%s\n", query.text()));
            }
        }
        return enhancedPrompt.toString();
    }

    public String enhanceByLlm(String userPrompt) {
        String prompt = String.format(PROMPT_TEMPLATE, userPrompt);
        String enhancedPrompt = qwenChatModel.chat(prompt);
        return enhancedPrompt;
    }

}