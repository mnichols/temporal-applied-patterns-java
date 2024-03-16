package io.temporal.applied.patterns.cachingdataconverter.temporal;

public class CacheContextInfo {
    private String namespace;
    private String workflowId;

    public CacheContextInfo() {
    }

    public CacheContextInfo(String namespace, String workflowId) {
        this.namespace = namespace;
        this.workflowId = workflowId;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

}
