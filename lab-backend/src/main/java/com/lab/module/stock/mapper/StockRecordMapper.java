package com.lab.module.stock.mapper;

import com.lab.module.stock.entity.StockRecord;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.util.Map;

public interface StockRecordMapper {
    List<StockRecord> page(@Param("itemId") Long itemId,
                           @Param("type") Integer type,
                           @Param("reservationId") Long reservationId,
                           @Param("userId") Long userId,
                           @Param("scopeLabIds") List<Long> scopeLabIds);

    int insert(StockRecord record);

    /** 检查指定耗材是否有出入库记录。 */
    int countByItemId(@Param("itemId") Long itemId);

    /** 入库总量（统计用）。 */
    Integer sumInQty(@Param("scopeLabIds") List<Long> scopeLabIds);

    /** 出库总量（统计用）。 */
    Integer sumOutQty(@Param("scopeLabIds") List<Long> scopeLabIds);

    /**
     * 出库 Top N 耗材（按累计出库数量倒序）。
     *
     * <p>每条记录为一个 LinkedHashMap，键为：
     * <ul>
     *   <li>{@code itemId}   - 耗材 ID</li>
     *   <li>{@code itemCode} - 耗材编号</li>
     *   <li>{@code itemName} - 耗材名称</li>
     *   <li>{@code unit}     - 计量单位</li>
     *   <li>{@code qty}      - 累计出库数量</li>
     * </ul>
     */
    List<Map<String, Object>> topUsage(@Param("limit") Integer limit,
                                       @Param("scopeLabIds") List<Long> scopeLabIds);

    List<StockRecord> findAll(@Param("scopeLabIds") List<Long> scopeLabIds);
}
