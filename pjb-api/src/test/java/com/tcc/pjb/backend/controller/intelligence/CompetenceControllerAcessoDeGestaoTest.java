package com.tcc.pjb.backend.controller.intelligence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class CompetenceControllerAcessoDeGestaoTest {

    @Test
    void cargaDasVarasSoEhVistaPorQuemFazGestaoJudiciariaNuncaPorParteOuAdvogado() throws Exception {
        String regra = CompetenceController.class
                .getMethod("analyzeRedistribution", double.class)
                .getAnnotation(PreAuthorize.class)
                .value();

        assertThat(regra).startsWith("hasAnyRole(").contains("'MAGISTRADO'", "'SERVIDOR'", "'DESEMBARGADOR'", "'ADMINISTRADOR'");
        assertThat(regra).doesNotContain("ADVOGADO", "CIDADAO", "PARTE", "PROCURADOR", "DEFENSOR", "MINISTERIO_PUBLICO",
                "ADMINISTRADOR_JUDICIAL", "isAuthenticated", "permitAll");
    }
}
