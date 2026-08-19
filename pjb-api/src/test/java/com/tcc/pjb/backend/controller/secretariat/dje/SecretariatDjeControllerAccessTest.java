package com.tcc.pjb.backend.controller.secretariat.dje;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class SecretariatDjeControllerAccessTest {

    @Test
    void lifecycleRunPermaneceRestritoASecretaria() throws NoSuchMethodException {
        Method method = SecretariatDjeController.class.getMethod("lifecycleRun", java.time.LocalDate.class, Integer.class);
        String roles = method.getAnnotation(PreAuthorize.class).value();

        assertThat(roles).contains("SERVIDOR_JUDICIARIO", "SUPERVISOR", "DIRETOR_SECRETARIA", "CHEFE_SECRETARIA");
        assertThat(roles).doesNotContain("MAGISTRADO", "JUIZ");
    }

    @Test
    void publicacaoDoProcessoPassaAAceitarMagistratura() throws NoSuchMethodException {
        Method method = SecretariatDjeController.class.getMethod("publicacaoDoProcesso", Long.class);
        String roles = method.getAnnotation(PreAuthorize.class).value();

        assertThat(roles).contains("SERVIDOR_JUDICIARIO", "MAGISTRADO", "JUIZ", "JUIZ_TRABALHISTA");
    }
}
