package com.hus.mim_backend.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
@EnableConfigurationProperties(AppRedisProperties.class)
public class RedisCacheConfig {
    private static final Logger log = LoggerFactory.getLogger(RedisCacheConfig.class);

    @Bean
    public RedisConnectionFactory redisConnectionFactory(AppRedisProperties properties) {
        RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration();
        configuration.setHostName(properties.getHost());
        configuration.setPort(properties.getPort());
        if (StringUtils.hasText(properties.getPassword())) {
            configuration.setPassword(RedisPassword.of(properties.getPassword()));
        }
        return new LettuceConnectionFactory(configuration);
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper());
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                .disableCachingNullValues()
                .entryTtl(Duration.ofMinutes(5))
                .computePrefixWith((cacheName) -> "mim:v2:" + cacheName + ":");

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        cacheConfigurations.put(CacheNames.PUBLIC_POSTS, defaultConfig.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put(CacheNames.PUBLIC_RESEARCH_PAPERS, defaultConfig.entryTtl(Duration.ofMinutes(60)));
        cacheConfigurations.put(CacheNames.PUBLIC_NEWS, defaultConfig.entryTtl(Duration.ofMinutes(2)));
        cacheConfigurations.put(CacheNames.PUBLIC_POST_DETAILS, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put(CacheNames.PUBLIC_RESEARCH_PAPER_DETAILS, defaultConfig.entryTtl(Duration.ofHours(1)));
        cacheConfigurations.put(CacheNames.PUBLIC_NEWS_DETAILS, defaultConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put(CacheNames.PUBLIC_RESEARCH_CATEGORIES, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(CacheNames.RESEARCH_CATEGORIES_ALL, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(CacheNames.PUBLIC_SPECIALIZATIONS, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(CacheNames.SPECIALIZATIONS_ALL, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(CacheNames.PUBLIC_RECRUITMENT_CATEGORIES, defaultConfig.entryTtl(Duration.ofMinutes(10)));
        cacheConfigurations.put(CacheNames.RECRUITMENT_CATEGORIES_ALL, defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(redisObjectMapper());
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(serializer);
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public CacheErrorHandler cacheErrorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis cache GET failed for cache={} key={}. Falling back to source.",
                        safeCacheName(cache),
                        key,
                        exception);
            }

            @Override
            public void handleCachePutError(RuntimeException exception, Cache cache, Object key, Object value) {
                log.warn("Redis cache PUT failed for cache={} key={}. Skipping cache write.",
                        safeCacheName(cache),
                        key,
                        exception);
            }

            @Override
            public void handleCacheEvictError(RuntimeException exception, Cache cache, Object key) {
                log.warn("Redis cache EVICT failed for cache={} key={}.",
                        safeCacheName(cache),
                        key,
                        exception);
            }

            @Override
            public void handleCacheClearError(RuntimeException exception, Cache cache) {
                log.warn("Redis cache CLEAR failed for cache={}.",
                        safeCacheName(cache),
                        exception);
            }

            private String safeCacheName(Cache cache) {
                return cache == null ? "unknown" : cache.getName();
            }
        };
    }

    private ObjectMapper redisObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        objectMapper.activateDefaultTypingAsProperty(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.EVERYTHING,
                "@class"
        );
        return objectMapper;
    }
}
