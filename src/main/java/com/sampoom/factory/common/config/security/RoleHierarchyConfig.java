package com.sampoom.factory.common.config.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.access.hierarchicalroles.RoleHierarchyImpl;

@Configuration
public class RoleHierarchyConfig {
    @Bean
    public RoleHierarchy roleHierarchy() {
        return RoleHierarchyImpl.fromHierarchy("""
                ROLE_ADMIN > ROLE_MD
                ROLE_ADMIN > ROLE_SALES
                ROLE_ADMIN > ROLE_INVENTORY
                ROLE_ADMIN > ROLE_PRODUCTION
                ROLE_ADMIN > ROLE_PURCHASE
                ROLE_ADMIN > ROLE_HR
                ROLE_ADMIN > ROLE_AGENCY
                ROLE_MD > ROLE_USER
                ROLE_SALES > ROLE_USER
                ROLE_INVENTORY > ROLE_USER
                ROLE_PRODUCTION > ROLE_USER
                ROLE_PURCHASE > ROLE_USER
                ROLE_HR > ROLE_USER
                ROLE_AGENCY > ROLE_USER
                """);
    }
}