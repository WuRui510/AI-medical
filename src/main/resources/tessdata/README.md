# Tesseract OCR 语言包目录

此目录用于存放 Tesseract 的语言训练数据文件：

- `chi_sim.traineddata` —— 简体中文（约 42 MB）
- `eng.traineddata`     —— 英文（约 22 MB）

这些文件体积较大且可随时下载，因此**未纳入版本控制**
（参见根目录 `.gitignore`）。

## 请在此目录放入语言包

```bash
cd src/main/resources/tessdata

# tessdata_fast 版本（推荐，速度快、体积小）
curl -LO https://github.com/tesseract-ocr/tessdata_fast/raw/main/chi_sim.traineddata
curl -LO https://github.com/tesseract-ocr/tessdata_fast/raw/main/eng.traineddata
```

完整说明（含国内 CDN、apt 安装等方式）见
[`docs/TESSDATA.md`](../../../../docs/TESSDATA.md)。

## 配置对应关系

`application.properties` 中的配置项：

```properties
tesseract.language=chi_sim+eng
tesseract.datapath=classpath:tessdata/
```

`OcrConfig` 会据此初始化 Tesseract 实例。
