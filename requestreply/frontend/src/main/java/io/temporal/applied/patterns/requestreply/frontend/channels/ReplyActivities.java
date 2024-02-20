package io.temporal.applied.patterns.requestreply.frontend.channels;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.applied.patterns.requestreply.messaging.DomainReply;

@ActivityInterface
public interface ReplyActivities {
    @ActivityMethod
    void handleDomainReply(DomainReply reply);
}
