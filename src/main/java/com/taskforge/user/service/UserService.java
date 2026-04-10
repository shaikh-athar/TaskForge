package com.taskforge.user.service;

import com.taskforge.user.model.User;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.UUID;

public interface UserService {
    public User getOrCreateUser(Jwt jwt);
}