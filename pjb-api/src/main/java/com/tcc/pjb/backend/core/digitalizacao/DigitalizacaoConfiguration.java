package com.tcc.pjb.backend.core.digitalizacao;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "pjb.runtime.barrier.integrations", name = "digitalizacao", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DigitalizacaoProperties.class)
public class DigitalizacaoConfiguration {

    @Bean
    @ConditionalOnMissingBean(OcrEnginePort.class)
    public OcrEnginePort ocrEnginePort(DigitalizacaoProperties properties) {
        if (properties.enabled()) {
            return new TesseractOcrEngineAdapter(properties);
        }
        return (imagem, idioma) -> new OcrPageResult("OCR_MOCK_" + (imagem == null ? 0 : imagem.length), 95.0d);
    }
}
