package org.springframework.samples.petclinic.customers.web;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Preserve Boot 2 endpoint matching during the platform-only migration. */
@Configuration(proxyBeanMethods = false)
class LegacyPathConfiguration implements WebMvcConfigurer {
    @Override
    @SuppressWarnings("deprecation")
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseTrailingSlashMatch(true);
    }
}
