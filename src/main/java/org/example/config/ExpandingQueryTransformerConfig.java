package org.example.config;

import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.rag.query.transformer.ExpandingQueryTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExpandingQueryTransformerConfig {

    @Autowired
    private QwenChatModel qwenChatModel;

    @Bean
    public ExpandingQueryTransformer expandingQueryTransformer() {
        return ExpandingQueryTransformer.builder()
                .chatLanguageModel(qwenChatModel)
                .n(3)
                .build();
    }
}
