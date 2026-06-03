package com.cloud.disk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.disk.common.result.PageResult;
import com.cloud.disk.repository.entity.FileInfo;
import com.cloud.disk.repository.entity.RecycleBin;
import com.cloud.disk.repository.mapper.FileInfoMapper;
import com.cloud.disk.repository.mapper.RecycleBinMapper;
import com.cloud.disk.service.api.IFileService;
import com.cloud.disk.service.storage.ILocalStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

@Service
public class FileServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements IFileService {

    @Autowired
    private RecycleBinMapper recycleBinMapper;

    @Autowired
    private ILocalStorageService localStorageService;

    @Override
    @Cacheable(value = "fileList", key = "#userId + '_' + #parentId")
    public PageResult<FileInfo> listByParentId(Long userId, Long parentId, int page, int pageSize) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getUserId, userId)
                .eq(parentId != null, FileInfo::getParentId, parentId)
                .eq(FileInfo::getDeleted, 0)
                .ne(FileInfo::getFileStatus, 2)   // 排除回收站中的文件
                .orderByDesc(FileInfo::getIsFolder)
                .orderByAsc(FileInfo::getFileName);

        Page<FileInfo> pageResult = this.page(new Page<>(page, pageSize), wrapper);
        PageResult<FileInfo> result = new PageResult<>(
                pageResult.getRecords(), (int) pageResult.getTotal());
        result.setPageSize(pageSize);
        result.setCurrentPage(page);
        return result;
    }

    @Override
    @Cacheable(value = "fileSearch", key = "#userId + '_' + #fileName")
    public List<FileInfo> searchByName(Long userId, String fileName) {
        return this.list(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getUserId, userId)
                .like(FileInfo::getFileName, fileName)
                .eq(FileInfo::getDeleted, 0)
                .orderByDesc(FileInfo::getCreatedAt));
    }

    @Override
    @CacheEvict(value = {"fileList", "fileSearch"}, allEntries = true)
    public boolean createFolder(Long userId, String folderName, Long parentId) {
        // 同名检查
        if (existsSameName(userId, parentId, folderName, null)) {
            return false;
        }
        FileInfo folder = new FileInfo();
        folder.setUserId(userId);
        folder.setParentId(parentId);
        folder.setFileName(folderName);
        folder.setIsFolder(1);
        folder.setFileStatus(1);
        folder.setStorageType("LOCAL");
        folder.setFileSize(0L);
        folder.setDeleted(0);
        folder.setFilePath(localStorageService.saveFolder(userId, folderName, parentId));
        return this.save(folder);
    }

    @Override
    @CacheEvict(value = {"fileList", "fileSearch"}, allEntries = true)
    public boolean rename(Long fileId, String newName) {
        FileInfo f = this.getById(fileId);
        if (f == null) return false;

        // 同名检查（排除自身）
        if (existsSameName(f.getUserId(), f.getParentId(), newName, fileId)) {
            return false;
        }

        f.setFileName(newName);
        f.setUpdatedAt(LocalDateTime.now());
        return this.updateById(f);
    }

    @Override
    public boolean move(Long fileId, Long newParentId) {
        FileInfo f = this.getById(fileId);
        if (f == null) return false;

        // 同名检查（排除自身）
        if (existsSameName(f.getUserId(), newParentId, f.getFileName(), fileId)) {
            return false;
        }

        f.setParentId(newParentId);
        f.setUpdatedAt(LocalDateTime.now());
        return this.updateById(f);
    }

    @Override
    public boolean copy(Long fileId, Long targetParentId) {
        FileInfo src = this.getById(fileId);
        if (src == null) return false;
        FileInfo target = new FileInfo();
        target.setUserId(src.getUserId());
        target.setParentId(targetParentId);
        target.setFileName(src.getFileName());
        target.setFilePath(src.getFilePath());
        target.setFileSize(src.getFileSize());
        target.setFileType(src.getFileType());
        target.setStorageType(src.getStorageType());
        target.setIsFolder(src.getIsFolder());
        target.setFileStatus(1);
        target.setVersion(1);
        target.setDeleted(0);
        return this.save(target);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"fileList", "fileSearch"}, allEntries = true)
    public boolean deleteToRecycle(Long fileId) {
        FileInfo f = this.getById(fileId);
        if (f == null) return false;

        // 更新文件状态为回收站
        f.setFileStatus(2);
        f.setUpdatedAt(LocalDateTime.now());
        this.updateById(f);

        // 写入 recycle_bin 表（必须设置 file_path/storage_type，表有 NOT NULL 约束）
        RecycleBin rb = new RecycleBin();
        rb.setUserId(f.getUserId());
        rb.setFileId(f.getId());
        rb.setFileName(f.getFileName());
        rb.setFilePath(f.getFilePath());
        rb.setFileSize(f.getFileSize());
        rb.setFileType(f.getFileType());
        rb.setStorageType(f.getStorageType());
        rb.setIsFolder(f.getIsFolder());
        rb.setDeletedAt(LocalDateTime.now());
        rb.setCreatedAt(LocalDateTime.now());
        rb.setDeleted(0);
        recycleBinMapper.insert(rb);

        return true;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean batchDeleteToRecycle(List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) return false;
        for (Long fileId : fileIds) {
            deleteToRecycle(fileId);
        }
        return true;
    }

    @Override
    public String getDownloadUrl(Long fileId) {
        FileInfo f = this.getById(fileId);
        if (f == null) return null;
        return "/api/file/download/" + fileId;
    }

    @Override
    public String getPreviewUrl(Long fileId) {
        FileInfo f = this.getById(fileId);
        if (f == null) return null;
        return "/api/file/preview/" + fileId;
    }

    @Override
    public Long getUsedSpace(Long userId) {
        return 0L;
    }

    @Override
    public Map<String, Object> getStorageStats(Long userId) {
        List<FileInfo> allFiles = this.list(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getIsFolder, 0)
                .eq(FileInfo::getDeleted, 0)
                .ne(FileInfo::getFileStatus, 2));

        long totalSpace = 10L * 1024 * 1024 * 1024;
        long usedSpace = allFiles.stream().mapToLong(f -> f.getFileSize() != null ? f.getFileSize() : 0).sum();

        Map<String, long[]> typeStats = new LinkedHashMap<>();
        typeStats.put("image", new long[]{0, 0});
        typeStats.put("doc", new long[]{0, 0});
        typeStats.put("video", new long[]{0, 0});
        typeStats.put("audio", new long[]{0, 0});
        typeStats.put("other", new long[]{0, 0});

        for (FileInfo f : allFiles) {
            String ext = f.getFileType();
            if (ext == null) ext = "";
            ext = ext.toLowerCase();
            String cat;
            if (List.of("jpg","jpeg","png","gif","webp","svg","bmp","ico").contains(ext)) cat = "image";
            else if (List.of("pdf","doc","docx","xls","xlsx","ppt","pptx","txt","md","csv","json","xml").contains(ext)) cat = "doc";
            else if (List.of("mp4","webm","ogg","ogv","mov","avi","mkv","flv","wmv","m4v").contains(ext)) cat = "video";
            else if (List.of("mp3","wav","ogg","oga","flac","aac","m4a","wma").contains(ext)) cat = "audio";
            else cat = "other";
            typeStats.get(cat)[0]++;
            typeStats.get(cat)[1] += f.getFileSize() != null ? f.getFileSize() : 0;
        }

        List<Map<String, Object>> typeList = new ArrayList<>();
        typeStats.forEach((k, v) -> typeList.add(Map.of("type", k, "count", v[0], "size", v[1])));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalSpace", totalSpace);
        result.put("usedSpace", usedSpace);
        result.put("typeStats", typeList);
        return result;
    }

    @Override
    public boolean existsSameName(Long userId, Long parentId, String fileName, Long excludeFileId) {
        LambdaQueryWrapper<FileInfo> wrapper = new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getUserId, userId)
                .eq(parentId != null, FileInfo::getParentId, parentId)
                .eq(FileInfo::getFileName, fileName)
                .eq(FileInfo::getDeleted, 0);
        if (excludeFileId != null) {
            wrapper.ne(FileInfo::getId, excludeFileId);
        }
        return this.count(wrapper) > 0;
    }

    // ==================== 文件上传（本地存储版） ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    @CacheEvict(value = {"fileList", "fileSearch"}, allEntries = true)
    public FileInfo uploadFile(Long userId, MultipartFile file, Long parentId) throws Exception {
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isEmpty()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        // 同名检查（防止同一目录下存在同名文件）
        if (existsSameName(userId, parentId, originalName, null)) {
            throw new IllegalArgumentException("目标位置已存在同名文件: " + originalName);
        }

        // 委托本地存储服务保存文件到磁盘
        String savedPath = localStorageService.saveFile(userId, file, originalName);

        // 图片文件：生成缩略图
        String thumbPath = null;
        if (isImageFile(originalName)) {
            thumbPath = generateThumbnail(savedPath);
        }

        // 写入数据库
        FileInfo fileInfo = new FileInfo();
        fileInfo.setUserId(userId);
        fileInfo.setParentId(parentId);
        fileInfo.setFileName(originalName);
        fileInfo.setFilePath(savedPath);
        fileInfo.setFileSize(file.getSize());
        fileInfo.setFileType(getFileExtension(originalName));
        fileInfo.setStorageType("LOCAL");
        fileInfo.setIsFolder(0);
        fileInfo.setFileStatus(1);
        fileInfo.setVersion(1);
        fileInfo.setDeleted(0);
        fileInfo.setThumbPath(thumbPath);
        fileInfo.setCreatedAt(LocalDateTime.now());
        fileInfo.setUpdatedAt(LocalDateTime.now());

        this.save(fileInfo);
        return fileInfo;
    }

    @Override
    public String initChunkUpload(Long userId, String fileName, Long fileSize, Integer totalChunks, Long parentId) {
        // 同名检查
        if (existsSameName(userId, parentId, fileName, null)) {
            throw new IllegalArgumentException("目标位置已存在同名文件: " + fileName);
        }

        // 生成唯一 uploadId
        String uploadId = UUID.randomUUID().toString().replace("-", "");
        // 初始化分片临时目录（含 parentId）
        localStorageService.initChunkUpload(uploadId, fileName, totalChunks, parentId);
        return uploadId;
    }

    @Override
    public void uploadChunk(String uploadId, int chunkIndex, MultipartFile chunkFile) throws Exception {
        localStorageService.saveChunk(uploadId, chunkIndex, chunkFile);
    }

    @Override
    public List<Integer> getChunkProgress(String uploadId) {
        return localStorageService.getUploadedChunks(uploadId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public FileInfo mergeChunks(Long userId, String uploadId) throws Exception {
        // 合并分片为完整文件（内部从 meta.txt 读取原始文件名）
        String savedPath = localStorageService.mergeChunks(uploadId, userId, null);

        // 从路径中提取 UUID 和扩展名构造文件名（兜底使用 uploadId 相关信息）
        java.io.File mergedFile = new java.io.File(savedPath);
        long fileSize = mergedFile.length();

        // 尝试从元数据获取原始文件名，用于 FileInfo 展示
        String displayFileName = localStorageService.getOriginalFileName(uploadId);
        if (displayFileName == null || displayFileName.isEmpty()) {
            displayFileName = mergedFile.getName();
        }

        // 清理临时目录
        localStorageService.cleanTemp(uploadId);

        // 写入数据库
        Long parentId = localStorageService.getParentId(uploadId);

        FileInfo fileInfo = new FileInfo();
        fileInfo.setUserId(userId);
        fileInfo.setParentId(parentId);
        fileInfo.setFileName(displayFileName);
        fileInfo.setFilePath(savedPath);
        fileInfo.setFileSize(fileSize);
        fileInfo.setFileType(getFileExtension(displayFileName));
        fileInfo.setStorageType("LOCAL");
        fileInfo.setIsFolder(0);
        fileInfo.setFileStatus(1);
        fileInfo.setVersion(1);
        fileInfo.setDeleted(0);
        fileInfo.setCreatedAt(LocalDateTime.now());
        fileInfo.setUpdatedAt(LocalDateTime.now());

        this.save(fileInfo);
        return fileInfo;
    }

    // ==================== 私有工具方法 ====================

    /**
     * 提取文件扩展名（不含点号）
     */
    private String getFileExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * 判断是否可生成缩略图的图片文件
     */
    private boolean isImageFile(String fileName) {
        if (fileName == null) return false;
        String ext = getFileExtension(fileName);
        return List.of("jpg", "jpeg", "png", "gif", "bmp", "webp").contains(ext);
    }

    /**
     * 为图片文件生成缩略图（最大 200x200），保存到原文件同目录。
     * @return 缩略图路径，失败返回 null
     */
    private String generateThumbnail(String originalPath) {
        try {
            File original = new File(originalPath);
            if (!original.exists()) return null;

            BufferedImage img = ImageIO.read(original);
            if (img == null) return null;

            // 缩放到 200px 宽以内，保持比例
            int thumbWidth = 200;
            int thumbHeight = thumbWidth;
            int w = img.getWidth();
            int h = img.getHeight();
            if (w > h) {
                thumbHeight = (int) ((double) h / w * thumbWidth);
            } else {
                thumbWidth = (int) ((double) w / h * thumbHeight);
            }
            if (thumbWidth < 1) thumbWidth = 1;
            if (thumbHeight < 1) thumbHeight = 1;

            // 生成缩略图
            Image scaled = img.getScaledInstance(thumbWidth, thumbHeight, Image.SCALE_SMOOTH);
            BufferedImage thumb = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = thumb.createGraphics();
            g2d.drawImage(scaled, 0, 0, null);
            g2d.dispose();

            // 保存到原文件同目录，加 _thumb 后缀
            String parent = original.getParent();
            String name = original.getName();
            int dot = name.lastIndexOf('.');
            String stem = dot > 0 ? name.substring(0, dot) : name;
            String thumbPath = parent + File.separator + stem + "_thumb.jpg";
            ImageIO.write(thumb, "jpg", new File(thumbPath));

            return thumbPath;
        } catch (Exception e) {
            // 缩略图生成失败不影响正常流程
            log.warn("缩略图生成失败: " + originalPath);
            return null;
        }
    }
}