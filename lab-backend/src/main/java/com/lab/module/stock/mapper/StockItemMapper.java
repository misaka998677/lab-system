package com.lab.module.stock.mapper;

import com.lab.module.stock.entity.StockItem;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface StockItemMapper {
    List<StockItem> page(@Param("keyword") String keyword,
                         @Param("labId") Long labId,
                         @Param("warningOnly") Integer warningOnly,
                         @Param("scopeLabIds") List<Long> scopeLabIds);

    StockItem findById(@Param("id") Long id);

    int insert(StockItem item);

    int update(StockItem item);

    int deleteById(@Param("id") Long id);

    int updateQty(@Param("id") Long id, @Param("qty") Integer qty);

    List<StockItem> warningList(@Param("limit") Integer limit,
                                @Param("scopeLabIds") List<Long> scopeLabIds);

    int countWarning(@Param("scopeLabIds") List<Long> scopeLabIds);

    List<StockItem> findAll(@Param("scopeLabIds") List<Long> scopeLabIds);
}
