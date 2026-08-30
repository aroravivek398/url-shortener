package com.url.shortener.exception;

public class UrlOwnershipException extends RuntimeException {
    public UrlOwnershipException(String message) {
        super(message);
    }
}