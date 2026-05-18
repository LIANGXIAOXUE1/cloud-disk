package com.cloud.disk.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file_info")
public class FileInfo {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long parentId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private String storageType;
    private String fileMd5;
    private Integer isFolder;
    private Integer fileStatus;
    private Integer version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}