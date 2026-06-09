package com.cloud.disk.service.storage;

import com.cloud.disk.config.StorageConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 本地存储服务实现
 * 基于本地磁盘进行文件读写，支持普通上传和分片上传（断点续传）
 */
@Service
public class LocalStorageServiceImpl implements ILocalStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageServiceImpl.class);

    @Autowired
    private StorageConfig storageConfig;

    // ---------- 子目录常量 ----------
    /** 普通文件上传子目录 */
    private static final String UPLOAD_DIR = "upload";
    /** 分片临时目录 */
    private static final String TEMP_DIR = "temp";

    @Override
    public String saveFile(Long userId, MultipartFile file, String fileName) throws Exception {
        // 构造目标路径: {rootPath}/upload/{userId}/{uuid}.{ext}
        String ext = getExtension(fileName);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String baseName = uuid;
        String relativePath = UPLOAD_DIR + File.separator + userId + File.separator + baseName + "." + ext;
        String targetPath = storageConfig.getRootPath() + File.separator + relativePath;

        // 处理同名冲突（理论上 UUID 冲突概率极低，但仍做兜底）
        targetPath = resolveDuplicate(targetPath);

        // 确保父目录存在
        File targetFile = new File(targetPath);
        ensureParentDir(targetFile);

        // 写入磁盘
        file.transferTo(targetFile);
        log.info("文件保存成功: {}", targetPath);
        return targetPath;
    }

    @Override
    public String saveFolder(Long userId, String folderName, Long parentId) {
        // 文件夹无实际磁盘文件，返回逻辑路径
        return UPLOAD_DIR + File.separator + userId + File.separator + "folder_" + UUID.randomUUID().toString().replace("-", "");
    }

    @Override
    public void initChunkUpload(String uploadId, String fileName, int totalChunks, Long parentId) {
        // 创建临时目录: {rootPath}/temp/{uploadId}
        String tempDir = storageConfig.getRootPath() + File.separator + TEMP_DIR + File.separator + uploadId;
        File dir = new File(tempDir);
        if (!dir.exists()) {
            boolean ok = dir.mkdirs();
            if (!ok) {
                throw new RuntimeException("创建分片临时目录失败: " + tempDir);
            }
        }
        // 写入元数据文件（fileName, totalChunks, parentId 供合并时使用）
        try {
            String metaPath = tempDir + File.separator + "meta.txt";
            String parentIdStr = parentId != null ? parentId.toString() : "";
            Files.write(Paths.get(metaPath),
                (fileName + "\n" + totalChunks + "\n" + parentIdStr).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("写入分片元数据失败", e);
        }
        log.info("初始化分片上传: uploadId={}, fileName={}, totalChunks={}, parentId={}", uploadId, fileName, totalChunks, parentId);
    }

    @Override
    public void saveChunk(String uploadId, int chunkIndex, MultipartFile chunkFile) throws Exception {
        // 分片路径: {rootPath}/temp/{uploadId}/chunk_{index}
        String chunkPath = storageConfig.getRootPath() + File.separator
                + TEMP_DIR + File.separator + uploadId + File.separator + "chunk_" + chunkIndex;
        File chunk = new File(chunkPath);
        // 分片已存在则跳过（幂等写入，支持重传）
        if (chunk.exists()) {
            log.info("分片已存在，跳过写入: uploadId={}, chunkIndex={}", uploadId, chunkIndex);
            return;
        }
        chunkFile.transferTo(chunk);
        log.info("分片保存成功: uploadId={}, chunkIndex={}", uploadId, chunkIndex);
    }

    @Override
    public List<Integer> getUploadedChunks(String uploadId) {
        String tempDir = storageConfig.getRootPath() + File.separator + TEMP_DIR + File.separator + uploadId;
        File dir = new File(tempDir);
        List<Integer> uploadedChunks = new ArrayList<>();
        if (!dir.exists() || !dir.isDirectory()) {
            return uploadedChunks;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return uploadedChunks;
        }
        for (File f : files) {
            String name = f.getName();
            if (name.startsWith("chunk_")) {
                try {
                    int index = Integer.parseInt(name.substring(6));
                    uploadedChunks.add(index);
                } catch (NumberFormatException e) {
                    // 忽略非分片文件
                }
            }
        }
        uploadedChunks.sort(Integer::compareTo);
        return uploadedChunks;
    }

    @Override
    public String mergeChunks(String uploadId, Long userId, String fileName) throws Exception {
        String tempDir = storageConfig.getRootPath() + File.separator + TEMP_DIR + File.separator + uploadId;
        File dir = new File(tempDir);
        if (!dir.exists()) {
            throw new RuntimeException("分片临时目录不存在: " + tempDir);
        }

        // 读取元数据文件，获取原始文件名
        String metaPath = tempDir + File.separator + "meta.txt";
        String originalFileName = fileName;
        try {
            List<String> lines = Files.readAllLines(Paths.get(metaPath), java.nio.charset.StandardCharsets.UTF_8);
            if (originalFileName == null && !lines.isEmpty()) {
                originalFileName = lines.get(0);  // 第一行是 fileName
            }
        } catch (IOException e) {
            log.warn("读取分片元数据失败，使用 uploadId 作为文件名: {}", uploadId);
        }
        if (originalFileName == null || originalFileName.isEmpty()) {
            originalFileName = uploadId;
        }

        // 确定最终文件扩展名
        String ext = getExtension(originalFileName);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String relativePath = UPLOAD_DIR + File.separator + userId + File.separator + uuid + "." + ext;
        String targetPath = storageConfig.getRootPath() + File.separator + relativePath;
        targetPath = resolveDuplicate(targetPath);

        File targetFile = new File(targetPath);
        ensureParentDir(targetFile);

        // 按索引顺序合并所有分片
        List<Integer> chunks = getUploadedChunks(uploadId);
        try (RandomAccessFile raf = new RandomAccessFile(targetFile, "rw")) {
            for (int index : chunks) {
                File chunkFile = new File(tempDir + File.separator + "chunk_" + index);
                byte[] data = Files.readAllBytes(chunkFile.toPath());
                raf.write(data);
            }
        }
        log.info("分片合并完成: uploadId={}, targetPath={}, chunks={}", uploadId, targetPath, chunks);
        return targetPath;
    }

    @Override
    public void cleanTemp(String uploadId) {
        String tempDir = storageConfig.getRootPath() + File.separator + TEMP_DIR + File.separator + uploadId;
        File dir = new File(tempDir);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    f.delete();
                }
            }
            dir.delete();
            log.info("分片临时目录已清理: uploadId={}", uploadId);
        }
    }

    @Override
    public String resolveDuplicate(String targetPath) {
        File f = new File(targetPath);
        if (!f.exists()) {
            return targetPath;
        }
        // 文件已存在：加数字后缀
        String parent = f.getParent();
        String name = f.getName();
        int dotIndex = name.lastIndexOf('.');
        String stem = dotIndex > 0 ? name.substring(0, dotIndex) : name;
        String ext = dotIndex > 0 ? name.substring(dotIndex) : "";

        int counter = 1;
        while (true) {
            String newName = stem + "(" + counter + ")" + ext;
            File candidate = new File(parent + File.separator + newName);
            if (!candidate.exists()) {
                return candidate.getAbsolutePath();
            }
            counter++;
        }
    }

    @Override
    public String buildAccessUrl(Long fileId, String filePath) {
        if (filePath != null && !filePath.isEmpty()) {
            return "/api/file/download/" + fileId;
        }
        return null;
    }

    @Override
    public String getOriginalFileName(String uploadId) {
        String metaPath = storageConfig.getRootPath() + File.separator
                + TEMP_DIR + File.separator + uploadId + File.separator + "meta.txt";
        File metaFile = new File(metaPath);
        if (!metaFile.exists()) {
            return null;
        }
        try {
            List<String> lines = Files.readAllLines(metaFile.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            if (!lines.isEmpty()) {
                return lines.get(0);  // 第一行是 fileName
            }
        } catch (IOException e) {
            log.warn("读取分片元数据失败: {}", uploadId);
        }
        return null;
    }

    @Override
    public Long getParentId(String uploadId) {
        String metaPath = storageConfig.getRootPath() + File.separator
                + TEMP_DIR + File.separator + uploadId + File.separator + "meta.txt";
        File metaFile = new File(metaPath);
        if (!metaFile.exists()) {
            return null;
        }
        try {
            List<String> lines = Files.readAllLines(metaFile.toPath(), java.nio.charset.StandardCharsets.UTF_8);
            if (lines.size() >= 3 && !lines.get(2).isEmpty()) {
                return Long.parseLong(lines.get(2));
            }
        } catch (IOException | NumberFormatException e) {
            log.warn("读取 parentId 元数据失败: {}", uploadId);
        }
        return null;
    }

    // ========== 私有辅助方法 ==========

    /**
     * 提取文件扩展名（不含点号），如 "pdf"
     */
    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * 确保文件的父目录已创建
     */
    private void ensureParentDir(File file) {
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            boolean ok = parent.mkdirs();
            if (!ok) {
                throw new RuntimeException("创建目录失败: " + parent.getAbsolutePath());
            }
        }
    }
}