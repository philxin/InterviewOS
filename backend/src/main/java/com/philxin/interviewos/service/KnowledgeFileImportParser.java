package com.philxin.interviewos.service;

import com.philxin.interviewos.common.BusinessException;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * 解析文件正文，当前支持 txt / md / pdf。
 */
@Component
public class KnowledgeFileImportParser {
    public static final long MAX_FILE_SIZE_BYTES = 5L * 1024L * 1024L;

    /**
     * 解析上传文件正文。
     */
    public String extractText(String fileName, byte[] content) {
        String extension = resolveExtension(fileName);
        return switch (extension) {
            case "txt", "md" -> new String(content, StandardCharsets.UTF_8).trim();
            case "pdf" -> extractPdfText(content);
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "Unsupported file type: " + extension);
        };
    }

    /**
     * 归一化文件名并生成知识点标题。
     */
    public String resolveTitle(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return "Imported Knowledge";
        }
        String normalized = fileName.trim();
        int slashIndex = Math.max(normalized.lastIndexOf('/'), normalized.lastIndexOf('\\'));
        if (slashIndex >= 0 && slashIndex + 1 < normalized.length()) {
            normalized = normalized.substring(slashIndex + 1);
        }
        int dotIndex = normalized.lastIndexOf('.');
        if (dotIndex > 0) {
            normalized = normalized.substring(0, dotIndex);
        }
        normalized = normalized.trim();
        if (normalized.isBlank()) {
            return "Imported Knowledge";
        }
        return normalized.length() <= 200 ? normalized : normalized.substring(0, 200);
    }

    public String resolveExtension(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "File name must not be blank");
        }
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == fileName.length() - 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Unsupported file type");
        }
        return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    public boolean isSupportedExtension(String extension) {
        return "txt".equals(extension) || "md".equals(extension) || "pdf".equals(extension);
    }

    private String extractPdfText(byte[] content) {
        try (
            ByteArrayInputStream inputStream = new ByteArrayInputStream(content);
            PdfReader reader = new PdfReader(inputStream);
            PdfDocument document = new PdfDocument(reader)
        ) {
            StringBuilder builder = new StringBuilder();
            for (int pageNo = 1; pageNo <= document.getNumberOfPages(); pageNo++) {
                if (pageNo > 1) {
                    builder.append('\n');
                }
                builder.append(PdfTextExtractor.getTextFromPage(document.getPage(pageNo)));
            }
            return builder.toString().trim();
        } catch (IOException exception) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Failed to parse PDF file");
        }
    }
}
