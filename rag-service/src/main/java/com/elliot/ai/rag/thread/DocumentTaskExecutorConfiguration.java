package com.elliot.ai.rag.thread;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableScheduling
public class DocumentTaskExecutorConfiguration {

    /**
     * {@code @Scheduled} 定时方法专用线程池。
     *
     * <p>只负责触发任务领取、超时扫描等轻量调度逻辑；
     * 不执行 PDF 解析、切块、向量化等耗时工作。</p>
     *
     * <p>Bean 名称使用 Spring 约定的 {@code taskScheduler}，
     * 因此 {@code @Scheduled} 会自动使用此线程池。</p>
     */
    @Bean("taskScheduler")
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("document-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(15);
        scheduler.initialize();
        return scheduler;
    }

    /**
     * 文档处理工作线程池，执行解析、切块和索引等耗时任务。
     */
    @Bean("documentTaskExecutor")
    public TaskExecutor documentTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("document-task-");
        /*
         * 队列满时直接拒绝。
         * Dispatcher 捕获后把任务重新放回 PENDING。
         * 不让调度线程自己执行耗时文档任务。
         */
        executor.setRejectedExecutionHandler(
                new ThreadPoolExecutor.AbortPolicy()
        );

        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);

        executor.initialize();

        return executor;
    }
}
