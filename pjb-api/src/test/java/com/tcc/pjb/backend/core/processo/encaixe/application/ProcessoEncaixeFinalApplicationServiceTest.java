package com.tcc.pjb.backend.core.processo.encaixe.application;

import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.core.processo.documental.application.ProcessoDocumentoApplicationService;
import com.tcc.pjb.backend.core.processo.documental.domain.ProcessoDocumentoAggregate;
import com.tcc.pjb.backend.core.processo.documental.domain.ProcessoDocumentoIdentity;
import com.tcc.pjb.backend.core.processo.execucao.application.ProcessoExecucaoApplicationService;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoAggregate;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoIdentity;
import com.tcc.pjb.backend.core.processo.integracao.application.ProcessoIntegracaoApplicationService;
import com.tcc.pjb.backend.core.processo.integracao.domain.ProcessoIntegracaoAggregate;
import com.tcc.pjb.backend.core.processo.integracao.domain.ProcessoIntegracaoIdentity;
import com.tcc.pjb.backend.core.processo.migracao.application.ProcessoMigracaoApplicationService;
import com.tcc.pjb.backend.core.processo.migracao.domain.ProcessoMigracaoAggregate;
import com.tcc.pjb.backend.core.processo.migracao.domain.ProcessoMigracaoIdentity;
import com.tcc.pjb.backend.core.processo.operacao.application.ProcessoOperacaoApplicationService;
import com.tcc.pjb.backend.core.processo.operacao.domain.ProcessoOperacaoAggregate;
import com.tcc.pjb.backend.core.processo.operacao.domain.ProcessoOperacaoIdentity;
import com.tcc.pjb.backend.core.processo.papel.application.ProcessoPapelApplicationService;
import com.tcc.pjb.backend.core.processo.papel.domain.ProcessoPapelAggregate;
import com.tcc.pjb.backend.core.processo.papel.domain.ProcessoPapelIdentity;
import com.tcc.pjb.backend.core.processo.prazo.application.ProcessoPrazoApplicationService;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoAggregate;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoCienciaProfile;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoIdentity;
import com.tcc.pjb.backend.core.processo.recursal.application.ProcessoRecursalApplicationService;
import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalAggregate;
import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalIdentity;
import com.tcc.pjb.backend.core.processo.timeline.application.ProcessoTimelineApplicationService;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineAggregate;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineIdentity;
import com.tcc.pjb.backend.core.processo.trabalho.application.ProcessoTrabalhoApplicationService;
import com.tcc.pjb.backend.core.processo.trabalho.domain.ProcessoTrabalhoAggregate;
import com.tcc.pjb.backend.core.processo.trabalho.domain.ProcessoTrabalhoIdentity;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoCompetencia;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoDiagnostico;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessoEncaixeFinalApplicationServiceTest {

    @Mock private ProcessoRepository processoRepository;
    @Mock private ProcessoUnificadoApplicationService processoUnificadoApplicationService;
    @Mock private ProcessoPrazoApplicationService processoPrazoApplicationService;
    @Mock private ProcessoTrabalhoApplicationService processoTrabalhoApplicationService;
    @Mock private ProcessoDocumentoApplicationService processoDocumentoApplicationService;
    @Mock private ProcessoRecursalApplicationService processoRecursalApplicationService;
    @Mock private ProcessoExecucaoApplicationService processoExecucaoApplicationService;
    @Mock private ProcessoPapelApplicationService processoPapelApplicationService;
    @Mock private ProcessoTimelineApplicationService processoTimelineApplicationService;
    @Mock private ProcessoIntegracaoApplicationService processoIntegracaoApplicationService;
    @Mock private ProcessoMigracaoApplicationService processoMigracaoApplicationService;
    @Mock private ProcessoOperacaoApplicationService processoOperacaoApplicationService;

    private ProcessoEncaixeFinalApplicationService service;

    @BeforeEach
    void setUp() {
        service = new ProcessoEncaixeFinalApplicationService(
                processoRepository,
                processoUnificadoApplicationService,
                processoPrazoApplicationService,
                processoTrabalhoApplicationService,
                processoDocumentoApplicationService,
                processoRecursalApplicationService,
                processoExecucaoApplicationService,
                processoPapelApplicationService,
                processoTimelineApplicationService,
                processoIntegracaoApplicationService,
                processoMigracaoApplicationService,
                processoOperacaoApplicationService
        );
    }

    @Test
    void shouldDetectStructuralGaps() {
        Processo processo = Processo.builder()
                .id(30L)
                .numeroProcesso("0003")
                .ramoDireito(RamoDireito.CIVIL)
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .faseAtual(FaseProcessual.CONHECIMENTO)
                .statusProcesso(StatusProcesso.DISTRIBUIDO)
                .tribunalCodigoRoteado("TJCE")
                .unidadeJudiciariaCodigo("3VC")
                .build();
        when(processoRepository.findById(30L)).thenReturn(Optional.of(processo));
        when(processoUnificadoApplicationService.detalhar(30L)).thenReturn(new ProcessoUnificadoAggregate(
                new ProcessoUnificadoIdentity(30L, "0003", "0003", "TJCE", "CE", "FORTALEZA", "3VC", "PROC", "ASSUNTO", "AUTOR", "REU", List.of()),
                new ProcessoUnificadoCompetencia("ESTADUAL", "1GRAU", "CIVIL", "COMUM_ORDINARIO", "CONHECIMENTO", "DISTRIBUIDO", "TJCE", "Tribunal de Justiça", "JUIZO", "3VC", "FILA", "MESA", "TERRITORIAL", "PREVENCAO", "SORTEIO", "GENERALISTA", "PADRAO", "NENHUM", "VALIDA", "LOW", "SECRETARIA", false, false, 24, List.of(), List.of(), List.of(), new java.util.LinkedHashMap<>()),
                new ProcessoUnificadoDiagnostico(true, 0, 0, 0, 0, 0, 0, List.of(), List.of(), Instant.now()),
                List.of(),
                List.of(),
                List.of(),
                Instant.now()
        ));
        when(processoPrazoApplicationService.detalhar(30L)).thenReturn(new ProcessoPrazoAggregate(new ProcessoPrazoIdentity(30L, "0003", "TJCE", "CE", "FORTALEZA", "3VC", "CIVIL", "COMUM_ORDINARIO", "CONHECIMENTO", "DISTRIBUIDO", List.of()), new ProcessoPrazoCienciaProfile("PADRAO", false, true, false, false, List.of(), List.of()), List.of(), 0, 0, 0, 0, "NAO_INFORMADA", List.of(), List.of(), Instant.now()));
        when(processoTrabalhoApplicationService.detalhar(30L)).thenReturn(new ProcessoTrabalhoAggregate(new ProcessoTrabalhoIdentity(30L, "0003", "CIVIL", "COMUM_ORDINARIO", "CONHECIMENTO", "DISTRIBUIDO", "TJCE", "3VC", List.of()), 0, 0, 0, 0, 0, 0, "TRIAGEM", List.of(), List.of(), List.of(), Instant.now()));
        when(processoDocumentoApplicationService.detalhar(30L)).thenReturn(new ProcessoDocumentoAggregate(new ProcessoDocumentoIdentity(30L, "0003", "CIVIL", "COMUM_ORDINARIO", "CONHECIMENTO", "DISTRIBUIDO", "TJCE", List.of()), 0, 0, 0, 0, 0, 0, List.of(), List.of(), List.of(), Instant.now()));
        when(processoRecursalApplicationService.detalhar(30L)).thenReturn(new ProcessoRecursalAggregate(new ProcessoRecursalIdentity(30L, "0003", "TJCE", "CIVIL", "COMUM_ORDINARIO", "CONHECIMENTO", "DISTRIBUIDO", List.of()), "OUTRA", "FIRST_INSTANCE", 0, 0, 0, 0, List.of(), List.of(), List.of(), List.of(), Instant.now()));
        when(processoExecucaoApplicationService.detalhar(30L)).thenReturn(new ProcessoExecucaoAggregate(new ProcessoExecucaoIdentity(30L, "0003", "TJCE", "CIVIL", "COMUM_ORDINARIO", "CONHECIMENTO", "DISTRIBUIDO", List.of()), false, 0, 0, 0, 0, List.of(), List.of(), List.of(), Instant.now()));
        when(processoPapelApplicationService.detalhar(30L)).thenReturn(new ProcessoPapelAggregate(new ProcessoPapelIdentity(30L, "0003", "CIVIL", "COMUM_ORDINARIO", "CONHECIMENTO", "DISTRIBUIDO", List.of()), 0, 0, 0, 0, List.of(), List.of(), Instant.now()));
        when(processoTimelineApplicationService.detalhar(30L)).thenReturn(new ProcessoTimelineAggregate(new ProcessoTimelineIdentity(30L, "0003", "CIVIL", "COMUM_ORDINARIO", "CONHECIMENTO", "DISTRIBUIDO", "TJCE", "3VC", List.of()), 0, 0, 0, List.of(), List.of(), List.of(), List.of(), List.of(), Instant.now()));
        when(processoIntegracaoApplicationService.detalhar(30L)).thenReturn(new ProcessoIntegracaoAggregate(new ProcessoIntegracaoIdentity(30L, "0003", "TJCE", "3VC", "CIVIL", "COMUM_ORDINARIO", "PJE", List.of()), "PJE", "READY", "READY", List.of(), List.of(), List.of(), List.of(), Instant.now()));
        when(processoMigracaoApplicationService.detalhar(30L)).thenReturn(new ProcessoMigracaoAggregate(new ProcessoMigracaoIdentity(30L, "0003", "TJCE", "3VC", "PJE", "PJB", List.of()), "READY", true, List.of(), List.of(), List.of(), List.of(), Instant.now()));
        when(processoOperacaoApplicationService.detalhar(30L)).thenReturn(new ProcessoOperacaoAggregate(new ProcessoOperacaoIdentity(30L, "0003", "TJCE", "3VC", "CIVIL", "COMUM_ORDINARIO", "CONHECIMENTO", "DISTRIBUIDO", List.of()), "PARTIAL_READY", "OBSERVAR", "ATTENTION", "READY", 35d, 0, List.of(), List.of(), List.of(), Instant.now()));

        var aggregate = service.detalhar(30L);

        assertThat(aggregate.findings()).isNotEmpty();
        assertThat(aggregate.score()).isLessThan(100L);
    }
}
