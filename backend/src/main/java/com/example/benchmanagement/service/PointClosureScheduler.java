package com.example.benchmanagement.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 点位封闭到期处理：应用启动后及每分钟检查到期封闭并自动解封，
 * 保证刷新页面时点状态与封闭起止时间一致。
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PointClosureScheduler {

    private final TreeNodeService treeNodeService;

    @Scheduled(cron = "0 * * * * ?")
    public void checkExpiry() {
        run();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void checkOnStartup() {
        run();
    }

    private void run() {
        try {
            int expired = treeNodeService.autoExpireClosedPoints();
            if (expired > 0) {
                log.info("点位封闭到期自动解封完成，共解封{}个点位", expired);
            }
        } catch (Exception e) {
            log.error("点位封闭到期自动解封失败", e);
        }
    }
}
