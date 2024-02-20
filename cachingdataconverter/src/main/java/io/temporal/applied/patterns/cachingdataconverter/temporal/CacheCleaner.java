package io.temporal.applied.patterns.cachingdataconverter.temporal;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface CacheCleaner {
    @ActivityMethod
    void markEvictable(MarkEvictableRequest req);
}
