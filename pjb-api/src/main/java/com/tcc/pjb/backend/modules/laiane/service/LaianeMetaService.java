package com.tcc.pjb.backend.modules.laiane.service;

import com.tcc.pjb.backend.model.repository.MetadadosSistemaRepository;
import com.tcc.pjb.backend.modules.laiane.dto.LaianeMetaDto;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class LaianeMetaService {

    private static final String DEFAULT_ASSISTANT_NAME = "Laiane";
    private static final String DEFAULT_MODULE_VERSION = "8.5-LAIANE";
    private static final String DEFAULT_REASONING_MODE = "PJB_INTERNAL_DECISION_ENGINE";
    private static final String DEFAULT_EXTERNAL_AI_ROLE = "RESEARCH_AUGMENTATION_ONLY";
    private final MetadadosSistemaRepository metadadosSistemaRepository;

    public LaianeMetaService(MetadadosSistemaRepository metadadosSistemaRepository) {
        this.metadadosSistemaRepository = Objects.requireNonNull(metadadosSistemaRepository);
    }

    public LaianeMetaDto meta() {
        return LaianeMetaDto.builder()
                .assistantName(resolve("LAIANE_ASSISTANT_NAME", DEFAULT_ASSISTANT_NAME))
                .moduleVersion(resolve("LAIANE_MODULE_VERSION", DEFAULT_MODULE_VERSION))
                .reasoningMode(resolve("LAIANE_REASONING_MODE", DEFAULT_REASONING_MODE))
                .externalAiRole(resolve("LAIANE_EXTERNAL_AI_ROLE", DEFAULT_EXTERNAL_AI_ROLE))
                .build();
    }

    private String resolve(String chave, String fallback) {
        return metadadosSistemaRepository.findByChaveIgnoreCase(chave)
                .map(meta -> meta.getValor())
                .filter(valor -> !valor.isBlank())
                .orElse(fallback);
    }
}
