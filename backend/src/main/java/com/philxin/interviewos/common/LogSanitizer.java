package com.philxin.interviewos.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * 统一输出日志摘要，避免把原始请求体、模型提示词或回答内容直接写入日志。
 */
public final class LogSanitizer {
    private static final int DEFAULT_PREVIEW_LENGTH = 96;

    private LogSanitizer() {
    }

    public static String summarize(String value) {
        return summarize(value, DEFAULT_PREVIEW_LENGTH);
    }

    public static String summarize(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, Math.max(0, maxLength)) + "...";
    }

    public static int length(String value) {
        return value == null ? 0 : value.length();
    }

    public static String fingerprint(String value) {
        if (value == null || value.isBlank()) {
            return "empty";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder();
            for (int index = 0; index < 6 && index < hash.length; index++) {
                builder.append(String.format("%02x", hash[index]));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException exception) {
            return Integer.toHexString(value.hashCode());
        }
    }
}
