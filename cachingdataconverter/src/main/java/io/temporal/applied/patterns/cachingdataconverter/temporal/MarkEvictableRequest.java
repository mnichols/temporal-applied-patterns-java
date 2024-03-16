package io.temporal.applied.patterns.cachingdataconverter.temporal;

public class MarkEvictableRequest {
    private  String runId;

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    private  String namespace;
    private String workflowId;
    private String workflowType;

    public MarkEvictableRequest(String namespace, String workflowType, String workflowId, String runId) {
        this.namespace = namespace;
        this.workflowId = workflowId;
        this.workflowType = workflowType;
        this.runId = runId;
    }

    public MarkEvictableRequest() {
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getWorkflowType() {
        return workflowType;
    }

    public void setWorkflowType(String workflowType) {
        this.workflowType = workflowType;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }
}
