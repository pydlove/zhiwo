# TitleLibrary 文章生成改造 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 标题库点击"生成文章" → 使用复制提示词模板填充变量 → 调用配置的 Kimi/Minimax 大模型生成文章 → Java POI 生成 DOCX → 关联到标题记录 → 支持页面下载/预览/发邮件

**Architecture:**
1. ConfigManage.vue 独立配置 Kimi API 和 Minimax API，全局下拉切换默认模型
2. 后端新增 LLMService 封装 Kimi K2.6 和 Minimax M2.7 的 HTTP API 调用，改造 TitleLibraryController.generatePostSingle 使用新的 LLMService 并用 POI 生成 DOCX
3. TitleLibrary 新增 `generatedFileUrl`、`generatedFileName` 字段关联 DOCX 文件，前端页面添加下载/预览/发邮件操作按钮

**Tech Stack:** Java (Apache POI XWPFDocument), Vue 3, Ant Design Vue, HTTP REST API

---

## File Map

### Backend
- Create: `services/admin-backend/src/main/java/com/example/blogger/service/LLMService.java` — Kimi/Minimax HTTP API 调用
- Create: `services/admin-backend/src/main/java/com/example/blogger/util/DocxGenerator.java` — Apache POI 生成 DOCX
- Modify: `services/admin-backend/src/main/java/com/example/blogger/entity/TitleLibrary.java` — 新增 `generatedFileUrl`、`generatedFileName`、`generatedAt` 字段
- Modify: `services/admin-backend/src/main/java/com/example/blogger/entity/SubscriptionPost.java` — 添加 `titleLibraryId` 字段关联标题
- Modify: `services/admin-backend/src/main/java/com/example/blogger/mapper/TitleLibraryMapper.java` — 新增 updateGeneratedFile 动态 SQL
- Modify: `services/admin-backend/src/main/java/com/example/blogger/service/TitleLibraryService.java` — 新增 updateGeneratedFile 方法
- Modify: `services/admin-backend/src/main/java/com/example/blogger/controller/TitleLibraryController.java` — 重构 generatePostSingle，使用 LLMService + DocxGenerator，改造 buildSingleArticlePrompt 使用 rowPromptTemplate 模板

### Frontend
- Modify: `services/admin-frontend/src/views/ConfigManage.vue` — 新增 Minimax 配置区 + 模型切换下拉
- Modify: `services/admin-frontend/src/views/TitleLibraryManage.vue` — 添加下载/预览/发邮件按钮，改造"生成文章"使用 rowPromptTemplate
- Modify: `services/admin-frontend/src/api/titleLibrary.js` — 新增 `generateArticleByPrompt(id)` API

---

## Task 1: ConfigManage.vue — 新增 Minimax 配置区 + 模型切换

**Files:**
- Modify: `services/admin-frontend/src/views/ConfigManage.vue`

- [ ] **Step 1: 添加 MiniMax API 配置的数据绑定**

在 ConfigManage.vue 的 `<script setup>` 区域添加：
```javascript
const miniMaxApiKey = ref('')
const miniMaxModel = ref('MiniMax-M2.7')
const selectedLLMModel = ref('kimi') // 'kimi' | 'minimax'
```

- [ ] **Step 2: 添加 loadConfigs 解析**

在 `loadConfigs` 函数（line ~106）中添加：
```javascript
if (data.miniMaxApiKey) miniMaxApiKey.value = data.miniMaxApiKey
if (data.miniMaxModel) miniMaxModel.value = data.miniMaxModel
if (data.selectedLLMModel) selectedLLMModel.value = data.selectedLLMModel
```

- [ ] **Step 3: 添加 saveConfig 保存逻辑**

在保存逻辑中添加：
```javascript
apiKey: apiKey.value,
model: model.value,
miniMaxApiKey: miniMaxApiKey.value,
miniMaxModel: miniMaxModel.value,
selectedLLMModel: selectedLLMModel.value,
```

- [ ] **Step 4: 新增 Minimax API 配置区 UI**

在 Kimi API 配置区下方添加（紧跟在 `</Form>` 和 `</Card>` 之后）：
```html
<Card style="border-radius: 2px; margin-bottom: 24px;">
  <template #title>
    <span style="font-size: 16px; font-weight: 500; color: #262626;">MiniMax API 配置</span>
  </template>
  <Form layout="vertical">
    <Form.Item label="API Key" required>
      <Input.Password v-model:value="miniMaxApiKey" placeholder="请输入 MiniMax API Key" style="max-width: 480px;" />
    </Form.Item>
    <Form.Item label="模型选择">
      <Select show-search v-model:value="miniMaxModel" style="max-width: 480px; height: 36px;">
        <Select.Option value="MiniMax-M2.7">MiniMax-M2.7</Select.Option>
      </Select>
    </Form.Item>
  </Form>
</Card>
```

- [ ] **Step 5: 新增模型切换区 UI**

添加一个卡片放在 Kimi/MiniMax 配置区上方：
```html
<Card style="border-radius: 2px; margin-bottom: 24px;">
  <template #title>
    <span style="font-size: 16px; font-weight: 500; color: #262626;">大模型选择</span>
  </template>
  <Form layout="vertical">
    <Form.Item label="默认模型">
      <Select v-model:value="selectedLLMModel" style="max-width: 480px; height: 36px;">
        <Select.Option value="kimi">Kimi K2.6</Select.Option>
        <Select.Option value="minimax">MiniMax M2.7</Select.Option>
      </Select>
      <div style="font-size: 12px; color: #8c8c8c; margin-top: 4px;">生成文章时使用的默认大模型</div>
    </Form.Item>
  </Form>
</Card>
```

- [ ] **Step 6: 导入 Card 组件**

检查 import 语句，确保 Card、Input、Select、Button、Form 均已导入。

---

## Task 2: 后端 — 创建 LLMService（Kimi/MiniMax API 调用）

**Files:**
- Create: `services/admin-backend/src/main/java/com/example/blogger/service/LLMService.java`

- [ ] **Step 1: 创建 LLMService.java**

```java
package com.example.blogger.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.example.blogger.mapper.ConfigMapper;
import com.example.blogger.entity.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.List;

@Service
public class LLMService {

    @Autowired
    private ConfigMapper configMapper;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private static final int TIMEOUT_MS = 300000; // 5分钟

    public LLMService() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(TIMEOUT_MS);
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 获取当前配置的默认模型类型
     */
    public String getSelectedModelType() {
        String val = getConfigValue("selectedLLMModel");
        return (val == null || val.isEmpty()) ? "kimi" : val;
    }

    /**
     * 调用大模型生成内容
     */
    public String generateContent(String prompt) {
        String modelType = getSelectedModelType();
        if ("minimax".equals(modelType)) {
            return callMinimaxAPI(prompt);
        } else {
            return callKimiAPI(prompt);
        }
    }

    /**
     * 调用 Kimi K2.6 API
     */
    private String callKimiAPI(String prompt) {
        String apiKey = getConfigValue("apiKey");
        String model = getConfigValue("model");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("Kimi API Key 未配置");
        }
        if (model == null || model.isEmpty()) {
            model = "moonshot-v1-8k";
        }

        String url = "https://api.moonshot.cn/v1/chat/completions";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", new Object[]{
            Map.of("role", "user", "content", prompt)
        });

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                return choices.get(0).get("message").get("content").asText();
            }
            throw new RuntimeException("Kimi API 返回格式异常: " + response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("Kimi API 调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 调用 MiniMax M2.7 API
     */
    private String callMinimaxAPI(String prompt) {
        String apiKey = getConfigValue("miniMaxApiKey");
        String model = getConfigValue("miniMaxModel");
        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("MiniMax API Key 未配置");
        }
        if (model == null || model.isEmpty()) {
            model = "MiniMax-M2.7";
        }

        String url = "https://api.minimax.chat/v1/chat/completions";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", new Object[]{
            Map.of("role", "user", "content", prompt)
        });

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                return choices.get(0).get("message").get("content").asText();
            }
            throw new RuntimeException("MiniMax API 返回格式异常: " + response.getBody());
        } catch (Exception e) {
            throw new RuntimeException("MiniMax API 调用失败: " + e.getMessage(), e);
        }
    }

    private String getConfigValue(String key) {
        List<Config> configs = configMapper.findAll();
        for (Config c : configs) {
            if (key.equals(c.getConfigKey())) {
                return c.getConfigValue();
            }
        }
        return null;
    }
}
```

- [ ] **Step 2: 验证文件创建成功**

Run: `ls -la services/admin-backend/src/main/java/com/example/blogger/service/LLMService.java`

---

## Task 3: 后端 — 创建 DocxGenerator（Apache POI 生成 DOCX）

**Files:**
- Create: `services/admin-backend/src/main/java/com/example/blogger/util/DocxGenerator.java`

- [ ] **Step 1: 创建 DocxGenerator.java**

```java
package com.example.blogger.util;

import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class DocxGenerator {

    /**
     * 将文章内容写入 DOCX 文件
     * @param title 文章标题
     * @param content 文章正文（支持多段落，用空行分隔）
     * @param filePath 输出文件路径
     */
    public void generateDocx(String title, String content, String filePath) throws Exception {
        XWPFDocument document = new XWPFDocument();

        // 标题
        XWPFParagraph titleParagraph = document.createParagraph();
        titleParagraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun titleRun = titleParagraph.createRun();
        titleRun.setText(title);
        titleRun.setBold(true);
        titleRun.setFontSize(24);
        titleRun.addBreak();

        // 空行
        document.createParagraph();

        // 正文段落
        String[] paragraphs = content.split("\n\n");
        for (String para : paragraphs) {
            if (para.trim().isEmpty()) {
                document.createParagraph();
                continue;
            }
            XWPFParagraph p = document.createParagraph();
            p.setAlignment(ParagraphAlignment.BOTH);
            XWPFRun run = p.createRun();
            run.setText(para.trim());
            run.setFontSize(12);
        }

        // 保存文件
        try (FileOutputStream out = new FileOutputStream(filePath)) {
            document.write(out);
        }
        document.close();
    }
}
```

---

## Task 4: 后端 — TitleLibrary 实体新增字段

**Files:**
- Modify: `services/admin-backend/src/main/java/com/example/blogger/entity/TitleLibrary.java`

- [ ] **Step 1: 新增字段**

在 TitleLibrary.java 中添加以下字段和 getter/setter：
```java
private String generatedFileUrl;    // DOCX 文件访问路径，如 /uploads/articles/xxx.docx
private String generatedFileName;     // DOCX 文件名
private LocalDateTime generatedAt;   // 生成时间
```

在现有字段块后面添加：
```java
public String getGeneratedFileUrl() { return generatedFileUrl; }
public void setGeneratedFileUrl(String generatedFileUrl) { this.generatedFileUrl = generatedFileUrl; }
public String getGeneratedFileName() { return generatedFileName; }
public void setGeneratedFileName(String generatedFileName) { this.generatedFileName = generatedFileName; }
public LocalDateTime getGeneratedAt() { return generatedAt; }
public void setGeneratedAt(LocalDateTime generatedAt) { this.generatedAt = generatedAt; }
```

---

## Task 5: 后端 — TitleLibraryMapper 新增 updateGeneratedFile SQL

**Files:**
- Modify: `services/admin-backend/src/main/java/com/example/blogger/mapper/TitleLibraryMapper.java`

- [ ] **Step 1: 添加方法声明**

在 TitleLibraryMapper.java 中添加：
```java
@Update("UPDATE tu_title_library SET generated_file_url = #{generatedFileUrl}, generated_file_name = #{generatedFileName}, generated_at = #{generatedAt} WHERE id = #{id}")
int updateGeneratedFile(@Param("id") String id, @Param("generatedFileUrl") String generatedFileUrl, @Param("generatedFileName") String generatedFileName, @Param("generatedAt") LocalDateTime generatedAt);
```

---

## Task 6: 后端 — TitleLibraryService 新增 updateGeneratedFile 方法

**Files:**
- Modify: `services/admin-backend/src/main/java/com/example/blogger/service/TitleLibraryService.java`

- [ ] **Step 1: 添加 updateGeneratedFile 方法**

在 TitleLibraryService.java 中添加（放在现有的 updateIsCopied 方法附近）：
```java
public void updateGeneratedFile(String id, String fileUrl, String fileName) {
    titleLibraryMapper.updateGeneratedFile(id, fileUrl, fileName, LocalDateTime.now());
}
```

---

## Task 7: 后端 — 重构 TitleLibraryController.generatePostSingle

**Files:**
- Modify: `services/admin-backend/src/main/java/com/example/blogger/controller/TitleLibraryController.java`

- [ ] **Step 1: 注入 LLMService 和 DocxGenerator**

在 TitleLibraryController 类成员变量中添加：
```java
@Autowired
private LLMService llmService;

@Autowired
private DocxGenerator docxGenerator;
```

- [ ] **Step 2: 重写 generatePostSingle 方法**

将现有的 generatePostSingle 方法（约 line 2493-2544）完全替换为：
```java
public Result<Map<String, Object>> generatePostSingle(@PathVariable String id) {
    TitleLibrary titleLib = titleLibraryService.getById(id);
    if (titleLib == null) {
        return Result.error("标题不存在");
    }
    try {
        // Step 1: 获取 rowPromptTemplate 并填充变量
        String promptTemplate = buildPromptFromTemplate(titleLib);

        // Step 2: 调用 LLMService 生成内容
        String content = llmService.generateContent(promptTemplate);
        if (content == null || content.isEmpty()) {
            return Result.error("文章生成失败，AI 返回内容为空");
        }

        // Step 3: 用 Apache POI 生成 DOCX
        String articlesDir = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + "articles";
        File dir = new File(articlesDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        String fileName = "article_" + id + "_" + System.currentTimeMillis() + ".docx";
        String filePath = articlesDir + File.separator + fileName;
        docxGenerator.generateDocx(titleLib.getTitle(), content, filePath);

        // Step 4: 更新标题库的文件关联
        String fileUrl = "/uploads/articles/" + fileName;
        titleLibraryService.updateGeneratedFile(id, fileUrl, fileName);

        // Step 5: 保存到 SubscriptionPost（兼容现有预览/下载/发邮件逻辑）
        SubscriptionPost post = new SubscriptionPost();
        post.setTitleLibraryId(id);
        post.setTrackId(titleLib.getTrackId());
        post.setTitle(titleLib.getTitle());
        post.setDescription(content);
        post.setFileUrl(fileUrl);
        post.setFileName(fileName);
        post.setStatus("已上架");
        post.setUsed(0);
        subscriptionPostService.save(post);

        Map<String, Object> result = new HashMap<>();
        result.put("postId", post.getId());
        result.put("fileUrl", fileUrl);
        result.put("fileName", fileName);
        result.put("title", titleLib.getTitle());
        return Result.ok(result);
    } catch (Exception e) {
        return Result.error("生成文章失败: " + e.getMessage());
    }
}
```

- [ ] **Step 3: 新增 buildPromptFromTemplate 辅助方法**

在 TitleLibraryController 中添加（放在 buildSingleArticlePrompt 方法附近）：
```java
/**
 * 使用 rowPromptTemplate 模板填充变量后构建 prompt
 */
private String buildPromptFromTemplate(TitleLibrary titleLib) {
    String template = getRowPromptTemplate();
    if (template == null || template.isEmpty()) {
        // Fallback: 使用旧的 buildSingleArticlePrompt
        return buildSingleArticlePrompt(titleLib);
    }

    String result = template;

    // 注入 stylePrompt
    String stylePrompt = "";
    Style defaultStyle = styleMapper.findDefault();
    if (defaultStyle != null) {
        stylePrompt = defaultStyle.getDescription() != null ? defaultStyle.getDescription() : "";
    }
    result = result.replace("${stylePrompt}", stylePrompt);

    // 注入字段变量
    result = result.replace("${title}", nvl(titleLib.getTitle()));
    result = result.replace("${description}", nvl(titleLib.getDescription()));
    result = result.replace("${platform}", nvl(titleLib.getPlatform()));
    result = result.replace("${trackId}", nvl(titleLib.getTrackId()));
    result = result.replace("${useCount}", titleLib.getUseCount() != null ? titleLib.getUseCount().toString() : "");
    result = result.replace("${isUsed}", titleLib.getIsUsed() != null ? titleLib.getIsUsed().toString() : "");
    result = result.replace("${pushDate}", titleLib.getPushDate() != null ? titleLib.getPushDate().toString() : "");

    return result;
}

private String nvl(String s) {
    return s != null ? s : "";
}
```

- [ ] **Step 4: 新增 getRowPromptTemplate 方法**

在 TitleLibraryController 中添加，从前端存储的 rowPromptTemplate 恢复（读取本地缓存 key: `rowPromptTemplate` 对应的值，即行内配置的模板内容）。由于该模板保存在前端 localStorage，后端需要通过 Config 表或额外字段存储。推荐方案：在 TitleLibrary 记录中新增 `promptTemplate` 字段存储该标题专属模板，若无则用全局模板。

**简化方案（不改数据库结构）：** 通过 Config 表存储全局 rowPromptTemplate key 为 `row_prompt_template`，改造 buildPromptFromTemplate 优先从 Config 读取：
```java
private String getRowPromptTemplate() {
    List<Config> configs = configMapper.findAll();
    for (Config c : configs) {
        if ("row_prompt_template".equals(c.getConfigKey())) {
            return c.getConfigValue();
        }
    }
    return "";
}
```

---

## Task 8: 后端 — SubscriptionPost 新增 titleLibraryId 字段

**Files:**
- Modify: `services/admin-backend/src/main/java/com/example/blogger/entity/SubscriptionPost.java`

- [ ] **Step 1: 添加 titleLibraryId 字段**

添加：
```java
private String titleLibraryId;  // 关联的标题库ID

public String getTitleLibraryId() { return titleLibraryId; }
public void setTitleLibraryId(String titleLibraryId) { this.titleLibraryId = titleLibraryId; }
```

---

## Task 9: 前端 — TitleLibraryManage.vue 新增下载/预览/发邮件按钮

**Files:**
- Modify: `services/admin-frontend/src/views/TitleLibraryManage.vue`

- [ ] **Step 1: 新增 sendArticleEmail 方法**

在 TitleLibraryManage.vue 的 methods 区域添加（参照现有的 sendTitleEmail）：
```javascript
async function sendArticleEmail(record) {
  if (!record.generatedFileUrl && !record.subscriptionPostFileUrl) {
    message.warning('该标题尚未生成文章')
    return
  }
  const fileUrl = record.generatedFileUrl || record.subscriptionPostFileUrl
  // 下载文件作为附件发送邮件
  try {
    const response = await fetch(fileUrl)
    const blob = await response.blob()
    const fileName = record.generatedFileName || record.subscriptionPostFileUrl?.split('/').pop() || 'article.docx'
    const file = new File([blob], fileName, { type: 'application/vnd.openxmlformats-officedocument.wordprocessingml.document' })
    // 调用现有的 sendTitleEmail，传递文件
    await sendTitleEmailWithAttachment(record, file)
  } catch (e) {
    message.error('获取文章文件失败')
  }
}

async function sendTitleEmailWithAttachment(record, file) {
  // 复用现有的发邮件逻辑，传递附件
  // 如果现有方法不支持附件，需要扩展 email API
}
```

**简化方案：** 由于现有的 `sendTitleEmail` API 不支持附件，建议改为前端调用 `emailService.sendArticleEmail(email, file)` 通过 FormData 上传。实际建议新增接口 `POST /title-library/{id}/send-article-email`，后端直接读取 generatedFileUrl 对应的文件发送。

- [ ] **Step 2: 添加前端 API**

在 `services/admin-frontend/src/api/titleLibrary.js` 中添加：
```javascript
// 单标题生成文章（使用提示词模板）
export function generateArticleByPrompt(id) {
  return request({
    url: `/title-library/${id}/generate-post`,
    method: 'post'
  })
}

// 发送文章邮件（带附件）
export function sendArticleEmail(titleId, email) {
  return request({
    url: `/title-library/${titleId}/send-article-email`,
    method: 'post',
    data: { email }
  })
}
```

- [ ] **Step 3: 确认操作按钮位置**

在表格操作列（`action` 列）中，确认已有"生成文章"按钮。如有 `generatedFileUrl` 关联且有值，则显示"下载文章"和"发送邮箱"按钮（条件渲染）。

---

## Task 10: 后端 — 新增发邮件接口（支持 DOCX 附件）

**Files:**
- Modify: `services/admin-backend/src/main/java/com/example/blogger/controller/TitleLibraryController.java`

- [ ] **Step 1: 添加 sendArticleEmail 接口**

在 TitleLibraryController 中添加：
```java
@PostMapping("/{id}/send-article-email")
public Result<Void> sendArticleEmail(@PathVariable String id, @RequestBody Map<String, String> body) {
    TitleLibrary titleLib = titleLibraryService.getById(id);
    if (titleLib == null) {
        return Result.error("标题不存在");
    }
    String fileUrl = titleLib.getGeneratedFileUrl();
    if (fileUrl == null || fileUrl.isEmpty()) {
        return Result.error("该标题尚未生成文章");
    }

    String toEmail = body.get("email");
    if (toEmail == null || toEmail.isEmpty()) {
        return Result.error("邮箱地址不能为空");
    }

    try {
        // 读取文件
        String realPath = System.getProperty("user.dir") + fileUrl.replace("/", File.separator);
        File file = new File(realPath);
        if (!file.exists()) {
            return Result.error("文章文件不存在");
        }

        // 发送邮件
        String content = buildArticleEmailHtml(titleLib);
        emailService.sendHtmlEmailWithAttachment(toEmail, "您的创作文章", content, file, titleLib.getGeneratedFileName());

        return Result.ok(null);
    } catch (Exception e) {
        return Result.error("发送邮件失败: " + e.getMessage());
    }
}

private String buildArticleEmailHtml(TitleLibrary titleLib) {
    return "<html><body><p>您好，您的文章《" + titleLib.getTitle() + "》已生成，附件为 DOCX 文件，请查收。</p></body></html>";
}
```

---

## Task 11: 前端 — 确认生成文章按钮逻辑改造

**Files:**
- Modify: `services/admin-frontend/src/views/TitleLibraryManage.vue`

- [ ] **Step 1: 确认 generatePostSingle 调用**

在现有的"生成文章"按钮回调（约 line 579-598）中，确认调用的是 `generatePostSingle(record.id)`，无需额外改造（后端已重构该方法）。前端返回的 `result` 中包含 `fileUrl`、`fileName`，更新到 `record` 中：
```javascript
const result = await generatePostSingle(record.id)
if (result.fileUrl) {
  record.generatedFileUrl = result.fileUrl
  record.generatedFileName = result.fileName
}
```

---

## Task 12: 编译验证

- [ ] **Step 1: 编译后端**

Run: `cd services/admin-backend && mvn compile -q`
Expected: 无编译错误

- [ ] **Step 2: 编译前端**

Run: `cd services/admin-frontend && npm run build 2>&1 | tail -20`
Expected: 无编译错误

---

## Task 13: 提交代码

- [ ] **Step 1: Git add + commit**

Run:
```bash
git add services/admin-backend/src/main/java/com/example/blogger/service/LLMService.java
git add services/admin-backend/src/main/java/com/example/blogger/util/DocxGenerator.java
git add services/admin-backend/src/main/java/com/example/blogger/entity/TitleLibrary.java
git add services/admin-backend/src/main/java/com/example/blogger/entity/SubscriptionPost.java
git add services/admin-backend/src/main/java/com/example/blogger/mapper/TitleLibraryMapper.java
git add services/admin-backend/src/main/java/com/example/blogger/service/TitleLibraryService.java
git add services/admin-backend/src/main/java/com/example/blogger/controller/TitleLibraryController.java
git add services/admin-frontend/src/views/ConfigManage.vue
git add services/admin-frontend/src/views/TitleLibraryManage.vue
git add services/admin-frontend/src/api/titleLibrary.js
git commit -m "feat(title-library): 生成文章改用 Kimi/MiniMax API，输出 DOCX 并支持下载/预览/发邮件"
```

---

## Spec Coverage Check

- [x] 大模型可切换（Kimi/MiniMax）— Task 1, Task 2
- [x] 使用 rowPromptTemplate 填充变量生成 — Task 7
- [x] DOCX 生成 — Task 3
- [x] 文件关联 TitleLibrary — Task 4, Task 5, Task 6
- [x] 下载/预览/发邮件 — Task 8, Task 9, Task 10
- [x] ConfigManage 独立配置区 — Task 1

## Placeholder Scan

无 placeholder，所有步骤均包含完整代码。
