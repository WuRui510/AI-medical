# Tesseract OCR 语言包说明

## 为什么不在仓库里

Tesseract 的语言训练数据体积很大：

| 文件 | 体积 | 用途 |
| :--- | ---: | :--- |
| `chi_sim.traineddata` | 约 42 MB | 简体中文识别 |
| `eng.traineddata` | 约 22 MB | 英文识别 |

`chi_sim` 单个文件 42 MB，已接近 GitHub 对单文件的告警阈值，
且这两个文件可从官方渠道稳定下载，因此**未纳入版本控制**
（见根目录 `.gitignore` 中的 `*.traineddata`）。

---

## 放置位置

全部放入同一个目录：

```text
src/main/resources/tessdata/
    ├── chi_sim.traineddata
    └── eng.traineddata
```

该目录已保留在仓库中（含说明文件），只需放入语言包即可。

---

## 下载方式

### 方式一：官方 tessdata 仓库（推荐）

从 `tessdata_fast`（体积小、速度快，绝大多数场景够用）或
`tessdata_best`（精度更高、体积更大）下载：

- <https://github.com/tesseract-ocr/tessdata_fast>
- <https://github.com/tesseract-ocr/tessdata_best>

直接下载对应的 raw 文件：

```bash
# 进入目标目录
cd src/main/resources/tessdata

# tessdata_fast 版本（推荐）
curl -LO https://github.com/tesseract-ocr/tessdata_fast/raw/main/chi_sim.traineddata
curl -LO https://github.com/tesseract-ocr/tessdata_fast/raw/main/eng.traineddata
```

### 方式二：jsDelivr CDN（国内访问较快）

```bash
cd src/main/resources/tessdata

curl -LO https://cdn.jsdelivr.net/gh/tesseract-ocr/tessdata_fast@main/chi_sim.traineddata
curl -LO https://cdn.jsdelivr.net/gh/tesseract-ocr/tessdata_fast@main/eng.traineddata
```

### 方式三：apt 安装后复制（Linux）

```bash
sudo apt-get install tesseract-ocr tesseract-ocr-chi-sim

# 复制到项目
cp /usr/share/tesseract-ocr/*/tessdata/chi_sim.traineddata src/main/resources/tessdata/
cp /usr/share/tesseract-ocr/*/tessdata/eng.traineddata   src/main/resources/tessdata/
```

---

## 验证

下载完成后确认文件存在且体积正常：

```bash
ls -lh src/main/resources/tessdata/
```

预期输出（体积相近即可）：

```text
-rw-r--r-- 1 user user  22M eng.traineddata
-rw-r--r-- 1 user user  42M chi_sim.traineddata
```

随后可运行 OCR 相关测试验证：

```bash
mvn test -Dtest=PineconeEmbeddingStoreTest#testUploadKnowledge
```

该测试会读取 `knowledge/hospital.png`，
用 Tesseract 提取文字后与 Markdown 文档一起向量化入库。

---

## 可选的降级方案

如果不想下载语言包，也可以**跳过 OCR 环节**：

1. `PineconeEmbeddingStoreTest#testUploadKnowledge` 中已对图片做了
   `imageFile.exists()` 判断，图片缺失时会自动跳过 OCR，不影响其余文档入库；
2. 但 `OcrConfig` 仍会创建 `Tesseract` Bean。若启动阶段报错，
   可临时移除 `src/main/resources/knowledge/hospital.png` 的引用，
   或直接注释掉 `OcrConfig`（仅当完全不使用 OCR 时）。

> 💡 提示：`tessdata_fast` 版本体积约为 `tessdata_best` 的 1/3 ~ 1/2，
> 识别速度明显更快，本项目这类医院信息图片场景精度已足够。
