package com.tcc.pjb.backend.platform.jusos.v2.execucao;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ExecucaoCumprimentoEngineRefinementArchitectureTest {

    @Test
    void engineDeveDelegarPlanejamentoESimulacaoParaSuportesDedicados() {
        Constructor<?> constructor = Arrays.stream(ExecucaoCumprimentoEngine.class.getDeclaredConstructors())
                .findFirst()
                .orElseThrow();

        Set<Class<?>> parameterTypes = Arrays.stream(constructor.getParameterTypes()).collect(Collectors.toSet());

        assertThat(constructor.getParameterCount()).isLessThanOrEqualTo(8);
        assertThat(parameterTypes)
                .contains(ExecucaoCumprimentoPlanningSupport.class, ExecucaoCumprimentoSimulationSupport.class);
    }

    @Test
    void engineNaoDeveReabsorverHeuristicasDeExecucao() {
        Set<String> methodNames = Arrays.stream(ExecucaoCumprimentoEngine.class.getDeclaredMethods())
                .map(method -> method.getName())
                .collect(Collectors.toSet());

        assertThat(methodNames)
                .doesNotContain(
                        "analisarCompliance",
                        "construirMatriz",
                        "ordenarMeios",
                        "construirChecklistExecucao",
                        "resolverPrazoResposta",
                        "resolverFundamento"
                );
    }
}
