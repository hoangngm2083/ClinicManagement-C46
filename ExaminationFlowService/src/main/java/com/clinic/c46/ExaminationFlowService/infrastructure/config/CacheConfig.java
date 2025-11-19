package com.clinic.c46.ExaminationFlowService.infrastructure.config;

import org.ehcache.config.builders.ExpiryPolicyBuilder;
import org.springframework.boot.autoconfigure.cache.JCacheManagerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.cache.configuration.MutableConfiguration;
import java.time.Duration;

@Configuration
public class CacheConfig {

    // 1. Đây chính là "biến" tái sử dụng của bạn
    private static final String DOMAIN_PACKAGE = "com.clinic.c46.ExaminationFlowService.domain.view";

    // 2. Tạo một cấu hình cache mẫu
    private MutableConfiguration<Object, Object> createDefaultConfig() {
        return new MutableConfiguration<>().setStoreByValue(false) // Quan trọng cho Hibernate
                .setExpiryPolicyFactory(
                        () -> (javax.cache.expiry.ExpiryPolicy) ExpiryPolicyBuilder.timeToIdleExpiration(
                                Duration.ofMinutes(10)))
                .setStatisticsEnabled(true);
    }

    // 3. Sử dụng Bean này để thêm các cache region
    @Bean
    public JCacheManagerCustomizer jCacheManagerCustomizer() {
        return cacheManager -> {

            // Lấy cấu hình Ehcache mặc định (tương đương <cache-template> trong XML)
            javax.cache.configuration.Configuration<Object, Object> defaultCacheConfig = createDefaultConfig();

            // 4. Dùng "biến" để định nghĩa các region
            cacheManager.createCache(DOMAIN_PACKAGE + ".PackageRepView", defaultCacheConfig);
            cacheManager.createCache(DOMAIN_PACKAGE + ".ServiceRepView", defaultCacheConfig);
            cacheManager.createCache(DOMAIN_PACKAGE + ".PackageRepView.services", defaultCacheConfig);

            // Region cho Query Cache
            cacheManager.createCache("org.hibernate.cache.queries", defaultCacheConfig);
        };
    }
}