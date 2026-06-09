package com.cloud.disk.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 分片合并请求 DTO
 */
@Schema(description = "分片合并请求")
public class ChunkMergeRequest {

    @NotBlank(message = "uploadId 不能为空")
    @Schema(description = "上传任务 ID", example = "abc123-def456")
    private String uploadId;

    public String getUploadId() { return uploadId; }
    public void setUploadId(String uploadId) { this.uploadId = uploadId; }
}