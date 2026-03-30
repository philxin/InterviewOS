package com.philxin.interviewos.controller.dto.knowledge;

import com.philxin.interviewos.entity.KnowledgeFileImport;
import java.util.UUID;

/**
 * 文件导入任务创建后的初始响应。
 */
public class KnowledgeFileImportStartResponse {
    private UUID importId;
    private UUID documentId;
    private String status;
    private String fileName;
    private long fileSize;
    private int totalChunks;
    private int embeddedChunks;
    private int failedChunks;

    public static KnowledgeFileImportStartResponse fromEntity(KnowledgeFileImport fileImport) {
        KnowledgeFileImportStartResponse response = new KnowledgeFileImportStartResponse();
        response.setImportId(fileImport.getId());
        response.setDocumentId(fileImport.getDocumentId());
        response.setStatus(fileImport.getStatus().name());
        response.setFileName(fileImport.getFileName());
        response.setFileSize(fileImport.getFileSize() == null ? 0L : fileImport.getFileSize());
        response.setTotalChunks(fileImport.getTotalChunks() == null ? 0 : fileImport.getTotalChunks());
        response.setEmbeddedChunks(fileImport.getEmbeddedChunks() == null ? 0 : fileImport.getEmbeddedChunks());
        response.setFailedChunks(fileImport.getFailedChunks() == null ? 0 : fileImport.getFailedChunks());
        return response;
    }

    public UUID getImportId() {
        return importId;
    }

    public void setImportId(UUID importId) {
        this.importId = importId;
    }

    public UUID getDocumentId() {
        return documentId;
    }

    public void setDocumentId(UUID documentId) {
        this.documentId = documentId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public int getTotalChunks() {
        return totalChunks;
    }

    public void setTotalChunks(int totalChunks) {
        this.totalChunks = totalChunks;
    }

    public int getEmbeddedChunks() {
        return embeddedChunks;
    }

    public void setEmbeddedChunks(int embeddedChunks) {
        this.embeddedChunks = embeddedChunks;
    }

    public int getFailedChunks() {
        return failedChunks;
    }

    public void setFailedChunks(int failedChunks) {
        this.failedChunks = failedChunks;
    }
}
