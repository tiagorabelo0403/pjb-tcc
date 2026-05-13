package com.tcc.pjb.backend.service.processual.substituicao.nacional.programa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.plataforma.substituicao.application.PjbSubstituicaoNacionalProgramaApplicationService;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbFechamentoStatus;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoNacionalOnda;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoNacionalProgramaAggregate;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class PjbSubstituicaoNacionalProgramaFacadeServiceTest {

    @Test
    void deveMapearProgramaNacionalComOndas() {
        PjbSubstituicaoNacionalProgramaApplicationService applicationService = mock(PjbSubstituicaoNacionalProgramaApplicationService.class);
        PjbSubstituicaoNacionalProgramaFacadeService facadeService = new PjbSubstituicaoNacionalProgramaFacadeService(applicationService);

        when(applicationService.avaliar()).thenReturn(new PjbSubstituicaoNacionalProgramaAggregate(
                86,
                true,
                false,
                true,
                4,
                1,
                5,
                2,
                List.of(
                        new PjbSubstituicaoNacionalOnda("shadow", "Shadow", PjbFechamentoStatus.CONCLUIDA, 90, true, "ok", List.of("c1"), List.of("b1"), List.of("g1"), List.of("r1"), List.of("PJe"), List.of()),
                        new PjbSubstituicaoNacionalOnda("assistida", "Assistida", PjbFechamentoStatus.PARCIAL, 84, false, "ok", List.of("c2"), List.of("b2"), List.of("g2"), List.of("r2"), List.of("eproc"), List.of("a2"))
                ),
                List.of("pend1"),
                "ok",
                List.of("f1"),
                Instant.now()
        ));

        var response = facadeService.avaliar();

        assertEquals(86, response.scoreGeral());
        assertFalse(response.prontoCutoverNacional());
        assertEquals(2, response.ondas().size());
        assertNotNull(response.geradoEm());
    }
}
