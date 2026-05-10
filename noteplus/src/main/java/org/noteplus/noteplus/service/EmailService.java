package org.noteplus.noteplus.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String resetLink);
}
