package io.temporal.applied.patterns.requestreply.backend.domain;

import io.temporal.activity.ActivityOptions;
import io.temporal.applied.patterns.requestreply.messaging.DomainReply;
import io.temporal.applied.patterns.requestreply.messaging.StartDomainWorkflowRequest;
import io.temporal.common.RetryOptions;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.ActivityStub;
import io.temporal.workflow.Promise;
import io.temporal.workflow.Workflow;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@WorkflowImpl(taskQueues = {"${spring.task-queues.domain}"})
public class DomainWorkflowImpl implements DomainWorkflow {
    DomainActivities acts = Workflow.newActivityStub(DomainActivities.class, ActivityOptions.newBuilder()
            .setStartToCloseTimeout(Duration.ofSeconds(5)).build());

    @Override
    public void execute(StartDomainWorkflowRequest params) {
        DomainWorkflowState state = new DomainWorkflowState();

        state.validationSucceeded = acts.validate(params.prefix);
        List<Promise<Void>> replyPromises = new ArrayList<>();
        if(params.replySpec != null && !Objects.equals(params.replySpec.taskQueue,"") && !Objects.equals(params.replySpec.activityName,"")) {
            ActivityStub replyActivity = Workflow.newUntypedActivityStub(
                    ActivityOptions.newBuilder().
                            setTaskQueue(params.replySpec.taskQueue).
                            setStartToCloseTimeout(Duration.ofSeconds(10)).
                            setRetryOptions(RetryOptions.newBuilder().setMaximumAttempts(3).build()).
                            build());
            DomainReply reply = new DomainReply();
            reply.businessValidationSucceeded = state.validationSucceeded;
            reply.value = String.format("%s-%s", params.prefix, Workflow.getInfo().getWorkflowId());
            // let's not block on this. just reply back with the state we have accumulated so far
            replyPromises.add(replyActivity.executeAsync(params.replySpec.activityName, Void.class, reply));
        }
        if(!replyPromises.isEmpty()) {
            Promise.allOf(replyPromises).get();
        }
    }
}
