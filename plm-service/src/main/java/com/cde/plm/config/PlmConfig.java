package com.cde.plm.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.*;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.*;
import org.springframework.web.client.RestTemplate;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
public class PlmConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    @Bean
    public RestTemplate restTemplate() { return new RestTemplate(); }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory cf) {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        RedisCacheConfiguration defaults = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(mapper)))
                .disableCachingNullValues();
        Map<String, RedisCacheConfiguration> configs = new HashMap<>();
        configs.put("bom-tree", defaults.entryTtl(Duration.ofMinutes(30)));
        configs.put("phases", defaults.entryTtl(Duration.ofHours(6)));
        return RedisCacheManager.builder(cf).cacheDefaults(defaults)
                .withInitialCacheConfigurations(configs).build();
    }
}
