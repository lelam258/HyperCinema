package com.cinema.hyperCinema.service.impl;

import com.cinema.hyperCinema.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    public EmailServiceImpl(JavaMailSender mailSender, TemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Override
    public void sendActivationCode(String to, String code) {
        String activationUrl = baseUrl + "/api/auth/verify-email?email=" + encode(to) + "&code=" + encode(code);
        Context context = new Context();
        context.setVariable("activationUrl", activationUrl);
        context.setVariable("expireText", "24 hours");

        sendHtmlMail(
                to,
                "HyperCinema - Account activation code",
                "email/account-activation",
                context
        );
    }

    @Override
    public void sendForgotPasswordCode(String to, String code) {
        String resetUrl = baseUrl + "/reset-password?email=" + encode(to) + "&code=" + encode(code);
        Context context = new Context();
        context.setVariable("code", code);
        context.setVariable("resetUrl", resetUrl);
        context.setVariable("expireText", "15 minutes");

        sendHtmlMail(
                to,
                "HyperCinema - Password reset code",
                "email/forgot-password",
                context
        );
    }

    private void sendHtmlMail(String to, String subject, String template, Context context) {
        try {
            String htmlContent = templateEngine.process(template, context);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, StandardCharsets.UTF_8.name());
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new IllegalStateException("Could not send email", e);
        }
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
