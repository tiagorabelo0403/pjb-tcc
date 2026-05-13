package com.tcc.pjb.backend.core.processo.vertical.estadual;

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
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoFinding;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity;
import com.tcc.pjb.backend.core.processo.vertical.estadual.civel.application.ProcessoVerticalCivelPrimeiroGrauApplicationService;
import com.tcc.pjb.backend.core.processo.vertical.estadual.penal.application.ProcessoVerticalPenalCustodiaApplicationService;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProcessoVerticalSlicesApplicationServiceTest {

    @Mock private ProcessoUnificadoApplicationService processoUnificadoApplicationService;
    @Mock private ProcessoPrazoApplicationService processoPrazoApplicationService;
    @Mock private ProcessoTrabalhoApplicationService processoTrabalhoApplicationService;
    @Mock private ProcessoDocumentoApplicationService processoDocumentoApplicationService;
    @Mock private ProcessoTimelineApplicationService processoTimelineApplicationService;
    @Mock private ProcessoPapelApplicationService processoPapelApplicationService;
    @Mock private ProcessoRecursalApplicationService processoRecursalApplicationService;
    @Mock private ProcessoExecucaoApplicationService processoExecucaoApplicationService;

    @BeforeEach
    void setup() {
        when(processoUnificadoApplicationService.detalhar(70L)).thenReturn(unificado());
        when(processoPrazoApplicationService.detalhar(70L)).thenReturn(new ProcessoPrazoAggregate(
                new ProcessoPrazoIdentity(70L, "0001", "TJCE", "CE", "Fortaleza", "1a Vara", "PENAL", "PROCEDIMENTO_PENAL_COMUM", "AUDIENCIA_CUSTODIA", "EM_ANDAMENTO", List.of("PENAL")),
                new ProcessoPrazoCienciaProfile("PESSOAL", true, true, false, true, List.of("GUARDA"), List.of("fundamento")),
                List.of(), 0, 0, 0, 0, "CONTROLADA", List.of("onda"), List.of("alerta"), Instant.now()));
        when(processoTrabalhoApplicationService.detalhar(70L)).thenReturn(new ProcessoTrabalhoAggregate(
                new com.tcc.pjb.backend.core.processo.trabalho.domain.ProcessoTrabalhoIdentity(70L, "0001", "PENAL", "PROCEDIMENTO_PENAL_COMUM", "AUDIENCIA_CUSTODIA", "EM_ANDAMENTO", "TJCE", "1a Vara", List.of("PENAL")),
                1, 1, 0, 0, 0, 1, "CONTROLADA", List.of(new ProcessoTrabalhoFila("fila", "Fila", 1, 1, 0, 0, null, List.of("PROMOTOR"), List.of())), List.of(), List.of("fluxo"), Instant.now()));
        when(processoDocumentoApplicationService.detalhar(70L)).thenReturn(new ProcessoDocumentoAggregate(
                new ProcessoDocumentoIdentity(70L, "0001", "PENAL", "PROCEDIMENTO_PENAL_COMUM", "AUDIENCIA_CUSTODIA", "EM_ANDAMENTO", "TJCE", List.of("PENAL")),
                2, 1, 1, 1, 1, 1, List.of(), List.of(), List.of("trilha"), Instant.now()));
        when(processoTimelineApplicationService.detalhar(70L)).thenReturn(new ProcessoTimelineAggregate(
                new ProcessoTimelineIdentity(70L, "0001", "PENAL", "PROCEDIMENTO_PENAL_COMUM", "AUDIENCIA_CUSTODIA", "EM_ANDAMENTO", "TJCE", "1a Vara", List.of("custodia")),
                1, 1, 0, List.of("custodia", "urgencia"), List.of(new ProcessoTimelineEvento("A", "Evento", "BASE", 1L, Instant.now(), true, false, "OPERACAO", List.of("x"), List.of("fundamento"))), List.of(new ProcessoTimelinePendencia("P", "Pendência", "OPERACIONAL", "ALTA", Instant.now(), "OPERACAO", true, List.of("fundamento"))), List.of("proximo"), List.of(), Instant.now()));
        when(processoRecursalApplicationService.detalhar(70L)).thenReturn(new ProcessoRecursalAggregate(
                new ProcessoRecursalIdentity(70L, "0001", "TJCE", "PENAL", "PROCEDIMENTO_PENAL_COMUM", "AUDIENCIA_CUSTODIA", "EM_ANDAMENTO", List.of("PENAL")),
                "PENAL", "FIRST_INSTANCE", 1, 1, 0, 0,
                List.of(new ProcessoRecursalJanela("APELACAO", "Apelação", "RECURSAL", true, false, true, false, true, "MESMOS_AUTOS", "TJCE", "JUÍZO_A_QUO", "CÂMARA", List.of("INTIMAR"), List.of("TEMPESTIVIDADE"), List.of("CPP"))),
                List.of("abrir_recurso"), List.of(), List.of(), Instant.now()));
        when(processoExecucaoApplicationService.detalhar(70L)).thenReturn(new ProcessoExecucaoAggregate(
                new ProcessoExecucaoIdentity(70L, "0001", "TJCE", "PENAL", "PROCEDIMENTO_PENAL_COMUM", "AUDIENCIA_CUSTODIA", "EM_ANDAMENTO", List.of("PENAL")),
                true, 1, 0, 1, 1,
                List.of(new ProcessoExecucaoTrilha("CUSTODIA", "Custódia", "EXECUCAO", "CUSTODIA", "UNIDADE", "UNIDADE_PRISIONAL", 1, false, 2, "CUMPRIMENTO", "ALTO", List.of("ORDENS"), List.of("APRESENTACAO"), List.of("CERTIFICAR"), List.of("CPP"))),
                List.of(), List.of("confirmar_custodia"), Instant.now()));
        for (String code : List.of(
                "MAGISTRADO_DIRETO", "ADVOGADO_DIRETO", "PROMOTORIA__PROMOTORIA_TITULAR", "NUCLEO_DEFENSORIA__NUCLEO_DEFENSORIA_TITULAR",
                "PROCURADORIA_PUBLICA__PROCURADORIA_PUBLICA_TITULAR", "CEJUSC__CEJUSC_AGENDAMENTO", "CONTADORIA__CONTADORIA_OPERACAO",
                "DELEGACIA__DELEGACIA_TITULAR", "POLICIA_PENAL__POLICIA_PENAL_CUSTODIA", "UNIDADE_PRISIONAL__UNIDADE_PRISIONAL_CUSTODIA", "UNIDADE_PRISIONAL__UNIDADE_PRISIONAL_DIRECAO"
        )) {
            when(processoPapelApplicationService.detalharPerfil(70L, code)).thenReturn(new ProcessoPapelPerfil(
                    code, code, "PAINEL", "PROFILE", "NIVEL", "red",
                    List.of(), List.of("Receber"), List.of("Preparar"), List.of("Aprovar"), List.of("Assinar"), List.of("Peticionar"), List.of("Certificar"), List.of("Redistribuir"), List.of("Recorrer"), List.of("Embargar"), List.of("base"), List.of("MFA"), List.of("fundamento")
            ));
        }
    }

    @Test
    void deveMontarFatiaCivelPrimeiroGrauComLanesEEtapas() {
        ProcessoVerticalCivelPrimeiroGrauApplicationService service = new ProcessoVerticalCivelPrimeiroGrauApplicationService(
                processoUnificadoApplicationService,
                processoPrazoApplicationService,
                processoTrabalhoApplicationService,
                processoDocumentoApplicationService,
                processoTimelineApplicationService,
                processoPapelApplicationService
        );
        var aggregate = service.detalhar(70L);
        assertThat(aggregate.sliceCode()).isEqualTo("CIVEL_COMUM_PRIMEIRO_GRAU");
        assertThat(aggregate.totalEtapas()).isEqualTo(6);
        assertThat(aggregate.totalLanes()).isPositive();
    }

    @Test
    void deveMontarFatiaPenalCustodiaComFluxoMaterial() {
        ProcessoVerticalPenalCustodiaApplicationService service = new ProcessoVerticalPenalCustodiaApplicationService(
                processoUnificadoApplicationService,
                processoPrazoApplicationService,
                processoTrabalhoApplicationService,
                processoDocumentoApplicationService,
                processoTimelineApplicationService,
                processoRecursalApplicationService,
                processoExecucaoApplicationService,
                processoPapelApplicationService
        );
        var aggregate = service.detalhar(70L);
        assertThat(aggregate.sliceCode()).isEqualTo("PENAL_CUSTODIA");
        assertThat(aggregate.totalEtapas()).isEqualTo(6);
        assertThat(aggregate.processChips()).contains("custodia");
    }

    private ProcessoUnificadoAggregate unificado() {
        return new ProcessoUnificadoAggregate(
                new ProcessoUnificadoIdentity(70L, "0001", "0001", "TJCE", "CE", "Fortaleza", "1a Vara", "Ação penal", "crime", "MP", "Réu", List.of("PENAL")),
                new ProcessoUnificadoCompetencia("ESTADUAL", "PRIMEIRO_GRAU", "PENAL", "PROCEDIMENTO_PENAL_COMUM", "AUDIENCIA_CUSTODIA", "EM_ANDAMENTO", "TJCE", "Tribunal", "Órgão", "1a Vara", "fila", "mesa", "LOCAL", "PREV", "SORTEIO", "PENAL", "PADRAO", "AUTO", "CONTROLADO", "GABINETE", false, false, 24, List.of(), List.of("fundamento"), List.of("check"), new LinkedHashMap<>()),
                new ProcessoUnificadoDiagnostico(true, 0, 0, 2, 0, 1, 1, List.<ProcessoUnificadoFinding>of(), List.of("fundamento"), Instant.now()),
                List.of(
                        new ProcessoUnificadoAto("ASSINAR_PARECER", "Assinar parecer", "MERITO", "PARECER", "MERITO", "fila", "inbox", "fundamento", "CONHECIMENTO", "CONHECIMENTO", "EM_ANDAMENTO", "EM_ANDAMENTO", true, false, false, true, false, true, "ok", "PROMOTOR", "TRANSICAO", List.of("custodia")),
                        new ProcessoUnificadoAto("CONFIRMAR_CUSTODIA", "Confirmar custódia", "EXECUCAO", "CERTIDAO", "CUSTODIA", "fila", "inbox", "fundamento", "AUDIENCIA_CUSTODIA", "EXECUCAO", "EM_ANDAMENTO", "EM_ANDAMENTO", true, false, false, true, false, true, "ok", "POLICIAL_PENAL", "TRANSICAO", List.of("custodia"))
                ),
                List.of(),
                List.of("ASSINAR_PARECER"),
                Instant.now()
        );
    }
}
