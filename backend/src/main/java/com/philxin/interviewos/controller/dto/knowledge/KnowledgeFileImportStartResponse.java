package com.philxin.interviewos.controller.dto.knowledge;

import com.philxin.interviewos.entity.KnowledgeFileImport;
import java.util.UUID;

/**
 * 文件导入任务创建后的初始响应。
 */
public class KnowledgeFileImportStartResponse {
    private UUID importId;
    private String status;

    public static KnowledgeFileImportStartResponse fromEntity(KnowledgeFileImport fileImport) {
        KnowledgeFileImportStartResponse response = new KnowledgeFileImportStartResponse();
        response.setImportId(fileImport.getId());
        response.setStatus(fileImport.getStatus().name());
        return response;
    }

    public UUID getImportId() {
        return importId;
    }

    public void setImportId(UUID importId) {
        this.importId = importId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
