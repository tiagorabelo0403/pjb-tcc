package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.comunicacao.judicial.CitacaoIntimacaoEngine;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.NationalRecursalMeshEngine;
import com.tcc.pjb.backend.core.kernel.recursal.mesh.rules.TribunalRegionalEleitoralRuleProfile;
import com.tcc.pjb.backend.core.kernel.recursal.template.impl.JuizadoRecursalTemplate;
import com.tcc.pjb.backend.core.kernel.recursal.template.impl.TrabalhistaRecursalTemplate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbArquiteturaSubstituicaoPilar;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbFechamentoStatus;
import com.tcc.pjb.backend.core.processo.lifecycle.eleitoral.EleitoralLifecyclePack;
import com.tcc.pjb.backend.core.processo.lifecycle.civel.JuizadoLifecyclePack;
import com.tcc.pjb.backend.core.processo.lifecycle.militar.MilitarLifecyclePack;
import com.tcc.pjb.backend.core.processo.recursal.application.ProcessoRecursalApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloInteligenteApplicationService;
import com.tcc.pjb.backend.core.processo.sigilo.application.ProcessoSigiloNotificacaoApplicationService;
import com.tcc.pjb.backend.core.processo.trabalho.application.ProcessoTrabalhoApplicationService;
import com.tcc.pjb.backend.core.processo.vertical.estadual.civel.application.ProcessoVerticalCivelPrimeiroGrauApplicationService;
import com.tcc.pjb.backend.core.processo.vertical.estadual.fazenda.application.ProcessoVerticalExecucaoFiscalFazendariaApplicationService;
import com.tcc.pjb.backend.core.processo.vertical.estadual.penal.application.ProcessoVerticalPenalCustodiaApplicationService;
import com.tcc.pjb.backend.core.processual.routing.RecursalCollegiateResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

class PjbArquiteturaMotorProcessualPilarEvaluatorTest {

    private static <T> ObjectProvider<T> providerOf(T value) {
        @SuppressWarnings("unchecked")
        ObjectProvider<T> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(value);
        return provider;
    }

    private PjbArquiteturaMotorProcessualPilarEvaluator evaluator(boolean disponivel) {
        return new PjbArquiteturaMotorProcessualPilarEvaluator(
                providerOf(disponivel ? mock(ProcessoVerticalCivelPrimeiroGrauApplicationService.class) : null),
                providerOf(disponivel ? mock(ProcessoVerticalPenalCustodiaApplicationService.class) : null),
                providerOf(disponivel ? mock(ProcessoVerticalExecucaoFiscalFazendariaApplicationService.class) : null),
                providerOf(disponivel ? mock(ProcessoTrabalhoApplicationService.class) : null),
                providerOf(disponivel ? mock(TrabalhistaRecursalTemplate.class) : null),
                providerOf(disponivel ? mock(JuizadoLifecyclePack.class) : null),
                providerOf(disponivel ? mock(JuizadoRecursalTemplate.class) : null),
                providerOf(disponivel ? mock(EleitoralLifecyclePack.class) : null),
                providerOf(disponivel ? mock(TribunalRegionalEleitoralRuleProfile.class) : null),
                providerOf(disponivel ? mock(MilitarLifecyclePack.class) : null),
                providerOf(disponivel ? mock(ProcessoRecursalApplicationService.class) : null),
                providerOf(disponivel ? mock(NationalRecursalMeshEngine.class) : null),
                providerOf(disponivel ? mock(ProcessoSigiloApplicationService.class) : null),
                providerOf(disponivel ? mock(ProcessoSigiloInteligenteApplicationService.class) : null),
                providerOf(disponivel ? mock(ProcessoSigiloNotificacaoApplicationService.class) : null),
                providerOf(disponivel ? mock(CitacaoIntimacaoEngine.class) : null),
                providerOf(disponivel ? mock(RecursalCollegiateResolver.class) : null)
        );
    }

    @Test
    void tudoDisponivel_pilarConcluidoEPronto() {
        PjbArquiteturaSubstituicaoPilar pilar = evaluator(true).avaliar();

        assertThat(pilar.codigo()).isEqualTo("motor-processual-nacional");
        assertThat(pilar.pronto()).isTrue();
        assertThat(pilar.status()).isEqualTo(PjbFechamentoStatus.CONCLUIDA);
        assertThat(pilar.capacidades()).hasSize(9);
        assertThat(pilar.capacidades()).allSatisfy(cap -> assertThat(cap.status()).isEqualTo(PjbFechamentoStatus.CONCLUIDA));
    }

    @Test
    void nadaDisponivel_pilarNaoConcluidoENaoPronto() {
        PjbArquiteturaSubstituicaoPilar pilar = evaluator(false).avaliar();

        assertThat(pilar.pronto()).isFalse();
        assertThat(pilar.status()).isNotEqualTo(PjbFechamentoStatus.CONCLUIDA);
        assertThat(pilar.capacidades()).allSatisfy(cap -> assertThat(cap.status()).isNotEqualTo(PjbFechamentoStatus.CONCLUIDA));
    }
}
