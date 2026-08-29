package com.url.shortener.service;

import com.url.shortener.dto.request.RegisterRequest;
import com.url.shortener.entity.Role;
import com.url.shortener.entity.User;
import com.url.shortener.exception.EmailAlreadyExistsException;
import com.url.shortener.exception.UsernameAlreadyExistsException;
import com.url.shortener.repository.UserRepository;
import com.url.shortener.security.jwt.JwtUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private UserService userService;

    @Test
    void registerUser_shouldThrowException_whenUsernameAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("existinguser");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        assertThrows(UsernameAlreadyExistsException.class,
                () -> userService.registerUser(request));
    }

    @Test
    void registerUser_shouldThrowException_whenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("existing@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        assertThrows(EmailAlreadyExistsException.class,
                () -> userService.registerUser(request));
    }

    @Test
    void registerUser_shouldSucceed_whenUsernameAndEmailAreAvailable() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("new@example.com");
        request.setPassword("password123");

        when(userRepository.existsByUsername("newuser")).thenReturn(false);
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setUsername("newuser");
        savedUser.setEmail("new@example.com");
        savedUser.setRole(Role.ROLE_USER);

        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        User result = userService.registerUser(request);

        org.junit.jupiter.api.Assertions.assertEquals("newuser", result.getUsername());
        org.junit.jupiter.api.Assertions.assertEquals(Role.ROLE_USER, result.getRole());
    }
}