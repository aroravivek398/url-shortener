package com.url.shortener.controller;

import com.url.shortener.entity.UrlMapping;
import com.url.shortener.service.UrlMappingService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class RedirectController {
    private final UrlMappingService urlMappingService;

    @GetMapping("/{shortUrl}")
    public ResponseEntity<Void> redirect(@PathVariable String shortUrl, HttpServletRequest request) {
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        String originalUrl = urlMappingService.getOriginalUrl(shortUrl, ipAddress, userAgent);

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Location", originalUrl);
        return ResponseEntity.status(302).headers(httpHeaders).build();
    }
}