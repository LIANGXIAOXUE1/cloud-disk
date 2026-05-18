package com.cloud.disk.controller;

import com.cloud.disk.service.api.ITransferTaskService;
import com.cloud.disk.repository.entity.TransferTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/transfer")
public class TransferController {

    @Autowired
    private ITransferTaskService transferTaskService;

    @PostMapping("/create")
    public String createTask(@RequestParam Long userId,
                              @RequestParam String sourceUrl,
                              @RequestParam(required = false) String extractCode,
                              @RequestParam String targetPath) {
        return transferTaskService.createTransferTask(userId, sourceUrl, extractCode, targetPath);
    }

    @GetMapping("/list")
    public List<TransferTask> list(@RequestParam Long userId) {
        return transferTaskService.listByUserId(userId);
    }

    @GetMapping("/detail")
    public TransferTask getDetail(@RequestParam String taskNo) {
        return transferTaskService.getTaskDetail(taskNo);
    }

    @PostMapping("/cancel")
    public String cancelTask(@RequestParam String taskNo, @RequestParam Long userId) {
        return transferTaskService.cancelTask(taskNo, userId) ? "OK" : "FAILED";
    }

    @PostMapping("/retry")
    public String retryTask(@RequestParam String taskNo, @RequestParam Long userId) {
        return transferTaskService.retryTask(taskNo, userId) ? "OK" : "FAILED";
    }
}