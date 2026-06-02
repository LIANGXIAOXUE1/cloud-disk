package com.cloud.disk.controller;

import com.cloud.disk.common.annotation.CheckFileOwner;
import com.cloud.disk.common.context.UserContext;
import com.cloud.disk.common.dto.ChunkInitRequest;
import com.cloud.disk.common.dto.ChunkMergeRequest;
import com.cloud.disk.common.dto.ChunkProgressResponse;
import com.cloud.disk.common.result.PageResult;
import com.cloud.disk.common.result.Result;
import com.cloud.disk.repository.entity.FileInfo;
import com.cloud.disk.service.api.IFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Tag(name = "文件管理", description = "文件/文件夹 CRUD、搜索、预览")
@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private IFileService fileService;

    @Operation(summary = "文件列表", description = "按父目录分页获取当前用户的文件列表")
    @GetMapping("/list")
    public Result<PageResult<FileInfo>> list(
            @Parameter(description = "父目录 ID，不传则查询根目录") @RequestParam(required = false) Long parentId,
            @Parameter(description = "页码（从 1 开始）") @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "每页条数") @RequestParam(defaultValue = "20") int pageSize) {
        Long userId = UserContext.getCurrentUserId();
        PageResult<FileInfo> result = fileService.listByParentId(userId, parentId, page, pageSize);
        return Result.success(result);
    }

    @Operation(summary = "搜索文件", description = "按文件名模糊搜索当前用户的文件")
    @GetMapping("/search")
    public Result<List<FileInfo>> search(
            @Parameter(description = "文件名关键词") @RequestParam String fileName) {
        Long userId = UserContext.getCurrentUserId();
        List<FileInfo> result = fileService.searchByName(userId, fileName);
        return Result.success(result);
    }

    @Operation(summary = "创建文件夹", description = "在指定目录下创建新文件夹，自动检查同名冲突")
    @PostMapping("/createFolder")
    public Result<Map<String, Object>> createFolder(
            @Parameter(description = "文件夹名称") @RequestParam String folderName,
            @Parameter(description = "父目录 ID，不传则在根目录创建") @RequestParam(required = false) Long parentId) {
        Long userId = UserContext.getCurrentUserId();
        boolean ok = fileService.createFolder(userId, folderName, parentId);
        if (!ok) {
            return Result.error(409, "目标位置已存在同名文件夹");
        }
        return Result.success(Map.of("result", "OK"));
    }

    @Operation(summary = "重命名", description = "重命名文件或文件夹，自动检查同名冲突")
    @CheckFileOwner
    @PostMapping("/rename")
    public Result<Map<String, Object>> rename(
            @Parameter(description = "文件 ID") @RequestParam Long fileId,
            @Parameter(description = "新名称") @RequestParam String newName) {
        boolean ok = fileService.rename(fileId, newName);
        if (!ok) {
            return Result.error(409, "重命名失败：文件不存在或目标名称已存在");
        }
        return Result.success(Map.of("result", "OK"));
    }

    @Operation(summary = "移动文件", description = "将文件/文件夹移动到新父目录，自动检查同名冲突")
    @CheckFileOwner
    @PostMapping("/move")
    public Result<Map<String, Object>> move(
            @Parameter(description = "文件 ID") @RequestParam Long fileId,
            @Parameter(description = "新父目录 ID") @RequestParam Long newParentId) {
        boolean ok = fileService.move(fileId, newParentId);
        if (!ok) {
            return Result.error(409, "移动失败：文件不存在或目标位置存在同名文件");
        }
        return Result.success(Map.of("result", "OK"));
    }

    @Operation(summary = "复制文件", description = "将文件复制到目标目录")
    @CheckFileOwner
    @PostMapping("/copy")
    public Result<Map<String, Object>> copy(
            @Parameter(description = "源文件 ID") @RequestParam Long fileId,
            @Parameter(description = "目标父目录 ID") @RequestParam Long targetParentId) {
        boolean ok = fileService.copy(fileId, targetParentId);
        if (!ok) {
            return Result.error(400, "复制失败：源文件不存在");
        }
        return Result.success(Map.of("result", "OK"));
    }

    @Operation(summary = "删除文件", description = "将文件移入回收站（同时写入 recycle_bin 表）")
    @CheckFileOwner
    @PostMapping("/delete")
    public Result<Map<String, Object>> delete(
            @Parameter(description = "文件 ID") @RequestParam Long fileId) {
        boolean ok = fileService.deleteToRecycle(fileId);
        if (!ok) {
            return Result.error(400, "删除失败：文件不存在");
        }
        return Result.success(Map.of("result", "OK"));
    }

    @Operation(summary = "批量删除", description = "批量将文件移入回收站")
    @CheckFileOwner(paramName = "fileIds")
    @PostMapping("/batchDelete")
    public Result<Map<String, Object>> batchDelete(
            @Parameter(description = "文件 ID 列表") @RequestBody List<Long> fileIds) {
        boolean ok = fileService.batchDeleteToRecycle(fileIds);
        if (!ok) {
            return Result.error(400, "批量删除失败");
        }
        return Result.success(Map.of("result", "OK"));
    }

    @Operation(summary = "获取下载 URL", description = "获取文件的下载地址")
    @CheckFileOwner
    @GetMapping("/downloadUrl")
    public Result<String> getDownloadUrl(
            @Parameter(description = "文件 ID") @RequestParam Long fileId) {
        String url = fileService.getDownloadUrl(fileId);
        return Result.success(url);
    }

    @Operation(summary = "获取预览 URL", description = "获取文件的在线预览地址")
    @CheckFileOwner
    @GetMapping("/previewUrl")
    public Result<String> getPreviewUrl(
            @Parameter(description = "文件 ID") @RequestParam Long fileId) {
        String url = fileService.getPreviewUrl(fileId);
        return Result.success(url);
    }

    @Operation(summary = "获取已用空间", description = "查询当前用户已使用的存储空间（字节）")
    @GetMapping("/usedSpace")
    public Result<Long> getUsedSpace() {
        Long userId = UserContext.getCurrentUserId();
        Long space = fileService.getUsedSpace(userId);
        return Result.success(space);
    }

    @Operation(summary = "流式输出文件", description = "根据文件 ID 将文件内容以流方式输出，用于图片/视频/音频/PDF在线预览")
    @GetMapping("/stream/{fileId}")
    public void stream(
            @Parameter(description = "文件 ID") @PathVariable Long fileId,
            HttpServletResponse response) throws IOException {
        FileInfo fileInfo = fileService.getById(fileId);
        if (fileInfo == null || fileInfo.getIsFolder() == 1) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "文件不存在");
            return;
        }

        Path filePath = Paths.get(fileInfo.getFilePath());
        if (!Files.exists(filePath)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND, "文件在磁盘上不存在");
            return;
        }

        String contentType = getContentType(fileInfo.getFileType());
        response.setContentType(contentType);
        response.setContentLengthLong(fileInfo.getFileSize());
        // 中文文件名需 URL 编码，否则 Tomcat 会拒绝整个响应头
        String encodedName = java.net.URLEncoder.encode(fileInfo.getFileName(), "UTF-8").replace("+", "%20");
        response.setHeader("Content-Disposition", "inline; filename*=UTF-8''" + encodedName);

        try (OutputStream out = response.getOutputStream()) {
            Files.copy(filePath, out);
        }
    }

    /**
     * 根据文件扩展名返回 MIME 类型
     */
    private String getContentType(String ext) {
        if (ext == null) return "application/octet-stream";
        return switch (ext.toLowerCase()) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "svg" -> "image/svg+xml";
            case "bmp" -> "image/bmp";
            case "ico" -> "image/x-icon";
            case "mp4" -> "video/mp4";
            case "webm" -> "video/webm";
            case "mp3" -> "audio/mpeg";
            case "wav" -> "audio/wav";
            case "ogg" -> "audio/ogg";
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain";
            case "html", "htm" -> "text/html";
            case "css" -> "text/css";
            case "js" -> "application/javascript";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "zip" -> "application/zip";
            default -> "application/octet-stream";
        };
    }

    // ==================== 文件上传（本地存储版） ====================

    @Operation(summary = "普通小文件上传", description = "接收 multipart/form-data，将文件保存到本地磁盘并写入数据库")
    @PostMapping("/upload")
    public Result<FileInfo> upload(
            @Parameter(description = "上传的文件") @RequestParam("file") MultipartFile file,
            @Parameter(description = "目标父目录 ID，不传则上传到根目录") @RequestParam(required = false) Long parentId) {
        Long userId = UserContext.getCurrentUserId();
        try {
            FileInfo fileInfo = fileService.uploadFile(userId, file, parentId);
            return Result.success(fileInfo);
        } catch (Exception e) {
            return Result.error(400, "上传失败: " + e.getMessage());
        }
    }

    @Operation(summary = "初始化分片上传", description = "创建分片上传任务，返回 uploadId 用于后续分片上传和合并")
    @PostMapping("/chunk/init")
    public Result<Map<String, String>> initChunk(
            @Parameter(description = "分片上传初始化参数") @Valid @RequestBody ChunkInitRequest request) {
        Long userId = UserContext.getCurrentUserId();
        try {
            String uploadId = fileService.initChunkUpload(
                    userId, request.getFileName(), request.getFileSize(),
                    request.getTotalChunks(), request.getParentId());
            return Result.success(Map.of("uploadId", uploadId));
        } catch (Exception e) {
            return Result.error(400, "初始化失败: " + e.getMessage());
        }
    }

    @Operation(summary = "分片上传", description = "上传单个分片，支持断点续传（分片已存在则跳过）")
    @PostMapping("/chunk/upload")
    public Result<Map<String, Object>> uploadChunk(
            @Parameter(description = "上传任务 ID（由 chunk/init 返回）") @RequestParam String uploadId,
            @Parameter(description = "分片索引（从 0 开始）") @RequestParam int chunkIndex,
            @Parameter(description = "分片文件") @RequestParam("file") MultipartFile file) {
        try {
            fileService.uploadChunk(uploadId, chunkIndex, file);
            return Result.success(Map.of("result", "OK", "chunkIndex", chunkIndex));
        } catch (Exception e) {
            return Result.error(400, "分片上传失败: " + e.getMessage());
        }
    }

    @Operation(summary = "查询上传进度", description = "根据 uploadId 查询已完成的分片索引列表，用于断点续传")
    @GetMapping("/chunk/progress")
    public Result<ChunkProgressResponse> chunkProgress(
            @Parameter(description = "上传任务 ID") @RequestParam String uploadId) {
        List<Integer> completed = fileService.getChunkProgress(uploadId);
        ChunkProgressResponse resp = new ChunkProgressResponse(uploadId, completed);
        return Result.success(resp);
    }

    @Operation(summary = "合并分片", description = "将所有分片合并为完整文件，写入 file_info 表并清理临时分片")
    @PostMapping("/chunk/merge")
    public Result<FileInfo> mergeChunk(
            @Parameter(description = "合并请求（uploadId）") @Valid @RequestBody ChunkMergeRequest request) {
        Long userId = UserContext.getCurrentUserId();
        try {
            FileInfo fileInfo = fileService.mergeChunks(userId, request.getUploadId());
            return Result.success(fileInfo);
        } catch (Exception e) {
            return Result.error(400, "合并失败: " + e.getMessage());
        }
    }
}