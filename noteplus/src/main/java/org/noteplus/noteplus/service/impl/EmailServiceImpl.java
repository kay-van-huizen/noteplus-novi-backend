package org.noteplus.noteplus.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setTo(toEmail);
            helper.setSubject("Reset your NotePlus password");
            helper.setFrom("noreply@noteplus.nl");
            helper.setText(buildHtml(resetLink), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send password reset email", e);
        }
    }

    private String buildHtml(String resetLink) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 520px; margin: 0 auto; padding: 32px 0;">
                  <h2 style="color: #1e293b; margin: 0 0 16px;">Reset your NotePlus password</h2>
                  <p style="color: #374151; line-height: 1.6;">
                    You requested to reset your NotePlus password.
                    Click the button below to set a new password.
                    This link expires in 60 minutes.
                  </p>
                  <div style="text-align: center; margin: 32px 0;">
                    <a href="%s"
                       style="
                         display: inline-block;
                         background-color: #4F46E5;
                         color: #ffffff;
                         text-decoration: none;
                         padding: 14px 32px;
                         border-radius: 8px;
                         font-size: 16px;
                         font-weight: 600;
                         font-family: Arial, sans-serif;
                         letter-spacing: 0.3px;
                       ">
                      Reset my password
                    </a>
                  </div>
                  <p style="font-size: 13px; color: #6b7280; text-align: center;">
                    Or copy this link: <a href="%s" style="color: #4F46E5;">%s</a>
                  </p>
                  <p style="font-size: 13px; color: #9ca3af; margin-top: 24px;">
                    If you did not request this, you can safely ignore this email.
                    Your password will not be changed.
                  </p>
                  <p style="font-size: 13px; color: #9ca3af;">— NotePlus</p>
                </div>
                """.formatted(resetLink, resetLink, resetLink);
    }
}
