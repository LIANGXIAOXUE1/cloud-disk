package com.cloud.disk.controller;

import com.cloud.disk.service.api.IRecycleBinService;
import com.cloud.disk.repository.entity.RecycleBin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/recycle")
public class RecycleController {

    @Autowired
    private IRecycleBinService recycleBinService;

    @GetMapping("/list")
    public List<RecycleBin> list(@RequestParam Long userId) {
        return recycleBinService.listByUserId(userId);
    }

    @PostMapping("/restore")
    public String restore(@RequestParam Long recycleId) {
        return recycleBinService.restore(recycleId) ? "OK" : "FAILED";
    }

    @PostMapping("/batchRestore")
    public String batchRestore(@RequestBody List<Long> recycleIds) {
        return recycleBinService.batchRestore(recycleIds) ? "OK" : "FAILED";
    }

    @PostMapping("/permanentDelete")
    public String permanentDelete(@RequestParam Long recycleId) {
        return recycleBinService.permanentDelete(recycleId) ? "OK" : "FAILED";
    }

    @PostMapping("/batchPermanentDelete")
    public String batchPermanentDelete(@RequestBody List<Long> recycleIds) {
        return recycleBinService.batchPermanentDelete(recycleIds) ? "OK" : "FAILED";
    }

    @PostMapping("/clear")
    public String clearRecycle(@RequestParam Long userId) {
        return recycleBinService.clearRecycle(userId) ? "OK" : "FAILED";
    }
}