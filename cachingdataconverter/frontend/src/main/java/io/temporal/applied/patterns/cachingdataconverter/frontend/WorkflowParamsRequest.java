package io.temporal.applied.patterns.cachingdataconverter.frontend;

public class WorkflowParamsRequest {
  private String workflowId;
  private boolean simulateValidationFailure;
  private long replyTimeoutSecs;

  public int getValue() {
    return value;
  }

  public void setValue(int value) {
    this.value = value;
  }

  private int value;
  public String getWorkflowId() {
    return workflowId;
  }

  public void setWorkflowId(String workflowId) {
    this.workflowId = workflowId;
  }

  public boolean shouldSimulateValidationFailure() {
    return simulateValidationFailure;
  }

  public void setSimulateValidationFailure(boolean simulateValidationFailure) {
    this.simulateValidationFailure = simulateValidationFailure;
  }

  public long getReplyTimeoutSecs() {
    return replyTimeoutSecs ;
  }

  public void setReplyTimeoutSecs(long replyTimeoutSecs) {
    this.replyTimeoutSecs = replyTimeoutSecs;
  }
}
