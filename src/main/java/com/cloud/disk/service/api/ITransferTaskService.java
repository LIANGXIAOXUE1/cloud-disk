package com.cloud.disk.service.api;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.disk.repository.entity.TransferTask;
import java.util.List;

public interface ITransferTaskService extends IService<TransferTask> {
    String createTransferTask(Long userId, String sourceUrl, String extractCode, String targetPath);
    List<TransferTask> listByUserId(Long userId);
    TransferTask getTaskDetail(String taskNo);
    boolean cancelTask(String taskNo, Long userId);
    boolean retryTask(String taskNo, Long userId);
    boolean updateProgress(String taskNo, Integer progress);
    boolean completeTask(String taskNo);
    boolean failTask(String taskNo, String errorMessage);
}