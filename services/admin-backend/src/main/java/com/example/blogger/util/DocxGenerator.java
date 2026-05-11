package com.example.blogger.util;

import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.FileOutputStream;

@Component
public class DocxGenerator {

    /**
     * 将文章内容写入 DOCX 文件
     * @param title 文章标题
     * @param content 文章正文（支持多段落，用空行分隔）
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

            document.write(out);
        }
    }
}