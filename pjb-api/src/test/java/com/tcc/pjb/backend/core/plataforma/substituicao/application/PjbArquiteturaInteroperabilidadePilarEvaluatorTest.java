package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.core.governance.idempotency.RequestIdempotencyService;
import com.tcc.pjb.backend.core.idempotency.ActionIdempotencyService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoPilar;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbFechamentoStatus;
import com.tcc.pjb.backend.core.processo.migracao.application.ProcessoMigracaoApplicationService;
import com.tcc.pjb.backend.core.processo.migracao.application.ProcessoMigracaoFactoryApplicationService;
import com.tcc.pjb.backend.core.processo.transicao.application.ProcessoConvivenciaTransicaoApplicationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class PjbArquiteturaInteroperabilidadePilarEvaluatorTest {

    private static <T> ObjectProvider<T> providerOf(T value) {
        @SuppressWarnings("unchecked")
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }

    @Test
    void tudoDisponivel_pilarConcluidoEPronto() {
        PjbArquiteturaInteroperabilidadePilarEvaluator evaluator = new PjbArquiteturaInteroperabilidadePilarEvaluator(
                providerOf(mock(PjbSubstituicaoLegadosApplicationService.class)),
                providerOf(mock(ProcessoMigracaoFactoryApplicationService.class)),
                providerOf(mock(ProcessoConvivenciaTransicaoApplicationService.class)),
                providerOf(mock(ProcessoMigracaoApplicationService.class)),
                providerOf(mock(AuditLedgerService.class)),
                providerOf(mock(ActionIdempotencyService.class)),
                providerOf(mock(RequestIdempotencyService.class))
        );

        PjbArquiteturaSubstituicaoPilar pilar = evaluator.avaliar();

        assertThat(pilar.codigo()).isEqualTo("interoperabilidade-migracao");
        assertThat(pilar.pronto()).isTrue();
        assertThat(pilar.status()).isEqualTo(PjbFechamentoStatus.CONCLUIDA);
        assertThat(pilar.capacidades()).hasSize(5);
    }

    @Test
    void nadaDisponivel_pilarNaoConcluidoENaoPronto() {
        PjbArquiteturaInteroperabilidadePilarEvaluator evaluator = new PjbArquiteturaInteroperabilidadePilarEvaluator(
                providerOf(null), providerOf(null), providerOf(null),
                providerOf(null), providerOf(null), providerOf(null), providerOf(null)
        );

        PjbArquiteturaSubstituicaoPilar pilar = evaluator.avaliar();

        assertThat(pilar.pronto()).isFalse();
        assertThat(pilar.status()).isNotEqualTo(PjbFechamentoStatus.CONCLUIDA);
        assertThat(pilar.capacidades()).allSatisfy(cap -> assertThat(cap.status()).isNotEqualTo(PjbFechamentoStatus.CONCLUIDA));
    }
}
