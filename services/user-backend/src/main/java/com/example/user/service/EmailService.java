package com.example.user.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

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
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("HTML邮件发送失败: " + e.getMessage(), e);
        } catch (org.springframework.mail.MailException e) {
            throw new RuntimeException("邮件发送失败，请检查SMTP配置: " + e.getMessage(), e);
        }
    }

    public void sendTestEmail(String to) {
        String subject = "【测试邮件】知我公众号创作助手 - 邮件接收功能验证";
        String html = buildTestEmailHtml();
        sendHtmlEmail(to, subject, html);
    }

    private String buildTestEmailHtml() {
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
                                <!-- 测试提示横幅 -->
                                <tr>
                                    <td style="background:linear-gradient(135deg, #e8f5e9 0%%, #c8e6c9 100%%); padding:16px 32px; text-align:center;">
                                        <p style="margin:0; color:#2e7d32; font-size:14px; font-weight:500;">
                                            ✓ 这是一封测试邮件，用于验证您的邮件接收功能是否正常
                                        </p>
                                    </td>
                                </tr>

                                <!-- 主标题区 -->
                                <tr>
                                    <td style="padding:40px 40px 20px; text-align:center;">
                                        <h1 style="margin:0 0 8px; font-size:26px; font-weight:700; color:#1a1a2e; line-height:1.3;">
                                            知我公众号创作助手
                                        </h1>
                                        <p style="margin:0; font-size:16px; color:#64748b; line-height:1.6;">
                                            让 AI 成为您的副业引擎，开启内容变现新赛道
                                        </p>
                                    </td>
                                </tr>

                                <!-- 核心卖点 -->
                                <tr>
                                    <td style="padding:0 40px 30px;">
                                        <table width="100%%" cellpadding="0" cellspacing="0" border="0">
                                            <tr>
                                                <td width="48%%" style="background:#f0f9ff; border-radius:12px; padding:20px; text-align:center;">
                                                    <div style="font-size:18px; font-weight:700; color:#0369a1; margin-bottom:6px;">副业盈利</div>
                                                    <div style="font-size:13px; color:#64748b; line-height:1.5;">AI 辅助创作，每天仅需 30 分钟，轻松产出爆款内容</div>
                                                </td>
                                                <td width="4%%"></td>
                                                <td width="48%%" style="background:#f0fdf4; border-radius:12px; padding:20px; text-align:center;">
                                                    <div style="font-size:18px; font-weight:700; color:#15803d; margin-bottom:6px;">复利投资</div>
                                                    <div style="font-size:13px; color:#64748b; line-height:1.5;">内容即资产，一篇好文章持续为您带来长尾收益</div>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>

                                <!-- 数据亮点 -->
                                <tr>
                                    <td style="padding:0 40px 30px;">
                                        <table width="100%%" cellpadding="0" cellspacing="0" border="0" style="background:#fafafa; border-radius:12px; border:1px solid #e2e8f0;">
                                            <tr>
                                                <td style="padding:24px; text-align:center;">
                                                    <table width="100%%" cellpadding="0" cellspacing="0" border="0">
                                                        <tr>
                                                            <td width="33%%" style="text-align:center; border-right:1px solid #e2e8f0;">
                                                                <div style="font-size:28px; font-weight:700; color:#07C160;">36+</div>
                                                                <div style="font-size:12px; color:#94a3b8; margin-top:4px;">热门赛道</div>
                                                            </td>
                                                            <td width="33%%" style="text-align:center; border-right:1px solid #e2e8f0;">
                                                                <div style="font-size:28px; font-weight:700; color:#07C160;">200+</div>
                                                                <div style="font-size:12px; color:#94a3b8; margin-top:4px;">头部博主</div>
                                                            </td>
                                                            <td width="33%%" style="text-align:center;">
                                                                <div style="font-size:28px; font-weight:700; color:#07C160;">10w+</div>
                                                                <div style="font-size:12px; color:#94a3b8; margin-top:4px;">爆款文章</div>
                                                            </td>
                                                        </tr>
                                                    </table>
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>

                                <!-- 金句区 -->
                                <tr>
                                    <td style="padding:0 40px 30px;">
                                        <div style="background:linear-gradient(135deg, #1e293b 0%%, #0f172a 100%%); border-radius:12px; padding:28px 32px; text-align:center;">
                                            <p style="margin:0 0 12px; font-size:18px; color:#ffffff; font-weight:600; line-height:1.6;">
                                                "这不是一次消费，而是一次投资"
                                            </p>
                                            <p style="margin:0; font-size:14px; color:#94a3b8; line-height:1.6;">
                                                别人在刷视频的时候，您已经在用 AI 批量生产内容。<br>
                                                时间差就是信息差，信息差就是财富差。
                                            </p>
                                        </div>
                                    </td>
                                </tr>

                                <!-- 功能亮点 -->
                                <tr>
                                    <td style="padding:0 40px 30px;">
                                        <h3 style="margin:0 0 16px; font-size:16px; font-weight:600; color:#1e293b;">为什么选择知我？</h3>
                                        <table width="100%%" cellpadding="0" cellspacing="0" border="0">
                                            <tr>
                                                <td style="padding:8px 0; color:#475569; font-size:14px;">
                                                    <span style="color:#07C160; margin-right:8px;">✓</span> 每日智能推荐，3分钟锁定创作方向
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style="padding:8px 0; color:#475569; font-size:14px;">
                                                    <span style="color:#07C160; margin-right:8px;">✓</span> AI 每日推荐爆款文章，解放双手
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style="padding:8px 0; color:#475569; font-size:14px;">
                                                    <span style="color:#07C160; margin-right:8px;">✓</span> 一键导出 Word，直接发布到公众号
                                                </td>
                                            </tr>
                                            <tr>
                                                <td style="padding:8px 0; color:#475569; font-size:14px;">
                                                    <span style="color:#07C160; margin-right:8px;">✓</span> 多平台分发：公众号 · 今日头条 · 百家号
                                                </td>
                                            </tr>
                                        </table>
                                    </td>
                                </tr>

                                <!-- CTA 按钮 -->
                                <tr>
                                    <td style="padding:0 40px 40px; text-align:center;">
                                        <a href="https://www.mmshuo.tech/" style="display:inline-block; background-color:#07C160; color:#ffffff !important; text-decoration:none; padding:16px 48px; border-radius:10px; font-size:16px; font-weight:600;">
                                            立即开启内容变现 →
                                        </a>
                                        <p style="margin:16px 0 0; font-size:12px; color:#94a3b8;">
                                            扫码联系客服开通账号，前 50 名用户享专属优惠
                                        </p>
                                    </td>
                                </tr>

                                <!-- 底部分隔线 -->
                                <tr>
                                    <td style="padding:0 40px;">
                                        <div style="border-top:1px solid #e2e8f0; padding:20px 0; text-align:center;">
                                            <p style="margin:0 0 8px; font-size:12px; color:#94a3b8;">
                                                知我公众号创作助手 · 让创作更简单，让变现更高效
                                            </p>
                                            <p style="margin:0; font-size:11px; color:#cbd5e1;">
                                                本邮件为系统测试邮件，如您不希望接收此类邮件，可在个人中心关闭邮件推送
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
            """;
    }
}
