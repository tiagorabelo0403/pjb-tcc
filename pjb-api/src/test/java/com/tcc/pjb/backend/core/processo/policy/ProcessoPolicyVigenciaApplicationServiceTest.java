package com.tcc.pjb.backend.core.processo.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tcc.pjb.backend.core.processo.dsl.application.ProcessoDslApplicationService;
import com.tcc.pjb.backend.core.processo.dsl.domain.ProcessoDslAggregate;
import com.tcc.pjb.backend.core.processo.dsl.domain.ProcessoDslBlock;
import com.tcc.pjb.backend.core.processo.dsl.domain.ProcessoDslRule;
import com.tcc.pjb.backend.core.processo.dsl.domain.ProcessoDslVersion;
import com.tcc.pjb.backend.core.processo.policy.application.ProcessoPolicyVigenciaApplicationService;
import com.tcc.pjb.backend.core.processo.prazo.application.ProcessoPrazoApplicationService;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoAggregate;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoCienciaProfile;
import com.tcc.pjb.backend.core.processo.prazo.domain.ProcessoPrazoIdentity;
import com.tcc.pjb.backend.core.processo.unificado.domain.ProcessoUnificadoIdentity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProcessoPolicyVigenciaApplicationServiceTest {

    @Mock
    private ProcessoDslApplicationService processoDslApplicationService;

    @Mock
    private ProcessoPrazoApplicationService processoPrazoApplicationService;

    @Test
    void deveAvaliarPoliticaPorVigenciaComJanelaAtiva() {
        when(processoDslApplicationService.detalhar(88L)).thenReturn(new ProcessoDslAggregate(
                new ProcessoUnificadoIdentity(88L, "0002", "0002", "TJCE", "CE", "Fortaleza", "2a Vara", "AÇÃO CÍVEL", "Contrato", "Autor", "Réu", List.of("CIVEL")),
                new ProcessoDslVersion("DSL", "2026.1", LocalDate.of(2026, 1, 1), null, "CATALOGO_VERSIONADO", List.of("FLUXO_BASE")),
                1,
                0,
                List.of(new ProcessoDslBlock("FLUXO_BASE", "Fluxo", "2026.1", List.of(new ProcessoDslRule("PETICIONAR", "FLUXO_BASE", "Peticionar", "true", "ALLOW", List.of("DEFENSOR"), List.of("CHECK"), false, LocalDate.of(2026, 1, 1), null)), List.of("service"))),
                List.of("INVARIANTE"),
                Instant.now()
        ));
        when(processoPrazoApplicationService.detalhar(88L)).thenReturn(new ProcessoPrazoAggregate(
                new ProcessoPrazoIdentity(88L, "0002", "TJCE", "CE", "Fortaleza", "2a Vara", "CIVEL", "COMUM", "CONHECIMENTO", "EM_ANDAMENTO", List.of("CIVEL")),
                new ProcessoPrazoCienciaProfile("ELETRONICA", false, false, false, false, List.of(), List.of()),
                List.of(),
                0,
                0,
                0,
                0,
                "CONTROLADA",
                List.of(),
                List.of(),
                Instant.now()
        ));

        ProcessoPolicyVigenciaApplicationService service = new ProcessoPolicyVigenciaApplicationService(processoDslApplicationService, processoPrazoApplicationService);
        var aggregate = service.avaliar(88L, LocalDate.of(2026, 3, 21));

        assertThat(aggregate.activeWindows()).isEqualTo(1);
        assertThat(aggregate.decisions()).singleElement().extracting("active").isEqualTo(true);
    }
}
