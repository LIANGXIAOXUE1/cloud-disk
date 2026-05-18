package com.cloud.disk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.disk.repository.entity.StorageBinding;
import com.cloud.disk.repository.mapper.StorageBindingMapper;
import com.cloud.disk.service.api.IStorageBindingService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class StorageBindingServiceImpl extends ServiceImpl<StorageBindingMapper, StorageBinding> implements IStorageBindingService {

    @Override
    public List<StorageBinding> listByUserId(Long userId) {
        return this.list(new LambdaQueryWrapper<StorageBinding>()
                .eq(StorageBinding::getUserId, userId)
                .eq(StorageBinding::getIsBind, 1)
                .eq(StorageBinding::getDeleted, 0));
    }

    @Override
    public StorageBinding getByUserAndType(Long userId, String storageType) {
        return this.getOne(new LambdaQueryWrapper<StorageBinding>()
                .eq(StorageBinding::getUserId, userId)
                .eq(StorageBinding::getStorageType, storageType)
                .eq(StorageBinding::getIsBind, 1)
                .eq(StorageBinding::getDeleted, 0));
    }

    @Override
    public boolean bindStorage(Long userId, String storageType, String accessToken, String refreshToken) {
        StorageBinding existing = getByUserAndType(userId, storageType);
        if (existing != null) {
            existing.setAccessToken(accessToken);
            existing.setRefreshToken(refreshToken);
            existing.setIsBind(1);
            existing.setBindTime(LocalDateTime.now());
            return this.updateById(existing);
        }
        StorageBinding binding = new StorageBinding();
        binding.setUserId(userId);
        binding.setStorageType(storageType);
        binding.setAccessToken(accessToken);
        binding.setRefreshToken(refreshToken);
        binding.setIsBind(1);
        binding.setBindTime(LocalDateTime.now());
        return this.save(binding);
    }

    @Override
    public boolean unbindStorage(Long userId, String storageType) {
        StorageBinding binding = getByUserAndType(userId, storageType);
        if (binding == null) return false;
        binding.setIsBind(0);
        return this.updateById(binding);
    }

    @Override
    public boolean isTokenValid(Long bindingId) {
        StorageBinding binding = this.getById(bindingId);
        if (binding == null) return false;
        LocalDateTime expireTime = binding.getTokenExpireTime();
        return expireTime == null || expireTime.isAfter(LocalDateTime.now());
    }

    @Override
    public boolean refreshToken(Long bindingId) {
        return false;
    }
}