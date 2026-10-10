package org.example.controller;

import org.example.assistant.Assistant;
import org.example.bean.ChatForm;
import org.example.query.QueryRewriter;
import org.example.service.RagService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/medical")
public class AiController {

    @Autowired
    private QueryRewriter queryRewriter;

    @Autowired
    private Assistant assistant;

    @Autowired
    private RagService ragService;

    @PostMapping(value = "/chat", produces = "text/stream;charset=utf-8")
    public Flux<String> chat(@RequestBody ChatForm chatForm) {
        String rewriteMessage = queryRewriter.rewrite(chatForm.getMessage());
        return assistant.chatMedical(chatForm.getMemoryId(), rewriteMessage);
    }

    @PostMapping(value = "/rag", produces = "text/stream;charset=utf-8")
    public Flux<String> rag(@RequestBody ChatForm chatForm) {
        return ragService.executeRag(chatForm.getMemoryId(), chatForm.getMessage());
    }
}