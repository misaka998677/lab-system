package com.lab.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Component;

/**
 * 统一的缓存失效入口。
 *
 * <p>业务写操作完成后，调用本类对应方法触发统计缓存失效。
 * 使用方式：在需要触发失效的 Controller / Service 中 @Autowired StatCacheInvalidator inv，
 * 然后在写操作完成后调用 inv.evictOverview() / inv.evictAll() 等。</p>
 */
@Component
public class StatCacheInvalidator {

    private static final Logger log = LoggerFactory.getLogger(StatCacheInvalidator.class);

    @CacheEvict(value = "stat_overview", allEntries = true)
    public void evictOverview() { log.info("[CACHE-EVICT] 清除 stat_overview 缓存（所有条目）"); }

    @CacheEvict(value = "stat_usage", allEntries = true)
    public void evictUsage() { log.info("[CACHE-EVICT] 清除 stat_usage 缓存（所有条目）"); }

    @CacheEvict(value = "stat_fault", allEntries = true)
    public void evictFault() { log.info("[CACHE-EVICT] 清除 stat_fault 缓存（所有条目）"); }

    @CacheEvict(value = "stat_stock", allEntries = true)
    public void evictStock() { log.info("[CACHE-EVICT] 清除 stat_stock 缓存（所有条目）"); }

    @CacheEvict(value = {"stat_overview", "stat_usage", "stat_fault", "stat_stock"}, allEntries = true)
    public void evictAll() { log.info("[CACHE-EVICT] 清除所有统计缓存"); }
}
