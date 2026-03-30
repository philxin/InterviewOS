package com.philxin.interviewos.common;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 基础 prompt injection 防护：对外部引用文本做最小过滤，避免把指令型内容直接送入 prompt。
 */
public final class PromptInjectionGuard {
    private static final String FILTERED_LINE_PLACEHOLDER = "[潜在提示注入内容已过滤]";
    private static final List<Pattern> INJECTION_PATTERNS = List.of(
        Pattern.compile("(?i)ignore\\s+(all\\s+)?previous\\s+instructions?"),
        Pattern.compile("(?i)disregard\\s+(all\\s+)?previous\\s+instructions?"),
        Pattern.compile("(?i)follow\\s+these\\s+instructions\\s+instead"),
        Pattern.compile("(?i)system\\s+prompt"),
        Pattern.compile("(?i)developer\\s+message"),
        Pattern.compile("(?i)^\\s*(system|assistant|developer)\\s*:"),
        Pattern.compile("(?i)<\\|im_start\\|>|<\\|im_end\\|>"),
        Pattern.compile("(?i)jailbreak|do\\s+anything\\s+now|bypass\\s+safety"),
        Pattern.compile("(?i)tool\\s+call|function\\s+call")
    );

    private PromptInjectionGuard() {
    }

    public static String sanitizeReferenceText(String rawText, int maxLength) {
        if (rawText == null || rawText.isBlank()) {
            return "";
        }
        String normalized = rawText
            .replace("\r\n", "\n")
            .replace("\r", "\n")
            .replaceAll("[\\p{Cntrl}&&[^\n\t]]", " ")
            .trim();
        if (normalized.isEmpty()) {
            return "";
        }

        StringBuilder builder = new StringBuilder();
        String[] lines = normalized.split("\n");
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            if (isInjectionLike(trimmed)) {
                appendLine(builder, FILTERED_LINE_PLACEHOLDER);
                continue;
            }
            appendLine(builder, trimmed);
        }

        String safeText = builder.toString().trim();
        if (safeText.isEmpty()) {
            safeText = FILTERED_LINE_PLACEHOLDER;
        }
        if (safeText.length() <= maxLength) {
            return safeText;
        }
        return safeText.substring(0, Math.max(0, maxLength));
    }

    private static boolean isInjectionLike(String line) {
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(line).find()) {
                return true;
            }
        }
        return false;
    }

    private static void appendLine(StringBuilder builder, String line) {
        if (!builder.isEmpty()) {
            builder.append('\n');
        }
        builder.append(line);
    }
}
