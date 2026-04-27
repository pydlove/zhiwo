package com.example.blogger.service;

import com.example.blogger.entity.Guide;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class GuideGenerationService {

    private final ClaudeCodeService claudeCodeService;
    private final ImageDownloadService imageDownloadService;
    private final ObjectMapper objectMapper;

    public GuideGenerationService(ClaudeCodeService claudeCodeService,
                                  ImageDownloadService imageDownloadService,
                                  ObjectMapper objectMapper) {
        this.claudeCodeService = claudeCodeService;
        this.imageDownloadService = imageDownloadService;
        this.objectMapper = objectMapper;
    }

    public List<Guide> generateGuides(String category, int count) {
        if (count < 1 || count > 5) {
            throw new IllegalArgumentException("生成数量必须在 1-5 之间");
        }

        List<Guide> guides = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String prompt = buildPrompt(category);
            String response = claudeCodeService.callClaude(prompt);
            Guide guide = parseAndBuildGuide(response, category);
            guides.add(guide);
        }
        return guides;
    }

    private String buildPrompt(String category) {
        return "你是资深的新媒体内容创作专家，擅长创作高质量、实用、吸引人的公众号创作技巧文章。\n\n" +
                "请为「" + category + "」类别创作一篇创作技巧文章，要求：\n\n" +
                "## 标题要求\n" +
                "- 主标题：吸引人、有痛点共鸣、实用性强（10-18字）\n" +
                "- 英文副标题：2-15个字母的英文短语，与主题呼应，简洁有格调\n" +
                "- 中文副标题：一句话概括核心观点（15-30字）\n\n" +
                "## 内容结构\n" +
                "1. **引言**：1-2段，用痛点场景引入，引发共鸣，语言有温度\n" +
                "2. **核心章节**：3-4个章节，每个章节包含：\n" +
                "   - 章节序号名称（用中文大写数字：壹、贰、叁、肆）\n" +
                "   - 章节小标题：具体、有画面感、带冒号分隔（如\"慢食：一粥一饭的温度\"）\n" +
                "   - 正文：2-3段，每段包含实用技巧+具体案例，避免空洞说教\n" +
                "   - 金句/引用：1句行业名人名言或经典语录，标注来源\n" +
                "   - 行动清单：2-3条具体可执行的建议\n" +
                "3. **结尾**：总结升华+情感共鸣，给读者力量和方向\n\n" +
                "## 风格要求\n" +
                "- 语言风格：温暖、有质感、有洞察力，像一位经验丰富的前辈在真诚分享\n" +
                "- 内容深度：不是泛泛而谈，要有具体的方法、步骤、案例\n" +
                "- 实用价值：读者看完能立刻动手实践\n" +
                "- 情感共鸣：触及读者内心深处的焦虑和渴望\n\n" +
                "## 图片相关\n" +
                "请提供2-3个图片场景描述（用于AI生成配图），每个15-30字中文描述，要与标题和章节内容呼应。\n\n" +
                "## 输出格式\n" +
                "请严格按以下JSON格式输出（不要包含markdown代码块标记，直接输出JSON文本）：\n\n" +
                "{\n" +
                "  \"englishTitle\": \"英文副标题\",\n" +
                "  \"mainTitle\": \"主标题\",\n" +
                "  \"subTitle\": \"中文副标题\",\n" +
                "  \"introQuote\": \"引言金句，一句话\",\n" +
                "  \"introParagraphs\": [\"段落1\", \"段落2\"],\n" +
                "  \"sections\": [\n" +
                "    {\n" +
                "      \"number\": \"壹\",\n" +
                "      \"title\": \"章节小标题\",\n" +
                "      \"paragraphs\": [\"段落1\", \"段落2\"],\n" +
                "      \"quote\": \"金句内容\",\n" +
                "      \"quoteSource\": \"来源\",\n" +
                "      \"actions\": [\"行动1\", \"行动2\"]\n" +
                "    }\n" +
                "  ],\n" +
                "  \"ending\": {\n" +
                "    \"title\": \"写在最后\",\n" +
                "    \"paragraphs\": [\"段落1\", \"段落2\"],\n" +
                "    \"signOff\": \"收尾金句\"\n" +
                "  },\n" +
                "  \"imageDescriptions\": [\"图片描述1\", \"图片描述2\", \"图片描述3\"]\n" +
                "}";
    }

    private Guide parseAndBuildGuide(String response, String category) {
        String jsonText = extractJson(response);
        try {
            JsonNode root = objectMapper.readTree(jsonText);

            String mainTitle = root.path("mainTitle").asText("创作技巧分享");
            String subTitle = root.path("subTitle").asText("");
            String introQuote = root.path("introQuote").asText("");

            // 下载图片
            List<String> imageUrls = new ArrayList<>();
            JsonNode imgDescs = root.path("imageDescriptions");
            if (imgDescs.isArray()) {
                List<String> descriptions = new ArrayList<>();
                for (JsonNode desc : imgDescs) {
                    if (!desc.asText("").isBlank()) {
                        descriptions.add(desc.asText());
                    }
                }
                imageUrls = imageDownloadService.downloadImages(descriptions);
            }

            // 构建 HTML
            String html = buildZhongguStyleHtml(root, imageUrls);

            // 提取描述
            String description = extractDescription(root);

            Guide guide = new Guide();
            guide.setId(UUID.randomUUID().toString().replace("-", ""));
            guide.setTitle(mainTitle);
            guide.setCategory(category);
            guide.setDescription(description);
            guide.setContent(html);
            guide.setSortOrder(1);
            guide.setStatus("已上架");
            guide.setIsRecommended(0);
            guide.setCreatedAt(LocalDateTime.now());
            guide.setUpdatedAt(LocalDateTime.now());

            return guide;

        } catch (JsonProcessingException e) {
            throw new RuntimeException("解析 AI 生成内容失败: " + e.getMessage(), e);
        }
    }

    private String extractJson(String response) {
        if (response == null) {
            return "{}";
        }
        String trimmed = response.trim();
        // 去除 markdown 代码块标记
        if (trimmed.startsWith("```")) {
            int firstBrace = trimmed.indexOf("{");
            int lastBrace = trimmed.lastIndexOf("}");
            if (firstBrace >= 0 && lastBrace > firstBrace) {
                return trimmed.substring(firstBrace, lastBrace + 1);
            }
        }
        // 查找第一个 { 和最后一个 }
        int firstBrace = trimmed.indexOf("{");
        int lastBrace = trimmed.lastIndexOf("}");
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1);
        }
        return trimmed;
    }

    private String extractDescription(JsonNode root) {
        StringBuilder sb = new StringBuilder();
        JsonNode introParagraphs = root.path("introParagraphs");
        if (introParagraphs.isArray() && introParagraphs.size() > 0) {
            String text = introParagraphs.get(0).asText("");
            if (text.length() > 80) {
                text = text.substring(0, 80) + "...";
            }
            sb.append(text);
        }
        String subTitle = root.path("subTitle").asText("");
        if (!subTitle.isBlank()) {
            if (sb.length() > 0) sb.insert(0, subTitle + " | ");
            else sb.append(subTitle);
        }
        return sb.toString();
    }

    private String buildZhongguStyleHtml(JsonNode root, List<String> imageUrls) {
        StringBuilder html = new StringBuilder();

        // 容器
        html.append("<div style=\"font-family: Georgia, 'Noto Serif SC', 'Songti SC', serif; max-width: 680px; margin: 0 auto; padding: 40px 24px; background: #FAF8F5; color: #3D3229; line-height: 1.8;\">\n");

        // 英文副标题
        String englishTitle = root.path("englishTitle").asText("");
        if (!englishTitle.isBlank()) {
            html.append("  <p style=\"text-align: center; color: #B85C38; font-size: 14px; letter-spacing: 3px; margin-bottom: 8px; font-family: Georgia, serif;\">")
                    .append(escapeHtml(englishTitle))
                    .append("</p>\n");
        }

        // 主标题
        String mainTitle = root.path("mainTitle").asText("创作技巧");
        html.append("  <h2 style=\"text-align: center; color: #3D3229; font-size: 32px; font-weight: bold; margin: 0 0 16px; line-height: 1.4;\">")
                .append(escapeHtml(mainTitle))
                .append("</h2>\n");

        // 中文副标题
        String subTitle = root.path("subTitle").asText("");
        if (!subTitle.isBlank()) {
            html.append("  <p style=\"text-align: center; color: #7D7067; font-size: 20px; font-weight: bold; margin: 0 0 8px;\">")
                    .append(escapeHtml(subTitle))
                    .append("</p>\n");
        }

        // 引言金句
        String introQuote = root.path("introQuote").asText("");
        if (!introQuote.isBlank()) {
            html.append("  <p style=\"text-align: center; color: #A89B91; font-size: 16px; font-style: italic; margin: 0 0 24px;\">")
                    .append(escapeHtml(introQuote))
                    .append("</p>\n");
        }

        // 首图
        if (!imageUrls.isEmpty()) {
            html.append(buildImageBlock(imageUrls.get(0), introQuote.isBlank() ? mainTitle : introQuote));
        }

        // 分隔符
        html.append("  <p style=\"text-align: center; color: #B85C38; font-size: 20px; margin: 24px 0;\">◈</p>\n");

        // 引言段落
        JsonNode introParagraphs = root.path("introParagraphs");
        if (introParagraphs.isArray() && introParagraphs.size() > 0) {
            html.append("  <div style=\"background: #F0EBE3; border-top: 1px solid #D4C8BC; border-bottom: 1px solid #D4C8BC; padding: 20px 24px; margin: 20px 0;\">\n");
            for (JsonNode p : introParagraphs) {
                html.append("    <p style=\"color: #7D7067; font-size: 16px; line-height: 1.8; margin: 0 0 12px;\">")
                        .append(escapeHtml(p.asText("")))
                        .append("</p>\n");
            }
            html.append("  </div>\n");
        }

        // 强调金句
        String subTitleForQuote = root.path("subTitle").asText("");
        if (!subTitleForQuote.isBlank()) {
            html.append("  <p style=\"text-align: center; color: #7D7067; font-size: 16px; margin: 16px 0;\">\n");
            html.append("    <strong style=\"color: #B85C38;\">◆</strong> ")
                    .append(escapeHtml(subTitleForQuote))
                    .append("\n");
            html.append("  </p>\n");
        }

        // 分隔符
        html.append("  <p style=\"text-align: center; color: #B85C38; font-size: 20px; margin: 24px 0;\">◈</p>\n");

        // 章节
        JsonNode sections = root.path("sections");
        if (sections.isArray()) {
            int imgIndex = 1;
            for (JsonNode section : sections) {
                html.append("  <div style=\"margin: 32px 0;\">\n");

                // 章节序号
                String number = section.path("number").asText("");
                if (!number.isBlank()) {
                    html.append("    <p style=\"text-align: center; color: #B85C38; font-size: 14px; margin: 0 0 4px; letter-spacing: 2px;\">「</p>\n");
                    html.append("    <p style=\"text-align: center; color: #B85C38; font-size: 24px; font-weight: bold; margin: 0;\">")
                            .append(escapeHtml(number))
                            .append("</p>\n");
                    html.append("    <p style=\"text-align: center; color: #B85C38; font-size: 14px; margin: 4px 0 8px; letter-spacing: 2px;\">」</p>\n");
                }

                // 章节标题
                String sectionTitle = section.path("title").asText("");
                if (!sectionTitle.isBlank()) {
                    html.append("    <p style=\"text-align: center; color: #3D3229; font-size: 20px; font-weight: bold; margin: 0 0 8px;\">")
                            .append(escapeHtml(sectionTitle))
                            .append("</p>\n");
                }

                // 分隔线
                html.append("    <p style=\"text-align: center; color: #D4C8BC; font-size: 14px; margin: 0 0 20px; letter-spacing: 4px;\">━━━━━━</p>\n");

                // 正文段落
                JsonNode paragraphs = section.path("paragraphs");
                if (paragraphs.isArray()) {
                    for (JsonNode p : paragraphs) {
                        html.append("    <p style=\"color: #3D3229; font-size: 15px; line-height: 1.8; margin: 0 0 12px; text-indent: 2em;\">")
                                .append(escapeHtml(p.asText("")))
                                .append("</p>\n");
                    }
                }

                // 章节图片（中间插入一张）
                if (imgIndex < imageUrls.size()) {
                    html.append(buildImageBlock(imageUrls.get(imgIndex), sectionTitle));
                    imgIndex++;
                }

                // 金句引用
                String quote = section.path("quote").asText("");
                String quoteSource = section.path("quoteSource").asText("");
                if (!quote.isBlank()) {
                    html.append("    <p style=\"color: #7D7067; font-size: 15px; line-height: 1.8; margin: 16px 0; padding: 12px 16px; background: #F7F3EE; border-left: 3px solid #B85C38;\">\n");
                    html.append("      <span style=\"color: #B85C38;\">※</span> ")
                            .append(escapeHtml(quote));
                    if (!quoteSource.isBlank()) {
                        html.append("<br><span style=\"font-size: 13px; color: #A89B91;\">—— ")
                                .append(escapeHtml(quoteSource))
                                .append("</span>");
                    }
                    html.append("\n    </p>\n");
                }

                // 行动清单
                JsonNode actions = section.path("actions");
                if (actions.isArray() && actions.size() > 0) {
                    html.append("    <div style=\"margin: 16px 0; padding: 16px 20px; background: #F7F3EE; border-radius: 4px;\">\n");
                    html.append("      <p style=\"color: #3D3229; font-size: 15px; margin: 0 0 8px; font-weight: bold;\">行动建议：</p>\n");
                    for (JsonNode action : actions) {
                        html.append("      <p style=\"color: #7D7067; font-size: 14px; line-height: 1.8; margin: 4px 0;\">\n");
                        html.append("        <span style=\"color: #B85C38; margin-right: 8px;\">·</span>")
                                .append(escapeHtml(action.asText("")))
                                .append("\n");
                        html.append("      </p>\n");
                    }
                    html.append("    </div>\n");
                }

                html.append("  </div>\n");

                // 章节分隔符
                html.append("  <p style=\"text-align: center; color: #B85C38; font-size: 20px; margin: 24px 0;\">◈</p>\n");
            }
        }

        // 结尾
        JsonNode ending = root.path("ending");
        if (!ending.isMissingNode()) {
            html.append("  <div style=\"text-align: center; margin-top: 24px;\">\n");

            String endingTitle = ending.path("title").asText("写在最后");
            html.append("    <p style=\"color: #7D7067; font-size: 18px; font-weight: bold; margin: 0 0 12px;\">")
                    .append(escapeHtml(endingTitle))
                    .append("</p>\n");

            html.append("    <p style=\"color: #D4C8BC; font-size: 14px; margin: 0 0 16px; letter-spacing: 3px;\">━━━━━━</p>\n");

            JsonNode endingParagraphs = ending.path("paragraphs");
            if (endingParagraphs.isArray()) {
                for (JsonNode p : endingParagraphs) {
                    html.append("    <p style=\"color: #3D3229; font-size: 15px; line-height: 1.8; margin: 0 0 12px;\">")
                            .append(escapeHtml(p.asText("")))
                            .append("</p>\n");
                }
            }

            String signOff = ending.path("signOff").asText("");
            if (!signOff.isBlank()) {
                html.append("    <p style=\"color: #A89B91; font-size: 15px; font-style: italic; margin: 16px 0 0;\">")
                        .append(escapeHtml(signOff))
                        .append("</p>\n");
            }

            html.append("  </div>\n");
        }

        html.append("</div>");

        return html.toString();
    }

    private String buildImageBlock(String imageUrl, String caption) {
        StringBuilder sb = new StringBuilder();
        sb.append("  <div style=\"text-align: center; margin: 24px 0; border: 1px solid #D4C8BC; padding: 8px; background: #fff;\">\n");
        sb.append("    <img src=\"").append(imageUrl).append("\" style=\"width: 100%; aspect-ratio: 16/9; object-fit: cover; display: block;\" alt=\"\" />\n");
        if (caption != null && !caption.isBlank()) {
            sb.append("    <p style=\"text-align: center; color: #A89B91; font-size: 13px; font-style: italic; margin: 8px 0 0;\">▲ ")
                    .append(escapeHtml(caption))
                    .append("</p>\n");
        }
        sb.append("  </div>\n");
        return sb.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
