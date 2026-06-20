package com.lab.module.lab.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.lab.common.BizException;
import com.lab.common.ExcelImportUtil;
import com.lab.common.ImportResult;
import com.lab.common.PageResult;
import com.lab.common.SqlLikeUtil;
import com.lab.module.lab.entity.LabDevice;
import com.lab.module.lab.entity.LabRoom;
import com.lab.module.lab.mapper.LabDeviceMapper;
import com.lab.module.lab.mapper.LabDeviceRepairMapper;
import com.lab.module.lab.mapper.LabRoomMapper;
import com.lab.module.system.service.ImportTaskService;
import com.lab.security.DataScopeUtil;
import com.lab.security.SecurityUtil;
import com.lab.websocket.StatPushService;
import org.apache.poi.ss.usermodel.Sheet;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LabDeviceService {
    private final LabDeviceMapper deviceMapper;
    private final LabRoomMapper roomMapper;
    private final LabDeviceRepairMapper repairMapper;
    private final StatPushService statPushService;
    private final ImportTaskService importTaskService;

    public LabDeviceService(LabDeviceMapper m, LabRoomMapper rm,
                            LabDeviceRepairMapper rp, @Lazy StatPushService sps,
                            @Lazy ImportTaskService its) {
        this.deviceMapper = m; this.roomMapper = rm; this.repairMapper = rp;
        this.statPushService = sps; this.importTaskService = its;
    }

    public PageResult<LabDevice> page(int pageNum, int pageSize, String keyword,
                                      Long labId, String category, Integer status) {
        List<Long> scope = DataScopeUtil.getLabIdsForTeacher();
        Long filteredLabId = scopeFilter(labId, scope);
        PageHelper.startPage(pageNum, pageSize);
        return PageResult.of(new PageInfo<>(
            deviceMapper.page(SqlLikeUtil.escape(keyword), filteredLabId, category, status, scope)));
    }

    public List<LabDevice> findByLab(Long labId) { return deviceMapper.findByLab(labId); }
    public LabDevice detail(Long id)             { return deviceMapper.findById(id); }

    public Long create(LabDevice d) {
        if (d.getStatus() == null) d.setStatus(1);
        validateLabOwnership(d.getLabId());
        deviceMapper.insert(d);
        statPushService.pushOverviewUpdate();
        return d.getId();
    }

    public void update(LabDevice d) {
        if (DataScopeUtil.isLabAdmin()) {
            LabDevice existing = deviceMapper.findById(d.getId());
            if (existing == null) throw new BizException("设备不存在");
            validateLabOwnership(existing.getLabId());
        }
        deviceMapper.update(d);
        statPushService.pushOverviewUpdate();
    }

    public void delete(Long id) {
        LabDevice existing = deviceMapper.findById(id);
        if (existing == null) throw new BizException("设备不存在");

        // 删除前检查：若有未完成的维修单，不允许删除
        int pendingCount = repairMapper.countByDeviceIdAndStatus(id);
        if (pendingCount > 0) {
            throw new BizException("该设备有 " + pendingCount + " 条未完成的维修记录，请先处理完成后再删除");
        }

        if (DataScopeUtil.isLabAdmin()) {
            validateLabOwnership(existing.getLabId());
        }
        deviceMapper.deleteById(id);
        statPushService.pushOverviewUpdate();
    }

    public void status(Long id, Integer s) {
        if (DataScopeUtil.isLabAdmin()) {
            LabDevice existing = deviceMapper.findById(id);
            if (existing == null) throw new BizException("设备不存在");
            validateLabOwnership(existing.getLabId());
        }
        deviceMapper.updateStatus(id, s);
        statPushService.pushOverviewUpdate();
    }

    /**
     * 设备导入（同步版，保持向后兼容）。
     */
    @Transactional
    public ImportResult importDevices(InputStream in) throws Exception {
        return doImportDevices(null, in, null);
    }

    /**
     * 设备导入（异步版，返回 taskId，前端轮询 /import-task/{taskId} 获取进度）。
     */
    public String importDevicesAsync(InputStream in) {
        int total;
        try {
            Sheet sheet = ExcelImportUtil.readFirstSheet(in);
            total = sheet.getPhysicalNumberOfRows() - 1; // 减表头行
        } catch (Exception e) {
            throw new BizException("读取Excel失败：" + e.getMessage());
        }
        Long userId = SecurityUtil.currentUserId();
        String taskId = importTaskService.createTask(userId, "lab-device", total);

        importTaskService.executeAsync(taskId, () -> {
            try {
                // 重新读取流（上一个方法已消耗）
                doImportDevices(taskId, new java.io.ByteArrayInputStream(
                        ((java.io.ByteArrayInputStream) in).readAllBytes()), null);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        });
        return taskId;
    }

    /**
     * 设备导入核心逻辑。
     * @param taskId 若非 null，则更新 ImportTask 进度
     */
    @Transactional
    ImportResult doImportDevices(String taskId, InputStream in,
                                 java.util.function.BiConsumer<Integer, Integer> progressCallback) throws Exception {
        ImportResult result = new ImportResult();
        Sheet sheet = ExcelImportUtil.readFirstSheet(in);
        List<String> headers = ExcelImportUtil.readHeader(sheet.getRow(0));
        if (headers.isEmpty() || headers.stream().allMatch(String::isBlank)) {
            throw new BizException("Excel 内容为空，请先下载模板填写数据");
        }
        List<Long> scope = DataScopeUtil.getLabIdsForLabAdmin();
        List<LabRoom> allowedRooms = roomMapper.all(scope);
        Map<String, Long> nameToId = new HashMap<>();
        for (LabRoom r : allowedRooms) {
            if (r.getName() != null) nameToId.put(r.getName(), r.getId());
            if (r.getCode() != null) nameToId.putIfAbsent(r.getCode(), r.getId());
        }

        Map<String, Integer> statusMap = new HashMap<>();
        statusMap.put("在用", 1);
        statusMap.put("维修", 2);
        statusMap.put("报废", 3);

        int[] counter = new int[]{0};
        ExcelImportUtil.parseRows(sheet, headers, result, row -> {
            counter[0]++;
            int displayRowNum = counter[0] + 1;
            if (taskId != null && counter[0] % 5 == 0) {
                importTaskService.updateProgress(taskId, true); // 每5行上报一次进度
            }
            try {
                String assetNo = ExcelImportUtil.require(row, "资产编号");
                String name = ExcelImportUtil.require(row, "名称");
                String category = ExcelImportUtil.optional(row, "分类");
                String brand = ExcelImportUtil.optional(row, "品牌");
                String model = ExcelImportUtil.optional(row, "型号");
                String labRef = ExcelImportUtil.require(row, "实验室");
                String statusText = ExcelImportUtil.optional(row, "状态");

                Long labId;
                try { labId = Long.parseLong(labRef.trim()); }
                catch (NumberFormatException e) { labId = nameToId.get(labRef.trim()); }
                if (labId == null) {
                    result.addFail(displayRowNum, "实验室「" + labRef + "」不存在或无权限");
                    if (taskId != null) importTaskService.updateProgress(taskId, false);
                    return null;
                }
                Integer status = statusMap.get(statusText);
                if (status == null) status = 1;

                if (DataScopeUtil.isLabAdmin()) {
                    Long finalLabId = labId;
                    boolean allowed = allowedRooms.stream().anyMatch(r -> r.getId() != null && r.getId().equals(finalLabId));
                    if (!allowed) {
                        result.addFail(displayRowNum, "无权操作实验室「" + labRef + "」的设备");
                        if (taskId != null) importTaskService.updateProgress(taskId, false);
                        return null;
                    }
                }

                LabDevice dev = new LabDevice();
                dev.setAssetNo(assetNo);
                dev.setName(name);
                dev.setCategory(category);
                dev.setBrand(brand);
                dev.setModel(model);
                dev.setLabId(labId);
                dev.setStatus(status);
                deviceMapper.insert(dev);
                if (taskId != null) importTaskService.updateProgress(taskId, true);
                return dev;
            } catch (BizException e) {
                result.addFail(displayRowNum, e.getMessage());
                if (taskId != null) importTaskService.updateProgress(taskId, false);
                return null;
            }
        });

        if (result.hasFailure()) {
            throw new BizException("导入失败，请检查后重试");
        }
        statPushService.pushOverviewUpdate();
        return result;
    }

    private Long scopeFilter(Long labId, List<Long> scope) {
        if (scope == null || scope.isEmpty()) return labId;
        if (labId == null) return null;
        if (!scope.contains(labId)) {
            return null;
        }
        return labId;
    }

    private void validateLabOwnership(Long labId) {
        if (labId == null) throw new BizException(400, "设备必须归属某个实验室");
        List<Long> myLabs = DataScopeUtil.getLabIdsForLabAdmin();
        if (myLabs == null) return;
        if (myLabs.isEmpty() || !myLabs.contains(labId)) {
            throw new BizException(403, "无权操作其他实验室的设备");
        }
    }

    public List<LabDevice> listAll() {
        List<Long> scope = DataScopeUtil.getLabIdsForTeacher();
        return deviceMapper.findAll(scope);
    }
}
