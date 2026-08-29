package com.url.shortener.service;


import com.url.shortener.dto.request.LoginRequest;
import com.url.shortener.dto.request.RegisterRequest;
import com.url.shortener.dto.response.LoginResponse;
import com.url.shortener.entity.Role;
import com.url.shortener.entity.User;
import com.url.shortener.exception.EmailAlreadyExistsException;
import com.url.shortener.exception.UsernameAlreadyExistsException;
import com.url.shortener.repository.UserRepository;
import com.url.shortener.security.jwt.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final JwtUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;

    public User registerUser(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException("Username already taken: " + request.getUsername());
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already registered: " + request.getEmail());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(Role.ROLE_USER);
        return userRepository.save(user);
    }

    public LoginResponse authenticateUser(LoginRequest loginRequest){
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(),
                            loginRequest.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String jwt=jwtUtils.generateToken(userDetails);

        LoginResponse response = new LoginResponse();
        response.setToken(jwt);
        response.setId(userDetails.getId());
        response.setUsername(userDetails.getUsername());
        response.setEmail(userDetails.getEmail());
        return response;
    }

    public User findByUsername(String name) {
            return  userRepository.findByUsername(name).orElseThrow(
                    ()-> new UsernameNotFoundException("user not found with username: "+name)
            );

    }
}
