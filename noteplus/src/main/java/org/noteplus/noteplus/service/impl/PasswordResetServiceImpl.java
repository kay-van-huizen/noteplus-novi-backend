package org.noteplus.noteplus.service.impl;

import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.entity.PasswordResetToken;
import org.noteplus.noteplus.repository.PasswordResetTokenRepository;
import org.noteplus.noteplus.repository.UserRepository;
import org.noteplus.noteplus.service.EmailService;
import org.noteplus.noteplus.service.PasswordResetService;
import org.noteplus.noteplus.util.TokenUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final TokenUtil tokenUtil;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.password-reset.expiry-minutes:60}")
    private long expiryMinutes;

    @Override
    public void requestReset(String email) {
        tokenRepository.deleteExpiredAndUsed(LocalDateTime.now());

        var userOpt = userRepository.findByEmail(email);

        // SECURITY: always return without revealing whether email exists
        if (userOpt.isEmpty()) {
            return;
        }

        var user = userOpt.get();
        String rawToken = tokenUtil.generateRawToken();
        String tokenHash = tokenUtil.hashToken(rawToken);

        var resetToken = new PasswordResetToken();
        resetToken.setTokenHash(tokenHash);
        resetToken.setUser(user);
        resetToken.setExpiresAt(LocalDateTime.now().plusMinutes(expiryMinutes));
        resetToken.setUsed(false);
        tokenRepository.save(resetToken);

        String resetLink = baseUrl + "/reset-password?token=" + rawToken;
        emailService.sendPasswordResetEmail(user.getEmail(), resetLink);
    }

    @Override
    public void resetPassword(String rawToken, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }
        if (newPassword.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }

        String tokenHash = tokenUtil.hashToken(rawToken);

        var resetToken = tokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset link"));

        if (resetToken.isUsed()) {
            throw new IllegalArgumentException("This reset link has already been used");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("This reset link has expired. Please request a new one");
        }

        var user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }
}
