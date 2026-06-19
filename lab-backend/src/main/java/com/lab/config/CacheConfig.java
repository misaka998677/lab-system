package com.lab.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

/**
 * Caffeine 缓存配置。
 * 统一维护业务使用的命名缓存：menu_tree / dept_tree / user_info / stat_overview / stat_usage。
 * 业务代码使用 @Cacheable(value = CacheNames.STAT_OVERVIEW, ...) 即可命中。
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /** 业务命名缓存——集中声明，避免业务中散落字符串。 */
    public static final class CacheNames {
        public static final String MENU_TREE    = "menu_tree";
        public static final String DEPT_TREE    = "dept_tree";
        public static final String USER_INFO    = "user_info";
        public static final String STAT_OVERVIEW = "stat_overview";
        public static final String STAT_USAGE   = "stat_usage";
        public static final String STAT_FAULT   = "stat_fault";
        public static final String STAT_STOCK   = "stat_stock";

        private CacheNames() {}
    }

    @Bean
    public CacheManager caffeineCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.setCaffeine(Caffeine.newBuilder()
                .initialCapacity(32)
                .maximumSize(2000)
                .expireAfterWrite(30, TimeUnit.SECONDS)
                .recordStats());
        manager.setCacheNames(Arrays.asList(
                CacheNames.MENU_TREE,
                CacheNames.DEPT_TREE,
                CacheNames.USER_INFO,
                CacheNames.STAT_OVERVIEW,
                CacheNames.STAT_USAGE,
                CacheNames.STAT_FAULT,
                CacheNames.STAT_STOCK
        ));
        return manager;
    }
}
