package io.temporal.applied.patterns.cachingdataconverter.temporal;

import io.temporal.activity.LocalActivityOptions;
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptor;
import io.temporal.common.interceptors.WorkflowInboundCallsInterceptorBase;
import io.temporal.workflow.CancellationScope;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public class CacheWorkflowInboundCallsInterceptor extends WorkflowInboundCallsInterceptorBase {
    Logger logger = LoggerFactory.getLogger(CacheWorkflowInboundCallsInterceptor.class);
    public CacheWorkflowInboundCallsInterceptor(WorkflowInboundCallsInterceptor next) {
        super(next);
    }

    @Override
    public WorkflowOutput execute(WorkflowInput input) {

        try {
            return super.execute(input);
        } finally {
            WorkflowInfo info = Workflow.getInfo();
            try {
                CancellationScope scope = Workflow.newDetachedCancellationScope(() -> {
                    CacheCleaner cleaner = Workflow.newLocalActivityStub(
                            CacheCleaner.class,
                            LocalActivityOptions.newBuilder().
                                    setStartToCloseTimeout(Duration.ofSeconds(2)).
                                    build());
                    // NOTE: This has been disabled due to irregularity of detecting the correct RunId at this stage of execution
                    //cleaner.markEvictable(new MarkEvictableRequest(info.getNamespace(), info.getWorkflowType(), info.getWorkflowId(),info.getRunId()));
                });
                scope.run();

            } catch (Exception e) {
                // gulp
                logger.error("failed to clean cache", e);
            }
        }

    }

}
