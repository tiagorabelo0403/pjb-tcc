package com.tcc.pjb.backend.core.digitalizacao;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class DigitalizacaoPropertiesTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(DigitalizacaoPropertiesConfiguration.class);

    @Test
    void deveSerCriadaComoJavaBeanSemQuebrarOApplicationContext() {
        DigitalizacaoProperties properties = new DigitalizacaoProperties();

        assertFalse(properties.enabled());
        assertEquals(70.0d, properties.confiancaMinimaAuto());
        assertEquals("por", properties.idioma());
        assertEquals(20, properties.reviewBatchSize());
        assertEquals(60L, properties.staleProcessingMinutes());
        assertEquals(300_000L, properties.governanceFixedRateMs());
    }

    @Test
    void deveReceberBindingRelaxadoDoSpringBootSemConstrutorCanonicoDeRecord() {
        contextRunner
                .withPropertyValues(
                        "pjb.digitalizacao.enabled=true",
                        "pjb.digitalizacao.confianca-minima-auto=81.5",
                        "pjb.digitalizacao.idioma-default=por+eng",
                        "pjb.digitalizacao.tesseract-executable=/opt/tesseract/bin/tesseract",
                        "pjb.digitalizacao.review-batch-size=33",
                        "pjb.digitalizacao.stale-processing-minutes=44",
                        "pjb.digitalizacao.governance-fixed-rate-ms=55000")
                .run(context -> {
                    assertTrue(context.containsBean("pjb.digitalizacao-com.tcc.pjb.backend.core.digitalizacao.DigitalizacaoProperties"));
                    DigitalizacaoProperties properties = context.getBean(DigitalizacaoProperties.class);
                    assertTrue(properties.enabled());
                    assertEquals(81.5d, properties.confiancaMinimaAuto());
                    assertEquals("por+eng", properties.idioma());
                    assertEquals("/opt/tesseract/bin/tesseract", properties.tesseractExecutable());
                    assertEquals(33, properties.reviewBatchSize());
                    assertEquals(44L, properties.staleProcessingMinutes());
                    assertEquals(55_000L, properties.governanceFixedRateMs());
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(DigitalizacaoProperties.class)
    static class DigitalizacaoPropertiesConfiguration {
    }
}
