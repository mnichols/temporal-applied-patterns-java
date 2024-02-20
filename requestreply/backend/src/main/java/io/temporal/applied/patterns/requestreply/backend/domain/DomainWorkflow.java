package io.temporal.applied.patterns.requestreply.backend.domain;

import io.temporal.applied.patterns.requestreply.messaging.StartDomainWorkflowRequest;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface DomainWorkflow {
    @WorkflowMethod
    void execute(StartDomainWorkflowRequest params);
}
