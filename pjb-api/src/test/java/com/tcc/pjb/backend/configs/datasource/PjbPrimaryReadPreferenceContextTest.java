package com.tcc.pjb.backend.configs.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class PjbPrimaryReadPreferenceContextTest {

    @Test
    void shouldPreferPrimaryUntilDeadlineAndThenClear() throws Exception {
        PjbPrimaryReadPreferenceContext context = new PjbPrimaryReadPreferenceContext();
        context.preferPrimaryFor(Duration.ofMillis(50));
        assertThat(context.isPrimaryPreferred()).isTrue();
        Thread.sleep(80L);
        assertThat(context.isPrimaryPreferred()).isFalse();
    }

    @Test
    void shouldClearPreferenceExplicitly() {
        PjbPrimaryReadPreferenceContext context = new PjbPrimaryReadPreferenceContext();
        context.preferPrimaryFor(Duration.ofSeconds(1));
        context.clear();
        assertThat(context.isPrimaryPreferred()).isFalse();
    }
}
