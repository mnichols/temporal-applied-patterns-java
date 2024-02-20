package io.temporal.applied.patterns.cachingdataconverter.temporal;

import io.temporal.common.converter.JacksonJsonPayloadConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class CacheConfig {
    @Bean
    public RedisTemplate<?, ?> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<?, ?> template = new RedisTemplate<>();

        // this is vital to perform ops against the keys directly downstream (SpringBoot cache prepends keys with binary cache bits)
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setConnectionFactory(connectionFactory);

        return template;
    }

    @Bean
    public CachingPayloadConverter cachingPayloadConverter(RedisTemplate<String, CacheablePayload> template) {
        return new RedisCachingPayloadConverterImpl(template, new JacksonJsonPayloadConverter());
    }
//
//    @Bean
//    public CachingPayloadCodec cachingPayloadCodec(RedisTemplate<String, CacheablePayload> template, CachingPayloadConverter converter) {
//        return new CachingPayloadCodecImpl(template, converter);
//    }
    @Bean
    public CacheCleaner cacheCleaner(RedisTemplate<String, CacheablePayload> template) {
        return new RedisCacheCleanerImpl(template);
    }
}
