package org.noteplus.noteplus.service.impl;

import lombok.RequiredArgsConstructor;
import org.noteplus.noteplus.dto.request.ChangePasswordRequest;
import org.noteplus.noteplus.dto.response.MeResponse;
import org.noteplus.noteplus.dto.response.UserResponse;
import org.noteplus.noteplus.entity.Role;
import org.noteplus.noteplus.entity.User;
import org.noteplus.noteplus.exception.ForbiddenException;
import org.noteplus.noteplus.exception.ResourceNotFoundException;
import org.noteplus.noteplus.repository.UserRepository;
import org.noteplus.noteplus.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void changePassword(ChangePasswordRequest request, String username) {
        var user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new ForbiddenException("Current password is incorrect");
        }

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("New passwords do not match");
        }

        if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
            throw new IllegalArgumentException("New password must be different from the current password");
        }

        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByRole(String roleName) {
        return userRepository.findByRoleName(roleName).stream()
                .map(u -> new UserResponse(u.getId(), u.getUsername(), u.getEmail()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MeResponse getCurrentUser(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .toList();
        return new MeResponse(user.getId(), user.getName(), user.getUsername(), roles);
    }
}
