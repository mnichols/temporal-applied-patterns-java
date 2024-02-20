package io.temporal.applied.patterns.requestreply.frontend.channels;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

@Component
public class RepliesImpl<T> implements Replies<T> {
    private final Map<String, CountDownLatch> latches = new HashMap<>();
    private final Map<String, T> replies = new HashMap<>();

    @Override
    public void putLatch(String id, CountDownLatch latch) {
        if(latches.containsKey(id)) {
            throw new RuntimeException(String.format("latch with id %s already exists", id));
        }
        latches.put(id, latch);
    }

    @Override
    public T obtainReply(String id) {
        T out = replies.get(id);
        replies.remove(id);
        latches.remove(id);
        return out;
    }

    @Override
    public void putReply(String id, T reply) {
        replies.put(id, reply);

        if(latches.containsKey(id)) {
            CountDownLatch latch = latches.get(id);
            latches.get(id).countDown();
            if(latch.getCount()==0) {
                latches.remove(id);
            }
        }
    }
}
