package com.tcc.pjb.backend.core.processo.painel.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.competencia.domain.ProcessoCompetenciaMalhaAggregate;
import com.tcc.pjb.backend.core.processo.competencia.domain.ProcessoCompetenciaMalhaItem;
import com.tcc.pjb.backend.core.processo.distribuicao.domain.ProcessoDistribuicaoMalhaAggregate;
import com.tcc.pjb.backend.core.processo.distribuicao.domain.ProcessoDistribuicaoMalhaMotivo;
import com.tcc.pjb.backend.core.processo.runtime.application.ProcessoMalhaParallelExecutor;
import com.tcc.pjb.backend.core.processo.runtime.domain.ProcessoRuntimeContext;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProcessoPainelRotaTaticaApplicationServiceTest {

    @Test
    void priorizaDesbloqueioQuandoHaBloqueantes() {
        var distribuicaoService = mock(com.tcc.pjb.backend.core.processo.distribuicao.application.ProcessoDistribuicaoMalhaApplicationService.class);
        var competenciaService = mock(com.tcc.pjb.backend.core.processo.competencia.application.ProcessoCompetenciaMalhaApplicationService.class);
        var runtimeResolver = mock(com.tcc.pjb.backend.core.processo.runtime.application.ProcessoRuntimeResolver.class);
        var parallelExecutor = mock(ProcessoMalhaParallelExecutor.class);
        ProcessoPainelRotaTaticaApplicationService service = new ProcessoPainelRotaTaticaApplicationService(distribuicaoService, competenciaService, runtimeResolver, parallelExecutor);

        Processo processo = Processo.builder().id(40L).numeroProcesso("000040").ramoDireito(RamoDireito.CIVIL).rito(RitoProcessual.COMUM_ORDINARIO).build();
        when(runtimeResolver.resolver(40L)).thenReturn(new ProcessoRuntimeContext(processo, 40L, "000040", "000040", RamoDireito.CIVIL, RitoProcessual.COMUM_ORDINARIO, TipoUsuario.SERVIDOR_FORUM, "TJCE", "1VC", "Fortaleza", "CE", false));

        ProcessoDistribuicaoMalhaAggregate distribuicao = new ProcessoDistribuicaoMalhaAggregate(40L, "000040", "DESBLOQUEAR_FLUXO", "FILA", "INBOX", "TJCE", "1VC", 10, true, false, false, false, List.of(new ProcessoDistribuicaoMalhaMotivo("M1", "DISTRIBUICAO", "ALTA", true, "Bloqueio relevante", "REF", List.of("fundamento distribuicao"))), List.of("fundamento distribuicao"), Instant.now());
        ProcessoCompetenciaMalhaAggregate competencia = new ProcessoCompetenciaMalhaAggregate(40L, "000040", "CIVIL", "COMPETENCIA", "TJCE", "1VC", "TJCE", "1VC", "MANTER_COMPETENCIA", false, false, false, List.of(new ProcessoCompetenciaMalhaItem("C1", "COMPETENCIA", "MANTER_COMPETENCIA", "TJCE", "1VC", false, 0.95d, List.of("fundamento competencia"))), List.of("fundamento competencia"), Instant.now());
        when(parallelExecutor.executar2(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(new ProcessoMalhaParallelExecutor.Dupla<>(distribuicao, competencia));

        var aggregate = service.detalhar(40L);

        assertThat(aggregate.itens()).isNotEmpty();
        assertThat(aggregate.itens().getFirst().code()).isEqualTo("ROTA_DISTRIBUICAO");
        assertThat(aggregate.itens().getFirst().acao()).isEqualTo("DESBLOQUEAR_FLUXO");
    }
}
