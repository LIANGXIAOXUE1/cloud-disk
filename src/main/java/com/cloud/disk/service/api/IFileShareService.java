package com.cloud.disk.service.api;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.disk.repository.entity.FileShare;
import java.util.List;

public interface IFileShareService extends IService<FileShare> {
    String createShare(Long fileId, Long userId, String password, Integer expireType);
    FileShare getByShareId(String shareId);
    boolean verifyExtractCode(String shareId, String password);
    boolean accessShare(String shareId);
    boolean downloadShare(String shareId);
    boolean cancelShare(String shareId, Long userId);
    List<FileShare> listByUserId(Long userId);
    void cleanExpiredShares();
}