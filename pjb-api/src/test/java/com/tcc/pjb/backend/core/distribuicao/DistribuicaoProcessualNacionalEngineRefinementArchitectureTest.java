package com.tcc.pjb.backend.core.distribuicao;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class DistribuicaoProcessualNacionalEngineRefinementArchitectureTest {

    @Test
    void engineDeveDelegarTrilhoEspecializadoEAdaptacaoDeProcessoParaSuportesDedicados() {
        Constructor<?> constructor = Arrays.stream(DistribuicaoProcessualNacionalEngine.class.getDeclaredConstructors())
                .findFirst()
                .orElseThrow();

        Set<Class<?>> parameterTypes = Arrays.stream(constructor.getParameterTypes()).collect(Collectors.toSet());

        assertThat(constructor.getParameterCount()).isLessThanOrEqualTo(8);
        assertThat(parameterTypes)
                .contains(DistribuicaoProcessualTrackSupport.class, DistribuicaoProcessualProcessoSupport.class);
    }

    @Test
    void engineNaoDeveReabsorverHeuristicasDeTrilhoEInferenciaDeProcesso() {
        Set<String> methodNames = Arrays.stream(DistribuicaoProcessualNacionalEngine.class.getDeclaredMethods())
                .map(method -> method.getName())
                .collect(Collectors.toSet());

        assertThat(methodNames)
                .doesNotContain(
                        "buildFromProcesso",
                        "resolveSpecializedTrack",
                        "buildSpecializedAlertas",
                        "buildSpecializedFundamentos",
                        "buildSpecializedReviewChecklist",
                        "inferAreaEspecializada",
                        "isUrgentProcess",
                        "requiresGovernanceEscalation"
                );
    }
}
