package com.taskforge.user.service.impl;

import com.taskforge.user.model.User;
import com.taskforge.user.repository.UserRepository;
import com.taskforge.user.service.UserService;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;
import org.springframework.cache.annotation.Cacheable;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    @Cacheable(value = "users", key = "#jwt.subject")
    public User getOrCreateUser(Jwt jwt) {

        UUID userId = UUID.fromString(jwt.getSubject());
        String email = jwt.getClaimAsString("email");

        return userRepository.findById(userId)
                .orElseGet(() -> userRepository.findByEmail(email)
                        .orElseGet(() -> {
                            User user = new User();
                            user.setId(userId);
                            user.setEmail(email);
                            user.setDisplayName(jwt.getClaimAsString("name"));
                            user.setAvatarUrl(jwt.getClaimAsString("picture"));
                            return userRepository.save(user);
                        }));
    }
}
