package org.example.config;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeEmbeddingStore;
import dev.langchain4j.store.embedding.pinecone.PineconeServerlessIndexConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EmbeddingStoreConfig {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Value("${pinecone.index-name:medical-index1}")
    private String indexName;

    @Value("${pinecone.namespace:medical-namespace1}")
    private String namespace;

    @Value("${pinecone.cloud:AWS}")
    private String cloud;

    @Value("${pinecone.region:us-east-1}")
    private String region;

    @Bean
    public EmbeddingStore<TextSegment> embeddingStore() {
        // 从环境变量读取 Pinecone API Key，避免密钥进入代码仓库
        String apiKey = System.getenv("PINECONE_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("环境变量 PINECONE_API_KEY 未设置，无法连接 Pinecone 向量库");
        }

        // 创建向量存储
        return PineconeEmbeddingStore.builder()
                .apiKey(apiKey)
                // 如果指定的索引不存在，将创建一个新的索引
                .index(indexName)
                // 指定的名称空间
                .nameSpace(namespace)
                .createIndex(PineconeServerlessIndexConfig.builder()
                        // 指定索引部署在云服务上
                        .cloud(cloud)
                        // 指定索引所在区域
                        .region(region)
                        // 指定索引的向量维度，该维度与 embeddingModel 生成的向量维度相同
                        .dimension(embeddingModel.dimension())
                        .build())
                .build();
    }
}
