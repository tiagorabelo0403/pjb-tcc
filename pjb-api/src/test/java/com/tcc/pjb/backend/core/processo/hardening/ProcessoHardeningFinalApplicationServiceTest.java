package com.tcc.pjb.backend.core.processo.hardening;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.encaixe.application.ProcessoEncaixeFinalApplicationService;
import com.tcc.pjb.backend.core.processo.encaixe.domain.ProcessoEncaixeFinalAggregate;
import com.tcc.pjb.backend.core.processo.encaixe.domain.ProcessoEncaixeFinding;
import com.tcc.pjb.backend.core.processo.hardening.application.ProcessoHardeningFinalApplicationService;
import com.tcc.pjb.backend.core.processo.operacao.application.ProcessoOperacaoApplicationService;
import com.tcc.pjb.backend.core.processo.operacao.domain.ProcessoOperacaoAggregate;
import com.tcc.pjb.backend.core.processo.operacao.domain.ProcessoOperacaoIdentity;
import com.tcc.pjb.backend.core.processo.policy.application.ProcessoPolicyVigenciaApplicationService;
import com.tcc.pjb.backend.core.processo.policy.domain.ProcessoPolicyAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloAggregate;
import com.tcc.pjb.backend.core.processo.timeline.application.ProcessoTimelineApplicationService;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineAggregate;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineIdentity;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoCompetencia;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoDiagnostico;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessoHardeningFinalApplicationServiceTest {

    @Mock private ProcessoUnificadoApplicationService processoUnificadoApplicationService;
    @Mock private ProcessoEncaixeFinalApplicationService processoEncaixeFinalApplicationService;
    @Mock private ProcessoSigiloApplicationService processoSigiloApplicationService;
    @Mock private ProcessoPolicyVigenciaApplicationService processoPolicyVigenciaApplicationService;
    @Mock private ProcessoTimelineApplicationService processoTimelineApplicationService;
    @Mock private ProcessoOperacaoApplicationService processoOperacaoApplicationService;

    @Test
    void deveConsolidarFindingsDeHardening() {
        when(processoUnificadoApplicationService.detalhar(77L)).thenReturn(new ProcessoUnificadoAggregate(
                new ProcessoUnificadoIdentity(77L, "77", "77", "TJCE", "CE", "Fortaleza", "Vara", "Classe", "Assunto", "Autor", "Réu", List.of("CIVEL")),
                new ProcessoUnificadoCompetencia("ESTADUAL", "PRIMEIRO_GRAU", "CIVEL", "COMUM", "CONHECIMENTO", "EM_ANDAMENTO", "TJCE", "Tribunal", "Vara", "Vara", "fila", "mesa", "LOCAL", "PREV", "SORTEIO", "CIVEL", "PADRAO", "AUTO", "CONTROLADO", "GABINETE", false, false, 24, List.of(), List.of("fundamento"), List.of("check"), new LinkedHashMap<>()),
                new ProcessoUnificadoDiagnostico(true, 0, 0, 0, 0, 1, 1, List.of(), List.of(), Instant.now()), List.of(), List.of(), List.of(), Instant.now()));
        when(processoEncaixeFinalApplicationService.detalhar(77L)).thenReturn(new ProcessoEncaixeFinalAggregate(77L, "77", "ATTENTION", 72, 1, 1, List.of("COERENCIA"), List.of(new ProcessoEncaixeFinding("F1", "F1", "COERENCIA", "CRITICAL", true, "detalhe", "corrigir")), List.of("corrigir"), Instant.now()));
        when(processoSigiloApplicationService.detalhar(77L)).thenReturn(new ProcessoSigiloAggregate(
                new ProcessoUnificadoIdentity(77L, "77", "77", "TJCE", "CE", "Fortaleza", "Vara", "Classe", "Assunto", "Autor", "Réu", List.of("CIVEL")),
                NivelSigilo.SEGREDO_JUSTICA, "SEGREDO_JUDICIAL", true, true, false, 0, 0, 1, 1, List.of("sigilo"), List.of("MAGISTRADO_DIRETO"), List.of(), List.of(), List.of("fundamento"), Instant.now()));
        when(processoPolicyVigenciaApplicationService.avaliar(77L)).thenReturn(new ProcessoPolicyAggregate(
                new ProcessoUnificadoIdentity(77L, "77", "77", "TJCE", "CE", "Fortaleza", "Vara", "Classe", "Assunto", "Autor", "Réu", List.of("CIVEL")),
                LocalDate.now(), 1, 1, 1, List.of(), List.of(), List.of("fundamento"), Instant.now()));
        when(processoTimelineApplicationService.detalhar(77L)).thenReturn(new ProcessoTimelineAggregate(
                new ProcessoTimelineIdentity(77L, "77", "CIVEL", "COMUM", "CONHECIMENTO", "EM_ANDAMENTO", "TJCE", "Vara", List.of("civel")),
                1, 1, 1, List.of("civel"), List.of(), List.of(), List.of(), List.of(), Instant.now()));
        when(processoOperacaoApplicationService.detalhar(77L)).thenReturn(new ProcessoOperacaoAggregate(
                new ProcessoOperacaoIdentity(77L, "77", "TJCE", "Vara", "CIVEL", "COMUM", "CONHECIMENTO", "EM_ANDAMENTO", List.of("CIVEL")),
                "ATTENTION", "ATTENTION", "ATTENTION", "NAO_AVALIADA", 0.8, 1, List.of(), List.of("acao"), List.of(), Instant.now()));

        ProcessoHardeningFinalApplicationService service = new ProcessoHardeningFinalApplicationService(
                processoUnificadoApplicationService,
                processoEncaixeFinalApplicationService,
                processoSigiloApplicationService,
                processoPolicyVigenciaApplicationService,
                processoTimelineApplicationService,
                processoOperacaoApplicationService
        );
        var aggregate = service.detalhar(77L);
        assertThat(aggregate.totalFindings()).isPositive();
        assertThat(aggregate.readiness()).isEqualTo("NOT_READY");
        assertThat(aggregate.hardeningAxes()).contains("SIGILO", "OPERACAO");
    }
}
