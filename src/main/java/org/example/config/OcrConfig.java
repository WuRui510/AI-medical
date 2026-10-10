package org.example.config;

import net.sourceforge.tess4j.Tesseract;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OcrConfig {

    @Value("${tesseract.language:chi_sim+eng}")
    private String language;

    @Value("${tesseract.datapath:classpath:tessdata/}")
    private String datapath;

    @Bean
    public Tesseract tesseract() {
        Tesseract tesseract = new Tesseract();
        // 统一使用配置项，避免硬编码路径导致打包后运行时找不到训练数据
        tesseract.setDatapath(datapath);
        tesseract.setLanguage(language);

        return tesseract;
    }

}