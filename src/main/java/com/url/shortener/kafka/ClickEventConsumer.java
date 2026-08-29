package com.url.shortener.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.url.shortener.entity.ClickEvent;
import com.url.shortener.entity.UrlMapping;
import com.url.shortener.exception.UrlNotFoundException;
import com.url.shortener.repository.ClickEventRepository;
import com.url.shortener.repository.UrlMappingRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
public class ClickEventConsumer {

    private final UrlMappingRepository urlMappingRepository;
    private final ClickEventRepository clickEventRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ClickEventConsumer(UrlMappingRepository urlMappingRepository,
                               ClickEventRepository clickEventRepository) {
        this.urlMappingRepository = urlMappingRepository;
        this.clickEventRepository = clickEventRepository;
    }

    @KafkaListener(topics = "click-events", groupId = "url-shortener-click-consumer")
    public void consume(String message) throws Exception {
        Map<String, Object> data = objectMapper.readValue(message, Map.class);

        String shortUrl = (String) data.get("shortUrl");
        String ipAddress = (String) data.get("ipAddress");
        String userAgent = (String) data.get("userAgent");

        UrlMapping urlMapping = urlMappingRepository.findByShortUrl(shortUrl)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortUrl));

        urlMapping.setClickCount(urlMapping.getClickCount() + 1);
        urlMappingRepository.save(urlMapping);

        ClickEvent clickEvent = new ClickEvent();
        clickEvent.setUrlMapping(urlMapping);
        clickEvent.setIpAddress(ipAddress);
        clickEvent.setUserAgent(userAgent);
        clickEventRepository.save(clickEvent);
    }
}