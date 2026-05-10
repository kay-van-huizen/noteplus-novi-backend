package org.noteplus.noteplus.service;

public interface PasswordResetService {
    void requestReset(String email);
    void resetPassword(String rawToken, String newPassword, String confirmPassword);
}
