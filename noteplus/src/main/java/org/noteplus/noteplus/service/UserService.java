package org.noteplus.noteplus.service;

import org.noteplus.noteplus.dto.request.ChangePasswordRequest;

public interface UserService {
    void changePassword(ChangePasswordRequest request, String username);
}
