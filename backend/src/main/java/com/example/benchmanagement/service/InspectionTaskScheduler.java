package com.example.benchmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 巡检任务自动生成：应用启动后及每天凌晨为启用中的计划生成到期任务。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class InspectionTaskScheduler {

    private final InspectionPlanService planService;

    @Scheduled(cron = "0 30 0 * * ?")
    public void generateDaily() {
        run();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void generateOnStartup() {
        run();
    }

    private void run() {
        try {
            int created = planService.generateDueTasks();
            if (created > 0) {
                log.info("巡检任务自动生成完成，新增{}个待执行任务", created);
            }
        } catch (Exception e) {
            log.error("巡检任务自动生成失败", e);
        }
    }
}
