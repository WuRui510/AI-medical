package org.example.query;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class QueryRewriter {

    @Autowired
    private QwenChatModel qwenChatModel;

    /**
     * 查询重写
     * @param query 原始查询文本
     * @return 重写后的查询文本
     */
    public String rewrite(String query) {
        String promptTemplate = """
                优化以下查询：修正拼写/语法错误、补全核心信息、口语转书面语，仅返回重写结果。
                原始查询：%s
                重写结果：
                """;

        // 构建Prompt并调用大模型完成重写
        return qwenChatModel.chat(String.format(promptTemplate, query));
    }
}
