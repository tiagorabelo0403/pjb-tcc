package com.tcc.pjb.backend.service.triagem;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class TriagemNacionalIAEngineRefinementArchitectureTest {

    @Test
    void engineDeveDelegarValidacaoEInferenciaParaSuportesDedicados() {
        Constructor<?> constructor = Arrays.stream(TriagemNacionalIAEngine.class.getDeclaredConstructors())
                .findFirst()
                .orElseThrow();

        Set<Class<?>> parameterTypes = Arrays.stream(constructor.getParameterTypes()).collect(Collectors.toSet());

        assertThat(constructor.getParameterCount()).isLessThanOrEqualTo(6);
        assertThat(parameterTypes)
                .contains(TriagemNacionalValidationSupport.class, TriagemNacionalInferenceSupport.class);
    }

    @Test
    void engineNaoDeveReabsorverHeuristicasDeTriagem() {
        Set<String> methodNames = Arrays.stream(TriagemNacionalIAEngine.class.getDeclaredMethods())
                .map(method -> method.getName())
                .collect(Collectors.toSet());

        assertThat(methodNames)
                .doesNotContain(
                        "normalizarPedido",
                        "verificarDocumentosDasPartes",
                        "verificarDocumentosObrigatorios",
                        "sugerirClassificacao",
                        "analisarPrescricao",
                        "analisarCompetencia",
                        "detectarConexos",
                        "construirResumo"
                );
    }
}
