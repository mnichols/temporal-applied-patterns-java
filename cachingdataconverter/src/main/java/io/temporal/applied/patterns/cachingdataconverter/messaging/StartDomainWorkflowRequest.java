package io.temporal.applied.patterns.cachingdataconverter.messaging;

public class StartDomainWorkflowRequest {
    public String prefix;
    public ReplySpec replySpec;
    public int value;

    public StartDomainWorkflowRequest() {
    }
}
