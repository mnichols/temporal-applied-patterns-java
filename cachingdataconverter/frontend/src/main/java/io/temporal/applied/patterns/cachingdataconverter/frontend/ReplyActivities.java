package io.temporal.applied.patterns.cachingdataconverter.frontend;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.applied.patterns.cachingdataconverter.messaging.DomainReply;

@ActivityInterface
public interface ReplyActivities {
    @ActivityMethod
    void handleDomainReply(DomainReply reply);
}
