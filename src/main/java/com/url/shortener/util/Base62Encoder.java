package com.url.shortener.util;

public class Base62Encoder {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = 62;

    public static String encode(long id) {
        if (id == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }

        StringBuilder result = new StringBuilder();
        while (id > 0) {
            int remainder = (int) (id % BASE);
            result.append(ALPHABET.charAt(remainder));
            id = id / BASE;
        }

        return result.reverse().toString();
    }
}