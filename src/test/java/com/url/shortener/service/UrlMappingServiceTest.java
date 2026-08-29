package com.url.shortener.service;

import com.url.shortener.dto.request.CreateUrlRequest;
import com.url.shortener.dto.response.UrlMappingResponse;
import com.url.shortener.entity.UrlMapping;
import com.url.shortener.entity.User;
import com.url.shortener.exception.AliasAlreadyTakenException;
import com.url.shortener.kafka.ClickEventProducer;
import com.url.shortener.repository.ClickEventRepository;
import com.url.shortener.repository.UrlMappingRepository;
import com.url.shortener.service.UrlMappingService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UrlMappingServiceTest {

    @Mock
    private UrlMappingRepository urlMappingRepository;

    @Mock
    private ClickEventRepository clickEventRepository;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ClickEventProducer clickEventProducer;

    @InjectMocks
    private UrlMappingService urlMappingService;

    @Test
    void createShortUrl_shouldThrowException_whenCustomAliasAlreadyExists() {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://example.com");
        request.setCustomAlias("my-alias");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        UrlMapping existingMapping = new UrlMapping();
        existingMapping.setShortUrl("my-alias");

        when(urlMappingRepository.findByShortUrl("my-alias"))
                .thenReturn(Optional.of(existingMapping));

        assertThrows(AliasAlreadyTakenException.class,
                () -> urlMappingService.createShortUrl(request, user));
    }

    @Test
    void createShortUrl_shouldSucceed_whenCustomAliasIsAvailable() {
        CreateUrlRequest request = new CreateUrlRequest();
        request.setOriginalUrl("https://example.com");
        request.setCustomAlias("my-alias");

        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");

        when(urlMappingRepository.findByShortUrl("my-alias"))
                .thenReturn(Optional.empty());

        UrlMapping savedMapping = new UrlMapping();
        savedMapping.setId(1L);
        savedMapping.setOriginalUrl("https://example.com");
        savedMapping.setShortUrl("my-alias");
        savedMapping.setUser(user);

        when(urlMappingRepository.save(any(UrlMapping.class)))
                .thenReturn(savedMapping);

        UrlMappingResponse response = urlMappingService.createShortUrl(request, user);

        assertEquals("my-alias", response.getShortUrl());
        assertEquals("https://example.com", response.getOriginalUrl());
    }
}