package com.elliot.ai.rag.service;

import java.util.List;
import java.util.UUID;

/**
 * 文档处理任务领取服务。
 *
 * <p>由 Dispatcher 调用，将待执行或到达重试时间的任务原子地领取给当前 Worker。</p>
 */
public interface DocumentTaskClaimService {

    /**
     * 领取一批可执行的文档处理任务。
     *
     * <p>实现应在同一短事务内通过 {@code FOR UPDATE SKIP LOCKED} 锁定候选任务，
     * 并将其状态更新为 {@code RUNNING}，同时记录 Worker 和锁信息，避免多个
     * 应用实例重复领取同一任务。</p>
     *
     * @param limit  最多领取的任务数量，必须大于 {@code 0}
     * @param workId 当前 Worker 的唯一标识，用于记录任务归属
     * @return 本次成功领取的任务 ID；没有可执行任务时返回空列表
     */
    List<UUID> claim(int limit, String workId);
}
