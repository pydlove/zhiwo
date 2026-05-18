package com.example.blogger.util;

import org.apache.poi.util.Units;
import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTColor;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTFonts;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTRPr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DocxGenerator {

    private static final Logger log = LoggerFactory.getLogger(DocxGenerator.class);
    private static final String HIGHLIGHT_COLOR = "fa541c";
    private static final int DEFAULT_H3_FONT_SIZE = 16; // 16pt
    private static final int DEFAULT_NORMAL_FONT_SIZE = 12; // 12pt

    private static final Pattern H1_PATTERN = Pattern.compile("<h1[^>]*>(.*?)</h1>");
    private static final Pattern H3_PATTERN = Pattern.compile("<h3[^>]*>(.*?)</h3>");
    private static final Pattern S_PATTERN = Pattern.compile("<s>(.*?)</s>");
    private static final Pattern IMG_PATTERN = Pattern.compile("<img\\s+src=\"([^\"]+)\"[^>]*>");

    /**
     * 将文章内容写入 DOCX 文件（使用默认主题色和字号）
     * @param title 文章标题
     * @param content 文章正文（支持 <h3> 章节标题 和 <s> 着重加强 标签）
     * @param filePath 输出文件路径
     */
    public void generateDocx(String title, String content, String filePath) throws Exception {
        generateDocx(title, content, filePath, HIGHLIGHT_COLOR, null, null);
    }

    /**
     * 将文章内容写入 DOCX 文件（使用自定义主题色）
     * @param title 文章标题
     * @param content 文章正文（支持 <h3> 章节标题 和 <s> 着重加强 标签）
     * @param filePath 输出文件路径
     * @param themeColor 主题色（十六进制，如 fa541c）
     */
    public void generateDocx(String title, String content, String filePath, String themeColor) throws Exception {
        generateDocx(title, content, filePath, themeColor, null, null);
    }

    /**
     * 将文章内容写入 DOCX 文件（完整版：支持主题色和字号配置）
     * @param title 文章标题
     * @param content 文章正文（支持 <h3> 章节标题 和 <s> 着重加强 标签）
     * @param filePath 输出文件路径
     * @param themeColor 主题色（十六进制，如 fa541c）
     * @param titleFontSize 标题字号（pt），null 时使用默认值 16
     * @param contentFontSize 正文字号（pt），null 时使用默认值 12
     */
    public void generateDocx(String title, String content, String filePath, String themeColor, Integer titleFontSize, Integer contentFontSize) throws Exception {
        // 兜底：移除大模型思考过程标签及其内容
        content = stripThinkingTags(content);
        // 清洗常见 HTML 标签（保留 h1, h3, s, img 供后续专项处理）
        content = cleanHtmlTags(content);

        String color = (themeColor != null && !themeColor.isEmpty()) ? themeColor.replace("#", "") : HIGHLIGHT_COLOR;
        int h3Size = (titleFontSize != null && titleFontSize > 0) ? titleFontSize : DEFAULT_H3_FONT_SIZE;
        int normalSize = (contentFontSize != null && contentFontSize > 0) ? contentFontSize : DEFAULT_NORMAL_FONT_SIZE;

        // title 参数保留用于兼容调用方，但不再写入文档正文
        try (FileOutputStream out = new FileOutputStream(filePath);
             XWPFDocument document = new XWPFDocument()) {
            // 预处理：标准化换行符，把单个 \n 变成 \n\n，确保块级标签独占段落
            // 把连续三个及以上 \n 压缩为 \n\n，避免过多空段落
            String normalizedContent = content.replaceAll("(?<!\n)\n(?!\n)", "\n\n")
                                               .replaceAll("\n{3,}", "\n\n");
            // 正文段落：按 \n\n 分割段落（不再单独写入标题，标题已体现在文件名中）
            String[] paragraphs = normalizedContent.split("\n\n+");
            for (int i = 0; i < paragraphs.length; i++) {
                String para = paragraphs[i];
                if (para.trim().isEmpty()) {
                    document.createParagraph();
                    continue;
                }
                String trimmed = para.trim();

                // 跳过 <h1> 文章标题段落（标题已体现在文件名中）
                Matcher h1Matcher = H1_PATTERN.matcher(trimmed);
                if (h1Matcher.matches()) {
                    continue;
                }

                // 检查是否是 <img> 图片段落
                Matcher imgMatcher = IMG_PATTERN.matcher(trimmed);
                if (imgMatcher.matches()) {
                    String src = imgMatcher.group(1);
                    addImageParagraph(document, src);
                    continue;
                }

                // 检查是否是 <h3> 标题段落
                Matcher h3Matcher = H3_PATTERN.matcher(trimmed);
                if (h3Matcher.matches()) {
                    String h3Text = h3Matcher.group(1);
                    addH3Paragraph(document, h3Text, color, h3Size);
                } else if (!trimmed.contains("<s>") && !endsWithPunctuation(trimmed)) {
                    // 智能标题识别：独立段落且结尾无标点，大概率是标题
                    String cleanText = trimmed.replaceAll("<[^>]+>", "");
                    addH3Paragraph(document, cleanText, color, h3Size);
                } else {
                    // 普通段落：可能包含 <s> 标签
                    addNormalParagraph(document, trimmed, color, normalSize);
                }

                // 暂时注释：每个段落之间增加一个换行（空段落），避免排版拥挤
                // if (i < paragraphs.length - 1) {
                //     document.createParagraph();
                // }
            }

            document.write(out);
        }
    }

    /**
     * 兜底过滤：移除大模型思考过程标签及其内容。
     * 如果整篇文章被单个 <think> 包裹（过滤后为空），则只去掉标签本身，保留内容。
     */
    private String stripThinkingTags(String text) {
        if (text == null || text.isEmpty()) return text;
        String cleaned = text.replaceAll("(?is)<thinking\\b[^>]*>.*?</thinking>", "")
                             .replaceAll("(?is)<think\\b[^>]*>.*?</think>", "")
                             .replaceAll("(?is)<thought\\b[^>]*>.*?</thought>", "")
                             .replaceAll("(?is)<reasoning\\b[^>]*>.*?</reasoning>", "")
                             .trim();
        // 兜底：如果过滤后内容为空，回退为只移除标签保留内容
        if (cleaned.isEmpty()) {
            cleaned = text.replaceAll("(?is)</?thinking\\b[^>]*>", "")
                          .replaceAll("(?is)</?think\\b[^>]*>", "")
                          .replaceAll("(?is)</?thought\\b[^>]*>", "")
                          .replaceAll("(?is)</?reasoning\\b[^>]*>", "")
                          .trim();
        }
        return cleaned;
    }

    /**
     * 清洗常见 HTML 标签，保留 h1/h3/s/img 供后续专项处理。
     * 将 <br> 转为换行，解析 HTML 实体，去除 div/span/strong/em/p 等标签。
     */
    private String cleanHtmlTags(String text) {
        if (text == null || text.isEmpty()) return text;
        // 1. <br> 标签转为换行
        text = text.replaceAll("(?i)<br\\s*/?>", "\n");
        // 2. 处理常见 HTML 实体
        text = text.replaceAll("&nbsp;", " ")
                   .replaceAll("&lt;", "<")
                   .replaceAll("&gt;", ">")
                   .replaceAll("&amp;", "&")
                   .replaceAll("&quot;", "\"")
                   .replaceAll("&#39;", "'")
                   .replaceAll("&#34;", "\"")
                   .replaceAll("&#x27;", "'");
        // 3. 去除不需要的 HTML 标签（保留 h1, h3, s, img 供后续处理）
        text = text.replaceAll("(?i)<(?!/?h1\\b|/?h3\\b|/?s\\b|/?img\\b)[^>]*?>", "");
        // 4. 清理多余空行
        text = text.replaceAll("\n{3,}", "\n\n");
        return text.trim();
    }

    /**
     * 判断文本去除 HTML 标签后是否以标点符号结尾
     */
    private boolean endsWithPunctuation(String text) {
        String plain = text.replaceAll("<[^>]+>", "").trim();
        if (plain.isEmpty()) return false;
        char last = plain.charAt(plain.length() - 1);
        return "。，！？；：、.?!:;".indexOf(last) >= 0;
    }

    private void addH3Paragraph(XWPFDocument document, String text, String color, int fontSize) {
        XWPFParagraph p = document.createParagraph();
        p.setAlignment(ParagraphAlignment.LEFT);

        // 在标题文字前插入红色竖条符号（与文字天然基线对齐）
        XWPFRun slashRun = p.createRun();
        slashRun.setText("▌ ");
        slashRun.setFontSize(fontSize);
        slashRun.setColor("ff4d4f");
        slashRun.setBold(true);
        setRunFont(slashRun);

        XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontSize(fontSize);
        run.setColor(color);
        run.setBold(true);
        setRunFont(run);
    }

    private void addNormalParagraph(XWPFDocument document, String text, String color, int fontSize) {
        XWPFParagraph p = document.createParagraph();
        p.setAlignment(ParagraphAlignment.BOTH);

        // 解析 <s> 标签，将文本分段
        int lastEnd = 0;
        Matcher sMatcher = S_PATTERN.matcher(text);

        while (sMatcher.find()) {
            // 标签前的普通文本
            if (sMatcher.start() > lastEnd) {
                String normalText = text.substring(lastEnd, sMatcher.start());
                if (!normalText.isEmpty()) {
                    XWPFRun normalRun = p.createRun();
                    normalRun.setText(normalText);
                    normalRun.setFontSize(fontSize);
                    setRunFont(normalRun);
                }
            }
            // <s> 标签内的文本
            String sText = sMatcher.group(1);
            if (!sText.isEmpty()) {
                XWPFRun sRun = p.createRun();
                sRun.setText(sText);
                sRun.setFontSize(fontSize);
                sRun.setBold(true);
                sRun.setColor(color);
                setRunFont(sRun);
            }
            lastEnd = sMatcher.end();
        }

        // 剩余的普通文本
        if (lastEnd < text.length()) {
            String remainingText = text.substring(lastEnd);
            if (!remainingText.isEmpty()) {
                XWPFRun remainingRun = p.createRun();
                remainingRun.setText(remainingText);
                remainingRun.setFontSize(fontSize);
                setRunFont(remainingRun);
            }
        }
    }

    private void addImageParagraph(XWPFDocument document, String imageUrl) {
        XWPFParagraph p = document.createParagraph();
        p.setAlignment(ParagraphAlignment.CENTER);
        try {
            String imagePath = System.getProperty("user.dir") + imageUrl;
            File imageFile = new File(imagePath);
            if (!imageFile.exists()) {
                log.warn("[DocxGenerator] 图片文件不存在: {}", imagePath);
                return;
            }
            BufferedImage bufferedImage = ImageIO.read(imageFile);
            if (bufferedImage == null) {
                log.warn("[DocxGenerator] 无法读取图片: {}", imagePath);
                return;
            }
            int maxWidthPt = 400;
            int imgWidth = bufferedImage.getWidth();
            int imgHeight = bufferedImage.getHeight();
            double ratio = (double) imgHeight / imgWidth;
            int widthEMU = Units.toEMU(maxWidthPt);
            int heightEMU = Units.toEMU((int) (maxWidthPt * ratio));
            XWPFRun imageRun = p.createRun();
            try (FileInputStream fis = new FileInputStream(imageFile)) {
                int format = getImageFormat(imageFile.getName());
                imageRun.addPicture(fis, format, imageFile.getName(), widthEMU, heightEMU);
            }
        } catch (Exception e) {
            log.warn("[DocxGenerator] 插入图片失败: {}", e.getMessage());
        }
    }

    private int getImageFormat(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) return XWPFDocument.PICTURE_TYPE_PNG;
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return XWPFDocument.PICTURE_TYPE_JPEG;
        if (lower.endsWith(".gif")) return XWPFDocument.PICTURE_TYPE_GIF;
        if (lower.endsWith(".bmp")) return XWPFDocument.PICTURE_TYPE_BMP;
        return XWPFDocument.PICTURE_TYPE_JPEG;
    }

    private void setRunFont(XWPFRun run) {
        CTRPr rPr = run.getCTR().isSetRPr() ? run.getCTR().getRPr() : run.getCTR().addNewRPr();
        CTFonts fonts = rPr.sizeOfRFontsArray() > 0 ? rPr.getRFontsArray(0) : rPr.addNewRFonts();
        fonts.setAscii("Arial");
        fonts.setHAnsi("Arial");
        fonts.setEastAsia("微软雅黑");
        run.setFontFamily("Arial");
    }
}
