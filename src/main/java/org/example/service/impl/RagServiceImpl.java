package org.example.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import dev.langchain4j.community.model.dashscope.QwenChatModel;
import dev.langchain4j.data.message.*;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.Query;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.example.query.QueryRewriter;
import org.example.service.RagService;
import org.example.assistant.ToolsCallingAssistant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
@Slf4j
public class RagServiceImpl implements RagService {

    @Autowired
    private QueryRewriter queryRewriter;

    @Autowired
    private ContentRetriever pineconeRetriever;

    @Autowired
    private ChatMemoryProvider chatMemoryProvider;

    @Autowired
    private QwenChatModel qwenChatModel;

    @Autowired
    private ToolsCallingAssistant toolsCallingAssistant;


    public static final String RAG_PROMPT = """
        用户提示词
        %s
        
        检索上下文
        %s
        
        对话历史
        %s
        
        回答要求
        用上述所有信息综合回答
        """;

    @Override
    public Flux<String> executeRag(Long memoryId, String message) {
        if (memoryId == null || StringUtils.isBlank(message)) {
            return Flux.empty();
        }

        log.info("memoryId={}, message={}", memoryId, message);
        // 查询重写
        String rewriteMessage = queryRewriter.rewrite(message);
        log.info("rewriteMessage={}", rewriteMessage);

        // 检索
        List<Content> contentList = pineconeRetriever.retrieve(new Query(rewriteMessage));
        String retrieverContext = convertToString(contentList);
        log.info("retrieverContext={}", retrieverContext);

        // 对话历史
        String chatHistory = StringUtils.EMPTY;
        ChatMemory chatMemory = chatMemoryProvider.get(memoryId);
        if (chatMemory != null && CollectionUtil.isNotEmpty(chatMemory.messages())) {
            List<ChatMessage> chatMessageList = chatMemory.messages();
            chatHistory = ChatMessageSerializer.messagesToJson(chatMessageList);
            chatMemory.add(UserMessage.userMessage(rewriteMessage));
        }

        // 使用大模型总结压缩对话历史，防止输入过长
        String chatHistorySummary = StringUtils.EMPTY;
        if (StringUtils.isNotBlank(chatHistory)) {
            chatHistorySummary = qwenChatModel.chat("将这段对话进行总结，结果不得超过100字" + chatHistory);
        }
        log.info("chatHistorySummary={}", chatHistorySummary);

        // 构建最终输入给LLM的Prompt：用户提示词 + 检索上下文 + 对话历史 （+ 系统提示词）
        String userMessage = String.format(RAG_PROMPT,
                rewriteMessage,
                retrieverContext,
                chatHistorySummary);

        // 借助AIService实现Function Calling和流式输出
        Flux<String> aiMessageFlux = toolsCallingAssistant.chat(userMessage);

        StringBuilder sb = new StringBuilder();
        // 在流式传输的同时收集完整响应，防止阻塞
        return aiMessageFlux
                .doOnNext(chunk -> {
                    // 收集每个块
                    sb.append(chunk);
                })
                .doOnComplete(() -> {
                    // 流完成后，将完整的AI消息添加到聊天记忆
                    if (chatMemory != null && !sb.isEmpty()) {
                        String aiMessage = sb.toString();
                        chatMemory.add(AiMessage.from(aiMessage));
                    }
                });
    }

    private static String convertToString(List<Content> contents) {
        StringBuilder sb = new StringBuilder();
        for (Content content : contents) {
            String text = content.textSegment().text();
            sb.append(text).append("\n");
        }
        return sb.toString().trim();
    }

}
