package com.lab.module.system.controller;

import com.lab.common.Result;
import com.lab.module.system.entity.ImportTask;
import com.lab.module.system.service.ImportTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/import-task")
public class ImportTaskController {

    @Autowired
    private ImportTaskService importTaskService;

    /**
     * 查询导入任务进度。
     * 前端可轮询此接口获取当前处理进度。
     */
    @GetMapping("/{taskId}")
    public Result<ImportTask> getTask(@PathVariable String taskId) {
        ImportTask task = importTaskService.getTask(taskId);
        if (task == null) {
            return Result.ok((ImportTask) null);
        }
        return Result.ok(task);
    }
}
