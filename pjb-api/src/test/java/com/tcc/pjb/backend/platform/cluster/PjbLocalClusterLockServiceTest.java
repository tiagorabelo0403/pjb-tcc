package com.tcc.pjb.backend.platform.cluster;

import static org.assertj.core.api.Assertions.assertThat;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class PjbLocalClusterLockServiceTest {

    @Test
    void shouldSerializeAccessPerKey() {
        PjbLocalClusterLockService service = new PjbLocalClusterLockService("pjb:test:", null);

        PjbClusterLockService.Lease first = service.tryAcquire("scheduler", Duration.ofSeconds(5)).orElse(null);
        PjbClusterLockService.Lease second = service.tryAcquire("scheduler", Duration.ofSeconds(5)).orElse(null);

        assertThat(first).isNotNull();
        assertThat(second).isNull();

        first.close();

        PjbClusterLockService.Lease third = service.tryAcquire("scheduler", Duration.ofSeconds(5)).orElse(null);
        assertThat(third).isNotNull();
        third.close();
    }
}
