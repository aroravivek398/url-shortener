package com.url.shortener.kafka;

import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import java.time.LocalDateTime;

@Component
public class ClickEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public ClickEventProducer(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String shortUrl, String ipAddress, String userAgent) {
        String message = String.format(
                "{\"shortUrl\":\"%s\",\"ipAddress\":\"%s\",\"userAgent\":\"%s\",\"clickDate\":\"%s\"}",
                shortUrl, ipAddress, userAgent, LocalDateTime.now()
        );
        kafkaTemplate.send("click-events", message);
    }
}