package com.example.blogger.scheduler;

import com.example.blogger.service.ScheduledPushService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledPushScheduler {
    private final ScheduledPushService scheduledPushService;

    public ScheduledPushScheduler(ScheduledPushService scheduledPushService) {
        this.scheduledPushService = scheduledPushService;
    }

    @Scheduled(fixedRate = 60000)
    public void checkAndExecute() {
        scheduledPushService.executePending();
    }
}
