package org.example;

import org.example.service.RagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class RagServiceTest {

    @Autowired
    private RagService ragService;

    @Test
    public void testRag() {
        System.out.println(ragService.executeRag(22L, "使用联网搜索，告诉我彭斌医生如何"));
    }
}
