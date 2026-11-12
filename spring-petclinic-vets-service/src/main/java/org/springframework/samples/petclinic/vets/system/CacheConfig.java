/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.samples.petclinic.vets.system;

import java.time.Duration;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Cache could be disable in unit test.
 * @author Maciej Szarlinski
 */
@Configuration
@EnableCaching
@Profile("production")
class CacheConfig {

    @Bean
    CacheManager cacheManager(VetsProperties properties) {
        VetsProperties.Cache settings = properties.getCache();
        CaffeineCacheManager manager = new CaffeineCacheManager("vets");
        manager.setCaffeine(Caffeine.newBuilder()
            .expireAfterWrite(Duration.ofSeconds(settings.getTtl()))
            .maximumSize(settings.getHeapSize())
            .recordStats());
        return manager;
    }
}
