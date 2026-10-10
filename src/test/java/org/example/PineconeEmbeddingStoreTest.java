package org.example;

import dev.langchain4j.data.document.Document;
import dev.langchain4j.data.document.loader.FileSystemDocumentLoader;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.*;
import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.Tesseract;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
@Slf4j
public class PineconeEmbeddingStoreTest {

    @Autowired
    private EmbeddingModel embeddingModel;

    @Autowired
    private EmbeddingStore embeddingStore;

    @Autowired
    private Tesseract tesseract;

    @Test
    public void pineconeEmbed() {

        TextSegment segment1 = TextSegment.from("我喜欢羽毛球");
        // 文本转换成向量
        Embedding embedding1 = embeddingModel.embed(segment1).content();
        // 存储向量数据库
        embeddingStore.add(embedding1, segment1);

        TextSegment segment2 = TextSegment.from("今天天气很好");
        Embedding embedding2 = embeddingModel.embed(segment2).content();
        embeddingStore.add(embedding2, segment2);
    }

    @Test
    public void pineconeSearch() {
        // 将问题转换成向量数据
        Embedding query = embeddingModel.embed("你最喜欢的运动是什么").content();
        // 创建向量搜索请求对象
        EmbeddingSearchRequest searchRequest = EmbeddingSearchRequest.builder()
                .queryEmbedding(query)
                // 匹配最相似的一条记录
                .maxResults(1)
                // 相似度在0.8及以上
                .minScore(0.8)
                .build();

        // 从向量数据库中进行搜索
        EmbeddingSearchResult<TextSegment> search = embeddingStore.search(searchRequest);
        // 获取第一个匹配的选项
        EmbeddingMatch<TextSegment> textSegmentEmbeddingMatch = search.matches().get(0);
        // 获取匹配项的相似度得分
        System.out.println(textSegmentEmbeddingMatch.score());
        // 获取匹配项的文本
        System.out.println(textSegmentEmbeddingMatch.embedded().text());

    }

    // 提前将数据存储到向量数据库中
    @Test
    public void testUploadKnowledge() {
        // 获取resources目录下knowledge文件夹的路径
        String knowledgePath = "src/main/resources/knowledge/";

        // 使用FileSystemDocumentLoader读取项目resources目录下的知识库文档
        Document document1 = FileSystemDocumentLoader.loadDocument(knowledgePath + "医院信息.md");
        Document document2 = FileSystemDocumentLoader.loadDocument(knowledgePath + "科室信息.md");
        Document document3 = FileSystemDocumentLoader.loadDocument(knowledgePath + "神经内科.md");
        List<Document> documents = new ArrayList<>(Arrays.asList(document1, document2, document3));

        try {
            File imageFile = new File(knowledgePath + "hospital.png");
            if (imageFile.exists()) {
                // 使用OCR提取图片文字
                BufferedImage image = ImageIO.read(imageFile);
                String ocrText = tesseract.doOCR(image);
                ocrText = ocrText.trim();
                // 创建图片文档
                String content = "OCR识别内容:\n" + ocrText;
                Document imageDocument = Document.from(content);
                documents.add(imageDocument);
                log.info("do OCR success, content: {}", content);
            }
        } catch (Exception e) {
            log.error("do OCR error", e);
        }

        // 文本向量化并存入向量数据库
        EmbeddingStoreIngestor
                .builder()
                .embeddingStore(embeddingStore)
                .embeddingModel(embeddingModel)
                .build()
                .ingest(documents);
    }

}
