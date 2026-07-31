package com.tcc.pjb.backend.tribunal.regras;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import com.tcc.pjb.backend.platform.jusos.v2.prazo.NationalPrazoEngine;
import com.tcc.pjb.backend.platform.jusos.v2.rules.NationalRulePackEngine;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TribunalRuleEngineTest {

    @Test
    void shouldTrimPluginRegistry() throws Exception {
        NationalRulePackEngine nationalRulePackEngine = mock(NationalRulePackEngine.class);
        NationalPrazoEngine nationalPrazoEngine = mock(NationalPrazoEngine.class);
        SalarioMinimoNacionalService salarioMinimoNacionalService = mock(SalarioMinimoNacionalService.class);
        TribunalRuleResolutionSupport resolutionSupport = new TribunalRuleResolutionSupport(
                nationalRulePackEngine, nationalPrazoEngine, salarioMinimoNacionalService);
        TribunalRulePackSynchronizationSupport rulePackSynchronizationSupport = new TribunalRulePackSynchronizationSupport(
                nationalRulePackEngine, resolutionSupport);
        TribunalRuleEngine engine = new TribunalRuleEngine(
                nationalRulePackEngine,
                nationalPrazoEngine,
                salarioMinimoNacionalService,
                resolutionSupport,
                rulePackSynchronizationSupport
        );

        for (int i = 0; i < 320; i++) {
            engine.substituirRegrasPlugin(
                    "plugin-" + i,
                    List.of(TribunalRuleEngine.EntradaRegra.nacional(
                            TribunalRuleEngine.ChaveRegra.PRAZO_SENTENCA,
                            i,
                            TribunalRuleEngine.TipoValor.INTEIRO,
                            "fundamento",
                            "descricao"
                    ))
            );
        }

        Field registryField = TribunalRuleEngine.class.getDeclaredField("pluginsRegistrados");
        registryField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ?> registry = (Map<String, ?>) registryField.get(engine);

        Field touchField = TribunalRuleEngine.class.getDeclaredField("pluginTouch");
        touchField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ?> touch = (Map<String, ?>) touchField.get(engine);

        assertThat(registry).hasSizeLessThanOrEqualTo(256);
        assertThat(touch).hasSizeLessThanOrEqualTo(256);
    }
}
