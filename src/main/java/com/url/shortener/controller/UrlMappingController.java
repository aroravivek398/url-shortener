package com.url.shortener.controller;

import com.url.shortener.dto.ClickEventDTO;
import com.url.shortener.dto.request.CreateUrlRequest;
import com.url.shortener.dto.response.DeviceStatsResponse;
import com.url.shortener.dto.response.UrlMappingResponse;
import com.url.shortener.entity.User;
import com.url.shortener.util.QrCodeGenerator;
import org.springframework.beans.factory.annotation.Value;
import com.url.shortener.exception.UrlNotFoundException;
import com.url.shortener.repository.UrlMappingRepository;
import com.url.shortener.repository.UserRepository;
import com.url.shortener.service.UrlMappingService;
import com.url.shortener.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/urls")
@RequiredArgsConstructor
public class UrlMappingController {
    private final UrlMappingService urlMappingService;
    private final UserService userService;
    private final UrlMappingRepository urlMappingRepository;


    @PostMapping("/shorten")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UrlMappingResponse> createShortUrl(@Valid @RequestBody CreateUrlRequest request,
                                                             Principal principal) {
        User user = userService.findByUsername(principal.getName());
        UrlMappingResponse urlMappingResponse = urlMappingService.createShortUrl(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(urlMappingResponse);
    }

    @GetMapping("/myurls")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<UrlMappingResponse>> getUserUrls(Principal principal) {
        User user = userService.findByUsername(principal.getName());
        List<UrlMappingResponse> urls = urlMappingService.getUrlsByUser(user);
        return ResponseEntity.ok(urls);
    }

    @GetMapping("/analytics/{shortUrl}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<List<ClickEventDTO>> getUrlAnalytics(@PathVariable String shortUrl,
                                                               @RequestParam("startDate") String startDate,
                                                               @RequestParam("endDate") String endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        LocalDateTime start = LocalDateTime.parse(startDate, formatter);
        LocalDateTime end = LocalDateTime.parse(endDate, formatter);
        List<ClickEventDTO> clickEventDTOS = urlMappingService.getClickEventsByDate(shortUrl, start, end);
        return ResponseEntity.ok(clickEventDTOS);
    }

    @GetMapping("/totalClicks")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Map<LocalDate, Long>> getTotalClicksByDate(Principal principal,
                                                                     @RequestParam("startDate") String startDate,
                                                                     @RequestParam("endDate") String endDate) {
        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE;
        User user = userService.findByUsername(principal.getName());
        LocalDate start = LocalDate.parse(startDate, formatter);
        LocalDate end = LocalDate.parse(endDate, formatter);
        Map<LocalDate, Long> totalClicksByUserAndDate = urlMappingService.getTotalClicksByUserAndDate(user, start, end);
        return ResponseEntity.ok(totalClicksByUserAndDate);
    }

    @Value("${app.base-url}")
    private String baseUrl;

    @GetMapping("/{shortUrl}/qrcode")
    public ResponseEntity<byte[]> getQrCode(@PathVariable String shortUrl) {
        urlMappingRepository.findByShortUrl(shortUrl)
                .orElseThrow(() -> new UrlNotFoundException("Short URL not found: " + shortUrl));

        String fullShortUrl = baseUrl + "/" + shortUrl;

        try {
            byte[] qrCodeImage = QrCodeGenerator.generate(fullShortUrl);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(qrCodeImage);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code", e);
        }
    }
    @GetMapping("/analytics/{shortUrl}/devices")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<DeviceStatsResponse> getDeviceStats(@PathVariable String shortUrl) {
        DeviceStatsResponse stats = urlMappingService.getDeviceStats(shortUrl);
        return ResponseEntity.ok(stats);
    }

    @DeleteMapping("/{shortUrl}")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<Void> deleteUrl(@PathVariable String shortUrl, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        urlMappingService.deleteUrl(shortUrl, user);
        return ResponseEntity.noContent().build();
    }

}