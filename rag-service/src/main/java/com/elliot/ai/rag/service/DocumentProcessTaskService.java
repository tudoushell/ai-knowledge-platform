package com.elliot.ai.rag.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.elliot.ai.rag.dto.DocumentProcessTaskPageDto;
import com.elliot.ai.rag.entity.DocumentProcessTask;

import java.util.UUID;

/**
 * 文档处理任务业务服务。
 *
 * <p>负责创建和查询文档异步处理任务；实际的解析、切块、索引工作
 * 由 Dispatcher 领取任务后交给 Worker 执行。</p>
 */
public interface DocumentProcessTaskService extends IService<DocumentProcessTask> {

    /**
     * 为指定文档创建“解析、切块、索引”完整处理任务。
     *
     * <p>同一文档同一时刻只允许存在一个活动任务。若任务已经处于
     * {@code PENDING}、{@code RUNNING} 或 {@code RETRY_WAIT} 状态，
     * 则直接返回已有任务，保证重复提交时的幂等性。</p>
     *
     * @param documentId 待处理的知识库文档 ID
     * @return 新建的任务，或该文档已有的活动任务
     * @throws com.elliot.ai.common.exception.BusinessException 文档不存在时抛出
     */
    DocumentProcessTask createFullProcessTask(UUID documentId);

    /**
     * 分页查询指定文档的处理任务历史。
     *
     * @param documentId 文档 ID
     * @param page       页码，从 1 开始
     * @param size       每页数量
     * @return 任务历史分页结果
     */
    DocumentProcessTaskPageDto pageTaskHistory(UUID documentId, int page, int size);
}
