package com.url.shortener.service;

import com.url.shortener.dto.ClickEventDTO;
import com.url.shortener.dto.request.CreateUrlRequest;
import com.url.shortener.dto.response.UrlMappingResponse;
import com.url.shortener.entity.ClickEvent;
import com.url.shortener.entity.UrlMapping;
import com.url.shortener.entity.User;
import com.url.shortener.exception.AliasAlreadyTakenException;
import com.url.shortener.exception.UrlNotFoundException;
import com.url.shortener.kafka.ClickEventProducer;
import com.url.shortener.repository.ClickEventRepository;
import com.url.shortener.repository.UrlMappingRepository;
import com.url.shortener.util.Base62Encoder;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class UrlMappingService {
    private UrlMappingRepository urlMappingRepository;
    private ClickEventRepository clickEventRepository;

    private final RedisTemplate<String, String> redisTemplate;
    private final ClickEventProducer clickEventProducer; // we'll build this next

    private static final String CACHE_PREFIX = "shorturl:";
    private static final long CACHE_TTL_HOURS = 1;


    public UrlMappingResponse createShortUrl(CreateUrlRequest request, User user) {
        UrlMapping urlMapping = new UrlMapping();
        urlMapping.setOriginalUrl(request.getOriginalUrl());
        urlMapping.setUser(user);

        if (request.getCustomAlias() != null && !request.getCustomAlias().isBlank()) {
            // Custom alias path — needs an existence check, since arbitrary text
            // isn't guaranteed unique the way an auto-incremented ID is
            Optional<UrlMapping> existing = urlMappingRepository.findByShortUrl(request.getCustomAlias());
            if (existing.isPresent()) {
                throw new AliasAlreadyTakenException("This alias is already in use");
            }
            urlMapping.setShortUrl(request.getCustomAlias());
            urlMapping.setCustomAlias(true);
            UrlMapping savedUrlMapping = urlMappingRepository.save(urlMapping);
            return convertToDto(savedUrlMapping);
        }

        // Auto-generated path — Base62 encoding, no collision possible
        urlMapping.setShortUrl(""); // temporary placeholder
        UrlMapping savedUrlMapping = urlMappingRepository.save(urlMapping);

        String shortUrl = Base62Encoder.encode(savedUrlMapping.getId());
        savedUrlMapping.setShortUrl(shortUrl);
        savedUrlMapping = urlMappingRepository.save(savedUrlMapping);

        return convertToDto(savedUrlMapping);
    }
    private UrlMappingResponse convertToDto(UrlMapping urlMapping){
        UrlMappingResponse urlMappingDTO =new UrlMappingResponse();
        urlMappingDTO.setId(urlMapping.getId());
        urlMappingDTO.setOriginalUrl(urlMapping.getOriginalUrl());
        urlMappingDTO.setShortUrl(urlMapping.getShortUrl());
        urlMappingDTO.setClickCount(urlMapping.getClickCount());
        urlMappingDTO.setCreatedDate(urlMapping.getCreateDate());
        urlMappingDTO.setUsername(urlMapping.getUser().getUsername());
        return  urlMappingDTO;
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
        List<UrlMapping> urlMappings =urlMappingRepository.findByUser(user);
        List<ClickEvent> clickEvents =clickEventRepository.findByUrlMappingInAndClickDateBetween(urlMappings,start.atStartOfDay(),end.plusDays(1).atStartOfDay());
        return clickEvents.stream()
                .collect(Collectors.groupingBy(click-> click.getClickDate().toLocalDate(),Collectors.counting()));

    }

    public String getOriginalUrl(String shortUrl, String ipAddress, String userAgent) {
        String cacheKey = CACHE_PREFIX + shortUrl;
        String originalUrl = redisTemplate.opsForValue().get(cacheKey);

        if (originalUrl == null) {
            // Cache miss — fetch from database, then populate cache
            UrlMapping urlMapping = urlMappingRepository.findByShortUrl(shortUrl)
                    .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortUrl));
            originalUrl = urlMapping.getOriginalUrl();
            redisTemplate.opsForValue().set(cacheKey, originalUrl, CACHE_TTL_HOURS, TimeUnit.HOURS);
        }

        // Publish to Kafka for async click tracking — doesn't block this response
        clickEventProducer.publish(shortUrl, ipAddress, userAgent);

        return originalUrl;
    }
}
