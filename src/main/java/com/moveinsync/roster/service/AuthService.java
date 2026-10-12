package com.moveinsync.roster.service;

import com.moveinsync.roster.dto.request.LoginRequest;
import com.moveinsync.roster.dto.request.UserCreateRequest;
import com.moveinsync.roster.dto.response.LoginResponse;
import com.moveinsync.roster.exception.DuplicateResourceException;
import com.moveinsync.roster.model.entity.User;
import com.moveinsync.roster.model.enums.UserRole;
import com.moveinsync.roster.repository.UserRepository;
import com.moveinsync.roster.security.JwtTokenProvider;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthenticationManager authenticationManager, JwtTokenProvider jwtTokenProvider,
                       UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        
        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();
        String token = jwtTokenProvider.generateToken(user.getUsername(), user.getRole().name());
        
        return new LoginResponse(token, user.getUsername(), user.getRole().name(), jwtTokenProvider.getExpirationMs());
    }

    public void createUser(UserCreateRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new DuplicateResourceException("Username already exists: " + request.getUsername());
        }
        UserRole role = UserRole.valueOf(request.getRole().toUpperCase());
        User user = new User(request.getUsername(), passwordEncoder.encode(request.getPassword()),
                request.getFullName(), role);
        userRepository.save(user);
    }
}
