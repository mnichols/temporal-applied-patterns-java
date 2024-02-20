package io.temporal.applied.patterns.requestreply.frontend.channels;

import io.temporal.applied.patterns.requestreply.backend.domain.DomainWorkflow;
import io.temporal.applied.patterns.requestreply.messaging.DomainReply;
import io.temporal.applied.patterns.requestreply.messaging.ReplySpec;
import io.temporal.applied.patterns.requestreply.messaging.StartDomainWorkflowRequest;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Controller
public class APIController {
    @Autowired
    WorkflowClient temporalClient;
    @Autowired
    Replies<DomainReply> replies;

    @Value("${spring.application.task-queues.replies}")
    private String repliesTaskQueue;

    @Value("${spring.application.task-queues.domain}")
    private String domainTaskQueue;

    @PostMapping(
            value = "/workflows",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.TEXT_HTML_VALUE})
    ResponseEntity post(@RequestBody WorkflowParamsRequest params) {

        WorkflowOptions workflowOptions =
                WorkflowOptions.newBuilder()
                        .setTaskQueue(domainTaskQueue)
                        .setWorkflowId(params.getWorkflowId())
                        // you will rarely ever want to set this on WorkflowOptions
                        // .setRetryOptions()
                        .build();
        StartDomainWorkflowRequest cmd = new StartDomainWorkflowRequest();
        cmd.prefix = "ok";
        if(params.getReplyTimeoutSecs() > 0) {
            cmd.replySpec = new ReplySpec();
            cmd.replySpec.taskQueue = repliesTaskQueue;
            cmd.replySpec.activityName = "HandleDomainReply";
        }
        if(params.shouldSimulateValidationFailure()) {
            cmd.prefix = "notok";
        }

        CountDownLatch latch = new CountDownLatch(1);
        DomainWorkflow workflow = temporalClient.newWorkflowStub(DomainWorkflow.class, workflowOptions);
        replies.putLatch(params.getWorkflowId(), latch);
        CompletableFuture<Void> execution = WorkflowClient.execute(workflow::execute, cmd);
        try {
            if(params.getReplyTimeoutSecs() > 0) {
                // wait for 60 seconds to unblock our thread (reply has been received)
                boolean replyReceived = latch.await(params.getReplyTimeoutSecs(), TimeUnit.SECONDS);
                if(!replyReceived) {
                    String message = String.format("reply was not received in %d seconds, but the workflow '%s' has continued",
                            params.getReplyTimeoutSecs(),
                            params.getWorkflowId());
                    // bypass thymeleaf, don't return template name just result
                    return new ResponseEntity<>("\"" + message + "\"", HttpStatus.REQUEST_TIMEOUT);
                }
            } else {
                latch.await();
            }
        } catch (InterruptedException e) {
            // handle with an error status code
            throw new RuntimeException(e);
        }
        DomainReply reply = replies.obtainReply(params.getWorkflowId());
        String message = "workflow '%s' replied with value '%s' validated as '%b'";
        String out = String.format(message, params.getWorkflowId(),
                reply.value,
                reply.businessValidationSucceeded
        );
        HttpStatus status = reply.businessValidationSucceeded ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
        // bypass thymeleaf, don't return template name just result
        return new ResponseEntity<>("\"" + out + "\"", status);
    }
}
