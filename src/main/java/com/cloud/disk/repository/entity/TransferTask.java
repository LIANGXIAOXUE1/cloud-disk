package com.cloud.disk.repository.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("transfer_task")
public class TransferTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskNo;
    private Long userId;
    private Integer taskType;
    private String sourceUrl;
    private String extractCode;
    private String targetPath;
    private Integer taskStatus;
    private Integer progress;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
    private LocalDateTime updatedAt;
    private Integer deleted;
}