package io.temporal.applied.patterns.requestreply.messaging;

public class StartDomainWorkflowRequest {
    public String prefix;
    public ReplySpec replySpec;
    public StartDomainWorkflowRequest() {
    }
}
