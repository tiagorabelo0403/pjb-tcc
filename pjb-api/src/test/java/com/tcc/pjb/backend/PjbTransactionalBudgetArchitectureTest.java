package com.tcc.pjb.backend;

import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

class PjbTransactionalBudgetArchitectureTest {

    @Test
    void hotspotsTransacionaisDevemDeclararBudgetExplicito() {
        List<Class<?>> hotspots = List.of(
                com.tcc.pjb.backend.core.transito.TransitoJulgadoArquivamentoEngine.class,
                com.tcc.pjb.backend.core.transito.ExecutionMeshStateService.class,
                com.tcc.pjb.backend.integration.judicial.financeiro.InfojudConsultaService.class,
                com.tcc.pjb.backend.integration.judicial.financeiro.InfojudApplicationService.class,
                com.tcc.pjb.backend.integration.judicial.financeiro.RenajudRestricaoService.class,
                com.tcc.pjb.backend.integration.judicial.financeiro.RenajudApplicationService.class,
                com.tcc.pjb.backend.integration.judicial.financeiro.SisbajudBloqueioService.class,
                com.tcc.pjb.backend.integration.judicial.financeiro.SisbajudApplicationService.class,
                com.tcc.pjb.backend.integration.judicial.financeiro.IntegracaoJudicialFinanceiraLifecycleService.class,
                com.tcc.pjb.backend.integration.datajud.feed.DataJudFeedService.class,
                com.tcc.pjb.backend.integration.datajud.feed.DataJudApplicationService.class,
                com.tcc.pjb.backend.service.security.govbr.GovBrStepUpService.class,
                com.tcc.pjb.backend.service.security.govbr.GovBrAccountProfileSynchronizationService.class,
                com.tcc.pjb.backend.service.security.govbr.GovBrStepUpStateCleanupJob.class
        );

        for (Class<?> hotspot : hotspots) {
            for (Method method : hotspot.getDeclaredMethods()) {
                if (!method.isAnnotationPresent(Transactional.class)) {
                    continue;
                }
                assertThat(method.isAnnotationPresent(PjbTransactionalBudget.class))
                        .as(hotspot.getSimpleName() + "." + method.getName())
                        .isTrue();
            }
        }
    }
}
