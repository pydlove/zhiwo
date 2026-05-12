package com.example.blogger.util;

import org.apache.poi.xwpf.usermodel.*;
import org.openxmlformats.schemas.wordprocessingml.x2006.main.CTColor;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DocxGenerator {

    private static final String HIGHLIGHT_COLOR = "fa541c";
    private static final int H3_FONT_SIZE = 16; // 16pt
    private static final int NORMAL_FONT_SIZE = 12; // 12pt

    private static final Pattern H3_PATTERN = Pattern.compile("<h3>(.*?)</h3>");
    private static final Pattern S_PATTERN = Pattern.compile("<s>(.*?)</s>");

    /**
     * 将文章内容写入 DOCX 文件
     * @param title 文章标题
     * @param content 文章正文（支持 <h3> 章节标题 和 <s> 着重加强 标签）
     * @param filePath 输出文件路径
     */
    public void generateDocx(String title, String content, String filePath) throws Exception {
        try (FileOutputStream out = new FileOutputStream(filePath);
             XWPFDocument document = new XWPFDocument()) {
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

            // 正文段落：按 \n\n 分割段落
            String[] paragraphs = content.split("\n\n");
            for (String para : paragraphs) {
                if (para.trim().isEmpty()) {
                    document.createParagraph();
                    continue;
                }
                String trimmed = para.trim();

                // 检查是否是 <h3> 标题段落
                Matcher h3Matcher = H3_PATTERN.matcher(trimmed);
                if (h3Matcher.matches()) {
                    String h3Text = h3Matcher.group(1);
                    addH3Paragraph(document, h3Text);
                    continue;
                }

                // 普通段落：可能包含 <s> 标签
                addNormalParagraph(document, trimmed);
            }

            document.write(out);
        }
    }

    private void addH3Paragraph(XWPFDocument document, String text) {
        XWPFParagraph p = document.createParagraph();
        p.setAlignment(ParagraphAlignment.LEFT);
        XWPFRun run = p.createRun();
        run.setText(text);
        run.setFontSize(H3_FONT_SIZE);
        run.setColor(HIGHLIGHT_COLOR);
        run.setBold(true);
    }

    private void addNormalParagraph(XWPFDocument document, String text) {
        XWPFParagraph p = document.createParagraph();
        p.setAlignment(ParagraphAlignment.BOTH);

        // 解析 <s> 标签，将文本分段
        // 使用正则找到所有 <s>...</s> 和普通文本的混合
        int lastEnd = 0;
        Matcher sMatcher = S_PATTERN.matcher(text);

        while (sMatcher.find()) {
            // 标签前的普通文本
            if (sMatcher.start() > lastEnd) {
                String normalText = text.substring(lastEnd, sMatcher.start());
                if (!normalText.isEmpty()) {
                    XWPFRun normalRun = p.createRun();
                    normalRun.setText(normalText);
                    normalRun.setFontSize(NORMAL_FONT_SIZE);
                }
            }
            // <s> 标签内的文本
            String sText = sMatcher.group(1);
            if (!sText.isEmpty()) {
                XWPFRun sRun = p.createRun();
                sRun.setText(sText);
                sRun.setFontSize(NORMAL_FONT_SIZE);
                sRun.setBold(true);
                sRun.setColor(HIGHLIGHT_COLOR);
            }
            lastEnd = sMatcher.end();
        }

        // 剩余的普通文本
        if (lastEnd < text.length()) {
            String remainingText = text.substring(lastEnd);
            if (!remainingText.isEmpty()) {
                XWPFRun remainingRun = p.createRun();
                remainingRun.setText(remainingText);
                remainingRun.setFontSize(NORMAL_FONT_SIZE);
            }
        }

        // 如果没有匹配到任何 <s> 标签，整段都是普通文本
        if (lastEnd == 0 && !sMatcher.find(0)) {
            XWPFRun run = p.createRun();
            run.setText(text);
            run.setFontSize(NORMAL_FONT_SIZE);
        }
    }
}
