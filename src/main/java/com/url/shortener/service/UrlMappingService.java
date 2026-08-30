package com.url.shortener.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.url.shortener.dto.ClickEventDTO;
import com.url.shortener.dto.request.CreateUrlRequest;
import com.url.shortener.dto.response.DeviceStatsResponse;
import com.url.shortener.dto.response.UrlMappingResponse;
import com.url.shortener.entity.ClickEvent;
import com.url.shortener.entity.UrlMapping;
import com.url.shortener.entity.User;
import com.url.shortener.exception.AliasAlreadyTakenException;
import com.url.shortener.exception.UrlExpiredException;
import com.url.shortener.exception.UrlNotFoundException;
import com.url.shortener.exception.UrlOwnershipException;
import com.url.shortener.kafka.ClickEventProducer;
import com.url.shortener.repository.ClickEventRepository;
import com.url.shortener.repository.UrlMappingRepository;
import com.url.shortener.util.Base62Encoder;
import com.url.shortener.util.DeviceClassifier;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UrlMappingService {
    private final UrlMappingRepository urlMappingRepository;
    private final ClickEventRepository clickEventRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final ClickEventProducer clickEventProducer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String CACHE_PREFIX = "shorturl:";
    private static final long CACHE_TTL_HOURS = 1;

    public UrlMappingResponse createShortUrl(CreateUrlRequest request, User user) {
        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setOriginalUrl(request.getOriginalUrl());
        urlMapping.setUser(user);
        setExpiryIfRequested(urlMapping, request.getExpiryDays());

        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            Optional<UrlMapping> existing = urlMappingRepository.findByShortUrl(request.getCustomAlias());
            if (existing.isPresent()) {
                throw new AliasAlreadyTakenException("This alias is already in use");
            }

            urlMapping.setShortUrl(request.getCustomAlias());
            urlMapping.setCustomAlias(true);
            UrlMapping savedUrlMapping = urlMappingRepository.save(urlMapping);
            return convertToDto(savedUrlMapping);
        }

        urlMapping.setShortUrl("");
        UrlMapping savedUrlMapping = urlMappingRepository.save(urlMapping);

        String shortUrl = Base62Encoder.encode(savedUrlMapping.getId());
        savedUrlMapping.setShortUrl(shortUrl);
        savedUrlMapping = urlMappingRepository.save(savedUrlMapping);

        return convertToDto(savedUrlMapping);
    }

    private void setExpiryIfRequested(UrlMapping urlMapping, Integer expiryDays) {
        if (expiryDays != null) {
            urlMapping.setExpiryDate(LocalDateTime.now().plusDays(expiryDays));
        }
    }

    private UrlMappingResponse convertToDto(UrlMapping urlMapping) {
        UrlMappingResponse urlMappingDTO = new UrlMappingResponse();
        urlMappingDTO.setId(urlMapping.getId());
        urlMappingDTO.setOriginalUrl(urlMapping.getOriginalUrl());
        urlMappingDTO.setShortUrl(urlMapping.getShortUrl());
        urlMappingDTO.setClickCount(urlMapping.getClickCount());
        urlMappingDTO.setCreatedDate(urlMapping.getCreateDate());
        urlMappingDTO.setUsername(urlMapping.getUser().getUsername());
        return urlMappingDTO;
    }

    public List<UrlMappingResponse> getUrlsByUser(User user) {
        return urlMappingRepository.findByUser(user).stream()
                .map(this::convertToDto)
                .toList();
    }

    public List<ClickEventDTO> getClickEventsByDate(String shortUrl, LocalDateTime start, LocalDateTime end) {
        UrlMapping urlMapping = urlMappingRepository.findByShortUrl(shortUrl)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortUrl));

        return clickEventRepository.findByUrlMappingAndClickDateBetween(urlMapping, start, end).stream()
                .collect(Collectors.groupingBy(click -> click.getClickDate().toLocalDate(), Collectors.counting()))
                .entrySet().stream()
                .map(entry -> {
                    ClickEventDTO clickEventDTO = new ClickEventDTO();
                    clickEventDTO.setClickDate(entry.getKey());
                    clickEventDTO.setCount(entry.getValue());
                    return clickEventDTO;
                })
                .collect(Collectors.toList());
    }

    public Map<LocalDate, Long> getTotalClicksByUserAndDate(User user, LocalDate start, LocalDate end) {
        List<UrlMapping> urlMappings = urlMappingRepository.findByUser(user);
        List<ClickEvent> clickEvents = clickEventRepository.findByUrlMappingInAndClickDateBetween(urlMappings, start.atStartOfDay(), end.plusDays(1).atStartOfDay());
        return clickEvents.stream()
                .collect(Collectors.groupingBy(click -> click.getClickDate().toLocalDate(), Collectors.counting()));
    }

    public String getOriginalUrl(String shortUrl, String ipAddress, String userAgent) {
        String cacheKey = CACHE_PREFIX + shortUrl;
        String cachedValue = redisTemplate.opsForValue().get(cacheKey);

        String originalUrl;

        if (cachedValue != null) {
            CachedUrlData cachedData = parseCachedValue(cachedValue);
            checkNotExpired(cachedData.expiryDate(), shortUrl);
            originalUrl = cachedData.originalUrl();
        } else {
            UrlMapping urlMapping = urlMappingRepository.findByShortUrl(shortUrl)
                    .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortUrl));

            checkNotExpired(urlMapping.getExpiryDate(), shortUrl);

            originalUrl = urlMapping.getOriginalUrl();
            String valueToCache = buildCacheValue(originalUrl, urlMapping.getExpiryDate());
            redisTemplate.opsForValue().set(cacheKey, valueToCache, CACHE_TTL_HOURS, TimeUnit.HOURS);
        }

        clickEventProducer.publish(shortUrl, ipAddress, userAgent);

        return originalUrl;
    }

    private void checkNotExpired(LocalDateTime expiryDate, String shortUrl) {
        if (expiryDate != null && expiryDate.isBefore(LocalDateTime.now())) {
            throw new UrlExpiredException("This short URL has expired: " + shortUrl);
        }
    }

    private String buildCacheValue(String originalUrl, LocalDateTime expiryDate) {
        String expiryDateJson = (expiryDate != null)
                ? "\"" + expiryDate + "\""
                : "null";
        return String.format(
                "{\"originalUrl\":\"%s\",\"expiryDate\":%s}",
                originalUrl, expiryDateJson
        );
    }

    private CachedUrlData parseCachedValue(String cachedValue) {
        try {
            Map<String, Object> data = objectMapper.readValue(cachedValue, Map.class);
            String originalUrl = (String) data.get("originalUrl");
            String expiryDateStr = (String) data.get("expiryDate");
            LocalDateTime expiryDate = (expiryDateStr != null) ? LocalDateTime.parse(expiryDateStr) : null;
            return new CachedUrlData(originalUrl, expiryDate);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse cached URL data", e);
        }
    }

    private record CachedUrlData(String originalUrl, LocalDateTime expiryDate) {}
    public DeviceStatsResponse getDeviceStats(String shortUrl) {
        UrlMapping urlMapping = urlMappingRepository.findByShortUrl(shortUrl)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortUrl));

        List<ClickEvent> clickEvents = clickEventRepository.findByUrlMapping(urlMapping);

        long mobileCount = 0;
        long desktopCount = 0;
        long tabletCount = 0;
        long unknownCount = 0;

        for (ClickEvent event : clickEvents) {
            String deviceType = DeviceClassifier.classifyDevice(event.getUserAgent());
            switch (deviceType) {
                case "Mobile" -> mobileCount++;
                case "Desktop" -> desktopCount++;
                case "Tablet" -> tabletCount++;
                default -> unknownCount++;
            }
        }

        DeviceStatsResponse response = new DeviceStatsResponse();
        response.setMobileClicks(mobileCount);
        response.setDesktopClicks(desktopCount);
        response.setTabletClicks(tabletCount);
        response.setUnknownClicks(unknownCount);
        return response;
    }
    public void deleteUrl(String shortUrl, User user) {
        UrlMapping urlMapping = urlMappingRepository.findByShortUrl(shortUrl)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortUrl));

        if (!urlMapping.getUser().getId().equals(user.getId())) {
            throw new UrlOwnershipException("You do not have permission to delete this URL");
        }

        urlMappingRepository.delete(urlMapping);
        redisTemplate.delete(CACHE_PREFIX + shortUrl);
    }
}