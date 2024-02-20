package io.temporal.applied.patterns.cachingdataconverter.backend;

import io.temporal.applied.patterns.cachingdataconverter.messaging.StartDomainWorkflowRequest;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface DomainWorkflow {
    @WorkflowMethod
    void execute(StartDomainWorkflowRequest params);
}
