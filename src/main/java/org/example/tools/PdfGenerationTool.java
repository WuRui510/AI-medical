package org.example.tools;

import cn.hutool.core.io.FileUtil;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * PDF 生成工具
 */
@Component
@Slf4j
public class PdfGenerationTool {

    /**
     * PDF 输出目录，可通过 pdf.output-dir 配置覆盖。
     * 默认写到项目工作目录下的 output/pdf，避免依赖开发者本机的绝对路径。
     */
    @Value("${pdf.output-dir:./output/pdf}")
    private String outputDir;

    @Tool(name = "PDF生成工具", value = "根据指定的文件名和内容生成PDF文件")
    public String generatePDF(
            @P(value = "文件名", required = true) String fileName,
            @P(value = "文件内容", required = false) String content) {
        try {
            // 创建目录
            FileUtil.mkdir(outputDir);
            String filePath = outputDir + "/" + fileName;

            // 创建 PdfWriter 和 PdfDocument 对象
            try (PdfWriter writer = new PdfWriter(filePath);
                 PdfDocument pdf = new PdfDocument(writer);
                 Document document = new Document(pdf)) {
                // 使用内置中文字体
                PdfFont font = PdfFontFactory.createFont("STSongStd-Light", "UniGB-UCS2-H");
                document.setFont(font);
                // 创建段落
                Paragraph paragraph = new Paragraph(content);
                // 添加段落并关闭文档
                document.add(paragraph);
            }
            log.info("PDF generated successfully to: {}", filePath);
            return "PDF generated successfully to: " + filePath;
        } catch (IOException e) {
            log.error("Error generating PDF", e);
            return "Error generating PDF: " + e.getMessage();
        }
    }
}
