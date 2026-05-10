package org.noteplus.noteplus.service.impl;

import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.base-url}")
    private String baseUrl;

    @Override
    public void sendPasswordResetEmail(String toEmail, String resetLink) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Reset your NotePlus password");
        message.setText("""
                Hello,

                You requested to reset your NotePlus password.
                Click the link below to set a new password. This link expires in 60 minutes.

                %s

                If you did not request this, you can safely ignore this email.
                Your password will not be changed.

                — NotePlus
                """.formatted(resetLink));
        message.setFrom("noreply@noteplus.nl");

        mailSender.send(message);
    }
}
