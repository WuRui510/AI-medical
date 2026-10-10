package org.example.assistant;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

@AiService(
        //因为在配置文件中同时配置了多个大语言模型，所以需要在这里明确指定（EXPLICIT）模型的beanName（qwenChatModel）
        wiringMode = AiServiceWiringMode.EXPLICIT,
        streamingChatModel = "qwenStreamingChatModel",
        chatMemoryProvider = "chatMemoryProvider",
        contentRetriever = "pineconeRetriever",
        tools = {"appointmentTools", "pdfGenerationTool", "webSearchTool"}
)
public interface Assistant {

    // 医疗AI
    @SystemMessage(fromResource = "medical-prompt-template.txt")
    Flux<String> chatMedical(@MemoryId long memoryId, @UserMessage String message);

}
