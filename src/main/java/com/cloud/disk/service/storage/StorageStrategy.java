package com.cloud.disk.service.storage;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;
import java.util.Map;

public interface StorageStrategy {

    String getType();

    String saveFile(Long userId, MultipartFile file, String fileName, String relativeDir) throws Exception;

    InputStream readFile(String storagePath) throws Exception;

    void deleteFile(String storagePath) throws Exception;

    boolean healthCheck(Map<String, String> config);

    String getRootInfo(Map<String, String> config);
}