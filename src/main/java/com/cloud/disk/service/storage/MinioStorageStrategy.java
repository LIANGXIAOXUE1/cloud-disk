package com.cloud.disk.service.storage;

import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 对象存储策略。
 * 配置示例：{ "endpoint": "http://localhost:9000", "accessKey": "admin",
 *              "secretKey": "admin123", "bucket": "cloud-disk" }
 */
@Component
public class MinioStorageStrategy implements StorageStrategy {

    private static final Logger log = LoggerFactory.getLogger(MinioStorageStrategy.class);

    @Override
    public String getType() {
        return "MINIO";
    }

    @Override
    public String saveFile(Long userId, MultipartFile file, String fileName, String relativeDir) throws Exception {
        MinioClient client = null;
        try {
            client = buildClient();
            String bucket = getDefaultBucket();
            ensureBucket(client, bucket);

            String ext = getExtension(fileName);
            String uuid = UUID.randomUUID().toString().replace("-", "");
            String objectName = "upload/" + relativeDir + "/" + uuid + "." + ext;

            // 保存为临时文件再上传（MinIO SDK 需要已知大小）
            File tmpFile = File.createTempFile("minio-upload-", ".tmp");
            file.transferTo(tmpFile);

            client.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .filename(tmpFile.getAbsolutePath())
                            .contentType(file.getContentType())
                            .build()
            );
            tmpFile.delete();
            log.info("MinIO 文件上传成功: {}/{}", bucket, objectName);
            return bucket + "/" + objectName;
        } finally {
            closeClient(client);
        }
    }

    @Override
    public InputStream readFile(String storagePath) throws Exception {
        MinioClient client = null;
        try {
            client = buildClient();
            String bucket = getDefaultBucket();
            // storagePath 格式: bucket/upload/... 或直接是 objectName
            String objectName = storagePath;
            if (objectName.contains("/")) {
                int idx = objectName.indexOf('/');
                bucket = objectName.substring(0, idx);
                objectName = objectName.substring(idx + 1);
            }

            return client.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } finally {
            // Don't close client here - the stream needs it open
            // Client will be closed when stream is consumed
        }
    }

    @Override
    public void deleteFile(String storagePath) throws Exception {
        MinioClient client = null;
        try {
            client = buildClient();
            String bucket = getDefaultBucket();
            String objectName = storagePath;
            if (objectName.contains("/")) {
                int idx = objectName.indexOf('/');
                bucket = objectName.substring(0, idx);
                objectName = objectName.substring(idx + 1);
            }

            client.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectName)
                            .build()
            );
        } finally {
            closeClient(client);
        }
    }

    @Override
    public boolean healthCheck(Map<String, String> config) {
        MinioClient client = null;
        try {
            client = buildClientWithConfig(config);
            client.listBuckets();
            return true;
        } catch (Exception e) {
            log.warn("MinIO 连接测试失败: {}", e.getMessage());
            return false;
        } finally {
            closeClient(client);
        }
    }

    @Override
    public String getRootInfo(Map<String, String> config) {
        String endpoint = config.getOrDefault("endpoint", "http://localhost:9000");
        String bucket = config.getOrDefault("bucket", "cloud-disk");
        return endpoint + "/" + bucket;
    }

    // ====== Helpers ======

    private MinioClient buildClient() {
        return MinioClient.builder()
                .endpoint(getConfig("endpoint", "http://localhost:9000"))
                .credentials(getConfig("accessKey", "admin"), getConfig("secretKey", "admin123"))
                .build();
    }

    private MinioClient buildClientWithConfig(Map<String, String> config) {
        return MinioClient.builder()
                .endpoint(config.getOrDefault("endpoint", "http://localhost:9000"))
                .credentials(config.getOrDefault("accessKey", "admin"),
                             config.getOrDefault("secretKey", "admin123"))
                .build();
    }

    private String getDefaultBucket() {
        return getConfig("bucket", "cloud-disk");
    }

    private String getConfig(String key, String defaultVal) {
        // Read from StorageConfig or system properties
        String val = System.getProperty("storage.minio." + key);
        return val != null ? val : defaultVal;
    }

    private void ensureBucket(MinioClient client, String bucket) throws Exception {
        boolean exists = client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!exists) {
            client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    private void closeClient(MinioClient client) {
        // MinioClient doesn't need explicit close in newer versions
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
