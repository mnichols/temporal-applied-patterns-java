package io.temporal.applied.patterns.cachingdataconverter.temporal;

import com.google.protobuf.ByteString;
import io.temporal.api.common.v1.Payload;
import io.temporal.common.converter.DataConverterException;
import io.temporal.common.converter.EncodingKeys;
import io.temporal.common.converter.PayloadConverter;
import io.temporal.payload.codec.PayloadCodec;
import io.temporal.payload.context.HasWorkflowSerializationContext;
import io.temporal.payload.context.SerializationContext;
import io.temporal.workflow.unsafe.WorkflowUnsafe;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

// RedisDataModel
// key: <workflowId>
// value: Hash{ <cacheKey> : <convertedPayloadByteArray> }
@Component
public class RedisCachingPayloadCodecImpl implements CachingPayloadCodec {
    Logger logger = LoggerFactory.getLogger(RedisCachingPayloadCodecImpl.class);
    private PayloadConverter inner ;
    private RedisTemplate<String, CacheablePayload> template;
    private CacheContextInfo cacheContextInfo;
    static final ByteString METADATA_ENCODING =
            ByteString.copyFrom("cached/plain", StandardCharsets.UTF_8);

    static final String METADATA_CACHE_KEY_ID_KEY = "cache-key";
    static final String METADATA_WORKFLOW_ID_KEY = "workflow-id";

    static final String METADATA_RUN_ID_KEY = "run-id";
    private static final Charset UTF_8 = StandardCharsets.UTF_8;

    public RedisCachingPayloadCodecImpl(RedisTemplate<String, CacheablePayload> template, PayloadConverter inner) {

        this.inner = inner;
        this.template = template;
    }

    public RedisCachingPayloadCodecImpl(PayloadConverter inner, RedisTemplate<String, CacheablePayload> template, CacheContextInfo cacheContextInfo) {
        this.inner = inner;
        this.template = template;
        this.cacheContextInfo = cacheContextInfo;
    }

    @Nonnull
    @Override
    public List<Payload> encode(@Nonnull List<Payload> payloads) {
        return payloads.stream().map(this::storeAndConvertPayload).collect(Collectors.toList());
    }

    @Nonnull
    @Override
    public List<Payload> decode(@Nonnull List<Payload> payloads) {
        return payloads.stream().map(this::retrieveActualPayload).collect(Collectors.toList());
    }

    private Payload retrieveActualPayload(Payload source) {
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
        var ref = new Object() {
            CacheablePayload cacheValue;
        };

        WorkflowUnsafe.deadlockDetectorOff(() -> {
            try {
                Object out = template.opsForHash().get(workflowId, key);
                if (out != null) {
                    ref.cacheValue = (CacheablePayload) out;
                }
            } catch(Exception e) {
                throw new DataConverterException("failed to fetch payload", e);
            }
        });

        if (ref.cacheValue == null) {
            throw new DataConverterException("cannot find payload");
        }
        try {
            CacheablePayload cachedPayload = ref.cacheValue;
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

    private Payload storeAndConvertPayload(Payload actual) {
        if(this.cacheContextInfo == null) {
            throw new DataConverterException("cacheContextInfo is required");
        }
        try {
            String cacheKey = UUID.randomUUID().toString();
            CacheablePayload cacheablePayload = new CacheablePayload(cacheKey, actual.toByteArray());
            logger.debug("setting payload for {}", cacheKey);

            WorkflowUnsafe.deadlockDetectorOff(() -> template.
                    opsForHash().
                    put(cacheContextInfo.getWorkflowId(), cacheKey, cacheablePayload));

            return Payload.newBuilder().
                    putMetadata(EncodingKeys.METADATA_ENCODING_KEY, METADATA_ENCODING).
                    putMetadata(METADATA_CACHE_KEY_ID_KEY, ByteString.copyFromUtf8(cacheKey)).
                    putMetadata(METADATA_WORKFLOW_ID_KEY, ByteString.copyFromUtf8(this.cacheContextInfo.getWorkflowId())).
                    build();
        } catch(Exception e) {
            logger.error("failed to cache payload", e);
            throw new DataConverterException("failed to cache payload", e);
        }
    }

    @Nonnull
    @Override
    public PayloadCodec withContext(@Nonnull SerializationContext context) {
        if(context instanceof HasWorkflowSerializationContext) {
            HasWorkflowSerializationContext wfContext = ((HasWorkflowSerializationContext) context);
            return new RedisCachingPayloadCodecImpl(inner,
                    template,
                    new CacheContextInfo(wfContext.getNamespace(), wfContext.getWorkflowId()));
        }

        return CachingPayloadCodec.super.withContext(context);
    }
}
