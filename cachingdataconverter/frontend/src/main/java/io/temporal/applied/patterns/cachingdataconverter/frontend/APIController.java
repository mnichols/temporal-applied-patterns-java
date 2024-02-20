package io.temporal.applied.patterns.cachingdataconverter.frontend;

import com.google.protobuf.util.JsonFormat;
import io.temporal.api.common.v1.Payload;
import io.temporal.api.common.v1.Payloads;
import io.temporal.applied.patterns.cachingdataconverter.backend.DomainWorkflow;
import io.temporal.applied.patterns.cachingdataconverter.messaging.DomainReply;
import io.temporal.applied.patterns.cachingdataconverter.messaging.ReplySpec;
import io.temporal.applied.patterns.cachingdataconverter.messaging.StartDomainWorkflowRequest;
import io.temporal.applied.patterns.cachingdataconverter.temporal.CachingPayloadCodec;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.*;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
@CrossOrigin
public class APIController {
//    @Autowired
//    CachingPayloadCodec codec;
    Logger logger = LoggerFactory.getLogger(APIController.class);
    public static final JsonFormat.Parser JSON_FORMAT = JsonFormat.parser();
    public static final JsonFormat.Printer JSON_PRINTER = JsonFormat.printer();

    @Autowired
    private CachingPayloadCodec payloadCodec;

    @Autowired
    WorkflowClient temporalClient;
    @Autowired
    Replies<DomainReply> replies;

    @Value("${spring.application.task-queues.replies}")
    private String repliesTaskQueue;

    @Value("${spring.application.task-queues.domain}")
    private String domainTaskQueue;
    @PostMapping(
            value="/decode",
            consumes = {MediaType.APPLICATION_JSON_VALUE},
            produces = {MediaType.APPLICATION_JSON_VALUE}
    ) ResponseEntity post(HttpEntity<byte[]> requestEntity, HttpServletResponse response) {
        byte[] data = requestEntity.getBody();
        if(data == null) {
            return ResponseEntity.ok(null);
        }
        Payloads.Builder incomingPayloads = Payloads.newBuilder();
        InputStream stream = new ByteArrayInputStream(data);
        try(InputStreamReader reader = new InputStreamReader(stream)) {
            JSON_FORMAT.merge(reader, incomingPayloads);
        } catch (IOException e) {
            logger.error("failed to read incoming data", e);
            return ResponseEntity.badRequest().build();
        }

        List<Payload> incomingPayloadsList = incomingPayloads.build().getPayloadsList();

        List<Payload> outgoingPayloadsList = payloadCodec.decode(incomingPayloadsList);

        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            JSON_PRINTER.appendTo(Payloads.newBuilder().addAllPayloads(outgoingPayloadsList).build(), writer);
        } catch (IOException e) {
            logger.error("failed to write converted data", e);
            return ResponseEntity.internalServerError().build();
        } finally{
            if(writer != null ) {
                writer.flush();
            }
        }
        return ResponseEntity.badRequest().build();
    }

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
        cmd.value = params.getValue();
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

//    @PostMapping(
//            value = "/dc",
//            consumes = {MediaType.APPLICATION_JSON_VALUE},
//            produces = {MediaType.TEXT_HTML_VALUE})
//    ResponseEntity post(@RequestBody DataConverterParams params) {
//        WorkflowOptions workflowOptions =
//                WorkflowOptions.newBuilder()
//                        .setTaskQueue(domainTaskQueue)
//                        .setWorkflowId(params.getWorkflowId())
//                        // you will rarely ever want to set this on WorkflowOptions
//                        // .setRetryOptions()
//                        .build();
//        StartDomainWorkflowRequest cmd = new StartDomainWorkflowRequest();
//        cmd.prefix = params.getPrefix();
//        cmd.value = params.getValue();
//
//
//        CountDownLatch latch = new CountDownLatch(1);
//        DomainWorkflow workflow = temporalClient.newWorkflowStub(DomainWorkflow.class, workflowOptions);
//        replies.putLatch(params.getWorkflowId(), latch);
//        CompletableFuture<Void> execution = WorkflowClient.execute(workflow::execute, cmd);
//        try {
//            if(params.getReplyTimeoutSecs() > 0) {
//                // wait for 60 seconds to unblock our thread (reply has been received)
//                boolean replyReceived = latch.await(params.getReplyTimeoutSecs(), TimeUnit.SECONDS);
//                if(!replyReceived) {
//                    String message = String.format("reply was not received in %d seconds, but the workflow '%s' has continued",
//                            params.getReplyTimeoutSecs(),
//                            params.getWorkflowId());
//                    // bypass thymeleaf, don't return template name just result
//                    return new ResponseEntity<>("\"" + message + "\"", HttpStatus.REQUEST_TIMEOUT);
//                }
//            } else {
//                latch.await();
//            }
//        } catch (InterruptedException e) {
//            // handle with an error status code
//            throw new RuntimeException(e);
//        }
//        DomainReply reply = replies.obtainReply(params.getWorkflowId());
//        String message = "workflow '%s' replied with value '%s' validated as '%b'";
//        String out = String.format(message, params.getWorkflowId(),
//                reply.value,
//                reply.businessValidationSucceeded
//        );
//        HttpStatus status = reply.businessValidationSucceeded ? HttpStatus.OK : HttpStatus.BAD_REQUEST;
//        // bypass thymeleaf, don't return template name just result
//        return new ResponseEntity<>("\"" + out + "\"", status);
//
//    }
}
