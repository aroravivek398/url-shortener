package com.url.shortener.service;

import com.url.shortener.entity.UrlMapping;
import com.url.shortener.entity.User;
import com.url.shortener.repository.UrlMappingRepository;
import com.url.shortener.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@Testcontainers
class UrlMappingServiceCacheTest {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("urlshortenerdb_test")
            .withUsername("test_user")
            .withPassword("test_pass");

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", mysql::getJdbcUrl);
        registry.add("spring.datasource.username", mysql::getUsername);
        registry.add("spring.datasource.password", mysql::getPassword);

        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private UrlMappingService urlMappingService;

    @Autowired
    private UrlMappingRepository urlMappingRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void getOriginalUrl_shouldServeFromCache_evenAfterDatabaseRowIsDeleted() {
        User user = new User();
        user.setUsername("cachetestuser");
        user.setEmail("cachetest@example.com");
        user.setPassword("irrelevant-for-this-test");
        User savedUser = userRepository.save(user);

        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setOriginalUrl("https://testcache.com");
        urlMapping.setShortUrl("cachekey1");
        urlMapping.setUser(savedUser);
        urlMappingRepository.save(urlMapping);

        // First call — cache miss, reads from DB, populates Redis
        String firstResult = urlMappingService.getOriginalUrl("cachekey1", "127.0.0.1", "test-agent");
        assertEquals("https://testcache.com", firstResult);

        // Delete the row directly from the database — the cache has no idea this happened
        urlMappingRepository.delete(urlMapping);

        // Second call — if this STILL succeeds, it proves the result came from Redis,
        // not the database (which no longer has this row at all)
        String secondResult = urlMappingService.getOriginalUrl("cachekey1", "127.0.0.1", "test-agent");
        assertEquals("https://testcache.com", secondResult);
    }
}