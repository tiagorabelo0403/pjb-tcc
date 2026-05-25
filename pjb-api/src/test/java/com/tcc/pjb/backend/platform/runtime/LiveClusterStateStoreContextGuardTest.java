package com.tcc.pjb.backend.platform.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.configs.live.LiveClusterStateStore;
import com.tcc.pjb.backend.configs.live.LiveClusterStateStoreConfiguration;
import com.tcc.pjb.backend.configs.live.NoOpLiveClusterStateStore;
import com.tcc.pjb.backend.configs.live.RedisLiveClusterStateStore;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = LiveClusterStateStoreConfiguration.class)
@ActiveProfiles("test")
class LiveClusterStateStoreContextGuardTest {

    @Autowired
    ApplicationContext context;

    @Autowired
    LiveClusterStateStore store;

    @Test
    void deveRegistrarFallbackLocalQuandoRedisNaoEstiverDisponivel() {
        assertThat(store).isInstanceOf(NoOpLiveClusterStateStore.class);
        assertThat(context.getBeansOfType(LiveClusterStateStore.class)).hasSize(1);
    }

    @Test
    void deveManterImplementacoesForaDeEstereotiposDuplicados() {
        assertThat(RedisLiveClusterStateStore.class.isAnnotationPresent(org.springframework.stereotype.Component.class)).isFalse();
        assertThat(NoOpLiveClusterStateStore.class.isAnnotationPresent(org.springframework.stereotype.Component.class)).isFalse();
    }

    @Test
    void deveFalharFechadoQuandoClusterEstiverHabilitadoSemRedis() {
        new ApplicationContextRunner()
                .withUserConfiguration(LiveClusterStateStoreConfiguration.class)
                .withPropertyValues("pjb.live.cluster.enabled=true")
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNotNull();
                    assertThat(context.getStartupFailure())
                            .hasMessageContaining("pjb.live.cluster.enabled=true");
                });
    }
}
