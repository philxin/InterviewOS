package com.philxin.interviewos.entity;

/**
 * 文件导入任务状态。
 */
public enum KnowledgeFileImportStatus {
    PENDING,
    PROCESSING,
    CHUNKING,
    EMBEDDING,
    SUCCESS,
    PARTIAL,
    FAILED
}
