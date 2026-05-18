package com.cloud.disk.service.api;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.disk.repository.entity.FileInfo;
import java.util.List;

public interface IFileService extends IService<FileInfo> {
    List<FileInfo> listByParentId(Long userId, Long parentId);
    List<FileInfo> searchByName(Long userId, String fileName);
    boolean createFolder(Long userId, String folderName, Long parentId);
    boolean rename(Long fileId, String newName);
    boolean move(Long fileId, Long newParentId);
    boolean copy(Long fileId, Long targetParentId);
    boolean deleteToRecycle(Long fileId);
    String getDownloadUrl(Long fileId);
    String getPreviewUrl(Long fileId);
    Long getUsedSpace(Long userId);
}