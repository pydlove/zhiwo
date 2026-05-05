package com.example.user.service;

import com.example.user.entity.Config;
import com.example.user.mapper.ConfigMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class MailNotifyService {

    private final JavaMailSender mailSender;
    private final ConfigMapper configMapper;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public MailNotifyService(JavaMailSender mailSender, ConfigMapper configMapper) {
        this.mailSender = mailSender;
        this.configMapper = configMapper;
    }

    /**
     * 开户成功后发送邮件通知管理员
     */
    public void sendOpenAccountNotice(String nickName, String wxName, String email) {
        try {
            Config enabledConfig = configMapper.findByKey("notifyEmailEnabled");
            if (enabledConfig == null || !"1".equals(enabledConfig.getConfigValue())) {
                return; // 未启用
            }

            Config addressConfig = configMapper.findByKey("notifyEmailAddress");
            String toEmail = addressConfig != null ? addressConfig.getConfigValue() : null;
            if (toEmail == null || toEmail.isEmpty()) {
                System.err.println("[MailNotify] 管理员邮箱未配置，跳过通知");
                return;
            }

            String subject = "【开户通知】有新的用户提交了开户申请";
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String content = String.format(
                "有新的用户提交了开户申请，请及时审核。\n\n" +
                "微信名称：%s\n" +
                "公众号名称：%s\n" +
                "邮箱：%s\n" +
                "申请时间：%s\n\n" +
                "—— 知我公众号创作助手",
                nickName != null ? nickName : "-",
                wxName != null ? wxName : "-",
                email != null ? email : "-",
                time
            );

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(content);
            mailSender.send(message);

            System.out.println("[MailNotify] 开户通知邮件已发送至 " + toEmail);
        } catch (Exception e) {
            System.err.println("[MailNotify] 发送邮件通知失败: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
