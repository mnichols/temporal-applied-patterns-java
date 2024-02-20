package io.temporal.applied.patterns.cachingdataconverter.temporal;

import io.temporal.api.common.v1.Payload;
import io.temporal.common.converter.PayloadConverter;
import io.temporal.payload.codec.PayloadCodec;

public interface CachingPayloadCodec extends PayloadCodec {
    // extractPayload allows for Payload to be pulled from cache and properly returning the underlying Payload.
    // this makes this function useful for hosting in an HTTP server to fetch payloads to the UI
   // Payload extractPayload(Payload source);
    // storePayload stores the "real" data the application needs, given the context, and returns the Payload
    // that should be sent across the wire to Temporal
//    Payload storePayload(Payload actual, CacheContextInfo cacheContextInfo);
}
