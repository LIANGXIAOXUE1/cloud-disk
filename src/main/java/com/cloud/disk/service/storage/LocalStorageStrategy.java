package com.cloud.disk.service.storage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.UUID;

/**
 * 本地磁盘存储策略 — 文件直接保存在服务器硬盘上。
 * 配置示例：{ "rootPath": "D:\\cloud-disk-files", "name": "本地磁盘" }
 */
@Component
public class LocalStorageStrategy implements StorageStrategy {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageStrategy.class);

    @Override
    public String getType() {
        return "LOCAL";
    }

    @Override
    public String saveFile(Long userId, MultipartFile file, String fileName, String relativeDir) throws Exception {
        String rootPath = getDefaultRootPath();
        String ext = getExtension(fileName);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String relative = relativeDir + File.separator + uuid + "." + ext;
        String target = rootPath + File.separator + relative;

        File targetFile = new File(target);
        ensureDir(targetFile.getParentFile());
        file.transferTo(targetFile);

        log.info("本地文件保存成功: {}", target);
        return target;
    }

    @Override
    public InputStream readFile(String storagePath) throws Exception {
        Path path = Paths.get(storagePath);
        if (!Files.exists(path)) {
            throw new FileNotFoundException("文件不存在: " + storagePath);
        }
        return Files.newInputStream(path);
    }

    @Override
    public void deleteFile(String storagePath) throws Exception {
        File f = new File(storagePath);
        if (f.exists()) {
            if (!f.delete()) throw new IOException("删除失败: " + storagePath);
        }
    }

    @Override
    public boolean healthCheck(Map<String, String> config) {
        String rootPath = config.getOrDefault("rootPath", getDefaultRootPath());
        File dir = new File(rootPath);
        if (!dir.exists()) {
            if (!dir.mkdirs()) return false;
        }
        return dir.isDirectory() && dir.canWrite();
    }

    @Override
    public String getRootInfo(Map<String, String> config) {
        return config.getOrDefault("rootPath", getDefaultRootPath());
    }

    private String getDefaultRootPath() {
        return "D:\\cloud-disk-files";
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }

    private void ensureDir(File dir) {
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new RuntimeException("创建目录失败: " + dir.getAbsolutePath());
            }
        }
    }
}
