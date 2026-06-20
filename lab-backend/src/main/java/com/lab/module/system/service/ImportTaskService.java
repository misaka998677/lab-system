package com.lab.module.system.service;

import com.lab.module.system.entity.ImportTask;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 批量导入任务进度跟踪服务。
 * 任务在内存中存储，10 分钟后自动清理（任务完成后）。
 */
@Service
public class ImportTaskService {

    /** taskId → ImportTask */
    private final Map<String, ImportTask> tasks = new ConcurrentHashMap<>();

    /** 任务完成后的保留时间（毫秒） */
    private static final long RETAIN_MS = 10 * 60 * 1000L;

    /** 创建任务并返回 taskId */
    public String createTask(Long userId, String module, int total) {
        String taskId = UUID.randomUUID().toString().replace("-", "");
        ImportTask task = new ImportTask();
        task.setTaskId(taskId);
        task.setUserId(userId);
        task.setModule(module);
        task.setTotal(total);
        task.setProcessed(0);
        task.setSuccess(0);
        task.setFail(0);
        task.setStatus("RUNNING");
        task.setStartTime(LocalDateTime.now());
        tasks.put(taskId, task);
        return taskId;
    }

    /** 更新进度（每处理一行调用一次） */
    public void updateProgress(String taskId, boolean success) {
        ImportTask task = tasks.get(taskId);
        if (task == null) return;
        task.setProcessed(task.getProcessed() + 1);
        if (success) {
            task.setSuccess(task.getSuccess() + 1);
        } else {
            task.setFail(task.getFail() + 1);
        }
    }

    /** 标记任务完成 */
    public void finishTask(String taskId, String errorMsg) {
        ImportTask task = tasks.get(taskId);
        if (task == null) return;
        task.setFinishTime(LocalDateTime.now());
        if (errorMsg != null) {
            task.setStatus("ERROR");
            task.setErrorMsg(errorMsg);
        } else {
            task.setStatus("DONE");
        }
    }

    /** 查询任务状态（返回 null 表示不存在或已过期清理） */
    public ImportTask getTask(String taskId) {
        ImportTask task = tasks.get(taskId);
        if (task == null) return null;
        // 完成后超过10分钟，移除
        if ("DONE".equals(task.getStatus()) || "ERROR".equals(task.getStatus())) {
            if (task.getFinishTime() != null) {
                long elapsed = java.time.Duration.between(task.getFinishTime(), LocalDateTime.now()).toMillis();
                if (elapsed > RETAIN_MS) {
                    tasks.remove(taskId);
                    return null;
                }
            }
        }
        return task;
    }

    /**
     * 异步执行导入任务。
     * 调用方传入的 Runnable 应包含完整的导入逻辑，
     * 内部自动更新进度和标记完成。
     */
    @Async
    public void executeAsync(String taskId, Runnable importLogic) {
        try {
            importLogic.run();
            finishTask(taskId, null);
        } catch (Exception e) {
            finishTask(taskId, e.getMessage());
        }
    }
}
