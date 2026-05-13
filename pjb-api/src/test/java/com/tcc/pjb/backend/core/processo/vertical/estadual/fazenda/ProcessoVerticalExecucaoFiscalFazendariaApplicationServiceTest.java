package com.tcc.pjb.backend.core.processo.vertical.estadual.fazenda;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.documental.application.ProcessoDocumentoApplicationService;
import com.tcc.pjb.backend.core.processo.documental.domain.ProcessoDocumentoAggregate;
import com.tcc.pjb.backend.core.processo.documental.domain.ProcessoDocumentoIdentity;
import com.tcc.pjb.backend.core.processo.execucao.application.ProcessoExecucaoApplicationService;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoAggregate;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoIdentity;
import com.tcc.pjb.backend.core.processo.execucao.domain.ProcessoExecucaoTrilha;
import com.tcc.pjb.backend.core.processo.papel.application.ProcessoPapelApplicationService;
import com.tcc.pjb.backend.core.processo.papel.domain.ProcessoPapelPerfil;
import com.tcc.pjb.backend.core.processo.prazo.application.ProcessoPrazoApplicationService;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoAggregate;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoCienciaProfile;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoIdentity;
import com.tcc.pjb.backend.core.processo.recursal.application.ProcessoRecursalApplicationService;
import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalAggregate;
import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalIdentity;
import com.tcc.pjb.backend.core.processo.recursal.domain.ProcessoRecursalJanela;
import com.tcc.pjb.backend.core.processo.timeline.application.ProcessoTimelineApplicationService;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineAggregate;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineEvento;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineIdentity;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelinePendencia;
import com.tcc.pjb.backend.core.processo.trabalho.application.ProcessoTrabalhoApplicationService;
import com.tcc.pjb.backend.core.processo.trabalho.domain.ProcessoTrabalhoAggregate;
import com.tcc.pjb.backend.core.processo.trabalho.domain.ProcessoTrabalhoFila;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAto;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoCompetencia;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoDiagnostico;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity;
import com.tcc.pjb.backend.core.processo.vertical.estadual.fazenda.application.ProcessoVerticalExecucaoFiscalFazendariaApplicationService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessoVerticalExecucaoFiscalFazendariaApplicationServiceTest {

    @Mock private ProcessoUnificadoApplicationService processoUnificadoApplicationService;
    @Mock private ProcessoPrazoApplicationService processoPrazoApplicationService;
    @Mock private ProcessoTrabalhoApplicationService processoTrabalhoApplicationService;
    @Mock private ProcessoDocumentoApplicationService processoDocumentoApplicationService;
    @Mock private ProcessoTimelineApplicationService processoTimelineApplicationService;
    @Mock private ProcessoRecursalApplicationService processoRecursalApplicationService;
    @Mock private ProcessoExecucaoApplicationService processoExecucaoApplicationService;
    @Mock private ProcessoPapelApplicationService processoPapelApplicationService;

    @BeforeEach
    void setup() {
        when(processoUnificadoApplicationService.detalhar(90L)).thenReturn(new ProcessoUnificadoAggregate(
                new ProcessoUnificadoIdentity(90L, "9001", "9001", "TJCE", "CE", "Fortaleza", "Vara Fazendária", "Execução fiscal", "tributos", "Fazenda", "Executado", List.of("FAZENDARIO")),
                new ProcessoUnificadoCompetencia("ESTADUAL", "PRIMEIRO_GRAU", "FAZENDARIO", "EXECUCAO_FISCAL", "EXECUCAO", "EM_ANDAMENTO", "TJCE", "Tribunal", "Vara Fazendária", "Vara Fazendária", "fila", "mesa", "LOCAL", "PREV", "SORTEIO", "FAZENDARIO", "PADRAO", "AUTO", "CONTROLADO", "GABINETE", false, false, 24, List.of(), List.of("fundamento"), List.of("check"), new LinkedHashMap<>()),
                new ProcessoUnificadoDiagnostico(true, 0, 0, 1, 0, 1, 1, List.of(), List.of("fundamento"), Instant.now()),
                List.of(
                        new ProcessoUnificadoAto("PETICIONAR_FAZENDA", "Peticionar fazenda", "MERITO", "PETICAO", "EXECUCAO", "fila", "inbox", "fundamento", "EXECUCAO", "EXECUCAO", "EM_ANDAMENTO", "EM_ANDAMENTO", true, false, false, true, false, true, "ok", "PROCURADOR", "TRANSICAO", List.of("fazendario")),
                        new ProcessoUnificadoAto("BLOQUEIO_EXECUTIVO", "Bloqueio executivo", "EXECUCAO", "ORDEM", "EXECUCAO", "fila", "inbox", "fundamento", "EXECUCAO", "EXECUCAO", "EM_ANDAMENTO", "EM_ANDAMENTO", true, false, false, true, false, true, "ok", "MAGISTRADO", "TRANSICAO", List.of("fazendario")),
                        new ProcessoUnificadoAto("EMBARGOS_EXECUCAO", "Embargos", "EMBARGOS", "PETICAO", "RECURSAL", "fila", "inbox", "fundamento", "EXECUCAO", "RECURSAL", "EM_ANDAMENTO", "EM_ANDAMENTO", true, false, false, true, false, true, "ok", "ADVOGADO", "TRANSICAO", List.of("fazendario"))
                ),
                List.of(),
                List.of("BLOQUEIO_EXECUTIVO"),
                Instant.now()
        ));
        when(processoPrazoApplicationService.detalhar(90L)).thenReturn(new ProcessoPrazoAggregate(
                new ProcessoPrazoIdentity(90L, "9001", "TJCE", "CE", "Fortaleza", "Vara Fazendária", "FAZENDARIO", "EXECUCAO_FISCAL", "EXECUCAO", "EM_ANDAMENTO", List.of("FAZENDARIO")),
                new ProcessoPrazoCienciaProfile("PESSOAL", true, false, false, true, List.of(), List.of("fundamento")),
                List.of(), 0, 0, 0, 0, "CONTROLADA", List.of("onda"), List.of(), Instant.now()));
        when(processoTrabalhoApplicationService.detalhar(90L)).thenReturn(new ProcessoTrabalhoAggregate(
                new com.tcc.pjb.backend.core.processo.trabalho.domain.ProcessoTrabalhoIdentity(90L, "9001", "FAZENDARIO", "EXECUCAO_FISCAL", "EXECUCAO", "EM_ANDAMENTO", "TJCE", "Vara Fazendária", List.of("FAZENDARIO")),
                1, 0, 0, 0, 0, 1, "CONTROLADA", List.of(new ProcessoTrabalhoFila("fila", "Fila", 1, 0, 0, 0, null, List.of("PROCURADORIA"), List.of())), List.of(), List.of("fluxo"), Instant.now()));
        when(processoDocumentoApplicationService.detalhar(90L)).thenReturn(new ProcessoDocumentoAggregate(
                new ProcessoDocumentoIdentity(90L, "9001", "FAZENDARIO", "EXECUCAO_FISCAL", "EXECUCAO", "EM_ANDAMENTO", "TJCE", List.of("FAZENDARIO")),
                3, 1, 1, 1, 1, 1, List.of(), List.of(), List.of("trilha"), Instant.now()));
        when(processoTimelineApplicationService.detalhar(90L)).thenReturn(new ProcessoTimelineAggregate(
                new ProcessoTimelineIdentity(90L, "9001", "FAZENDARIO", "EXECUCAO_FISCAL", "EXECUCAO", "EM_ANDAMENTO", "TJCE", "Vara Fazendária", List.of("execucao")),
                1, 1, 0, List.of("execucao", "fazendario"), List.of(new ProcessoTimelineEvento("A", "Evento", "BASE", 1L, Instant.now(), true, false, "OPERACAO", List.of(), List.of())), List.of(new ProcessoTimelinePendencia("P", "Pendência", "OPERACIONAL", "MEDIA", Instant.now(), "OPERACAO", false, List.of())), List.of("proximo"), List.of(), Instant.now()));
        when(processoRecursalApplicationService.detalhar(90L)).thenReturn(new ProcessoRecursalAggregate(
                new ProcessoRecursalIdentity(90L, "9001", "TJCE", "FAZENDARIO", "EXECUCAO_FISCAL", "EXECUCAO", "EM_ANDAMENTO", List.of("FAZENDARIO")),
                "FAZENDARIO", "FIRST_INSTANCE", 1, 1, 0, 1, List.of(new ProcessoRecursalJanela("APELACAO", "Apelação", "RECURSAL", true, false, true, false, true, "MESMOS_AUTOS", "TJCE", "JUÍZO_A_QUO", "CÂMARA", List.of(), List.of(), List.of("CPC"))), List.of("recorrer"), List.of(), List.of(), Instant.now()));
        when(processoExecucaoApplicationService.detalhar(90L)).thenReturn(new ProcessoExecucaoAggregate(
                new ProcessoExecucaoIdentity(90L, "9001", "TJCE", "FAZENDARIO", "EXECUCAO_FISCAL", "EXECUCAO", "EM_ANDAMENTO", List.of("FAZENDARIO")),
                true, 1, 0, 1, 0, List.of(new ProcessoExecucaoTrilha("EXECUCAO_FISCAL", "Execução fiscal", "EXECUCAO", "CONSTRICAO", "VARA", "MAGISTRADO", 1, false, 1, "CUMPRIMENTO", "ALTO", List.of("ORDEM"), List.of("BLOQUEIO"), List.of("CERTIDAO"), List.of("LEF"))), List.of(), List.of("satisfazer"), Instant.now()));
        for (String code : List.of("MAGISTRADO_DIRETO", "PROCURADORIA_PUBLICA__PROCURADORIA_PUBLICA_TITULAR", "PROCURADORIA_PUBLICA__PROCURADORIA_PUBLICA_ASSESSORIA", "CONTADORIA__CONTADORIA_OPERACAO", "ADVOGADO_DIRETO")) {
            when(processoPapelApplicationService.detalharPerfil(90L, code)).thenReturn(new ProcessoPapelPerfil(
                    code, code, "PAINEL", "PROFILE", "NIVEL", "red", List.of(), List.of("Receber"), List.of("Preparar"), List.of("Aprovar"), List.of("Assinar"), List.of("Peticionar"), List.of("Certificar"), List.of("Redistribuir"), List.of("Recorrer"), List.of("Embargar"), List.of("Sugerir"), List.of("sep"), List.of("guard"), List.of("fundamento")
            ));
        }
    }

    @Test
    void deveMontarFatiaExecucaoFiscalFazendaria() {
        ProcessoVerticalExecucaoFiscalFazendariaApplicationService service = new ProcessoVerticalExecucaoFiscalFazendariaApplicationService(
                processoUnificadoApplicationService,
                processoPrazoApplicationService,
                processoTrabalhoApplicationService,
                processoDocumentoApplicationService,
                processoTimelineApplicationService,
                processoRecursalApplicationService,
                processoExecucaoApplicationService,
                processoPapelApplicationService
        );
        var aggregate = service.detalhar(90L);
        assertThat(aggregate.sliceCode()).isEqualTo("EXECUCAO_FISCAL_FAZENDARIA");
        assertThat(aggregate.totalEtapas()).isEqualTo(6);
        assertThat(aggregate.processChips()).contains("execucao_fiscal");
    }
}
