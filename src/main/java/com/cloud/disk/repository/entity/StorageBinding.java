package com.cloud.disk.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("storage_binding")
public class StorageBinding {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String storageType;
    private String accessToken;
    private String refreshToken;
    private LocalDateTime tokenExpireTime;
    private Integer isBind;
    private LocalDateTime bindTime;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}