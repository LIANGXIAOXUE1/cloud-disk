package com.cloud.disk.controller;

import com.cloud.disk.service.api.IFileShareService;
import com.cloud.disk.repository.entity.FileShare;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/share")
public class ShareController {

    @Autowired
    private IFileShareService fileShareService;

    @PostMapping("/create")
    public String createShare(@RequestParam Long fileId,
                               @RequestParam Long userId,
                               @RequestParam(required = false) String password,
                               @RequestParam(required = false, defaultValue = "1") Integer expireType) {
        return fileShareService.createShare(fileId, userId, password, expireType);
    }

    @GetMapping("/info")
    public FileShare getShareInfo(@RequestParam String shareId) {
        return fileShareService.getByShareId(shareId);
    }

    @PostMapping("/verify")
    public String verifyPassword(@RequestParam String shareId, @RequestParam String password) {
        return fileShareService.verifyExtractCode(shareId, password) ? "OK" : "INVALID";
    }

    @PostMapping("/access")
    public String accessShare(@RequestParam String shareId) {
        return fileShareService.accessShare(shareId) ? "OK" : "EXPIRED";
    }

    @PostMapping("/download")
    public String downloadShare(@RequestParam String shareId) {
        return fileShareService.downloadShare(shareId) ? "OK" : "LIMITED";
    }

    @PostMapping("/cancel")
    public String cancelShare(@RequestParam String shareId, @RequestParam Long userId) {
        return fileShareService.cancelShare(shareId, userId) ? "OK" : "FAILED";
    }

    @GetMapping("/list")
    public List<FileShare> listByUser(@RequestParam Long userId) {
        return fileShareService.listByUserId(userId);
    }
}