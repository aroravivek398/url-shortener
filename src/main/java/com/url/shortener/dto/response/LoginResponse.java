package com.url.shortener.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginResponse {

    private String token;
    private Long id;
    private String username;
    private String email;
}