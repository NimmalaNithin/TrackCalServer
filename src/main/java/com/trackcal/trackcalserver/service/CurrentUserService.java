package com.trackcal.trackcalserver.service;

import com.trackcal.trackcalserver.exception.UserNotFoundException;
import com.trackcal.trackcalserver.model.User;
import com.trackcal.trackcalserver.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final UserRepository userRepository;

    public CurrentUserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User requireUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found"));
    }

    public String requireUserId(String email) {
        return requireUser(email).getId();
    }
}
