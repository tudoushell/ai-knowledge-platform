package com.elliot.ai.rag.scheduler;

import com.elliot.ai.rag.service.DocumentTaskClaimService;
import com.elliot.ai.rag.service.DocumentTaskStateService;
import com.elliot.ai.rag.worker.DocumentTaskWorker;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Component
public class DocumentTaskDispatcher {
    private static final int CLAIM_SIZE = 4;
    private final DocumentTaskClaimService claimService;
    private final DocumentTaskWorker worker;
    private final DocumentTaskStateService stateService;
    @Qualifier("documentTaskExecutor")
    private final TaskExecutor taskExecutor;

    private final String workerId = ManagementFactory.getRuntimeMXBean().getName();

    @Scheduled(fixedDelay = 1000)
    public void dispatch() {
        List<UUID> taskIds = claimService.claim(CLAIM_SIZE, workerId);
        for (UUID taskId : taskIds) {
            try {
                taskExecutor.execute(() -> worker.process(taskId));
            } catch (TaskRejectedException e) {
                stateService.resetToPending(taskId);
            }
        }
    }

}
