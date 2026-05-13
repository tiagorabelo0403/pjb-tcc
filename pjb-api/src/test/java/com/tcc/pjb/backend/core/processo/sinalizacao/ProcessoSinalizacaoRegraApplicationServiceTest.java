package com.tcc.pjb.backend.core.processo.sinalizacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.pregravacao.application.ProcessoPreGravacaoApplicationService;
import com.tcc.pjb.backend.core.processo.pregravacao.domain.ProcessoPreGravacaoAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloAggregate;
import com.tcc.pjb.backend.core.processo.sigilo.domain.ProcessoSigiloGuarda;
import com.tcc.pjb.backend.core.processo.sinalizacao.application.ProcessoSinalizacaoRegraApplicationService;
import com.tcc.pjb.backend.core.processo.timeline.application.ProcessoTimelineApplicationService;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineAggregate;
import com.tcc.pjb.backend.core.processo.timeline.domain.ProcessoTimelineIdentity;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAto;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoCompetencia;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoDiagnostico;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.StatusProcesso;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ProcessoSinalizacaoRegraApplicationServiceTest {

    @Test
    void deveElevarCorQuandoHouverBloqueioOuSigilo() {
        ProcessoRepository processoRepository = mock(ProcessoRepository.class);
        ProcessoTimelineApplicationService timeline = mock(ProcessoTimelineApplicationService.class);
        ProcessoSigiloApplicationService sigilo = mock(ProcessoSigiloApplicationService.class);
        ProcessoPreGravacaoApplicationService preGravacao = mock(ProcessoPreGravacaoApplicationService.class);
        ProcessoUnificadoApplicationService unificado = mock(ProcessoUnificadoApplicationService.class);

        ProcessoUnificadoIdentity identity = new ProcessoUnificadoIdentity(9L, "0009", "0009", "TJCE", "CE", "FORTALEZA", "1VC", "Classe", "Assunto", "Autor", "Réu", List.of());
        Processo processo = Processo.builder().id(9L).numeroProcesso("0009").ramoDireito(RamoDireito.PENAL).rito(RitoProcessual.PROCEDIMENTO_PENAL_COMUM).faseAtual(FaseProcessual.RECURSAL).statusProcesso(StatusProcesso.RECURSO_INTERPOSTO).nivelSigilo(NivelSigilo.SIGILO_N3).build();
        when(processoRepository.findById(9L)).thenReturn(Optional.of(processo));
        when(timeline.detalhar(9L)).thenReturn(new ProcessoTimelineAggregate(new ProcessoTimelineIdentity(9L, "0009", "PENAL", "PROCEDIMENTO_PENAL_COMUM", "RECURSAL", "RECURSO_INTERPOSTO", "TJCE", "VARA", List.of()), 2, 1, 1, List.of(), List.of(), List.of(), List.of("DECIDIR_RECURSO"), List.of("bloqueio"), Instant.now()));
        when(sigilo.detalhar(9L)).thenReturn(new ProcessoSigiloAggregate(identity, NivelSigilo.SIGILO_N3, "RESTRITO", true, true, true, 1L, 1L, 1L, 0L, List.of("SIGILO"), List.of(), List.of(new ProcessoSigiloGuarda("STEP_UP", "title", "PROCESSO", "ALTA", true, true, true, List.of(), List.of())), List.of(), List.of("base"), Instant.now()));
        when(unificado.detalhar(9L)).thenReturn(new ProcessoUnificadoAggregate(
                identity,
                new ProcessoUnificadoCompetencia("ESTADUAL", "PRIMEIRO_GRAU", "PENAL", "PROCEDIMENTO_PENAL_COMUM", "RECURSAL", "RECURSO_INTERPOSTO", "TJCE", "TJCE", "ORG", "1VC", "FILA", "MESA", "TERRITORIAL", "PREV", "DIST", "PENAL", "AUTO", "LINK", "ENVELOPE", "BAIXO", "MAGISTRADO", false, false, 24, List.of(), List.of("fund"), List.of(), new LinkedHashMap<>()),
                new ProcessoUnificadoDiagnostico(true, 0L, 0L, 1L, 0L, 1L, 1L, List.of(), List.of("fund"), Instant.now()),
                List.of(new ProcessoUnificadoAto("ASSINAR", "Assinar", "ATO", "TASK", "EIXO", "FILA", "INBOX", "fund", "RECURSAL", "RECURSAL", "A", "B", true, true, false, true, true, true, "motivo", "MAGISTRADO", "TK", List.of())),
                List.of(),
                List.of("DECIDIR_RECURSO"),
                Instant.now()));
        when(preGravacao.avaliar(9L, "MAGISTRATURA__MAGISTRADO_TITULAR", "ASSINAR")).thenReturn(new ProcessoPreGravacaoAggregate(identity, "MAGISTRATURA__MAGISTRADO_TITULAR", "ASSINAR", false, 1L, 1L, 1L, 1L, List.of("STEP_UP"), List.of(), List.of("corrigir"), List.of("base"), Instant.now()));

        ProcessoSinalizacaoRegraApplicationService service = new ProcessoSinalizacaoRegraApplicationService(processoRepository, timeline, sigilo, preGravacao, unificado);
        var aggregate = service.detalhar(9L, null);

        assertThat(aggregate.accentColor()).isIn("purple", "red", "black");
        assertThat(aggregate.separadores()).isNotEmpty();
    }
}
