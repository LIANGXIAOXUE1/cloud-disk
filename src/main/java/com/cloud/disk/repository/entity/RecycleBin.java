package com.cloud.disk.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("recycle_bin")
public class RecycleBin {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private Long fileId;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String fileType;
    private String storageType;
    private Integer isFolder;
    @TableField("delete_time")
    private LocalDateTime deletedAt;
    private LocalDateTime createdAt;
    private Integer deleted;
}
