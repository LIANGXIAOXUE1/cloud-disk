package com.cloud.disk.controller;

import com.cloud.disk.common.context.UserContext;
import com.cloud.disk.common.result.Result;
import com.cloud.disk.repository.entity.StorageBinding;
import com.cloud.disk.repository.mapper.StorageBindingMapper;
import com.cloud.disk.service.storage.StorageManager;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;

@Tag(name = "存储管理", description = "存储空间绑定、切换、连接测试")
@RestController
@RequestMapping("/api/storage")
public class StorageController {

    @Autowired
    private StorageManager storageManager;

    @Autowired(required = false)
    private StorageBindingMapper storageBindingMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Operation(summary = "获取存储类型列表")
    @GetMapping("/types")
    public Result<List<Map<String, Object>>> getTypes() {
        List<Map<String, Object>> types = new ArrayList<>();
        types.add(Map.of("type", "LOCAL", "name", "本地磁盘", "description", "存储在服务器硬盘上，零配置即用", "available", true));
        types.add(Map.of("type", "MINIO", "name", "MinIO", "description", "自建对象存储服务器，需要先部署 MinIO 服务", "available", true));
        types.add(Map.of("type", "ALIYUN_OSS", "name", "阿里云 OSS", "description", "阿里云对象存储服务，需注册阿里云并开通 OSS", "available", true));
        types.add(Map.of("type", "ALIYUN_PAN", "name", "阿里云盘", "description", "个人阿里云盘，需扫码授权登录", "available", false));
        types.add(Map.of("type", "QUARK", "name", "夸克云盘", "description", "夸克网盘，需扫码授权登录", "available", false));
        return Result.success(types);
    }

    @Operation(summary = "获取存储绑定列表")
    @GetMapping("/bindings")
    public Result<List<Map<String, Object>>> listBindings() {
        if (storageBindingMapper == null) {
            // 返回默认本地磁盘
            return Result.success(List.of(defaultLocalBinding()));
        }

        Long userId = UserContext.getCurrentUserId();
        List<StorageBinding> bindings = storageBindingMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<StorageBinding>()
                        .eq(StorageBinding::getUserId, userId)
                        .eq(StorageBinding::getIsBind, 1)
        );

        // 如果没有绑定，返回默认本地磁盘
        if (bindings.isEmpty()) {
            // 自动创建默认本地磁盘绑定
            StorageBinding defaultBinding = new StorageBinding();
            defaultBinding.setUserId(userId);
            defaultBinding.setStorageType("LOCAL");
            defaultBinding.setAccessToken("{}");
            defaultBinding.setIsBind(1);
            defaultBinding.setBindTime(LocalDateTime.now());
            storageBindingMapper.insert(defaultBinding);
            bindings = List.of(defaultBinding);
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (StorageBinding b : bindings) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", b.getId());
            item.put("type", b.getStorageType());
            item.put("name", getBindingName(b));
            item.put("config", parseConfig(b.getAccessToken()));
            item.put("rootInfo", storageManager.getRootInfo(b.getStorageType(), parseConfig(b.getAccessToken())));
            item.put("isDefault", "LOCAL".equals(b.getStorageType())); // 当前默认本地
            item.put("status", "LOCAL".equals(b.getStorageType()) ? "active" : "inactive");
            result.add(item);
        }
        return Result.success(result);
    }

    @Operation(summary = "添加存储绑定")
    @PostMapping("/binding")
    public Result<Map<String, Object>> addBinding(@RequestBody Map<String, Object> body) {
        Long userId = UserContext.getCurrentUserId();
        String type = (String) body.get("type");
        @SuppressWarnings("unchecked")
        Map<String, String> config = (Map<String, String>) body.get("config");

        if (type == null) return Result.error(400, "存储类型不能为空");

        // 保存配置到 storage_binding 表
        StorageBinding binding = new StorageBinding();
        binding.setUserId(userId);
        binding.setStorageType(type.toUpperCase());
        try {
            binding.setAccessToken(objectMapper.writeValueAsString(config));
        } catch (Exception e) {
            binding.setAccessToken("{}");
        }
        binding.setIsBind(1);
        binding.setBindTime(LocalDateTime.now());
        binding.setCreatedAt(LocalDateTime.now());
        binding.setUpdatedAt(LocalDateTime.now());

        if (storageBindingMapper != null) {
            storageBindingMapper.insert(binding);
        }

        return Result.success(Map.of("id", binding.getId(), "type", type, "status", "active"));
    }

    @Operation(summary = "删除存储绑定")
    @DeleteMapping("/binding/{id}")
    public Result<Map<String, Object>> deleteBinding(@PathVariable Long id) {
        if (storageBindingMapper == null) {
            return Result.error(400, "不支持删除");
        }
        StorageBinding binding = storageBindingMapper.selectById(id);
        if (binding == null) return Result.error(404, "存储绑定不存在");
        if ("LOCAL".equals(binding.getStorageType())) {
            return Result.error(400, "默认本地存储不能删除");
        }
        binding.setIsBind(0);
        storageBindingMapper.updateById(binding);
        return Result.success(Map.of("result", "OK"));
    }

    @Operation(summary = "测试存储连接")
    @PostMapping("/test")
    public Result<Map<String, Object>> testConnection(@RequestBody Map<String, Object> body) {
        String type = (String) body.get("type");
        @SuppressWarnings("unchecked")
        Map<String, String> config = (Map<String, String>) body.get("config");

        if (type == null) return Result.error(400, "存储类型不能为空");
        if (config == null) config = new HashMap<>();

        boolean ok = storageManager.testConnection(type, config);
        if (ok) {
            return Result.success(Map.of("result", "OK", "message", "连接成功"));
        } else {
            return Result.error(400, "连接失败，请检查配置信息");
        }
    }

    // ====== Helpers ======

    private Map<String, Object> defaultLocalBinding() {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", 0L);
        item.put("type", "LOCAL");
        item.put("name", "本地磁盘");
        item.put("config", Map.of("rootPath", "D:\\cloud-disk-files"));
        item.put("rootInfo", "D:\\cloud-disk-files");
        item.put("isDefault", true);
        item.put("status", "active");
        return item;
    }

    private String getBindingName(StorageBinding b) {
        Map<String, String> config = parseConfig(b.getAccessToken());
        String name = config.get("name");
        if (name != null && !name.isEmpty()) return name;
        return switch (b.getStorageType()) {
            case "LOCAL" -> "本地磁盘";
            case "MINIO" -> "MinIO";
            case "ALIYUN_OSS" -> "阿里云 OSS";
            default -> b.getStorageType();
        };
    }

    private Map<String, String> parseConfig(String token) {
        if (token == null || token.isEmpty()) return new HashMap<>();
        try {
            return objectMapper.readValue(token, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
