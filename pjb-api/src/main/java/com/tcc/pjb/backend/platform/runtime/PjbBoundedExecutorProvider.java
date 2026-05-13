package com.tcc.pjb.backend.platform.runtime;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Component
public class PjbBoundedExecutorProvider {

    private final PjbBoundedExecutorService ioExecutorService;
    private final PjbBoundedExecutorService burstExecutorService;
    private final PjbBoundedExecutorService externalIoExecutorService;
    private final PjbBoundedExecutorService liveExecutorService;
    private final PjbBoundedExecutorService jobExecutorService;

    public PjbBoundedExecutorProvider(@Qualifier("pjbIoExecutorService") PjbBoundedExecutorService ioExecutorService,
                                      @Qualifier("pjbBurstExecutorService") PjbBoundedExecutorService burstExecutorService,
                                      @Qualifier("pjbExternalIoExecutorService") PjbBoundedExecutorService externalIoExecutorService,
                                      @Qualifier("pjbLiveExecutorService") PjbBoundedExecutorService liveExecutorService,
                                      @Qualifier("pjbJobExecutorService") PjbBoundedExecutorService jobExecutorService) {
        this.ioExecutorService = ioExecutorService;
        this.burstExecutorService = burstExecutorService;
        this.externalIoExecutorService = externalIoExecutorService;
        this.liveExecutorService = liveExecutorService;
        this.jobExecutorService = jobExecutorService;
    }

    public PjbBoundedExecutorService io() {
        return ioExecutorService;
    }

    public PjbBoundedExecutorService burst() {
        return burstExecutorService;
    }

    public PjbBoundedExecutorService externalIo() {
        return externalIoExecutorService;
    }

    public PjbBoundedExecutorService live() {
        return liveExecutorService;
    }

    public PjbBoundedExecutorService job() {
        return jobExecutorService;
    }
}
