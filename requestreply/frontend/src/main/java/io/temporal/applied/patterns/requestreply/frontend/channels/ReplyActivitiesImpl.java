package io.temporal.applied.patterns.requestreply.frontend.channels;

import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;
import io.temporal.applied.patterns.requestreply.messaging.DomainReply;
import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@ActivityImpl(taskQueues = "${spring.application.task-queues.replies}")
public class ReplyActivitiesImpl implements ReplyActivities {
    private static final Logger logger = LoggerFactory.getLogger(ReplyActivitiesImpl.class);
    @Autowired
    private Replies<DomainReply> replies;
    @Override
    public void handleDomainReply(DomainReply reply) {
        ActivityExecutionContext executionContext = Activity.getExecutionContext();
        String workflowId = executionContext.getInfo().getWorkflowId();
        logger.info("received workflow '{}' domain reply '{}'", workflowId, reply.value);
        replies.putReply(workflowId, reply);
    }
}
