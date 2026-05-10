package org.noteplus.noteplus.service;

import org.noteplus.noteplus.dto.request.ChangePasswordRequest;
import org.noteplus.noteplus.dto.response.MeResponse;
import org.noteplus.noteplus.dto.response.UserResponse;

import java.util.List;

public interface UserService {
    void changePassword(ChangePasswordRequest request, String username);
    List<UserResponse> getUsersByRole(String roleName);
    MeResponse getCurrentUser(String username);
}
