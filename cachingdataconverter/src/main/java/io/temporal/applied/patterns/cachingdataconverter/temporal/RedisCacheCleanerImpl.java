package io.temporal.applied.patterns.cachingdataconverter.temporal;

import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ActivityImpl(taskQueues = {"${spring.task-queues.domain}"})
public class RedisCacheCleanerImpl implements CacheCleaner
{
    private static final Duration RETENTION_POLICY_DURATION = Duration.ofDays(30);
    Logger logger = LoggerFactory.getLogger(RedisCacheCleanerImpl.class);
    RedisTemplate<String, CacheablePayload> template;

    public RedisCacheCleanerImpl(RedisTemplate<String, CacheablePayload> template) {
        this.template = template;
    }


    @Override
    public void markEvictable(MarkEvictableRequest req) {
        logger.info("CLEAN REQUEST: {}/{}/{}", req.getNamespace(), req.getWorkflowType(), req.getWorkflowId());
        try {
            Boolean expire = template.expire(req.getWorkflowId(), RETENTION_POLICY_DURATION);
            logger.info("SET TTL on {}: {}", req.getWorkflowId(), expire);
        } catch (Exception e) {
            logger.error("failed to delete records for", e);
            throw e;
        }

    }
}
