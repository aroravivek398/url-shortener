package com.url.shortener.util;

public class DeviceClassifier {

    public static String classifyDevice(String userAgent) {
        if (userAgent == null) {
            return "Unknown";
        }

        String lowerCaseAgent = userAgent.toLowerCase();

        if (lowerCaseAgent.contains("ipad") || lowerCaseAgent.contains("tablet")) {
            return "Tablet";
        }

        if (lowerCaseAgent.contains("mobile") || lowerCaseAgent.contains("iphone") || lowerCaseAgent.contains("android")) {
            return "Mobile";
        }

        return "Desktop";
    }
}