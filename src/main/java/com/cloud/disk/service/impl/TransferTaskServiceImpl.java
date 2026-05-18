package com.cloud.disk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.disk.repository.entity.TransferTask;
import com.cloud.disk.repository.mapper.TransferTaskMapper;
import com.cloud.disk.service.api.ITransferTaskService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TransferTaskServiceImpl extends ServiceImpl<TransferTaskMapper, TransferTask> implements ITransferTaskService {

    @Override
    public String createTransferTask(Long userId, String sourceUrl, String extractCode, String targetPath) {
        String taskNo = generateTaskNo();
        TransferTask task = new TransferTask();
        task.setTaskNo(taskNo);
        task.setUserId(userId);
        task.setTaskType(1);
        task.setSourceUrl(sourceUrl);
        task.setExtractCode(extractCode);
        task.setTargetPath(targetPath);
        task.setTaskStatus(1);
        task.setProgress(0);
        task.setCreatedAt(LocalDateTime.now());
        this.save(task);
        return taskNo;
    }

    @Override
    public List<TransferTask> listByUserId(Long userId) {
        return this.list(new LambdaQueryWrapper<TransferTask>()
                .eq(TransferTask::getUserId, userId)
                .eq(TransferTask::getDeleted, 0)
                .orderByDesc(TransferTask::getCreatedAt));
    }

    @Override
    public TransferTask getTaskDetail(String taskNo) {
        return this.getOne(new LambdaQueryWrapper<TransferTask>()
                .eq(TransferTask::getTaskNo, taskNo)
                .eq(TransferTask::getDeleted, 0));
    }

    @Override
    public boolean cancelTask(String taskNo, Long userId) {
        TransferTask task = getTaskDetail(taskNo);
        if (task == null || !task.getUserId().equals(userId)) return false;
        task.setTaskStatus(3);
        return this.updateById(task);
    }

    @Override
    public boolean retryTask(String taskNo, Long userId) {
        TransferTask task = getTaskDetail(taskNo);
        if (task == null || !task.getUserId().equals(userId)) return false;
        task.setTaskStatus(1);
        task.setErrorMessage(null);
        task.setProgress(0);
        return this.updateById(task);
    }

    @Override
    public boolean updateProgress(String taskNo, Integer progress) {
        TransferTask task = getTaskDetail(taskNo);
        if (task == null) return false;
        task.setProgress(progress);
        return this.updateById(task);
    }

    @Override
    public boolean completeTask(String taskNo) {
        TransferTask task = getTaskDetail(taskNo);
        if (task == null) return false;
        task.setTaskStatus(2);
        task.setProgress(100);
        task.setFinishedAt(LocalDateTime.now());
        return this.updateById(task);
    }

    @Override
    public boolean failTask(String taskNo, String errorMessage) {
        TransferTask task = getTaskDetail(taskNo);
        if (task == null) return false;
        task.setTaskStatus(4);
        task.setErrorMessage(errorMessage);
        task.setFinishedAt(LocalDateTime.now());
        return this.updateById(task);
    }

    private String generateTaskNo() {
        return "TASK-" + UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }
}