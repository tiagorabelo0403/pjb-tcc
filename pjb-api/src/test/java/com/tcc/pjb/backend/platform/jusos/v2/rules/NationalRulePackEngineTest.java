package com.tcc.pjb.backend.platform.jusos.v2.rules;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.service.financeiro.SalarioMinimoNacionalService;
import java.lang.reflect.Field;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NationalRulePackEngineTest {

    @Test
    void shouldTrimCustomBucketsRegistry() throws Exception {
        NationalRulePackEngine engine = new NationalRulePackEngine(mock(SalarioMinimoNacionalService.class));

        for (int i = 0; i < 320; i++) {
            engine.registrarRegraCustomizada(
                    "TJ" + i,
                    RamoDireito.CIVIL,
                    new NationalRulePackEngine.RegraAlerta("COD_" + i, "desc", RamoDireito.CIVIL, "alerta", "INFO")
            );
        }

        Field customField = NationalRulePackEngine.class.getDeclaredField("regrasCustomizadas");
        customField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ?> custom = (Map<String, ?>) customField.get(engine);

        Field touchField = NationalRulePackEngine.class.getDeclaredField("bucketTouch");
        touchField.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, ?> touch = (Map<String, ?>) touchField.get(engine);

        assertThat(custom).hasSizeLessThanOrEqualTo(256);
        assertThat(touch).hasSizeLessThanOrEqualTo(256);
    }
}
