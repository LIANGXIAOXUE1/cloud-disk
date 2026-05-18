package com.cloud.disk.service.api;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cloud.disk.repository.entity.RecycleBin;
import java.util.List;

public interface IRecycleBinService extends IService<RecycleBin> {
    List<RecycleBin> listByUserId(Long userId);
    boolean restore(Long recycleId);
    boolean batchRestore(List<Long> recycleIds);
    boolean permanentDelete(Long recycleId);
    boolean batchPermanentDelete(List<Long> recycleIds);
    boolean clearRecycle(Long userId);
    void cleanExpiredFiles();
}