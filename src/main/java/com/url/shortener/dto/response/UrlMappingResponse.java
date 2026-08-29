package com.url.shortener.dto.response;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UrlMappingResponse {
    private Long id;
    private String originalUrl;
    private String shortUrl;
    private int clickCount ;
    private LocalDateTime createdDate;
    private String username;


}
