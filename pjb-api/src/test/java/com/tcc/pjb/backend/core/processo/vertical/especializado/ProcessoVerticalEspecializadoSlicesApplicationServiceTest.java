package com.tcc.pjb.backend.core.processo.vertical.especializado;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.documental.application.ProcessoDocumentoApplicationService;
import com.tcc.pjb.backend.core.processo.documental.domain.ProcessoDocumentoAggregate;
import com.tcc.pjb.backend.core.processo.documental.domain.ProcessoDocumentoIdentity;
import com.tcc.pjb.backend.core.processo.papel.application.ProcessoPapelApplicationService;
import com.tcc.pjb.backend.core.processo.papel.domain.ProcessoPapelPerfil;
import com.tcc.pjb.backend.core.processo.prazo.application.ProcessoPrazoApplicationService;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoAggregate;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoCienciaProfile;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoIdentity;
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
import com.tcc.pjb.backend.core.processo.vertical.eca.application.ProcessoVerticalEcaApplicationService;
import com.tcc.pjb.backend.core.processo.vertical.trabalhista.application.ProcessoVerticalTrabalhistaApplicationService;
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
class ProcessoVerticalEspecializadoSlicesApplicationServiceTest {

    @Mock private ProcessoUnificadoApplicationService processoUnificadoApplicationService;
    @Mock private ProcessoPrazoApplicationService processoPrazoApplicationService;
    @Mock private ProcessoTrabalhoApplicationService processoTrabalhoApplicationService;
    @Mock private ProcessoDocumentoApplicationService processoDocumentoApplicationService;
    @Mock private ProcessoTimelineApplicationService processoTimelineApplicationService;
    @Mock private ProcessoPapelApplicationService processoPapelApplicationService;

    @BeforeEach
    void setup() {
        when(processoUnificadoApplicationService.detalhar(80L)).thenReturn(unificadoTrabalhista());
        when(processoUnificadoApplicationService.detalhar(90L)).thenReturn(unificadoEca());
        when(processoPrazoApplicationService.detalhar(80L)).thenReturn(prazo(80L, "TRABALHISTA", "RITO_SUMARIO_TRABALHISTA"));
        when(processoPrazoApplicationService.detalhar(90L)).thenReturn(prazo(90L, "ECA", "ECA_ATO_INFRACIONAL"));
        when(processoTrabalhoApplicationService.detalhar(80L)).thenReturn(trabalho(80L, "TRABALHISTA"));
        when(processoTrabalhoApplicationService.detalhar(90L)).thenReturn(trabalho(90L, "ECA"));
        when(processoDocumentoApplicationService.detalhar(80L)).thenReturn(documental(80L, "TRABALHISTA"));
        when(processoDocumentoApplicationService.detalhar(90L)).thenReturn(documental(90L, "ECA"));
        when(processoTimelineApplicationService.detalhar(80L)).thenReturn(timeline(80L, "trabalhista"));
        when(processoTimelineApplicationService.detalhar(90L)).thenReturn(timeline(90L, "eca"));
        for (String code : List.of(
                "MAGISTRADO_DIRETO", "ADVOGADO_DIRETO",
                "MINISTERIO_PUBLICO_DO_TRABALHO__MPT_TITULAR",
                "MINISTERIO_PUBLICO__MP_INFANCIA_TITULAR",
                "CEJUSC__CEJUSC_AGENDAMENTO", "CONTADORIA__CONTADORIA_OPERACAO",
                "PROCURADORIA_PUBLICA__PROCURADORIA_PUBLICA_TITULAR",
                "NUCLEO_DEFENSORIA__NUCLEO_DEFENSORIA_TITULAR",
                "CONSELHO_TUTELAR__CONSELHO_TUTELAR_TITULAR",
                "ASSISTENCIA_SOCIAL__ASSISTENCIA_SOCIAL_OPERACAO"
        )) {
            when(processoPapelApplicationService.detalharPerfil(80L, code)).thenReturn(perfilMock(code));
            when(processoPapelApplicationService.detalharPerfil(90L, code)).thenReturn(perfilMock(code));
        }
    }

    @Test
    void deveMontarFatiaTrabalhistaComSeisEtapas() {
        ProcessoVerticalTrabalhistaApplicationService service = new ProcessoVerticalTrabalhistaApplicationService(
                processoUnificadoApplicationService,
                processoPrazoApplicationService,
                processoTrabalhoApplicationService,
                processoDocumentoApplicationService,
                processoTimelineApplicationService,
                processoPapelApplicationService
        );
        var aggregate = service.detalhar(80L);
        assertThat(aggregate.sliceCode()).isEqualTo("TRABALHISTA_PRIMEIRO_GRAU");
        assertThat(aggregate.totalEtapas()).isEqualTo(6);
        assertThat(aggregate.processChips()).contains("trabalhista");
    }

    @Test
    void deveMontarFatiaEcaComSigiloAbsolutoNosChips() {
        ProcessoVerticalEcaApplicationService service = new ProcessoVerticalEcaApplicationService(
                processoUnificadoApplicationService,
                processoPrazoApplicationService,
                processoTrabalhoApplicationService,
                processoDocumentoApplicationService,
                processoTimelineApplicationService,
                processoPapelApplicationService
        );
        var aggregate = service.detalhar(90L);
        assertThat(aggregate.sliceCode()).isEqualTo("ECA_INFANCIA_JUVENTUDE");
        assertThat(aggregate.totalEtapas()).isEqualTo(6);
        assertThat(aggregate.processChips()).contains("eca");
        assertThat(aggregate.processChips()).contains("sigilo_absoluto");
        assertThat(aggregate.alertas()).contains("SIGILO_ABSOLUTO_ATIVO");
    }

    private ProcessoUnificadoAggregate unificadoTrabalhista() {
        return new ProcessoUnificadoAggregate(
                new ProcessoUnificadoIdentity(80L, "0002", "0002", "TRT7", "CE", "Fortaleza", "1a VT", "Reclamação trabalhista", "verbas", "Reclamante", "Reclamado", List.of("TRABALHISTA")),
                new ProcessoUnificadoCompetencia("FEDERAL", "PRIMEIRO_GRAU", "TRABALHISTA", "RITO_SUMARIO_TRABALHISTA", "INSTRUCAO", "EM_ANDAMENTO", "TRT7", "Tribunal", "Órgão", "1a VT", "fila", "mesa", "LOCAL", "PREV", "SORTEIO", "TRABALHISTA", "SUMARIO", "AUTO", "CONTROLADO", "GABINETE", false, false, 48, List.of(), List.of("fundamento"), List.of("check"), new LinkedHashMap<>()),
                new ProcessoUnificadoDiagnostico(true, 0, 0, 1, 0, 1, 1, List.<ProcessoUnificadoFinding>of(), List.of("fundamento"), Instant.now()),
                List.of(new ProcessoUnificadoAto("JUNTAR_HOLERITE", "Juntar holerite", "MERITO", "DOCUMENTO", "MERITO", "fila", "inbox", "fundamento", "INSTRUCAO", "INSTRUCAO", "EM_ANDAMENTO", "EM_ANDAMENTO", true, false, false, true, false, false, "ok", "ADVOGADO", "TRANSICAO", List.of("trabalhista"))),
                List.of(),
                List.of("JUNTAR_HOLERITE"),
                Instant.now()
        );
    }

    private ProcessoUnificadoAggregate unificadoEca() {
        return new ProcessoUnificadoAggregate(
                new ProcessoUnificadoIdentity(90L, "0003", "0003", "TJCE", "CE", "Fortaleza", "VIJ", "Ato infracional", "adolescente", "MP", "Adolescente", List.of("ECA")),
                new ProcessoUnificadoCompetencia("ESTADUAL", "PRIMEIRO_GRAU", "ECA", "ECA_ATO_INFRACIONAL", "AUDIENCIA_CUSTODIA", "EM_ANDAMENTO", "TJCE", "Tribunal", "Órgão", "VIJ", "fila", "mesa", "LOCAL", "PREV", "SORTEIO", "ECA", "ESPECIAL", "AUTO", "SIGILOSO", "GABINETE", false, false, 24, List.of(), List.of("ECA art. 143"), List.of("check"), new LinkedHashMap<>()),
                new ProcessoUnificadoDiagnostico(true, 0, 0, 1, 0, 1, 1, List.<ProcessoUnificadoFinding>of(), List.of("fundamento"), Instant.now()),
                List.of(new ProcessoUnificadoAto("AUDIENCIA_APRESENTACAO", "Audiência de apresentação", "BASE", "AUDIENCIA", "BASE", "fila", "inbox", "fundamento", "AUDIENCIA_CUSTODIA", "CONHECIMENTO", "EM_ANDAMENTO", "EM_ANDAMENTO", true, false, false, true, false, false, "ok", "MAGISTRADO", "TRANSICAO", List.of("eca", "custodia"))),
                List.of(),
                List.of("AUDIENCIA_APRESENTACAO"),
                Instant.now()
        );
    }

    private ProcessoPrazoAggregate prazo(Long id, String ramo, String rito) {
        return new ProcessoPrazoAggregate(
                new ProcessoPrazoIdentity(id, "000" + id, "TJCE", "CE", "Fortaleza", "1a Vara", ramo, rito, "EM_ANDAMENTO", "EM_ANDAMENTO", List.of(ramo)),
                new ProcessoPrazoCienciaProfile("PESSOAL", true, true, false, true, List.of(), List.of("fundamento")),
                List.of(), 0, 0, 0, 0, "CONTROLADA", List.of(), List.of(), Instant.now());
    }

    private ProcessoTrabalhoAggregate trabalho(Long id, String ramo) {
        return new ProcessoTrabalhoAggregate(
                new com.tcc.pjb.backend.core.processo.trabalho.domain.ProcessoTrabalhoIdentity(id, "000" + id, ramo, "RITO", "FASE", "EM_ANDAMENTO", "TJCE", "1a Vara", List.of(ramo)),
                0, 0, 0, 0, 0, 0, "CONTROLADA", List.of(new ProcessoTrabalhoFila("fila", "Fila", 0, 0, 0, 0, null, List.of(), List.of())), List.of(), List.of(), Instant.now());
    }

    private ProcessoDocumentoAggregate documental(Long id, String ramo) {
        return new ProcessoDocumentoAggregate(
                new ProcessoDocumentoIdentity(id, "000" + id, ramo, "RITO", "FASE", "EM_ANDAMENTO", "TJCE", List.of(ramo)),
                1, 1, 0, 1, 0, 1, List.of(), List.of(), List.of("trilha"), Instant.now());
    }

    private ProcessoTimelineAggregate timeline(Long id, String eixo) {
        return new ProcessoTimelineAggregate(
                new ProcessoTimelineIdentity(id, "000" + id, eixo.toUpperCase(), "RITO", "FASE", "EM_ANDAMENTO", "TJCE", "1a Vara", List.of(eixo)),
                1, 0, 0, List.of(eixo), List.of(new ProcessoTimelineEvento("A", "Evento", "BASE", 1L, Instant.now(), true, false, "OPERACAO", List.of(), List.of("fundamento"))), List.of(new ProcessoTimelinePendencia("P", "Pendência", "OPERACIONAL", "MEDIA", Instant.now(), "OPERACAO", false, List.of("fundamento"))), List.of(), List.of(), Instant.now());
    }

    private ProcessoPapelPerfil perfilMock(String code) {
        return new ProcessoPapelPerfil(
                code, code, "PAINEL", "PROFILE", "NIVEL", "blue",
                List.of(), List.of("Receber"), List.of("Preparar"), List.of("Aprovar"), List.of("Assinar"), List.of("Peticionar"), List.of("Certificar"), List.of("Redistribuir"), List.of("Recorrer"), List.of("Embargar"), List.of("base"), List.of("MFA"), List.of("fundamento")
        );
    }
}
