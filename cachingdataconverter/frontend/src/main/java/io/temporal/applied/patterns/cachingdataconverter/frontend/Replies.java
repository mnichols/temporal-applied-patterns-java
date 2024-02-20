package io.temporal.applied.patterns.cachingdataconverter.frontend;

import java.util.concurrent.CountDownLatch;

public interface Replies<T> {
    void putLatch(String id, CountDownLatch latch);
    T obtainReply(String id);
    void putReply(String id, T reply);
}
