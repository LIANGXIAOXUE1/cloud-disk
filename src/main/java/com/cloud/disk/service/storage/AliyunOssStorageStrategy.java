package com.cloud.disk.service.storage;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * 阿里云 OSS 对象存储策略。
 * 配置示例：{ "endpoint": "oss-cn-hangzhou.aliyuncs.com",
 *              "accessKey": "LTAI5t...", "secretKey": "...", "bucket": "my-cloud-disk" }
 */
@Component
public class AliyunOssStorageStrategy implements StorageStrategy {

    private static final Logger log = LoggerFactory.getLogger(AliyunOssStorageStrategy.class);

    @Override
    public String getType() {
        return "ALIYUN_OSS";
    }

    @Override
    public String saveFile(Long userId, MultipartFile file, String fileName, String relativeDir) throws Exception {
        OSS client = buildClient();
        try {
            String bucket = getDefaultBucket();
            ensureBucket(client, bucket);

            String ext = getExtension(fileName);
            String uuid = UUID.randomUUID().toString().replace("-", "");
            String objectName = "upload/" + relativeDir + "/" + uuid + "." + ext;

            // 保存临时文件
            Path tmpPath = Files.createTempFile("oss-upload-", ".tmp");
            file.transferTo(tmpPath.toFile());

            PutObjectRequest request = new PutObjectRequest(bucket, objectName, tmpPath.toFile());
            ObjectMetadata meta = new ObjectMetadata();
            meta.setContentType(file.getContentType());
            request.setMetadata(meta);
            client.putObject(request);

            Files.deleteIfExists(tmpPath);
            log.info("OSS 文件上传成功: {}/{}", bucket, objectName);
            return bucket + "/" + objectName;
        } finally {
            shutdownClient(client);
        }
    }

    @Override
    public InputStream readFile(String storagePath) throws Exception {
        OSS client = buildClient();
        try {
            String bucket = getDefaultBucket();
            String objectName = storagePath;
            if (objectName.contains("/")) {
                int idx = objectName.indexOf('/');
                bucket = objectName.substring(0, idx);
                objectName = objectName.substring(idx + 1);
            }

            OSSObject ossObject = client.getObject(bucket, objectName);
            return ossObject.getObjectContent();
        } catch (Exception e) {
            shutdownClient(client);
            throw e;
        }
    }

    @Override
    public void deleteFile(String storagePath) throws Exception {
        OSS client = buildClient();
        try {
            String bucket = getDefaultBucket();
            String objectName = storagePath;
            if (objectName.contains("/")) {
                int idx = objectName.indexOf('/');
                bucket = objectName.substring(0, idx);
                objectName = objectName.substring(idx + 1);
            }
            client.deleteObject(bucket, objectName);
        } finally {
            shutdownClient(client);
        }
    }

    @Override
    public boolean healthCheck(Map<String, String> config) {
        OSS client = null;
        try {
            client = buildClientWithConfig(config);
            // 尝试列出 bucket 以验证连接
            client.listBuckets();
            return true;
        } catch (Exception e) {
            log.warn("OSS 连接测试失败: {}", e.getMessage());
            return false;
        } finally {
            if (client != null) shutdownClient(client);
        }
    }

    @Override
    public String getRootInfo(Map<String, String> config) {
        String endpoint = config.getOrDefault("endpoint", "oss-cn-hangzhou.aliyuncs.com");
        String bucket = config.getOrDefault("bucket", "my-cloud-disk");
        return "https://" + bucket + "." + endpoint;
    }

    // ====== Helpers ======

    private OSS buildClient() {
        return new OSSClientBuilder().build(
                getConfig("endpoint", "oss-cn-hangzhou.aliyuncs.com"),
                getConfig("accessKey", ""),
                getConfig("secretKey", "")
        );
    }

    private OSS buildClientWithConfig(Map<String, String> config) {
        return new OSSClientBuilder().build(
                config.getOrDefault("endpoint", "oss-cn-hangzhou.aliyuncs.com"),
                config.getOrDefault("accessKey", ""),
                config.getOrDefault("secretKey", "")
        );
    }

    private String getDefaultBucket() {
        return getConfig("bucket", "my-cloud-disk");
    }

    private String getConfig(String key, String defaultVal) {
        String val = System.getProperty("storage.oss." + key);
        return val != null ? val : defaultVal;
    }

    private void ensureBucket(OSS client, String bucket) {
        if (!client.doesBucketExist(bucket)) {
            client.createBucket(bucket);
        }
    }

    private void shutdownClient(OSS client) {
        if (client != null) client.shutdown();
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) return "";
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
    }
}
