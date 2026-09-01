package com.elliot.ai.rag.scheduler;

import com.elliot.ai.rag.mapper.DocumentProcessTaskMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@RequiredArgsConstructor
@Component
public class DocumentTaskRecoveryScheduler {
    private final DocumentProcessTaskMapper taskMapper;


    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void recoverStaleTasks() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime cutoff = now.minusMinutes(60);
        taskMapper.failExhaustedStaleTask(cutoff, now);
        taskMapper.recoverRetryableStaleTasks(cutoff, now.plusSeconds(10), now);
    }
}
