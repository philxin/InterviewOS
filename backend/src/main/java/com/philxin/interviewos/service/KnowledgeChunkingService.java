package com.philxin.interviewos.service;

import com.philxin.interviewos.common.BusinessException;
import com.philxin.interviewos.config.RagProperties;
import java.util.ArrayList;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

/**
 * 基于字符窗口的最小可用切片服务。
 */
@Service
public class KnowledgeChunkingService {
    private final RagProperties ragProperties;

    public KnowledgeChunkingService(RagProperties ragProperties) {
        this.ragProperties = ragProperties;
    }

    public List<ChunkDraft> split(String rawText) {
        String text = normalize(rawText);
        if (text.isBlank()) {
            return List.of();
        }
        int chunkSize = positive(ragProperties.getChunkSize(), 1200);
        int overlap = Math.max(0, ragProperties.getChunkOverlap() == null ? 200 : ragProperties.getChunkOverlap());
        if (overlap >= chunkSize) {
            overlap = Math.max(0, chunkSize / 4);
        }
        int minChunkLength = positive(ragProperties.getMinChunkLength(), 80);
        int maxChunks = positive(ragProperties.getMaxChunksPerDocument(), 128);
        List<ChunkDraft> chunks = new ArrayList<>();
        int start = 0;
        int textLength = text.length();
        while (start < textLength) {
            if (chunks.size() >= maxChunks) {
                throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "Document exceeds max chunk limit");
            }
            int end = Math.min(start + chunkSize, textLength);
            if (end < textLength) {
                int lastBreak = findBreakPosition(text, start, end);
                if (lastBreak > start + minChunkLength) {
                    end = lastBreak;
                }
            }
            String chunkText = text.substring(start, end).trim();
            if (!chunkText.isEmpty()) {
                chunks.add(new ChunkDraft(chunks.size(), chunkText, start, end, estimateTokenCount(chunkText)));
            }
            if (end >= textLength) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
        return chunks;
    }

    public String getChunkStrategy() {
        return "char-window-overlap-v1";
    }

    public String normalize(String rawText) {
        if (rawText == null) {
            return "";
        }
        String text = rawText.replace("\r\n", "\n").replace('\r', '\n');
        text = text.replaceAll("[\\u0000-\\u0008\\u000B\\u000C\\u000E-\\u001F]", "");
        text = text.replaceAll("[ \\t]+", " ");
        text = text.replaceAll("\\n{3,}", "\n\n");
        return text.trim();
    }

    private int findBreakPosition(String text, int start, int end) {
        int paragraphBreak = text.lastIndexOf("\n\n", end - 1);
        if (paragraphBreak >= start) {
            return paragraphBreak;
        }
        int lineBreak = text.lastIndexOf('\n', end - 1);
        if (lineBreak >= start) {
            return lineBreak;
        }
        int sentenceBreak = Math.max(text.lastIndexOf('。', end - 1), text.lastIndexOf('.', end - 1));
        if (sentenceBreak >= start) {
            return sentenceBreak + 1;
        }
        int whitespace = text.lastIndexOf(' ', end - 1);
        return whitespace >= start ? whitespace : end;
    }

    private int estimateTokenCount(String chunkText) {
        return Math.max(1, chunkText.length() / 4);
    }

    private int positive(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    /**
     * 切片草稿，供 processor 落库。
     */
    public record ChunkDraft(int chunkIndex, String text, int startOffset, int endOffset, int tokenCount) {
        public int charCount() {
            return text == null ? 0 : text.length();
        }
    }
}
