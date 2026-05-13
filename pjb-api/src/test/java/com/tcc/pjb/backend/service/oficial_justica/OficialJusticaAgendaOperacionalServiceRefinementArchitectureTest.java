package com.tcc.pjb.backend.service.oficial_justica;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class OficialJusticaAgendaOperacionalServiceRefinementArchitectureTest {

    @Test
    void serviceDeveDelegarMontagemTelemetriaEPainelParaSuportesDedicados() {
        Constructor<?> constructor = Arrays.stream(OficialJusticaAgendaOperacionalService.class.getDeclaredConstructors())
                .findFirst()
                .orElseThrow();

        Set<Class<?>> parameterTypes = Arrays.stream(constructor.getParameterTypes()).collect(Collectors.toSet());

        assertThat(constructor.getParameterCount()).isLessThanOrEqualTo(7);
        assertThat(parameterTypes)
                .contains(
                        OficialJusticaAgendaAssemblySupport.class,
                        OficialJusticaAgendaTelemetrySupport.class,
                        OficialJusticaAgendaPanelSupport.class
                );
    }

    @Test
    void serviceNaoDeveReabsorverHeuristicasDeAgendaOperacional() {
        Set<String> methodNames = Arrays.stream(OficialJusticaAgendaOperacionalService.class.getDeclaredMethods())
                .map(method -> method.getName())
                .collect(Collectors.toSet());

        assertThat(methodNames)
                .doesNotContain(
                        "toAgendaRow",
                        "buildTerritorialHints",
                        "buildLiveDigests",
                        "reorderRows",
                        "buildStatusBuckets",
                        "buildReplanningSummary",
                        "resolveFrustrationCode",
                        "enrichQuickActions"
                );
    }
}
