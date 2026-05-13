package com.tcc.pjb.backend.core.processo.pregravacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.application.InstitutionalProceduralCoherenceApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain.InstitutionalProceduralActEvaluation;
import com.tcc.pjb.backend.core.comunicacao.institucional.coerencia.domain.InstitutionalProceduralCoherenceFinding;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRiskSeverity;
import com.tcc.pjb.backend.core.processo.documental.application.ProcessoDocumentoApplicationService;
import com.tcc.pjb.backend.core.processo.documental.domain.ProcessoDocumentoAggregate;
import com.tcc.pjb.backend.core.processo.documental.domain.ProcessoDocumentoIdentity;
import com.tcc.pjb.backend.core.processo.papel.application.ProcessoPapelApplicationService;
import com.tcc.pjb.backend.core.processo.papel.domain.ProcessoPapelPerfil;
import com.tcc.pjb.backend.core.processo.policy.application.ProcessoPolicyVigenciaApplicationService;
import com.tcc.pjb.backend.core.processo.policy.domain.ProcessoPolicyAggregate;
import com.tcc.pjb.backend.core.processo.policy.domain.ProcessoPolicyDecision;
import com.tcc.pjb.backend.core.processo.policy.domain.ProcessoPolicyWindow;
import com.tcc.pjb.backend.core.processo.prazo.application.ProcessoPrazoApplicationService;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoAggregate;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoCienciaProfile;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoIdentity;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoMarco;
import com.tcc.pjb.backend.core.processo.pregravacao.application.ProcessoPreGravacaoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.application.ProcessoUnificadoApplicationService;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAggregate;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoAto;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoCompetencia;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoDiagnostico;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoFinding;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessoPreGravacaoApplicationServiceTest {

    @Mock
    private ProcessoUnificadoApplicationService processoUnificadoApplicationService;
    @Mock
    private ProcessoPrazoApplicationService processoPrazoApplicationService;
    @Mock
    private ProcessoDocumentoApplicationService processoDocumentoApplicationService;
    @Mock
    private ProcessoPolicyVigenciaApplicationService processoPolicyVigenciaApplicationService;
    @Mock
    private ProcessoPapelApplicationService processoPapelApplicationService;
    @Mock
    private InstitutionalProceduralCoherenceApplicationService institutionalProceduralCoherenceApplicationService;

    @Test
    void deveBloquearPersistenciaQuandoFluxoSensivelNaoTemBaseDocumentalENemCoerencia() {
        when(processoUnificadoApplicationService.detalhar(88L)).thenReturn(new ProcessoUnificadoAggregate(
                new ProcessoUnificadoIdentity(88L, "0001", "0001", "TJCE", "CE", "Fortaleza", "1a Vara", "Ação Penal", "crime", "MP", "Réu", List.of("PENAL")),
                new ProcessoUnificadoCompetencia("ESTADUAL", "PRIMEIRO_GRAU", "PENAL", "PROCEDIMENTO_PENAL_COMUM", "CONHECIMENTO", "EM_ANDAMENTO", "TJCE", "TJCE", "Órgão", "1a Vara", "fila", "mesa", "LOCAL", "PREV", "SORTEIO", "PENAL", "PADRAO", "AUTO", "ENVELOPE", "CONTROLADO", "GABINETE", false, false, 24, List.of(), List.of("fundamento"), List.of("checklist"), new LinkedHashMap<>()),
                new ProcessoUnificadoDiagnostico(true, 0, 0, 1, 0, 1, 1, List.<ProcessoUnificadoFinding>of(), List.of("fundamento"), Instant.now()),
                List.of(new ProcessoUnificadoAto("ASSINAR_PARECER", "Assinar parecer", "MERITO", "PARECER", "MERITO", "fila", "inbox", "fundamento", "CONHECIMENTO", "CONHECIMENTO", "EM_ANDAMENTO", "EM_ANDAMENTO", true, false, false, true, false, true, "ok", "PROMOTOR", "TRANSICAO", List.of("alerta"))),
                List.of(),
                List.of("ASSINAR_PARECER"),
                Instant.now()
        ));
        when(processoPrazoApplicationService.detalhar(88L)).thenReturn(new ProcessoPrazoAggregate(
                new ProcessoPrazoIdentity(88L, "0001", "TJCE", "CE", "Fortaleza", "1a Vara", "PENAL", "PROCEDIMENTO_PENAL_COMUM", "CONHECIMENTO", "EM_ANDAMENTO", List.of("PENAL")),
                new ProcessoPrazoCienciaProfile("PESSOAL", true, true, false, true, List.of("GUARDA"), List.of("fundamento")),
                List.of(new ProcessoPrazoMarco("TRILHA_RECURSAL", "Recursal", "APELACAO", "RECURSAL", LocalDate.now().minusDays(6), LocalDate.now().minusDays(1), 5, 5, -1, true, false, true, "EFEITO", List.of("vencido"), List.of("CPP"))),
                1, 1, 0, 1, "EXPIRADA_OU_IRREGULAR", List.of("onda"), List.of("alerta"), Instant.now()
        ));
        when(processoDocumentoApplicationService.detalhar(88L)).thenReturn(new ProcessoDocumentoAggregate(
                new ProcessoDocumentoIdentity(88L, "0001", "PENAL", "PROCEDIMENTO_PENAL_COMUM", "CONHECIMENTO", "EM_ANDAMENTO", "TJCE", List.of("PENAL")),
                0, 0, 0, 0, 0, 0, List.of(), List.of("sem_documento"), List.of(), Instant.now()
        ));
        when(processoPolicyVigenciaApplicationService.avaliar(88L)).thenReturn(new ProcessoPolicyAggregate(
                new ProcessoUnificadoIdentity(88L, "0001", "0001", "TJCE", "CE", "Fortaleza", "1a Vara", "Ação Penal", "crime", "MP", "Réu", List.of("PENAL")),
                LocalDate.now(), 1, 1, 1,
                List.of(new ProcessoPolicyWindow("MERITO", "Mérito", LocalDate.of(2026,1,1), null, true, 1, List.of("CPP"))),
                List.of(new ProcessoPolicyDecision("POLICY_MERITO", "MERITO", true, "CRITICA", "Cobertura parcial", "rationale", List.of("REGRA_1"), List.of("REGRA_2"))),
                List.of("invariante"), Instant.now()
        ));
        when(processoPapelApplicationService.detalharPerfil(88L, "PROMOTORIA__PROMOTORIA_TITULAR")).thenReturn(new ProcessoPapelPerfil(
                "PROMOTORIA__PROMOTORIA_TITULAR", "Promotor", "PAINEL_TITULAR", "PROMOTOR", "NIVEL_3", "red",
                List.of(), List.of(), List.of(), List.of(), List.of("Assinar parecer"), List.of("Emitir parecer"), List.of(), List.of(), List.of(), List.of(), List.of("merito"), List.of("MFA"), List.of("fundamento")
        ));
        when(institutionalProceduralCoherenceApplicationService.avaliarAto("PROMOTORIA__PROMOTORIA_TITULAR", "ASSINAR_PARECER", 88L, "PROCEDIMENTO_PENAL_COMUM", "CONHECIMENTO", "EM_ANDAMENTO", "PENAL"))
                .thenReturn(new InstitutionalProceduralActEvaluation(
                        "ASSINAR_PARECER",
                        "Assinar parecer",
                        false,
                        true,
                        10,
                        "Ato negado para o contexto corrente",
                        List.of("MFA", "CERTIFICADO"),
                        List.of(new InstitutionalProceduralCoherenceFinding("COERENCIA_NEGADA", InstitutionalRiskSeverity.ALTA, true, "negado", List.of("evidencia"), List.of("fundamento"))),
                        List.of("fundamento")
                ));

        ProcessoPreGravacaoApplicationService service = new ProcessoPreGravacaoApplicationService(
                processoUnificadoApplicationService,
                processoPrazoApplicationService,
                processoDocumentoApplicationService,
                processoPolicyVigenciaApplicationService,
                processoPapelApplicationService,
                institutionalProceduralCoherenceApplicationService
        );

        var aggregate = service.avaliar(88L, "PROMOTORIA__PROMOTORIA_TITULAR", "ASSINAR_PARECER");

        assertThat(aggregate.persistenciaPermitida()).isFalse();
        assertThat(aggregate.blockingTriggers()).isGreaterThan(0);
        assertThat(aggregate.stepUpTriggers()).isGreaterThan(0);
    }
}
