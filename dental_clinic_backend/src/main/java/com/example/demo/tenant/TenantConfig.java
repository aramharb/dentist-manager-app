package com.example.demo.tenant;

import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TenantConfig {
    @Bean
    public HibernatePropertiesCustomizer cabinetTenantCustomizer() {
        return properties -> properties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER,
                new CabinetTenantResolver());
    }
}
