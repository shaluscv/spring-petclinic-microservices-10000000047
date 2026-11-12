package org.springframework.samples.petclinic.vets.web;

import java.util.List;
import java.util.concurrent.TimeUnit;
import com.github.benmanes.caffeine.cache.Cache;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.samples.petclinic.vets.model.VetRepository;
import org.springframework.samples.petclinic.vets.system.VetsProperties;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.cloud.config.enabled=false", "eureka.client.enabled=false",
    "spring.sql.init.mode=always", "spring.sql.init.schema-locations=classpath:db/hsqldb/schema.sql",
    "spring.sql.init.data-locations=classpath:db/hsqldb/data.sql",
    "spring.jpa.hibernate.ddl-auto=none", "vets.cache.ttl=2", "vets.cache.heap-size=2"})
@AutoConfigureMockMvc
@ActiveProfiles("production")
class ProductionCacheTest {
    @Autowired MockMvc mvc;
    @Autowired CacheManager manager;
    @Autowired VetsProperties properties;
    @MockBean VetRepository repository;

    @Test
    void cachesReadsAndHonorsConfiguredLimits() throws Exception {
        when(repository.findAll()).thenReturn(List.of());
        CaffeineCache springCache = (CaffeineCache) manager.getCache("vets");
        assertThat(springCache).isNotNull();
        Cache<Object, Object> cache = springCache.getNativeCache();
        cache.invalidateAll();
        assertThat(properties.getCache().getTtl()).isEqualTo(2);
        assertThat(cache.policy().eviction().orElseThrow().getMaximum()).isEqualTo(2);
        assertThat(cache.policy().expireAfterWrite().orElseThrow().getExpiresAfter(TimeUnit.SECONDS)).isEqualTo(2);
        mvc.perform(get("/vets")).andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(get("/vets")).andExpect(status().isOk());
        verify(repository, times(1)).findAll();
        cache.policy().expireAfterWrite().orElseThrow().setExpiresAfter(0, TimeUnit.NANOSECONDS);
        cache.cleanUp();
        mvc.perform(get("/vets")).andExpect(status().isOk());
        verify(repository, times(2)).findAll();
        cache.put("a", 1);
        cache.put("b", 2);
        cache.put("c", 3);
        cache.cleanUp();
        assertThat(cache.estimatedSize()).isLessThanOrEqualTo(2);
    }
}
