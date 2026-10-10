package org.example.assistant;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

@AiService(
        wiringMode = AiServiceWiringMode.EXPLICIT,
        streamingChatModel = "qwenStreamingChatModel",
        tools = {"appointmentTools", "pdfGenerationTool", "webSearchTool"}
)
public interface ToolsCallingAssistant {

    // 只借助框架自动处理工具调用、流式输出
    @SystemMessage(fromResource = "medical-prompt-template.txt")
    Flux<String> chat(@UserMessage String userMessage);
}
