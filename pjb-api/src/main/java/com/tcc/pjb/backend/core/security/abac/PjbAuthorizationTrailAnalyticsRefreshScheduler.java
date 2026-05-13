package com.tcc.pjb.backend.core.security.abac;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class PjbAuthorizationTrailAnalyticsRefreshScheduler {

    private final PjbAuthorizationTrailAnalyticsRefreshQueueService refreshQueueService;
    private final PjbAuthorizationTrailAnalyticsRefreshProperties properties;

    public PjbAuthorizationTrailAnalyticsRefreshScheduler(PjbAuthorizationTrailAnalyticsRefreshQueueService refreshQueueService,
                                                          PjbAuthorizationTrailAnalyticsRefreshProperties properties) {
        this.refreshQueueService = refreshQueueService;
        this.properties = properties;
    }

    @Scheduled(fixedDelayString = "${pjb.authz.analytics.refresh.fixed-delay-ms:5000}")
    public void drainIncrementalQueue() {
        if (!properties.isEnabled()) {
            return;
        }
        refreshQueueService.drain(properties.getBatchSize());
    }

    @Scheduled(fixedDelayString = "${pjb.authz.analytics.refresh.cleanup-fixed-delay-ms:3600000}")
    public void cleanupCompletedQueueHistory() {
        if (!properties.isEnabled()) {
            return;
        }
        refreshQueueService.cleanupCompleted();
    }
}
