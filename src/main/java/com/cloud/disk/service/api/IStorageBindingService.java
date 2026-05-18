package com.cloud.disk.service.api;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.disk.repository.entity.StorageBinding;
import java.util.List;

public interface IStorageBindingService extends IService<StorageBinding> {
    List<StorageBinding> listByUserId(Long userId);
    StorageBinding getByUserAndType(Long userId, String storageType);
    boolean bindStorage(Long userId, String storageType, String accessToken, String refreshToken);
    boolean unbindStorage(Long userId, String storageType);
    boolean isTokenValid(Long bindingId);
    boolean refreshToken(Long bindingId);
}