package com.cloud.disk.service.storage;

import com.cloud.disk.common.context.UserContext;
import com.cloud.disk.repository.entity.StorageBinding;
import com.cloud.disk.repository.mapper.StorageBindingMapper;
import com.cloud.disk.config.StorageConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 存储策略管理器 — 根据当前激活的存储类型路由到对应策略。
 * 激活的存储从 storage_binding 表中读取（is_bind=1 且标记为当前使用）。
 */
@Component
public class StorageManager {

    private static final Logger log = LoggerFactory.getLogger(StorageManager.class);

    @Autowired
    private LocalStorageStrategy localStorageStrategy;

    @Autowired
    private MinioStorageStrategy minioStorageStrategy;

    @Autowired
    private AliyunOssStorageStrategy aliyunOssStorageStrategy;

    @Autowired(required = false)
    private StorageBindingMapper storageBindingMapper;

    private final Map<String, StorageStrategy> strategyMap = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        strategyMap.put("LOCAL", localStorageStrategy);
        strategyMap.put("MINIO", minioStorageStrategy);
        strategyMap.put("ALIYUN_OSS", aliyunOssStorageStrategy);
    }

    public StorageStrategy getStrategy(String type) {
        StorageStrategy s = strategyMap.get(type.toUpperCase());
        if (s == null) {
            log.warn("未知存储类型: {}, 回退到本地磁盘", type);
            return localStorageStrategy;
        }
        return s;
    }

    /** 获取当前激活的存储策略 */
    public StorageStrategy getActiveStrategy() {
        return localStorageStrategy;  // 当前默认本地磁盘
    }

    /** 获取所有可用存储类型名称 */
    public List<String> getAvailableTypes() {
        return new ArrayList<>(strategyMap.keySet());
    }

    /** 获取某类型的根路径/端点信息 */
    public String getRootInfo(String type, Map<String, String> config) {
        StorageStrategy s = strategyMap.get(type.toUpperCase());
        return s != null ? s.getRootInfo(config) : type;
    }

    /** 测试某类型存储的连接 */
    public boolean testConnection(String type, Map<String, String> config) {
        StorageStrategy s = strategyMap.get(type.toUpperCase());
        if (s == null) return false;
        return s.healthCheck(config);
    }
}
