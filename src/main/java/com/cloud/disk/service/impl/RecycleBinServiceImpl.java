package com.cloud.disk.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cloud.disk.repository.entity.RecycleBin;
import com.cloud.disk.repository.mapper.RecycleBinMapper;
import com.cloud.disk.service.api.IRecycleBinService;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class RecycleBinServiceImpl extends ServiceImpl<RecycleBinMapper, RecycleBin> implements IRecycleBinService {

    @Override
    public List<RecycleBin> listByUserId(Long userId) {
        return this.list(new LambdaQueryWrapper<RecycleBin>()
                .eq(RecycleBin::getUserId, userId)
                .eq(RecycleBin::getDeleted, 0)
                .orderByDesc(RecycleBin::getDeletedAt));
    }

    @Override
    public boolean restore(Long recycleId) {
        RecycleBin bin = this.getById(recycleId);
        if (bin == null) return false;
        return this.removeById(recycleId);
    }

    @Override
    public boolean batchRestore(List<Long> recycleIds) {
        return this.removeByIds(recycleIds);
    }

    @Override
    public boolean permanentDelete(Long recycleId) {
        return this.removeById(recycleId);
    }

    @Override
    public boolean batchPermanentDelete(List<Long> recycleIds) {
        return this.removeByIds(recycleIds);
    }

    @Override
    public boolean clearRecycle(Long userId) {
        return this.remove(new LambdaQueryWrapper<RecycleBin>()
                .eq(RecycleBin::getUserId, userId));
    }

    @Override
    public void cleanExpiredFiles() {
        this.remove(new LambdaQueryWrapper<RecycleBin>()
                .lt(RecycleBin::getDeletedAt, LocalDateTime.now().minusDays(30)));
    }
}