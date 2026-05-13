package com.tcc.pjb.backend.core.processo.producao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.integracao.application.ProcessoIntegracaoApplicationService;
import com.tcc.pjb.backend.core.processo.integracao.domain.ProcessoIntegracaoAggregate;
import com.tcc.pjb.backend.core.processo.integracao.domain.ProcessoIntegracaoIdentity;
import com.tcc.pjb.backend.core.processo.operacao.application.ProcessoOperacaoApplicationService;
import com.tcc.pjb.backend.core.processo.operacao.domain.ProcessoOperacaoAggregate;
import com.tcc.pjb.backend.core.processo.operacao.domain.ProcessoOperacaoFaixa;
import com.tcc.pjb.backend.core.processo.operacao.domain.ProcessoOperacaoIdentity;
import com.tcc.pjb.backend.core.processo.producao.application.ProcessoOperacaoTransversalApplicationService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProcessoOperacaoTransversalApplicationServiceTest {

    @Test
    void deveConsolidarControlesTransversais() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        ProcessoOperacaoApplicationService operacao = mock(ProcessoOperacaoApplicationService.class);
        ProcessoIntegracaoApplicationService integracao = mock(ProcessoIntegracaoApplicationService.class);
        when(processoRepository.findById(50L)).thenReturn(Optional.of(Processo.builder().id(50L).numeroProcesso("0050").build()));
        when(operacao.detalhar(50L)).thenReturn(new ProcessoOperacaoAggregate(new ProcessoOperacaoIdentity(50L, "0050", "TJCE", "1VC", "CIVIL", "COMUM_ORDINARIO", "CONHECIMENTO", "DISTRIBUIDO", List.of()), "READY", "FORTE", "STABLE", "READY", 40d, 0L, List.of(new ProcessoOperacaoFaixa("FLUXO", "Fluxo", "OPERACAO", "STABLE", 20d, 0L, List.of(), List.of())), List.of("A"), List.of(), Instant.now()));
        when(integracao.detalhar(50L)).thenReturn(new ProcessoIntegracaoAggregate(new ProcessoIntegracaoIdentity(50L, "0050", "TJCE", "1VC", "CIVIL", "COMUM_ORDINARIO", "PJE", List.of()), "PJE", "READY", "READY", List.of(), List.of(), List.of(), List.of(), Instant.now()));
        ProcessoOperacaoTransversalApplicationService service = new ProcessoOperacaoTransversalApplicationService(processoRepository, operacao, integracao);

        var aggregate = service.detalhar(50L);

        assertThat(aggregate.controles()).hasSizeGreaterThanOrEqualTo(6);
        assertThat(aggregate.coberturaGlobal()).isPositive();
    }
}
