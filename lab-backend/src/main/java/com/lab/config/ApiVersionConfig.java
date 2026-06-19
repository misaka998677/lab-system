package com.lab.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * API 版本配置
 *
 * <p>用于管理 API 版本前缀，支持环境变量配置。
 * 当前版本: v1
 * 访问路径: /api/v1/xxx
 */
@Configuration
public class ApiVersionConfig {

    @Value("${lab.api.version:v1}")
    private String version;

    /**
     * 获取 API 版本前缀路径
     *
     * @return 如 "/v1"，不带版本时返回 ""
     */
    public String getVersionPrefix() {
        if (version == null || version.isEmpty()) {
            return "";
        }
        return "/" + version;
    }

    /**
     * 获取完整的 context-path（包含版本前缀）
     *
     * @return 如 "/api/v1"
     */
    public String getContextPath() {
        return "/api" + getVersionPrefix();
    }

    public String getVersion() {
        return version;
    }
}
