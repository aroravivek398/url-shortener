package com.url.shortener.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateUrlRequest {

    @NotBlank
    @Pattern(regexp = "^(https?://).+", message = "URL must start with http:// or https://")
    private String originalUrl;

    @Min(value = 1, message = "Expiry must be at least 1 day")
    @Max(value = 365, message = "Expiry cannot exceed 365 days")
    private Integer expiryDays; // optional — null means "never expires"

    private String customAlias; // optional — null/blank means "generate one for me"
}