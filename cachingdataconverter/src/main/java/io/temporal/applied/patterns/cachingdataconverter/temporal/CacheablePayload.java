package io.temporal.applied.patterns.cachingdataconverter.temporal;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serial;
import java.io.Serializable;
import java.time.Duration;


// Set TTL by default as one year. Use expire to override this
@RedisHash(value = "payloads", timeToLive = 86400 * 365 )
public class CacheablePayload implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    @Id
    private String id;
    private byte[] bytes;

    public CacheablePayload() {
    }

    public CacheablePayload(String id, byte[] bytes) {
        this.id = id;
        this.bytes = bytes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public byte[] getBytes() {
        return bytes;
    }

}
