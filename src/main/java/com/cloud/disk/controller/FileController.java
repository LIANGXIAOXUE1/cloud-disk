package com.cloud.disk.controller;

import com.cloud.disk.service.api.IFileService;
import com.cloud.disk.repository.entity.FileInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/file")
public class FileController {

    @Autowired
    private IFileService fileService;

    @GetMapping("/list")
    public List<FileInfo> list(@RequestParam Long userId,
                                @RequestParam(required = false) Long parentId) {
        return fileService.listByParentId(userId, parentId);
    }

    @GetMapping("/search")
    public List<FileInfo> search(@RequestParam Long userId,
                                  @RequestParam String fileName) {
        return fileService.searchByName(userId, fileName);
    }

    @PostMapping("/createFolder")
    public String createFolder(@RequestParam Long userId,
                                @RequestParam String folderName,
                                @RequestParam(required = false) Long parentId) {
        return fileService.createFolder(userId, folderName, parentId) ? "OK" : "FAILED";
    }

    @PostMapping("/rename")
    public String rename(@RequestParam Long fileId, @RequestParam String newName) {
        return fileService.rename(fileId, newName) ? "OK" : "FAILED";
    }

    @PostMapping("/move")
    public String move(@RequestParam Long fileId, @RequestParam Long newParentId) {
        return fileService.move(fileId, newParentId) ? "OK" : "FAILED";
    }

    @PostMapping("/copy")
    public String copy(@RequestParam Long fileId, @RequestParam Long targetParentId) {
        return fileService.copy(fileId, targetParentId) ? "OK" : "FAILED";
    }

    @PostMapping("/delete")
    public String delete(@RequestParam Long fileId) {
        return fileService.deleteToRecycle(fileId) ? "OK" : "FAILED";
    }

    @GetMapping("/downloadUrl")
    public String getDownloadUrl(@RequestParam Long fileId) {
        return fileService.getDownloadUrl(fileId);
    }

    @GetMapping("/previewUrl")
    public String getPreviewUrl(@RequestParam Long fileId) {
        return fileService.getPreviewUrl(fileId);
    }

    @GetMapping("/usedSpace")
    public Long getUsedSpace(@RequestParam Long userId) {
        return fileService.getUsedSpace(userId);
    }
}