package io.temporal.applied.patterns.cachingdataconverter.temporal;

import com.google.protobuf.ByteString;
import io.temporal.api.common.v1.Payload;
import io.temporal.common.converter.DataConverterException;
import io.temporal.common.converter.EncodingKeys;
import io.temporal.common.converter.PayloadConverter;
import io.temporal.payload.context.HasWorkflowSerializationContext;
import io.temporal.payload.context.SerializationContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

// RedisDataModel
// key: <workflowId>
// value: Hash{ <cacheKey> : <convertedPayloadByteArray> }
@Component
public class RedisCachingPayloadConverterImpl implements CachingPayloadConverter {
    Logger logger = LoggerFactory.getLogger(RedisCachingPayloadConverterImpl.class);
    private PayloadConverter inner ;
    private RedisTemplate<String, CacheablePayload> template;
    private CacheContextInfo cacheContextInfo;
    static final ByteString METADATA_ENCODING =
            ByteString.copyFrom("cached/plain", StandardCharsets.UTF_8);

    static final String METADATA_CACHE_KEY_ID_KEY = "cache-key";
    static final String METADATA_WORKFLOW_ID_KEY = "workflow-id";
    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    public RedisCachingPayloadConverterImpl(RedisTemplate<String, CacheablePayload> template, PayloadConverter inner) {

        this.inner = inner;
        this.template = template;
    }

    public RedisCachingPayloadConverterImpl(PayloadConverter inner, RedisTemplate<String, CacheablePayload> template, CacheContextInfo cacheContextInfo) {
        this.inner = inner;
        this.template = template;
        this.cacheContextInfo = cacheContextInfo;
    }

    @Override
    public String getEncodingType() {
        return "cached/plain";
    }

    @Override
    public Optional<Payload> toData(Object obj) throws DataConverterException {
        if(cacheContextInfo == null ){
            throw new DataConverterException("caching requires context");
        }
        Optional<Payload> out = inner.toData(obj);
        // nothing to do here
        return out.map(actual -> storePayload(actual, cacheContextInfo));
    }

    @Override
    public <T> T fromData(Payload content, Class<T> valueType, Type valueGenericType) throws DataConverterException {
        try {
            Payload extracted = extractPayload(content);
            return inner.fromData(extracted, valueType, valueGenericType);
        } catch( Throwable e) {
            throw new DataConverterException(e);
        }
    }


    @Override
    public Payload extractPayload(Payload source) {
        if (!METADATA_ENCODING.equals(source.getMetadataOrDefault(EncodingKeys.METADATA_ENCODING_KEY, null))) {
            return source;
        }

        String key;
        String workflowId;
        try {
            // we are using the metadata as the cache key instead of the payload body to obtain the key
            key = source.getMetadataOrThrow(METADATA_CACHE_KEY_ID_KEY).toString(UTF_8);
        } catch (Exception e) {
            throw new DataConverterException(e);
        }
        try {
            workflowId = source.getMetadataOrThrow(METADATA_WORKFLOW_ID_KEY).toString(UTF_8);
        } catch (Exception e) {
            throw new DataConverterException(e);
        }
        Object cacheValue = null;
        try {
            cacheValue = template.opsForHash().get(workflowId, key);
        } catch (Exception e) {
            throw new DataConverterException("failed to get item from cache", e);
        }

        if (cacheValue == null) {
            throw new DataConverterException("cannot find payload");
        }
        try {
            CacheablePayload cachedPayload = (CacheablePayload) cacheValue;
            byte[] plainData;
            Payload decryptedPayload;
            plainData = cachedPayload.getBytes();
            decryptedPayload = Payload.parseFrom(plainData);
            return decryptedPayload;
        } catch (Throwable e) {
            logger.error("failed to extract from source", e);
            throw new DataConverterException("failed to extract from source", e);
        }
    }

    @Override
    public Payload storePayload(Payload actual, CacheContextInfo cacheContextInfo) {
        if(this.cacheContextInfo == null) {
            throw new DataConverterException("cacheContextInfo is required");
        }
        try {
            String cacheKey = UUID.randomUUID().toString();
            CacheablePayload cacheablePayload = new CacheablePayload(cacheKey, actual.toByteArray());
            logger.debug("setting payload for {}", cacheKey);
            template.opsForHash().put(cacheContextInfo.getWorkflowId(), cacheKey, cacheablePayload);
            Optional<Payload> out = inner.toData(cacheKey);
            return out.map(value -> Payload.newBuilder().
                    putMetadata(EncodingKeys.METADATA_ENCODING_KEY, METADATA_ENCODING).
                    putMetadata(METADATA_CACHE_KEY_ID_KEY, ByteString.copyFromUtf8(cacheKey)).
                    putMetadata(METADATA_WORKFLOW_ID_KEY, ByteString.copyFromUtf8(this.cacheContextInfo.getWorkflowId())).
                    setData(value.getData()).build()).orElse(null);
        } catch(Exception e) {
            logger.error("failed to cache payload", e);
            throw new DataConverterException("failed to cache payload", e);
        }
    }

    @Nonnull
    @Override
    public PayloadConverter withContext(@Nonnull SerializationContext context) {
        if(context instanceof HasWorkflowSerializationContext) {
            HasWorkflowSerializationContext wfContext = ((HasWorkflowSerializationContext) context);
            return new RedisCachingPayloadConverterImpl(inner, template, new CacheContextInfo(wfContext.getNamespace(), wfContext.getWorkflowId()));
        }
        return CachingPayloadConverter.super.withContext(context);
    }
}
