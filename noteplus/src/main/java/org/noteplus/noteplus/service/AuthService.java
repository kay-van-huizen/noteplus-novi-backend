package org.noteplus.noteplus.service;

import org.noteplus.noteplus.dto.request.LoginRequest;
import org.noteplus.noteplus.dto.request.RegisterRequest;
import org.noteplus.noteplus.dto.response.AuthResponse;

public interface AuthService {

    AuthResponse register(RegisterRequest request);

    AuthResponse login(LoginRequest request);
}