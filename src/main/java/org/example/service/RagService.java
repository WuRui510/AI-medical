package org.example.service;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.UserMessage;
import reactor.core.publisher.Flux;

public interface RagService {

    Flux<String> executeRag(@MemoryId Long memoryId, @UserMessage String message);
}
