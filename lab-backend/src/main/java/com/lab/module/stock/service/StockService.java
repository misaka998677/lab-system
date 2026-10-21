package com.lab.module.stock.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.lab.common.BizException;
import com.lab.common.PageResult;
import com.lab.common.SqlLikeUtil;
import com.lab.module.stock.dto.StockRecordDTO;
import com.lab.module.stock.entity.StockItem;
import com.lab.module.stock.entity.StockRecord;
import com.lab.module.stock.mapper.StockItemMapper;
import com.lab.module.stock.mapper.StockRecordMapper;
import com.lab.security.DataScopeUtil;
import com.lab.security.LoginUser;
import com.lab.security.SecurityUtil;
import com.lab.websocket.StatPushService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class StockService {

    private final StockItemMapper itemMapper;
    private final StockRecordMapper recordMapper;
    private final StatPushService statPushService;

    @Autowired
    public StockService(StockItemMapper itemMapper, StockRecordMapper recordMapper, @Lazy StatPushService sps) {
        this.itemMapper = itemMapper;
        this.recordMapper = recordMapper;
        this.statPushService = sps;
    }

    // ---------------- 档案 ----------------

    public PageResult<StockItem> itemPage(int pageNum, int pageSize,
                                          String keyword, Long labId, Integer warningOnly) {
        List<Long> scope = DataScopeUtil.getLabIdsForTeacher();
        Long filteredLabId = (labId != null && (scope == null || scope.isEmpty() || scope.contains(labId))) ? labId : null;
        PageHelper.startPage(pageNum, pageSize);
        return PageResult.of(new PageInfo<>(
            itemMapper.page(SqlLikeUtil.escape(keyword), filteredLabId, warningOnly, scope)));
    }

    public StockItem detail(Long id) {
        StockItem item = itemMapper.findById(id);
        if (item == null) return null;
        if (DataScopeUtil.isLabAdmin()) {
            List<Long> myLabs = DataScopeUtil.getLabIdsForLabAdmin();
            if (item.getLabId() == null || !myLabs.contains(item.getLabId())) {
                throw new BizException(403, "无权查看其他实验室的耗材");
            }
        }
        return item;
    }

    public Long create(StockItem item) {
        if (item.getQty() == null) item.setQty(0);
        if (item.getWarnQty() == null) item.setWarnQty(0);
        if (DataScopeUtil.isLabAdmin()) {
            if (item.getLabId() == null) {
                throw new BizException(400, "必须指定实验室");
            }
            List<Long> myLabs = DataScopeUtil.getLabIdsForLabAdmin();
            if (!myLabs.contains(item.getLabId())) {
                throw new BizException(403, "无权管理其他实验室的耗材");
            }
        }
        itemMapper.insert(item);
        return item.getId();
    }

    public void update(StockItem item) {
        if (DataScopeUtil.isLabAdmin()) {
            StockItem existing = itemMapper.findById(item.getId());
            if (existing == null) throw new BizException("耗材不存在");
            List<Long> myLabs = DataScopeUtil.getLabIdsForLabAdmin();
            if (existing.getLabId() == null || !myLabs.contains(existing.getLabId())) {
                throw new BizException(403, "无权修改其他实验室的耗材");
            }
        }
        itemMapper.update(item);
    }

    public void delete(Long id) {
        if (DataScopeUtil.isLabAdmin()) {
            StockItem existing = itemMapper.findById(id);
            if (existing == null) throw new BizException("耗材不存在");
            List<Long> myLabs = DataScopeUtil.getLabIdsForLabAdmin();
            if (existing.getLabId() == null || !myLabs.contains(existing.getLabId())) {
                throw new BizException(403, "无权删除其他实验室的耗材");
            }
        }
        itemMapper.deleteById(id);
    }

    // ---------------- 流水 ----------------

    public PageResult<StockRecord> recordPage(int pageNum, int pageSize,
                                              Long itemId, Integer type, Long reservationId) {
        List<Long> scope = DataScopeUtil.getLabIdsForTeacher();
        Long userId;
        if (DataScopeUtil.isAdmin()) {
            userId = null;
        } else if (DataScopeUtil.isLabAdmin()) {
            userId = null;
        } else {
            userId = SecurityUtil.currentUserId();
        }
        PageHelper.startPage(pageNum, pageSize);
        return PageResult.of(new PageInfo<>(recordMapper.page(itemId, type, reservationId, userId, scope)));
    }

    @Transactional
    public Long stockIn(StockRecordDTO dto) {
        StockItem item = validateForRecord(dto);
        int newQty = item.getQty() + dto.getQty();
        itemMapper.updateQty(item.getId(), newQty);

        StockRecord rec = buildRecord(dto, 1);
        recordMapper.insert(rec);
        statPushService.pushOverviewUpdate();
        return rec.getId();
    }

    @Transactional
    public Long stockOut(StockRecordDTO dto) {
        StockItem item = validateForRecord(dto);
        if (item.getQty() < dto.getQty()) {
            throw new BizException(400, "库存不足");
        }
        int newQty = item.getQty() - dto.getQty();
        itemMapper.updateQty(item.getId(), newQty);

        StockRecord rec = buildRecord(dto, 2);
        recordMapper.insert(rec);
        statPushService.pushOverviewUpdate();
        return rec.getId();
    }

    private StockItem validateForRecord(StockRecordDTO dto) {
        if (dto == null || dto.getItemId() == null) {
            throw new BizException(400, "耗材 id 不能为空");
        }
        if (dto.getQty() == null || dto.getQty() <= 0) {
            throw new BizException(400, "数量必须大于 0");
        }
        StockItem item = itemMapper.findById(dto.getItemId());
        if (item == null) throw new BizException(400, "耗材不存在");
        if (item.getQty() == null) item.setQty(0);
        if (DataScopeUtil.isLabAdmin()) {
            List<Long> myLabs = DataScopeUtil.getLabIdsForLabAdmin();
            if (item.getLabId() == null || !myLabs.contains(item.getLabId())) {
                throw new BizException(403, "无权操作其他实验室的耗材");
            }
        }
        return item;
    }

    private StockRecord buildRecord(StockRecordDTO dto, int type) {
        StockRecord rec = new StockRecord();
        rec.setItemId(dto.getItemId());
        rec.setType(type);
        rec.setQty(dto.getQty());
        rec.setReservationId(dto.getReservationId());
        rec.setUserId(dto.getUserId());
        rec.setRemark(dto.getRemark());
        return rec;
    }

    // ---------------- 统计 / 工作台 ----------------

    public List<StockItem> warningList(Integer limit) {
        List<Long> scope = DataScopeUtil.getLabIdsForTeacher();
        return itemMapper.warningList(limit, scope);
    }

    public int countWarning() {
        List<Long> scope = DataScopeUtil.getLabIdsForTeacher();
        return itemMapper.countWarning(scope);
    }

    public Integer sumInQty() {
        List<Long> scope = DataScopeUtil.getLabIdsForTeacher();
        Integer v = recordMapper.sumInQty(scope);
        return v == null ? 0 : v;
    }

    public Integer sumOutQty() {
        List<Long> scope = DataScopeUtil.getLabIdsForTeacher();
        Integer v = recordMapper.sumOutQty(scope);
        return v == null ? 0 : v;
    }

    public List<Map<String, Object>> topUsage(Integer limit) {
        List<Long> scope = DataScopeUtil.getLabIdsForTeacher();
        return recordMapper.topUsage(limit, scope);
    }

    public List<StockItem> itemListAll() {
        List<Long> scope = DataScopeUtil.getLabIdsForTeacher();
        return itemMapper.findAll(scope);
    }

    public List<StockRecord> recordListAll() {
        List<Long> scope = DataScopeUtil.getLabIdsForTeacher();
        return recordMapper.findAll(scope);
    }
}
