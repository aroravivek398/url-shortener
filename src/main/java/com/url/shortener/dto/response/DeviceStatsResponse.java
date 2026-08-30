package com.url.shortener.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeviceStatsResponse {
    private long mobileClicks;
    private long desktopClicks;
    private long tabletClicks;
    private long unknownClicks;
}