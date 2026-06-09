package com.cloud.disk.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 分片上传进度响应 DTO
 */
@Schema(description = "分片上传进度")
public class ChunkProgressResponse {

    @Schema(description = "上传任务 ID")
    private String uploadId;

    @Schema(description = "已完成的分片索引列表（升序）", example = "[0,1,2,3,5]")
    private java.util.List<Integer> completedChunks;

    public ChunkProgressResponse() {}

    public ChunkProgressResponse(String uploadId, java.util.List<Integer> completedChunks) {
        this.uploadId = uploadId;
        this.completedChunks = completedChunks;
    }

    public String getUploadId() { return uploadId; }
    public void setUploadId(String uploadId) { this.uploadId = uploadId; }
    public java.util.List<Integer> getCompletedChunks() { return completedChunks; }
    public void setCompletedChunks(java.util.List<Integer> completedChunks) { this.completedChunks = completedChunks; }
}