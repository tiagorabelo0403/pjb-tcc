package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalSensitiveActAuthorizationApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoPilar;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbFechamentoStatus;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.service.admin.AdministradorNacionalGovernanceService;
import com.tcc.pjb.backend.service.competencia.CompetenceResolverService;
import com.tcc.pjb.backend.service.profile.PerfilCapabilityMatrixService;
import com.tcc.pjb.backend.service.rito.RitoResolutionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class PjbArquiteturaGovernancaPilarEvaluatorTest {

    private static <T> ObjectProvider<T> providerOf(T value) {
        @SuppressWarnings("unchecked")
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }

    @Test
    void tudoDisponivelEBuildGateAprovado_pilarConcluidoEPronto() {
        AdministradorNacionalGovernanceService administrador = mock(AdministradorNacionalGovernanceService.class);
        PjbArquiteturaGovernancaPilarEvaluator evaluator = new PjbArquiteturaGovernancaPilarEvaluator(
                administrador,
                providerOf(mock(CompetenceResolverService.class)),
                providerOf(mock(RitoResolutionService.class)),
                providerOf(mock(PerfilCapabilityMatrixService.class)),
                providerOf(mock(InstitutionalSensitiveActAuthorizationApplicationService.class)),
                providerOf(mock(CapabilityRateLimiter.class))
        );

        PjbArquiteturaSubstituicaoPilar pilar = evaluator.avaliar(true);

        assertThat(pilar.codigo()).isEqualTo("governanca-nacional");
        assertThat(pilar.pronto()).isTrue();
        assertThat(pilar.status()).isEqualTo(PjbFechamentoStatus.CONCLUIDA);
        assertThat(pilar.capacidades()).hasSize(5);
        assertThat(pilar.capacidades()).allSatisfy(cap -> assertThat(cap.status()).isEqualTo(PjbFechamentoStatus.CONCLUIDA));
    }

    @Test
    void semColaboradoresEBuildGateReprovado_pilarNaoConcluidoENaoPronto() {
        AdministradorNacionalGovernanceService administrador = mock(AdministradorNacionalGovernanceService.class);
        PjbArquiteturaGovernancaPilarEvaluator evaluator = new PjbArquiteturaGovernancaPilarEvaluator(
                administrador,
                providerOf(null),
                providerOf(null),
                providerOf(null),
                providerOf(null),
                providerOf(null)
        );

        PjbArquiteturaSubstituicaoPilar pilar = evaluator.avaliar(false);

        assertThat(pilar.pronto()).isFalse();
        assertThat(pilar.status()).isNotEqualTo(PjbFechamentoStatus.CONCLUIDA);
        // gov.governanca-operacional so depende de administradorNacionalGovernanceService (sempre presente)
        assertThat(pilar.capacidades()).anySatisfy(cap ->
                assertThat(cap.codigo()).isEqualTo("gov.governanca-operacional"));
        long concluidas = pilar.capacidades().stream().filter(c -> c.status() == PjbFechamentoStatus.CONCLUIDA).count();
        assertThat(concluidas).isLessThan(pilar.capacidades().size());
    }
}
