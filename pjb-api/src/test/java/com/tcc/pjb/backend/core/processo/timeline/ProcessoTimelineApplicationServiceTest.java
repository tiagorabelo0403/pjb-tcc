package com.tcc.pjb.backend.core.processo.timeline;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.execucao.application.ProcessoExecucaoApplicationService;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoAggregate;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoIdentity;
import com.tcc.pjb.backend.core.processo.prazo.application.ProcessoPrazoApplicationService;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoAggregate;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoCienciaProfile;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoIdentity;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoMarco;
import com.tcc.pjb.backend.core.processo.recursal.application.ProcessoRecursalApplicationService;
import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalAggregate;
import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalIdentity;
import com.tcc.pjb.backend.core.processo.timeline.application.ProcessoTimelineApplicationService;
import com.tcc.pjb.backend.core.processo.trabalho.application.ProcessoTrabalhoApplicationService;
import com.tcc.pjb.backend.core.processo.trabalho.domain.ProcessoTrabalhoAggregate;
import com.tcc.pjb.backend.core.processo.trabalho.domain.ProcessoTrabalhoIdentity;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoCompetencia;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoDiagnostico;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoFinding;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoPaginaRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessoTimelineApplicationServiceTest {

    @Mock
    private ProcessoRepository processoRepository;
    @Mock
    private DocumentoProcessualRepository documentoProcessualRepository;
    @Mock
    private DocumentoPaginaRepository documentoPaginaRepository;
    @Mock
    private ProcessoUnificadoApplicationService processoUnificadoApplicationService;
    @Mock
    private ProcessoPrazoApplicationService processoPrazoApplicationService;
    @Mock
    private ProcessoTrabalhoApplicationService processoTrabalhoApplicationService;
    @Mock
    private ProcessoRecursalApplicationService processoRecursalApplicationService;
    @Mock
    private ProcessoExecucaoApplicationService processoExecucaoApplicationService;

    @Test
    void deveMontarLinhaDoTempoComPendenciasBloqueantes() {
        Processo processo = Processo.builder()
                .id(501L)
                .numeroProcesso("0000501-77.2026.8.06.0001")
                .tribunalCodigoRoteado("TJCE")
                .unidadeJudiciariaCodigo("1VCIV")
                .ramoDireito(RamoDireito.CIVIL)
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .faseAtual(FaseProcessual.CONHECIMENTO)
                 .statusProcesso(StatusProcesso.SENTENCA_PROFERIDA)
                .dataCriacao(LocalDateTime.now().minusDays(20))
                .dataDistribuicao(LocalDateTime.now().minusDays(19))
                .dataUltimaMovimentacao(LocalDateTime.now().minusHours(10))
                .resultadoFinal("Sentença de procedência parcial com condenação principal.")
                .pedidoPrincipal("Condenação ao pagamento e obrigação de fazer.")
                .materialProbatorioResumo("Contrato, laudo e mensagens anexadas.")
                .build();
        when(processoRepository.findById(501L)).thenReturn(Optional.of(processo));

        ProcessoTimelineApplicationService service = new ProcessoTimelineApplicationService(processoRepository, documentoProcessualRepository, documentoPaginaRepository);

        var aggregate = service.detalhar(501L);

        assertThat(aggregate.eventos()).isNotEmpty();
        assertThat(aggregate.eventos()).anyMatch(item -> item.codigo().equals("CADERNO_DECISORIO_RECURSAL"));
        assertThat(aggregate.pendencias()).anyMatch(item -> item.categoria().equals("PRAZO") && item.bloqueiaProximoPasso());
        assertThat(aggregate.alertas()).anyMatch(item -> item.contains("caderno decisório"));
    }

    private ProcessoUnificadoAggregate unificado() {
        return new ProcessoUnificadoAggregate(
                new com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity(501L, "0000501-77.2026.8.06.0001", "0000501-77.2026.8.06.0001", "TJCE", "CE", "Fortaleza", "1VCIV", "PROCEDIMENTO COMUM", "COBRANCA", "AUTOR", "REU", List.of("CIVIL")),
                new ProcessoUnificadoCompetencia("ESTADUAL", "PRIMEIRO_GRAU", "CIVIL", "COMUM_ORDINARIO", "CONHECIMENTO", "EM_ANDAMENTO", "TJCE", "Tribunal de Justiça", "Juízo Cível", "1VCIV", "DISTRIBUICAO", "TRIAGEM", "FORO", "NAO", "SORTEIO", "CIVEL", "PADRAO", "NAO", "ENVELOPE", "LOW", "SECRETARIA", false, false, 24, List.of(), List.of("fundamento"), List.of("check"), new LinkedHashMap<>()),
                new ProcessoUnificadoDiagnostico(false, 1, 1, 4, 1, 1, 0, List.of(new ProcessoUnificadoFinding("PROCESSO_SEM_ASSUNTO", "HIGH", true, "Assunto ausente", "A matéria precisa ser detalhada.")), List.of("fundamento"), Instant.now()),
                List.of(),
                List.of(),
                List.of("REGULARIZAR_ASSUNTO"),
                Instant.now()
        );
    }

    private ProcessoPrazoAggregate prazos() {
        return new ProcessoPrazoAggregate(
                new ProcessoPrazoIdentity(501L, "0000501-77.2026.8.06.0001", "TJCE", "CE", "Fortaleza", "1VCIV", "CIVIL", "COMUM_ORDINARIO", "CONHECIMENTO", "EM_ANDAMENTO", List.of("CIVIL")),
                new ProcessoPrazoCienciaProfile("ELETRONICA", false, true, false, false, List.of("GUARDA"), List.of("fundamento")),
                List.of(new ProcessoPrazoMarco("PRAZO_BASE", "Manifestação", "PRAZO_GENERICO", "TRILHA", LocalDate.now().minusDays(10), LocalDate.now().minusDays(1), 10, 10, -1, true, true, true, "EFEITO", List.of("Prazo vencido"), List.of("fundamento"))),
                1,
                1,
                1,
                1,
                "JANELA_CRITICA",
                List.of("MANIFESTAR"),
                List.of("Prazo estrutural"),
                Instant.now()
        );
    }

    private ProcessoTrabalhoAggregate trabalho() {
        return new ProcessoTrabalhoAggregate(
                new ProcessoTrabalhoIdentity(501L, "0000501-77.2026.8.06.0001", "CIVIL", "COMUM_ORDINARIO", "CONHECIMENTO", "EM_ANDAMENTO", "TJCE", "1VCIV", List.of("CIVIL")),
                2,
                1,
                0,
                1,
                1,
                0,
                "CRITICA",
                List.of(),
                List.of("EXISTE_BLOQUEANTE_ABERTO_NA_FILA"),
                List.of("DESTRAVAR_MINUTA"),
                Instant.now()
        );
    }

    private ProcessoRecursalAggregate recursal() {
        return new ProcessoRecursalAggregate(
                new ProcessoRecursalIdentity(501L, "0000501-77.2026.8.06.0001", "TJCE", "CIVIL", "COMUM_ORDINARIO", "CONHECIMENTO", "EM_ANDAMENTO", List.of("CIVIL")),
                "CIVEL",
                "FIRST_INSTANCE",
                1,
                1,
                0,
                0,
                List.of(),
                List.of("ABRIR_CONTRARRAZOES"),
                List.of(),
                List.of("Janela recursal aberta"),
                Instant.now()
        );
    }

    private ProcessoExecucaoAggregate execucao() {
        return new ProcessoExecucaoAggregate(
                new ProcessoExecucaoIdentity(501L, "0000501-77.2026.8.06.0001", "TJCE", "CIVIL", "COMUM_ORDINARIO", "CONHECIMENTO", "EM_ANDAMENTO", List.of("CIVIL")),
                false,
                0,
                0,
                0,
                0,
                List.of(),
                List.of(),
                List.of(),
                Instant.now()
        );
    }
}
