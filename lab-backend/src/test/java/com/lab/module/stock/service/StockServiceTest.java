package com.lab.module.stock.service;

import com.lab.common.BizException;
import com.lab.module.stock.dto.StockRecordDTO;
import com.lab.module.stock.entity.StockItem;
import com.lab.module.stock.entity.StockRecord;
import com.lab.module.stock.mapper.StockItemMapper;
import com.lab.module.stock.mapper.StockRecordMapper;
import com.lab.websocket.StatPushService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;

class StockServiceTest {

    private FakeStockItemMapper itemMapper;
    private FakeStockRecordMapper recordMapper;
    private StockService service;

    @BeforeEach
    void setUp() {
        itemMapper = new FakeStockItemMapper();
        recordMapper = new FakeStockRecordMapper();
        service = new StockService(itemMapper, recordMapper, new NoopStatPushService());
    }

    @Test
    void stockInIncreasesQuantityAndWritesRecord() {
        StockItem item = newItem(1L, "C-0001", "网线", 10);
        itemMapper.save(item);

        StockRecordDTO dto = new StockRecordDTO();
        dto.setItemId(1L);
        dto.setQty(5);
        dto.setUserId(99L);
        dto.setRemark("补货");

        service.stockIn(dto);

        StockItem after = itemMapper.findById(1L);
        assertEquals(15, after.getQty(), "入库后库存应为 10+5=15");

        assertEquals(1, recordMapper.records.size(), "应写入一条流水");
        StockRecord rec = recordMapper.records.get(0);
        assertEquals(1, rec.getType(), "type=1 表示入库");
        assertEquals(1L, rec.getItemId());
        assertEquals(5, rec.getQty());
        assertEquals(99L, rec.getUserId());
        assertEquals("补货", rec.getRemark());
        assertNull(rec.getReservationId(), "未关联预约时 reservationId 为 null");
    }

    @Test
    void stockOutRejectsQuantityGreaterThanCurrentStock() {
        itemMapper.save(newItem(2L, "C-0002", "纸", 3));

        StockRecordDTO dto = new StockRecordDTO();
        dto.setItemId(2L);
        dto.setQty(5);
        dto.setUserId(7L);

        BizException ex = assertThrows(BizException.class, () -> service.stockOut(dto));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("库存不足"));

        assertEquals(3, itemMapper.findById(2L).getQty(), "库存不足时不应改动 qty");
        assertEquals(0, recordMapper.records.size(), "库存不足时不应写流水");
    }

    @Test
    void stockOutDecreasesQuantityAndLinksReservation() {
        itemMapper.save(newItem(3L, "C-0003", "电阻", 10));

        StockRecordDTO dto = new StockRecordDTO();
        dto.setItemId(3L);
        dto.setQty(4);
        dto.setUserId(8L);
        dto.setReservationId(123L);
        dto.setRemark("配合预约 R001");

        service.stockOut(dto);

        assertEquals(6, itemMapper.findById(3L).getQty(), "出库后库存应为 10-4=6");
        assertEquals(1, recordMapper.records.size());
        StockRecord rec = recordMapper.records.get(0);
        assertEquals(2, rec.getType(), "type=2 表示出库");
        assertEquals(3L, rec.getItemId());
        assertEquals(4, rec.getQty());
        assertEquals(8L, rec.getUserId());
        assertEquals(123L, rec.getReservationId());
        assertEquals("配合预约 R001", rec.getRemark());
    }

    // ------------ helpers ------------

    private static StockItem newItem(Long id, String code, String name, int qty) {
        StockItem i = new StockItem();
        i.setId(id);
        i.setCode(code);
        i.setName(name);
        i.setQty(qty);
        i.setWarnQty(2);
        return i;
    }

    static class FakeStockItemMapper implements StockItemMapper {
        final Map<Long, StockItem> store = new HashMap<>();
        final AtomicLong seq = new AtomicLong(1000);

        void save(StockItem i) { store.put(i.getId(), i); }

        @Override public List<StockItem> page(String keyword, Long labId, Integer warningOnly) {
            return new ArrayList<>(store.values());
        }
        @Override public StockItem findById(Long id) { return store.get(id); }
        @Override public int insert(StockItem item) {
            if (item.getId() == null) item.setId(seq.incrementAndGet());
            store.put(item.getId(), item);
            return 1;
        }
        @Override public int update(StockItem item) { store.put(item.getId(), item); return 1; }
        @Override public int deleteById(Long id) { return store.remove(id) == null ? 0 : 1; }
        @Override public int updateQty(Long id, Integer qty) {
            StockItem i = store.get(id);
            if (i == null) return 0;
            i.setQty(qty);
            return 1;
        }
        @Override public List<StockItem> warningList(Integer limit) { return new ArrayList<>(); }
        @Override public int countWarning() { return 0; }
    }

    static class FakeStockRecordMapper implements StockRecordMapper {
        final List<StockRecord> records = new ArrayList<>();
        final AtomicLong seq = new AtomicLong(1);

        @Override public List<StockRecord> page(Long itemId, Integer type, Long reservationId, Long userId) { return records; }
        @Override public int insert(StockRecord record) {
            record.setId(seq.getAndIncrement());
            records.add(record);
            return 1;
        }
        @Override public Integer sumInQty() { return null; }
        @Override public Integer sumOutQty() { return null; }
        @Override public List<Map<String, Object>> topUsage(Integer limit) { return new ArrayList<>(); }
    }

    static class NoopStatPushService extends StatPushService {
        @Override public void pushOverviewUpdate() {}
        @Override public void pushModuleUpdate(String module) {}
    }
}
