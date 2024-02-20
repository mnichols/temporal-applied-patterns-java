package io.temporal.applied.patterns.requestreply.backend.domain;

import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;

@ActivityInterface
public interface DomainActivities {
    @ActivityMethod
    boolean validate(String prefix);
}
