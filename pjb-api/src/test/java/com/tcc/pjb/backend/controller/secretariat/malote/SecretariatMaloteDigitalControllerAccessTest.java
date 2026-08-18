package com.tcc.pjb.backend.controller.secretariat.malote;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.service.secretariat.ingest.ProcessoExternoCargaService;
import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class SecretariatMaloteDigitalControllerAccessTest {

    @Test
    void processarPermaneceRestritoASecretaria() throws NoSuchMethodException {
        Method method = SecretariatMaloteDigitalController.class.getMethod("processar", List.class);
        String roles = method.getAnnotation(PreAuthorize.class).value();

        assertThat(roles).contains("SERVIDOR_JUDICIARIO", "SUPERVISOR", "DIRETOR_SECRETARIA", "CHEFE_SECRETARIA");
        assertThat(roles).doesNotContain("MAGISTRADO", "JUIZ");
    }

    @Test
    void controllerDependeApenasDoServicoDeCargaExistente() throws NoSuchMethodException {
        var construtores = SecretariatMaloteDigitalController.class.getDeclaredConstructors();
        assertThat(construtores).hasSize(1);
        assertThat(construtores[0].getParameterTypes())
                .containsExactly(ProcessoExternoCargaService.class);
    }
}
