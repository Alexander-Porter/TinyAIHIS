package com.tinyhis.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus Configuration
 * 
 * Enables:
 * - Optimistic Locking: For flash sale protection in appointment booking
 * - Pagination: For paginated queries
 */
@Configuration
public class MybatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        
        // Optimistic locking plugin - prevents overselling in concurrent booking scenarios
        // When multiple users try to book the last slot, only one succeeds
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor());
        
        // Pagination plugin
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor());
        
        return interceptor;
    }
}
