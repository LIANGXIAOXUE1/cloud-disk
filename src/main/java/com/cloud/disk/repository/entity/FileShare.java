package com.cloud.disk.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_share")
public class FileShare {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String shareId;
    private Long fileId;
    private Long userId;
    private String sharePassword;
    private Integer expireType;
    private LocalDateTime expireTime;
    private Integer isPublic;
    private Integer downloadLimit;
    private Integer downloadCount;
    private Integer viewCount;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}