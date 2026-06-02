package com.cloud.disk.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 本地存储配置类
 * 读取 application.yml 中 cloud-disk.storage 配置项
 */
@Component
@ConfigurationProperties(prefix = "cloud-disk.storage")
public class StorageConfig {

    /**
     * 本地存储根路径，例如 D:\cloud-disk-files
     */
    private String rootPath;

    public String getRootPath() {
        return rootPath;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }
}
