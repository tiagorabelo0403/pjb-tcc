package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.explainability.DecisionTraceService;
import com.tcc.pjb.backend.core.governance.idempotency.RequestIdempotencyService;
import com.tcc.pjb.backend.core.idempotency.ActionIdempotencyService;
import com.tcc.pjb.backend.core.jobs.runtime.JobAdminService;
import com.tcc.pjb.backend.core.jobs.runtime.JobCircuitBreaker;
import com.tcc.pjb.backend.core.jobs.runtime.JobExecutionService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoPilar;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbFechamentoStatus;
import com.tcc.pjb.backend.core.processo.producao.application.ProcessoOperacaoTransversalApplicationService;
import com.tcc.pjb.backend.core.resilience.LocalCircuitBreaker;
import com.tcc.pjb.backend.core.security.abac.PjbAuthorizationService;
import com.tcc.pjb.backend.service.admin.AdministradorNacionalGovernanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class PjbArquiteturaConfiabilidadePilarEvaluatorTest {

    private static <T> ObjectProvider<T> providerOf(T value) {
        @SuppressWarnings("unchecked")
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }

    private PjbArquiteturaConfiabilidadePilarEvaluator evaluator(AdministradorNacionalGovernanceService administrador, boolean disponivel) {
        return new PjbArquiteturaConfiabilidadePilarEvaluator(
                administrador,
                providerOf(disponivel ? mock(ProcessoOperacaoTransversalApplicationService.class) : null),
                providerOf(disponivel ? mock(ActionIdempotencyService.class) : null),
                providerOf(disponivel ? mock(RequestIdempotencyService.class) : null),
                providerOf(disponivel ? mock(JobExecutionService.class) : null),
                providerOf(disponivel ? mock(JobAdminService.class) : null),
                providerOf(disponivel ? mock(LocalCircuitBreaker.class) : null),
                providerOf(disponivel ? mock(JobCircuitBreaker.class) : null),
                providerOf(disponivel ? mock(AuditLedgerService.class) : null),
                providerOf(disponivel ? mock(DecisionTraceService.class) : null),
                providerOf(disponivel ? mock(PjbAuthorizationService.class) : null)
        );
    }

    @Test
    void tudoDisponivelBuildGateAprovadoETaxaExpiracaoBaixa_pilarConcluidoEPronto() {
        PjbArquiteturaConfiabilidadePilarEvaluator evaluator = evaluator(mock(AdministradorNacionalGovernanceService.class), true);

        PjbArquiteturaSubstituicaoPilar pilar = evaluator.avaliar(100L, 2L, true);

        assertThat(pilar.codigo()).isEqualTo("confiabilidade-institucional");
        assertThat(pilar.pronto()).isTrue();
        assertThat(pilar.status()).isEqualTo(PjbFechamentoStatus.CONCLUIDA);
        assertThat(pilar.capacidades()).hasSize(5);
    }

    @Test
    void taxaExpiracaoAlta_rebaixaScoreDaObservabilidadeSemDerrubarAsDemais() {
        PjbArquiteturaConfiabilidadePilarEvaluator evaluator = evaluator(mock(AdministradorNacionalGovernanceService.class), true);

        PjbArquiteturaSubstituicaoPilar pilarBaixa = evaluator.avaliar(100L, 2L, true);
        PjbArquiteturaSubstituicaoPilar pilarAlta = evaluator.avaliar(100L, 50L, true);

        int scoreObservabilidadeBaixa = pilarBaixa.capacidades().stream()
                .filter(c -> c.codigo().equals("conf.observabilidade")).findFirst().orElseThrow().score();
        int scoreObservabilidadeAlta = pilarAlta.capacidades().stream()
                .filter(c -> c.codigo().equals("conf.observabilidade")).findFirst().orElseThrow().score();

        assertThat(scoreObservabilidadeAlta).isLessThan(scoreObservabilidadeBaixa);
    }

    @Test
    void buildGateReprovadoENadaDisponivel_pilarNaoPronto() {
        PjbArquiteturaConfiabilidadePilarEvaluator evaluator = evaluator(mock(AdministradorNacionalGovernanceService.class), false);

        PjbArquiteturaSubstituicaoPilar pilar = evaluator.avaliar(0L, 0L, false);

        assertThat(pilar.pronto()).isFalse();
        assertThat(pilar.status()).isNotEqualTo(PjbFechamentoStatus.CONCLUIDA);
    }
}
