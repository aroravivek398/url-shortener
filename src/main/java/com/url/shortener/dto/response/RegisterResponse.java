package com.url.shortener.dto.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class RegisterResponse {

    private Long id;
    private String username;
    private String email;
    private LocalDateTime createdAt;
}