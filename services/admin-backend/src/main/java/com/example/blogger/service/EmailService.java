package com.example.blogger.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${spring.mail.fromName:知我公众号创作助手}")
    private String fromName;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendSimpleEmail(String to, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(content);
        try {
            mailSender.send(message);
        } catch (org.springframework.mail.MailException e) {
            throw new RuntimeException("邮件发送失败，请检查SMTP配置: " + e.getMessage(), e);
        }
    }

    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (java.io.UnsupportedEncodingException | MessagingException e) {
            throw new RuntimeException("HTML邮件发送失败: " + e.getMessage(), e);
        } catch (org.springframework.mail.MailException e) {
            throw new RuntimeException("邮件发送失败，请检查SMTP配置: " + e.getMessage(), e);
        }
    }

    public void sendHtmlEmailWithAttachment(String to, String subject, String htmlContent, File attachment, String attachmentName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            if (attachment != null && attachment.exists()) {
                helper.addAttachment(attachmentName, new FileSystemResource(attachment));
            }
            mailSender.send(message);
        } catch (java.io.UnsupportedEncodingException | MessagingException e) {
            throw new RuntimeException("邮件发送失败: " + e.getMessage(), e);
        } catch (org.springframework.mail.MailException e) {
            throw new RuntimeException("邮件发送失败，请检查SMTP配置: " + e.getMessage(), e);
        }
    }

    /**
     * 发送帮助文档邮件
     */
    public void sendHelpArticleEmail(String to, String userName, String helpTitle, String helpContent, String category) {
        String subject = "【帮助文档】" + helpTitle + " —— 知我公众号创作助手";
        String html = buildHelpArticleHtml(userName, helpTitle, helpContent, category);
        sendHtmlEmail(to, subject, html);
    }

    private String buildHelpArticleHtml(String userName, String helpTitle, String helpContent, String category) {
        String displayName = userName != null && !userName.isEmpty() ? userName : "创作者";
        String displayCategory = category != null && !category.isEmpty() ? category : "帮助中心";
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin:0; padding:0; background-color:#f5f7fa; font-family:-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f5f7fa;">
                    <tr>
                        <td align="center" style="padding:40px 20px;">
                            <table width="600" cellpadding="0" cellspacing="0" border="0" style="max-width:600px; width:100%%; background:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 4px 20px rgba(0,0,0,0.08);">
                                <!-- 顶部品牌区 -->
                                <tr>
                                    <td style="background:linear-gradient(135deg, #2563eb 0%%, #1e40af 100%%); padding:32px 40px; text-align:center;">
                                        <h1 style="margin:0; color:#ffffff; font-size:22px; font-weight:700;">知我公众号创作助手</h1>
                                        <p style="margin:8px 0 0; color:rgba(255,255,255,0.85); font-size:14px;">让 AI 成为您的创作引擎</p>
                                    </td>
                                </tr>

                                <!-- 问候语 -->
                                <tr>
                                    <td style="padding:32px 40px 20px;">
                                        <p style="margin:0; font-size:16px; color:#374151; line-height:1.6;">
                                            尊敬的 <strong style="color:#2563eb;">%s</strong>，您好！
                                        </p>
                                    </td>
                                </tr>

                                <!-- 文档分类标签 -->
                                <tr>
                                    <td style="padding:0 40px 16px;">
                                        <span style="display:inline-block; background:#eff6ff; color:#2563eb; padding:4px 12px; border-radius:6px; font-size:13px; font-weight:500;">%s</span>
                                    </td>
                                </tr>

                                <!-- 文档标题 -->
                                <tr>
                                    <td style="padding:0 40px 24px;">
                                        <div style="background:#f8fafc; border-radius:12px; padding:20px 24px; border-left:4px solid #2563eb;">
                                            <p style="margin:0 0 8px; font-size:13px; color:#6b7280;">文档标题</p>
                                            <p style="margin:0; font-size:17px; font-weight:600; color:#111827; line-height:1.5;">%s</p>
                                        </div>
                                    </td>
                                </tr>

                                <!-- 文档内容 -->
                                <tr>
                                    <td style="padding:0 40px 32px;">
                                        <div style="font-size:15px; color:#4b5563; line-height:1.8;">
                                            %s
                                        </div>
                                    </td>
                                </tr>

                                <!-- 分隔线 -->
                                <tr>
                                    <td style="padding:0 40px;">
                                        <div style="border-top:1px solid #e5e7eb; padding:20px 0; text-align:center;">
                                            <p style="margin:0 0 8px; font-size:12px; color:#9ca3af;">
                                                知我公众号创作助手 · 让创作更简单，让变现更高效
                                            </p>
                                            <p style="margin:0; font-size:11px; color:#cbd5e1;">
                                                如您不希望接收此类邮件，可在个人中心关闭邮件推送
                                            </p>
                                        </div>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(displayName, displayCategory, helpTitle, helpContent);
    }

    /**
     * 发送每日推荐文章邮件（带附件）
     */
    public void sendDailyRecommendEmail(String to, String userName, String trackName, String articleTitle,
                                         String platform, File articleFile, String fileName) {
        String subject = "【每日推荐】" + articleTitle + " —— 知我公众号创作助手";
        String html = buildDailyRecommendHtml(userName, trackName, articleTitle, platform);
        sendHtmlEmailWithAttachment(to, subject, html, articleFile, fileName);
    }

    /**
     * 发送活动推广邮件
     */
    public void sendActivityEmail(String to, String userName, String activityTitle, String activityContent, String qrCodeUrl) {
        String subject = "【活动】" + activityTitle + " —— 知我公众号创作助手";
        String html = buildActivityHtml(userName, activityTitle, activityContent, qrCodeUrl);
        sendHtmlEmail(to, subject, html);
    }

    private String buildActivityHtml(String userName, String activityTitle, String activityContent, String qrCodeUrl) {
        String displayName = userName != null && !userName.isEmpty() ? userName : "创作者";
        String qrCodeHtml = "";
        if (qrCodeUrl != null && !qrCodeUrl.isEmpty()) {
            qrCodeHtml = """
                <!-- 二维码 -->
                <tr>
                    <td style="padding:0 40px 32px;">
                        <div style="background:#f8fafc; border-radius:12px; padding:24px; text-align:center; border:1px solid #e5e7eb;">
                            <p style="margin:0 0 16px; font-size:14px; color:#6b7280;">扫码了解更多活动详情</p>
                            <img src="%s" style="width:140px; height:140px; border-radius:8px;" alt="活动二维码">
                        </div>
                    </td>
                </tr>
                """.formatted(qrCodeUrl);
        }
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin:0; padding:0; background-color:#f5f7fa; font-family:-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f5f7fa;">
                    <tr>
                        <td align="center" style="padding:40px 20px;">
                            <table width="600" cellpadding="0" cellspacing="0" border="0" style="max-width:600px; width:100%%; background:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 4px 20px rgba(0,0,0,0.08);">
                                <!-- 顶部品牌区 -->
                                <tr>
                                    <td style="background:linear-gradient(135deg, #2563eb 0%%, #1e40af 100%%); padding:32px 40px; text-align:center;">
                                        <h1 style="margin:0; color:#ffffff; font-size:22px; font-weight:700;">知我公众号创作助手</h1>
                                        <p style="margin:8px 0 0; color:rgba(255,255,255,0.85); font-size:14px;">让 AI 成为您的创作引擎</p>
                                    </td>
                                </tr>

                                <!-- 问候语 -->
                                <tr>
                                    <td style="padding:32px 40px 20px;">
                                        <p style="margin:0; font-size:16px; color:#374151; line-height:1.6;">
                                            尊敬的 <strong style="color:#2563eb;">%s</strong>，您好！
                                        </p>
                                    </td>
                                </tr>

                                <!-- 活动标题 -->
                                <tr>
                                    <td style="padding:0 40px 24px;">
                                        <div style="background:#f0fdf4; border-radius:12px; padding:20px 24px; border-left:4px solid #07C160;">
                                            <p style="margin:0 0 8px; font-size:13px; color:#6b7280;">活动通知</p>
                                            <p style="margin:0; font-size:19px; font-weight:700; color:#111827; line-height:1.5;">%s</p>
                                        </div>
                                    </td>
                                </tr>

                                <!-- 活动内容 -->
                                <tr>
                                    <td style="padding:0 40px 32px;">
                                        <div style="font-size:15px; color:#4b5563; line-height:1.8;">
                                            %s
                                        </div>
                                    </td>
                                </tr>

                                %s

                                <!-- 分隔线 -->
                                <tr>
                                    <td style="padding:0 40px;">
                                        <div style="border-top:1px solid #e5e7eb; padding:20px 0; text-align:center;">
                                            <p style="margin:0 0 8px; font-size:12px; color:#9ca3af;">
                                                知我公众号创作助手 · 让创作更简单，让变现更高效
                                            </p>
                                            <p style="margin:0; font-size:11px; color:#cbd5e1;">
                                                如您不希望接收此类邮件，可在个人中心关闭邮件推送
                                            </p>
                                        </div>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(displayName, activityTitle, activityContent, qrCodeHtml);
    }

    private String buildDailyRecommendHtml(String userName, String trackName, String articleTitle, String platform) {
        String displayName = userName != null && !userName.isEmpty() ? userName : "创作者";
        String displayTrack = trackName != null && !trackName.isEmpty() ? trackName : "精选赛道";
        String displayPlatform = platform != null && !platform.isEmpty() ? platform : "公众号";
        return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
            </head>
            <body style="margin:0; padding:0; background-color:#f5f7fa; font-family:-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;">
                <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background-color:#f5f7fa;">
                    <tr>
                        <td align="center" style="padding:40px 20px;">
                            <table width="600" cellpadding="0" cellspacing="0" border="0" style="max-width:600px; width:100%%; background:#ffffff; border-radius:16px; overflow:hidden; box-shadow:0 4px 20px rgba(0,0,0,0.08);">
                                <!-- 顶部品牌区 -->
                                <tr>
                                    <td style="background:linear-gradient(135deg, #2563eb 0%%, #1e40af 100%%); padding:32px 40px; text-align:center;">
                                        <h1 style="margin:0; color:#ffffff; font-size:22px; font-weight:700;">知我公众号创作助手</h1>
                                        <p style="margin:8px 0 0; color:rgba(255,255,255,0.85); font-size:14px;">让 AI 成为您的创作引擎</p>
                                    </td>
                                </tr>

                                <!-- 问候语 -->
                                <tr>
                                    <td style="padding:32px 40px 20px;">
                                        <p style="margin:0; font-size:16px; color:#374151; line-height:1.6;">
                                            尊敬的 <strong style="color:#2563eb;">%s</strong>，您好！
                                        </p>
                                    </td>
                                </tr>

                                <!-- 正文内容 -->
                                <tr>
                                    <td style="padding:0 40px 24px;">
                                        <p style="margin:0 0 16px; font-size:15px; color:#4b5563; line-height:1.8;">
                                            今日为您推荐 <strong style="color:#2563eb;">%s</strong> 的优质文章，附件已随邮件送达，请查收。
                                        </p>
                                        <div style="background:#f8fafc; border-radius:12px; padding:20px 24px; border-left:4px solid #2563eb;">
                                            <p style="margin:0 0 8px; font-size:13px; color:#6b7280;">文章标题</p>
                                            <p style="margin:0 0 12px; font-size:17px; font-weight:600; color:#111827; line-height:1.5;">%s</p>
                                            <p style="margin:0; font-size:13px; color:#6b7280;">发布平台：<strong style="color:#2563eb;">%s</strong></p>
                                        </div>
                                    </td>
                                </tr>

                                <!-- 温馨提示 -->
                                <tr>
                                    <td style="padding:0 40px 32px;">
                                        <div style="background:#fff7ed; border-radius:10px; padding:16px 20px; border:1px solid #fed7aa;">
                                            <p style="margin:0; font-size:13px; color:#9a3412; line-height:1.6;">
                                                <strong>温馨提示：</strong><br>
                                                1. 附件为 Word 文档（.doc），可保存到本地；<br>
                                                2. 保存后可直接上传到公众号平台使用；<br>
                                                3. 文章内容已根据您的个人风格偏好进行排版。
                                            </p>
                                        </div>
                                    </td>
                                </tr>

                                <!-- 分隔线 -->
                                <tr>
                                    <td style="padding:0 40px;">
                                        <div style="border-top:1px solid #e5e7eb; padding:20px 0; text-align:center;">
                                            <p style="margin:0 0 8px; font-size:12px; color:#9ca3af;">
                                                知我公众号创作助手 · 让创作更简单，让变现更高效
                                            </p>
                                            <p style="margin:0; font-size:11px; color:#cbd5e1;">
                                                如您不希望接收此类邮件，可在个人中心关闭邮件推送
                                            </p>
                                        </div>
                                    </td>
                                </tr>
                            </table>
                        </td>
                    </tr>
                </table>
            </body>
            </html>
            """.formatted(displayName, displayTrack, articleTitle, displayPlatform);
    }
}
