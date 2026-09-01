package com.elliot.ai.rag.service.impl;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.elliot.ai.rag.entity.DocumentProcessTask;
import com.elliot.ai.rag.enums.DocumentTaskStatus;
import com.elliot.ai.rag.mapper.DocumentProcessTaskMapper;
import com.elliot.ai.rag.service.DocumentTaskClaimService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class DocumentTaskClaimServiceImpl implements DocumentTaskClaimService {

    private final TransactionTemplate transactionTemplate;
    private final DocumentProcessTaskMapper taskMapper;

    @Override
    public List<UUID> claim(int limit, String workId) {
        List<UUID> result = transactionTemplate.execute(status -> {
            List<UUID> taskIds = taskMapper.selectClaimableTaskIds(limit);
            if (taskIds.isEmpty()) {
                return List.of();
            }
            OffsetDateTime now = OffsetDateTime.now();
            LambdaUpdateWrapper<DocumentProcessTask> updateWrapper = new LambdaUpdateWrapper<>();
            updateWrapper.in(DocumentProcessTask::getId, taskIds)
                    .set(DocumentProcessTask::getStatus, DocumentTaskStatus.RUNNING)
                    .set(DocumentProcessTask::getWorkerId, workId)
                    .set(DocumentProcessTask::getLockedAt, now)
                    .set(DocumentProcessTask::getHeartbeatAt, now)
                    .set(DocumentProcessTask::getStartedAt, now)
                    .set(DocumentProcessTask::getUpdatedAt, now);
            taskMapper.update(updateWrapper);
            return List.copyOf(taskIds);
        });
        return result == null ? List.of() : result;
    }
}
