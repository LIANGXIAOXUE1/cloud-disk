package com.cloud.disk.controller;

import com.cloud.disk.common.context.UserContext;
import com.cloud.disk.common.result.Result;
import com.cloud.disk.repository.entity.RecycleBin;
import com.cloud.disk.service.api.IRecycleBinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "回收站", description = "回收站列表、恢复、彻底删除、清空")
@RestController
@RequestMapping("/api/recycle")
public class RecycleController {

    @Autowired
    private IRecycleBinService recycleBinService;

    @Operation(summary = "回收站列表")
    @GetMapping("/list")
    public Result<Map<String, Object>> list() {
        Long userId = UserContext.getCurrentUserId();
        List<RecycleBin> list = recycleBinService.listByUserId(userId);
        return Result.success(Map.of("list", list, "total", list.size()));
    }

    @Operation(summary = "恢复文件")
    @PostMapping("/restore")
    public Result<Map<String, Object>> restore(@RequestParam Long recycleId) {
        boolean ok = recycleBinService.restore(recycleId);
        return ok ? Result.success(Map.of("result", "OK"))
                  : Result.error(400, "恢复失败");
    }

    @Operation(summary = "批量恢复")
    @PostMapping("/batchRestore")
    public Result<Map<String, Object>> batchRestore(@RequestBody List<Long> recycleIds) {
        boolean ok = recycleBinService.batchRestore(recycleIds);
        return ok ? Result.success(Map.of("result", "OK"))
                  : Result.error(400, "批量恢复失败");
    }

    @Operation(summary = "彻底删除")
    @PostMapping("/permanentDelete")
    public Result<Map<String, Object>> permanentDelete(@RequestParam Long recycleId) {
        boolean ok = recycleBinService.permanentDelete(recycleId);
        return ok ? Result.success(Map.of("result", "OK"))
                  : Result.error(400, "彻底删除失败");
    }

    @Operation(summary = "清空回收站")
    @PostMapping("/clear")
    public Result<Map<String, Object>> clearRecycle() {
        Long userId = UserContext.getCurrentUserId();
        boolean ok = recycleBinService.clearRecycle(userId);
        return ok ? Result.success(Map.of("result", "OK"))
                  : Result.error(400, "清空失败");
    }
}
