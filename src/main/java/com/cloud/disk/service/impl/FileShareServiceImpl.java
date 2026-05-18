package com.cloud.disk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.disk.repository.entity.FileShare;
import com.cloud.disk.repository.mapper.FileShareMapper;
import com.cloud.disk.service.api.IFileShareService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class FileShareServiceImpl extends ServiceImpl<FileShareMapper, FileShare> implements IFileShareService {

    @Override
    public String createShare(Long fileId, Long userId, String password, Integer expireType) {
        String shareId = generateShareId();
        FileShare share = new FileShare();
        share.setShareId(shareId);
        share.setFileId(fileId);
        share.setUserId(userId);
        share.setSharePassword(password);
        share.setExpireType(expireType);
        if (expireType == 2) share.setExpireTime(LocalDateTime.now().plusDays(7));
        share.setIsPublic(1);
        share.setStatus(1);
        share.setDownloadCount(0);
        share.setViewCount(0);
        this.save(share);
        return shareId;
    }

    @Override
    public FileShare getByShareId(String shareId) {
        return this.getOne(new LambdaQueryWrapper<FileShare>()
                .eq(FileShare::getShareId, shareId)
                .eq(FileShare::getDeleted, 0));
    }

    @Override
    public boolean verifyExtractCode(String shareId, String password) {
        FileShare share = getByShareId(shareId);
        if (share == null) return false;
        if (share.getSharePassword() == null || share.getSharePassword().isEmpty()) return true;
        return share.getSharePassword().equals(password);
    }

    @Override
    public boolean accessShare(String shareId) {
        FileShare share = getByShareId(shareId);
        if (share == null) return false;
        if (share.getExpireType() == 2 && share.getExpireTime() != null
                && share.getExpireTime().isBefore(LocalDateTime.now())) {
            share.setStatus(2);
            this.updateById(share);
            return false;
        }
        share.setViewCount(share.getViewCount() + 1);
        return this.updateById(share);
    }

    @Override
    public boolean downloadShare(String shareId) {
        FileShare share = getByShareId(shareId);
        if (share == null) return false;
        if (share.getDownloadLimit() != null && share.getDownloadCount() >= share.getDownloadLimit()) return false;
        share.setDownloadCount(share.getDownloadCount() + 1);
        return this.updateById(share);
    }

    @Override
    public boolean cancelShare(String shareId, Long userId) {
        FileShare share = getByShareId(shareId);
        if (share == null || !share.getUserId().equals(userId)) return false;
        share.setStatus(0);
        return this.updateById(share);
    }

    @Override
    public List<FileShare> listByUserId(Long userId) {
        return this.list(new LambdaQueryWrapper<FileShare>()
                .eq(FileShare::getUserId, userId)
                .eq(FileShare::getDeleted, 0)
                .orderByDesc(FileShare::getCreatedAt));
    }

    @Override
    public void cleanExpiredShares() {}

    private String generateShareId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}