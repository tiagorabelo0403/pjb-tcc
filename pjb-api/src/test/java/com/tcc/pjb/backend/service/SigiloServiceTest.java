package com.tcc.pjb.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import org.junit.jupiter.api.Test;

class SigiloServiceTest {

    private final SigiloService service = new SigiloService();

    @Test
    void shouldKeepPublicForNullOrBlankCorpus() {
        SigiloService.SigiloDecision nullDecision = service.avaliarCorpus(null);
        SigiloService.SigiloDecision blankDecision = service.avaliarCorpus("   ");

        assertEquals(NivelSigilo.PUBLICO, nullDecision.nivel());
        assertEquals(NivelSigilo.PUBLICO, blankDecision.nivel());
        assertTrue(nullDecision.recomendacoes().contains("corpus_null"));
        assertTrue(blankDecision.recomendacoes().contains("corpus_vazio"));
    }

    @Test
    void shouldEscalateFamilyAndHealthSignalsToHighRestrictedLevel() {
        SigiloService.SigiloDecision decision = service.avaliarCorpus(
                "Ação de guarda de criança com laudo psiquiátrico e tratamento de saúde em prontuário médico."
        );

        assertEquals(NivelSigilo.SIGILO_N3, decision.nivel());
        assertTrue(decision.signals().contains(SigiloService.SigiloSignal.INFANCIA));
        assertTrue(decision.signals().contains(SigiloService.SigiloSignal.SAUDE));
        assertTrue(decision.recomendacoes().contains("ocultar_identificadores_de_menores"));
        assertTrue(decision.recomendacoes().contains("mascarar_dados_de_saude_em_resumos_publicos"));
        assertTrue(decision.recomendacoes().contains("exigir_justificativa_need_to_know"));
    }

    @Test
    void shouldApplyJusticeSecretForBankingTaxExposure() {
        SigiloService.SigiloDecision decision = service.avaliarCorpus(
                "Pedido com extrato bancário, dados fiscais e declaração de imposto de renda."
        );

        assertEquals(NivelSigilo.SEGREDO_JUSTICA, decision.nivel());
        assertTrue(decision.signals().contains(SigiloService.SigiloSignal.BANCARIO_FISCAL));
        assertTrue(decision.recomendacoes().contains("redacao_dados_financeiros_para_terceiros"));
    }

    @Test
    void shouldEvaluateProcessObjectWithPenalSensitiveAndLgpdSignals() {
        Processo processo = Processo.builder()
                .ramoDireito(RamoDireito.PENAL)
                .assunto("Interceptação em investigação de organização criminosa")
                .objetoProcessual("Coleta de biometria, CPF e dados sensíveis")
                .materialProbatorioResumo("Relatório de inteligência e investigação")
                .build();

        SigiloService.SigiloDecision decision = service.avaliar(processo);

        assertEquals(NivelSigilo.SIGILO_N3, decision.nivel());
        assertEquals(NivelSigilo.SIGILO_N3.nivel(), service.definirNivelSigilo(processo));
        assertTrue(decision.signals().contains(SigiloService.SigiloSignal.PENAL_SENSIVEL));
        assertTrue(decision.signals().contains(SigiloService.SigiloSignal.LGPD));
        assertTrue(decision.recomendacoes().contains("minimizar_divulgacao_de_medidas_sigilosas"));
    }

    @Test
    void shouldReturnPublicoForNullProcessoAndDefinirNivelZero() {
        SigiloService.SigiloDecision decision = service.avaliar(null);
        assertEquals(NivelSigilo.PUBLICO, decision.nivel());
        assertTrue(decision.recomendacoes().contains("processo_null"));
        assertEquals(0, service.definirNivelSigilo(null));
    }

    @Test
    void shouldReturnPublicoForEmptyProcesso() {
        Processo empty = Processo.builder().build();
        SigiloService.SigiloDecision decision = service.avaliar(empty);
        assertEquals(NivelSigilo.PUBLICO, decision.nivel());
        assertTrue(decision.recomendacoes().contains("corpus_vazio"));
    }

    @Test
    void shouldEscalateInfanciaAndViolenciaDomesticaSignals() {
        Processo infancia = Processo.builder().assunto("infancia").build();
        SigiloService.SigiloDecision d1 = service.avaliar(infancia);
        assertTrue(d1.nivel().nivel() >= NivelSigilo.SIGILO_N2.nivel());
        assertTrue(d1.signals().contains(SigiloService.SigiloSignal.INFANCIA));

        Processo vd = Processo.builder().assunto("violencia domestica").build();
        SigiloService.SigiloDecision d2 = service.avaliar(vd);
        assertTrue(d2.nivel().nivel() >= NivelSigilo.SIGILO_N2.nivel());
        assertTrue(d2.signals().contains(SigiloService.SigiloSignal.VIOLENCIA_DOMESTICA));
    }

    @Test
    void shouldEscalateSaudeSignalAndDefinirNivelAtLeastTwo() {
        Processo saude = Processo.builder().assunto("saude prontuario").build();
        SigiloService.SigiloDecision decision = service.avaliar(saude);
        assertTrue(decision.nivel().nivel() >= NivelSigilo.SIGILO_N2.nivel());
        assertTrue(decision.signals().contains(SigiloService.SigiloSignal.SAUDE));
        assertTrue(service.definirNivelSigilo(saude) >= 2);
    }

    @Test
    void shouldEscalatePenalSensivelFromSingleInterceptacaoCorpus() {
        SigiloService.SigiloDecision decision = service.avaliarCorpus("interceptacao");
        assertTrue(decision.nivel().nivel() >= NivelSigilo.SIGILO_N3.nivel());
        assertTrue(decision.signals().contains(SigiloService.SigiloSignal.PENAL_SENSIVEL));
    }

    @Test
    void shouldEscalateInfanciaFromCriancaGuardaCorpus() {
        SigiloService.SigiloDecision decision = service.avaliarCorpus("crianca guarda");
        assertTrue(decision.nivel().nivel() >= NivelSigilo.SIGILO_N2.nivel());
        assertTrue(decision.signals().contains(SigiloService.SigiloSignal.INFANCIA));
    }
}
