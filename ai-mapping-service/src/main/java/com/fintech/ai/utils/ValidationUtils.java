package com.fintech.ai.utils;

public class ValidationUtils {
    public static boolean validateAdminKey(String apiKey, String adminApiKey) {
        return adminApiKey.equals(apiKey);
    }
}
