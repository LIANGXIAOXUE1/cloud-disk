package com.cloud.disk.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 分片上传初始化请求 DTO
 */
@Schema(description = "分片上传初始化请求")
public class ChunkInitRequest {

    @NotBlank(message = "文件名不能为空")
    @Schema(description = "原始文件名（含扩展名）", example = "大型视频.mp4")
    private String fileName;

    @NotNull(message = "文件大小不能为空")
    @Schema(description = "文件总大小（字节）", example = "1073741824")
    private Long fileSize;

    @NotNull(message = "总分片数不能为空")
    @Schema(description = "总分片数", example = "20")
    private Integer totalChunks;

    @Schema(description = "父目录 ID，不传则上传到根目录")
    private Long parentId;

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public Integer getTotalChunks() { return totalChunks; }
    public void setTotalChunks(Integer totalChunks) { this.totalChunks = totalChunks; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
}