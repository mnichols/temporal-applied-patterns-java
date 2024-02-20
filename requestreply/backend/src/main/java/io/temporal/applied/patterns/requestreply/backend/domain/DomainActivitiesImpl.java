package io.temporal.applied.patterns.requestreply.backend.domain;

import io.temporal.spring.boot.ActivityImpl;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@ActivityImpl(taskQueues = {"${spring.task-queues.domain}"})
public class DomainActivitiesImpl implements DomainActivities {
    @Override
    public boolean validate(String prefix) {
        return Objects.equals(prefix, "ok");
    }
}
