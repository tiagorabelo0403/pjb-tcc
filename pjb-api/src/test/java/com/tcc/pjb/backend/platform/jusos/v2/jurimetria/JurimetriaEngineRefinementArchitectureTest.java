package com.tcc.pjb.backend.platform.jusos.v2.jurimetria;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class JurimetriaEngineRefinementArchitectureTest {

    @Test
    void engineDeveDelegarAgregacaoRiscoENarrativaParaSuportesDedicados() {
        Constructor<?> constructor = Arrays.stream(JurimetriaEngine.class.getDeclaredConstructors())
                .findFirst()
                .orElseThrow();

        Set<Class<?>> parameterTypes = Arrays.stream(constructor.getParameterTypes()).collect(Collectors.toSet());

        assertThat(constructor.getParameterCount()).isLessThanOrEqualTo(13);
        assertThat(parameterTypes)
                .contains(JurimetriaAggregationSupport.class, JurimetriaRiskAnalysisSupport.class, JurimetriaNarrativeSupport.class);
    }

    @Test
    void engineNaoDeveReabsorverHeuristicasDeAgregacaoRiscoOuNarrativa() {
        Set<String> methodNames = Arrays.stream(JurimetriaEngine.class.getDeclaredMethods())
                .map(method -> method.getName())
                .collect(Collectors.toSet());

        assertThat(methodNames)
                .doesNotContain(
                        "calcularIndicadores",
                        "calcularRisco",
                        "construirPerfilTribunal",
                        "gerarAlertasEstrategicos",
                        "construirCenarios",
                        "carregarBaseLocal",
                        "formatarPrecedentes",
                        "construirMetodologia",
                        "baseProbabilityByRamo",
                        "estimarTempoDias",
                        "interpretarTempo",
                        "buildDeadlineTriggers",
                        "mapearIndicadoresIA"
                );
    }
}
