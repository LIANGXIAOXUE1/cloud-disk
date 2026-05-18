package com.cloud.disk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.disk.repository.entity.FileInfo;
import com.cloud.disk.repository.mapper.FileInfoMapper;
import com.cloud.disk.service.api.IFileService;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class FileServiceImpl extends ServiceImpl<FileInfoMapper, FileInfo> implements IFileService {

    @Override
    public List<FileInfo> listByParentId(Long userId, Long parentId) {
        return this.list(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getUserId, userId)
                .eq(FileInfo::getParentId, parentId)
                .eq(FileInfo::getDeleted, 0)
                .orderByAsc(FileInfo::getIsFolder)
                .orderByAsc(FileInfo::getFileName));
    }

    @Override
    public List<FileInfo> searchByName(Long userId, String fileName) {
        return this.list(new LambdaQueryWrapper<FileInfo>()
                .eq(FileInfo::getUserId, userId)
                .like(FileInfo::getFileName, fileName)
                .eq(FileInfo::getDeleted, 0)
                .orderByDesc(FileInfo::getCreatedAt));
    }

    @Override
    public boolean createFolder(Long userId, String folderName, Long parentId) {
        FileInfo folder = new FileInfo();
        folder.setUserId(userId);
        folder.setParentId(parentId);
        folder.setFileName(folderName);
        folder.setIsFolder(1);
        folder.setFileStatus(1);
        folder.setStorageType("MINIO");
        folder.setFileSize(0L);
        return this.save(folder);
    }

    @Override
    public boolean rename(Long fileId, String newName) {
        FileInfo f = this.getById(fileId);
        if (f == null) return false;
        f.setFileName(newName);
        return this.updateById(f);
    }

    @Override
    public boolean move(Long fileId, Long newParentId) {
        FileInfo f = this.getById(fileId);
        if (f == null) return false;
        f.setParentId(newParentId);
        return this.updateById(f);
    }

    @Override
    public boolean copy(Long fileId, Long targetParentId) {
        FileInfo src = this.getById(fileId);
        if (src == null) return false;
        FileInfo target = new FileInfo();
        target.setUserId(src.getUserId());
        target.setParentId(targetParentId);
        target.setFileName(src.getFileName());
        target.setFilePath(src.getFilePath());
        target.setFileSize(src.getFileSize());
        target.setFileType(src.getFileType());
        target.setStorageType(src.getStorageType());
        target.setIsFolder(src.getIsFolder());
        target.setFileStatus(1);
        target.setVersion(1);
        return this.save(target);
    }

    @Override
    public boolean deleteToRecycle(Long fileId) {
        FileInfo f = this.getById(fileId);
        if (f == null) return false;
        f.setFileStatus(2);
        return this.updateById(f);
    }

    @Override
    public String getDownloadUrl(Long fileId) {
        FileInfo f = this.getById(fileId);
        if (f == null) return null;
        return "/api/file/download/" + fileId;
    }

    @Override
    public String getPreviewUrl(Long fileId) {
        FileInfo f = this.getById(fileId);
        if (f == null) return null;
        return "/api/file/preview/" + fileId;
    }

    @Override
    public Long getUsedSpace(Long userId) {
        return 0L;
    }
}