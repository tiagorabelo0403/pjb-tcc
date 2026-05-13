package com.tcc.pjb.backend.service.processual.sustentacao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.plataforma.sustentacao.application.PjbPlataformaSustentacaoApplicationService;
import com.tcc.pjb.backend.core.plataforma.sustentacao.domain.PjbPlataformaSustentacaoAggregate;
import com.tcc.pjb.backend.core.plataforma.sustentacao.domain.PjbPlataformaSustentacaoCenario;
import com.tcc.pjb.backend.core.plataforma.sustentacao.domain.PjbPlataformaSustentacaoEixo;
import com.tcc.pjb.backend.core.plataforma.sustentacao.domain.PjbPlataformaSustentacaoModulo;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PjbPlataformaSustentacaoFacadeServiceTest {

    @Test
    void shouldMapAggregateToExplicitDtoContract() {
        PjbPlataformaSustentacaoApplicationService applicationService = mock(PjbPlataformaSustentacaoApplicationService.class);
        PjbPlataformaSustentacaoFacadeService facadeService = new PjbPlataformaSustentacaoFacadeService(applicationService);

        when(applicationService.avaliar()).thenReturn(new PjbPlataformaSustentacaoAggregate(
                88,
                true,
                7,
                8,
                List.of(new PjbPlataformaSustentacaoEixo("gate.arquitetural", "Gate arquitetural", 91, "PRONTO", true, List.of("buildGate=true"), List.of(), List.of("manter disciplina"), Map.of("score", 91))),
                List.of(new PjbPlataformaSustentacaoModulo("mod.processual", "Núcleo processual", "CORE", 12, 93, "CONECTADO", List.of("routing", "sigilo"), List.of())),
                List.of(new PjbPlataformaSustentacaoCenario("civil-comum", "Cível comum", "TJCE", "CIVEL", "COMUM_ORDINARIO", 90, true, List.of(), List.of("cenário resolvido"))),
                List.of(),
                List.of("executar build local"),
                List.of("snapshot consolidado"),
                Instant.parse("2026-03-22T18:00:00Z")
        ));

        var response = facadeService.avaliar();

        assertThat(response.scoreGeral()).isEqualTo(88);
        assertThat(response.aptoPreBuild()).isTrue();
        assertThat(response.eixos()).hasSize(1);
        assertThat(response.modulos()).hasSize(1);
        assertThat(response.cenariosDourados()).hasSize(1);
        assertThat(response.eixos().getFirst().evidencias()).containsEntry("score", 91);
        assertThat(response.modulos().getFirst().status()).isEqualTo("CONECTADO");
        assertThat(response.cenariosDourados().getFirst().tribunalCodigo()).isEqualTo("TJCE");
    }
}
