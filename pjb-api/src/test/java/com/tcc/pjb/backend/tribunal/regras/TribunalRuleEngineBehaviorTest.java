package com.tcc.pjb.backend.tribunal.regras;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.platform.jusos.v2.rules.NationalRulePackEngine;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import com.tcc.pjb.backend.tribunal.regras.snapshot.RegraResolvida;
import com.tcc.pjb.backend.tribunal.regras.snapshot.RelatorioCoberturaTribunal;

class TribunalRuleEngineBehaviorTest {

    @Test
    void deveResolverPrazoTribunalEAcusarDesvioEmRelacaoAoNacional() {
        SalarioMinimoNacionalService salario = mock(SalarioMinimoNacionalService.class);
        when(salario.multiplicar(any(BigDecimal.class), any(LocalDate.class)))
                .thenAnswer(invocation -> ((BigDecimal) invocation.getArgument(0)).multiply(new BigDecimal("1500.00")));

        NationalRulePackEngine nationalRulePackEngine = mock(NationalRulePackEngine.class);
        NationalPrazoEngine nationalPrazoEngine = mock(NationalPrazoEngine.class);
        TribunalRuleResolutionSupport resolutionSupport = new TribunalRuleResolutionSupport(
                nationalRulePackEngine, nationalPrazoEngine, salario);
        TribunalRulePackSynchronizationSupport rulePackSynchronizationSupport = new TribunalRulePackSynchronizationSupport(
                nationalRulePackEngine, resolutionSupport);
        TribunalRuleEngine engine = new TribunalRuleEngine(
                nationalRulePackEngine,
                nationalPrazoEngine,
                salario,
                resolutionSupport,
                rulePackSynchronizationSupport
        );
        engine.seedRegrasPadraoNacional();
        engine.cadastrarParaTribunal(
                "TJCE",
                TribunalRuleEngine.ChaveRegra.PRAZO_SENTENCA,
                45,
                TribunalRuleEngine.TipoValor.DURACAO_DIAS,
                TribunalRuleEngine.ModoSobrescrita.SUBSTITUIR,
                "Ato normativo local",
                "Sentença local em 45 dias",
                RamoDireito.CIVIL,
                GrauJurisdicao.PRIMEIRO_GRAU
        );

        TribunalRuleEngine.ContextoResolucao contexto = TribunalRuleEngine.ContextoResolucao.agora(
                "TJCE",
                null,
                null,
                RamoDireito.CIVIL,
                GrauJurisdicao.PRIMEIRO_GRAU
        );

        RegraResolvida resolvida = engine.resolverOuDefault(
                TribunalRuleEngine.ChaveRegra.PRAZO_SENTENCA,
                contexto,
                30,
                TribunalRuleEngine.TipoValor.DURACAO_DIAS
        );

        assertThat(resolvida.inteiro()).isEqualTo(45);
        assertThat(resolvida.nivelUsado()).isEqualTo(TribunalRuleEngine.NivelRegra.TRIBUNAL);
        assertThat(engine.analisarDesviosTribunal("TJCE", RamoDireito.CIVIL, GrauJurisdicao.PRIMEIRO_GRAU))
                .anySatisfy(desvio -> {
                    assertThat(desvio.chave().canonical()).isEqualTo(TribunalRuleEngine.ChaveRegra.PRAZO_SENTENCA.canonical());
                    assertThat(desvio.percentDesvio()).isGreaterThan(0d);
                });
    }

    @Test
    void deveMedirCoberturaQuandoTribunalPossuiRegraPersonalizada() {
        SalarioMinimoNacionalService salario = mock(SalarioMinimoNacionalService.class);
        when(salario.multiplicar(any(BigDecimal.class), any(LocalDate.class)))
                .thenAnswer(invocation -> ((BigDecimal) invocation.getArgument(0)).multiply(new BigDecimal("1500.00")));

        NationalRulePackEngine nationalRulePackEngine = mock(NationalRulePackEngine.class);
        NationalPrazoEngine nationalPrazoEngine = mock(NationalPrazoEngine.class);
        TribunalRuleResolutionSupport resolutionSupport = new TribunalRuleResolutionSupport(
                nationalRulePackEngine, nationalPrazoEngine, salario);
        TribunalRulePackSynchronizationSupport rulePackSynchronizationSupport = new TribunalRulePackSynchronizationSupport(
                nationalRulePackEngine, resolutionSupport);
        TribunalRuleEngine engine = new TribunalRuleEngine(
                nationalRulePackEngine,
                nationalPrazoEngine,
                salario,
                resolutionSupport,
                rulePackSynchronizationSupport
        );
        engine.seedRegrasPadraoNacional();
        engine.cadastrarParaTribunal(
                "TJRN",
                TribunalRuleEngine.ChaveRegra.NOTIF_WHATSAPP_ATIVO,
                true,
                TribunalRuleEngine.TipoValor.BOOLEANO,
                TribunalRuleEngine.ModoSobrescrita.SUBSTITUIR,
                "Resolução local",
                "WhatsApp habilitado",
                null,
                null
        );

        RelatorioCoberturaTribunal relatorio = engine.relatorioCobertura("TJRN");

        assertThat(relatorio.tribunalCodigo()).isEqualTo("TJRN");
        assertThat(relatorio.totalRegrasNacionais()).isGreaterThan(0);
        assertThat(relatorio.regrasPersonalizadas()).isGreaterThan(0);
        assertThat(relatorio.chavesPersonalizadas())
                .contains(TribunalRuleEngine.ChaveRegra.NOTIF_WHATSAPP_ATIVO.canonical());
    }
}
