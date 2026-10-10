package com.tcc.pjb.backend;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.command.ajuizamento.AjuizarProcessoCommandPostCommitEffectsService;
import com.tcc.pjb.backend.inovacao.radar.RadarPadroesService;
import com.tcc.pjb.backend.model.dto.event.ProcessoAjuizadoEvent;
import com.tcc.pjb.backend.platform.runtime.PjbTransactionalBudget;
import com.tcc.pjb.backend.service.AjuizamentoService;
import com.tcc.pjb.backend.service.ajuizamento.AjuizamentoPostCommitOperationalEffectsService;
import com.tcc.pjb.backend.service.competencia.MapaCompetenciaDinamicoEngine;
import com.tcc.pjb.backend.service.distribuicao.ProcessoInitialDistributionSnapshotService;
import com.tcc.pjb.backend.service.ajuizamento.federal.FederalismoJudicialEngine;
import com.tcc.pjb.backend.service.identity.ProntuarioNacionalService;
import com.tcc.pjb.backend.service.painel.PainelNacionalJusticaService;
import com.tcc.pjb.backend.service.processo.ProcessoPostAjuizamentoOrchestratorService;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.event.TransactionalApplicationListenerMethodAdapter;

class PjbAjuizamentoPhaseSplitArchitectureTest {

    @Test
    void ajuizamentoServiceNaoDeveSegurarSideEffectsOperacionaisPosCommitNoCommandPath() {
        List<Class<?>> forbidden = List.of(
                MapaCompetenciaDinamicoEngine.class,
                ProcessoInitialDistributionSnapshotService.class,
                ProntuarioNacionalService.class,
                FederalismoJudicialEngine.class,
                PainelNacionalJusticaService.class,
                RadarPadroesService.class
        );

        List<Class<?>> fieldTypes = List.of(AjuizamentoService.class.getDeclaredFields()).stream()
                .map(Field::getType)
                .toList();

        assertThat(fieldTypes).doesNotContainAnyElementsOf(forbidden);
    }

    @Test
    void listenerPosCommitDoAjuizamentoDeveTerBudgetExplicito() throws NoSuchMethodException {
        Method method = AjuizamentoPostCommitOperationalEffectsService.class
                .getDeclaredMethod("onProcessoAjuizado", ProcessoAjuizadoEvent.class);

        assertThat(method.isAnnotationPresent(PjbTransactionalBudget.class)).isTrue();
    }

    @Test
    void listenersPosCommitDoAjuizamentoRodamNaOrdemContextoDistribuicaoEfeitosOperacionais() throws NoSuchMethodException {
        List<Integer> ordens = List.of(
                ordemDoListener(AjuizarProcessoCommandPostCommitEffectsService.class),
                ordemDoListener(ProcessoPostAjuizamentoOrchestratorService.class),
                ordemDoListener(AjuizamentoPostCommitOperationalEffectsService.class));

        assertThat(ordens)
                .as("os efeitos operacionais precisam ler o processo ja distribuido pelo orquestrador")
                .isSorted()
                .doesNotHaveDuplicates();
    }

    private static int ordemDoListener(Class<?> listener) throws NoSuchMethodException {
        Method metodo = listener.getDeclaredMethod("onProcessoAjuizado", ProcessoAjuizadoEvent.class);
        return new TransactionalApplicationListenerMethodAdapter(listener.getSimpleName(), listener, metodo).getOrder();
    }
}
