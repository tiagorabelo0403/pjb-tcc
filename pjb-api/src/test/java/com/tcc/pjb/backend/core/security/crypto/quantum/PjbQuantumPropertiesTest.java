package com.tcc.pjb.backend.core.security.crypto.quantum;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.BindException;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

class PjbQuantumPropertiesTest {

    private static PjbQuantumProperties bind(Map<String, Object> propriedades) {
        return new Binder(new MapConfigurationPropertySource(propriedades))
                .bind("pjb.crypto.pqc", PjbQuantumProperties.class)
                .get();
    }

    @Test
    void valorDoYamlLigaNoConjuntoDeParametros() {
        PjbQuantumProperties properties = bind(Map.of(
                "pjb.crypto.pqc.enabled", "true",
                "pjb.crypto.pqc.signature-algorithm", "ML-DSA-87"));

        assertThat(properties.enabled()).isTrue();
        assertThat(properties.signatureAlgorithm()).isEqualTo(PqcSignatureAlgorithm.ML_DSA_87);
    }

    @Test
    void algoritmoRetiradoDoProviderNaoSobeAAplicacao() {
        assertThatThrownBy(() -> bind(Map.of(
                "pjb.crypto.pqc.enabled", "true",
                "pjb.crypto.pqc.signature-algorithm", "DILITHIUM")))
                .isInstanceOf(BindException.class);
    }

    @Test
    void ausenciaDeAlgoritmoCaiNoNivelMaisAlto() {
        assertThat(bind(Map.of("pjb.crypto.pqc.enabled", "false")).signatureAlgorithm())
                .isEqualTo(PqcSignatureAlgorithm.ML_DSA_87);
    }
}
