package com.tcc.pjb.backend.core.distribuicao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbDistribuicaoStrategyResolverContextTest {

    private final PjbDistribuicaoStrategyResolver resolver = new PjbDistribuicaoStrategyResolver();

    @Test
    void nullContextRetornaOrdinaria() {
        PjbDistribuicaoStrategy strategy = resolver.resolveByContext(null);
        assertEquals("DISTRIBUICAO_ORDINARIA_CONTEXTUAL", strategy.strategyCode());
    }

    @Test
    void eleitoralSempreOrdinario() {
        PjbDistribuicaoContext ctx = ctx(RitoProcessual.ELEITORAL, true, true, false, false, 1);
        assertEquals("DISTRIBUICAO_ORDINARIA_CONTEXTUAL", resolver.resolveByContext(ctx).strategyCode());
    }

    @Test
    void militarSempreOrdinario() {
        PjbDistribuicaoContext ctx = ctx(RitoProcessual.MILITAR_PROCESSO_PENAL_MILITAR, true, true, false, false, 1);
        assertEquals("DISTRIBUICAO_ORDINARIA_CONTEXTUAL", resolver.resolveByContext(ctx).strategyCode());
    }

    @Test
    void altaComplexidadeOrdinario() {
        PjbDistribuicaoContext ctx = ctx(RitoProcessual.COMUM_ORDINARIO, false, false, false, false, 8);
        assertEquals("DISTRIBUICAO_ORDINARIA_CONTEXTUAL", resolver.resolveByContext(ctx).strategyCode());
    }

    @Test
    void sigiloSemJuizo100DigitalOrdinario() {
        PjbDistribuicaoContext ctx = ctx(RitoProcessual.COMUM_ORDINARIO, false, false, true, false, 1);
        assertEquals("DISTRIBUICAO_ORDINARIA_CONTEXTUAL", resolver.resolveByContext(ctx).strategyCode());
    }

    @Test
    void juizo100DigitalRoteiaNucleo() {
        PjbDistribuicaoContext ctx = ctx(RitoProcessual.COMUM_ORDINARIO, true, false, false, false, 1);
        assertEquals("DISTRIBUICAO_NUCLEO_DIGITAL_UNIVERSAL", resolver.resolveByContext(ctx).strategyCode());
    }

    @Test
    void nucleo40RoteiaNucleo() {
        PjbDistribuicaoContext ctx = ctx(RitoProcessual.COMUM_ORDINARIO, false, true, false, false, 1);
        assertEquals("DISTRIBUICAO_NUCLEO_DIGITAL_UNIVERSAL", resolver.resolveByContext(ctx).strategyCode());
    }

    @Test
    void juizadoEspecialRoteiaNucleoMesmoSemFlags() {
        PjbDistribuicaoContext ctx = ctx(RitoProcessual.JUIZADO_ESPECIAL_CIVEL, false, false, false, false, 1);
        assertEquals("DISTRIBUICAO_NUCLEO_DIGITAL_UNIVERSAL", resolver.resolveByContext(ctx).strategyCode());
    }

    @Test
    void sigiloComJuizo100DigitalRoteiaNucleo() {
        PjbDistribuicaoContext ctx = ctx(RitoProcessual.COMUM_ORDINARIO, true, false, true, false, 1);
        assertEquals("DISTRIBUICAO_NUCLEO_DIGITAL_UNIVERSAL", resolver.resolveByContext(ctx).strategyCode());
    }

    @Test
    void targetLaneNucleoCompoeLane() {
        PjbDistribuicaoContext ctx = new PjbDistribuicaoContext(
                RitoProcessual.JUIZADO_ESPECIAL_CIVEL, "TJSP", "SAO_PAULO", "001",
                "CIVEL", BigDecimal.valueOf(10000), "CONSUMIDOR", GrauJurisdicao.PRIMEIRO_GRAU,
                true, false, false, false, 1, List.of());
        PjbDistribuicaoStrategy strategy = resolver.resolveByContext(ctx);
        String lane = strategy.targetLaneFromContext(ctx);
        assertTrue(lane.contains("NUCLEO_DIGITAL_UNIVERSAL"));
        assertTrue(lane.contains("JUIZADO_ESPECIAL_CIVEL"));
        assertTrue(lane.contains("TJSP"));
    }

    @Test
    void targetLaneOrdinariaCompoeLane() {
        PjbDistribuicaoContext ctx = new PjbDistribuicaoContext(
                RitoProcessual.COMUM_ORDINARIO, "TJRJ", "RIO_DE_JANEIRO", "002",
                "CIVEL", BigDecimal.valueOf(50000), "CONTRATOS", GrauJurisdicao.PRIMEIRO_GRAU,
                false, false, false, false, 2, List.of());
        PjbDistribuicaoStrategy strategy = resolver.resolveByContext(ctx);
        String lane = strategy.targetLaneFromContext(ctx);
        assertTrue(lane.contains("VARA_COMUM_ORIGEM"));
        assertTrue(lane.contains("COMUM_ORDINARIO"));
        assertTrue(lane.contains("TJRJ"));
    }

    @Test
    void targetLaneFromContextNaoNuloParaQualquerStrategy() {
        PjbDistribuicaoStrategy nucleo = new PjbDistribuicaoNucleoDigitalStrategy();
        PjbDistribuicaoStrategy ordinaria = new PjbDistribuicaoOrdinariaStrategy();
        assertNotNull(nucleo.targetLaneFromContext(null));
        assertNotNull(ordinaria.targetLaneFromContext(null));
    }

    private PjbDistribuicaoContext ctx(RitoProcessual rito, boolean juizo100Digital, boolean nucleo40,
                                        boolean sigilo, boolean urgente, int complexidade) {
        return new PjbDistribuicaoContext(
                rito, "TJ", "COMARCA", "UNIDADE", "COMPETENCIA",
                BigDecimal.ONE, "MATERIA", GrauJurisdicao.PRIMEIRO_GRAU,
                juizo100Digital, nucleo40, sigilo, urgente, complexidade, List.of());
    }
}
