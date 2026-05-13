package com.tcc.pjb.backend.modules.intelligence.edge;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.edge-ai")
public record PjbEdgeAiProperties(
        boolean enabled,
        String modelPath,
        String promptPrefix
) {
    public PjbEdgeAiProperties {
        if (modelPath == null || modelPath.isBlank()) {
            modelPath = "${user.home}/PJB/Secure/models/pjb-law-quantized.onnx";
        }
        if (promptPrefix == null || promptPrefix.isBlank()) {
            promptPrefix = "Escreva uma sentença procedente com base em: ";
        }
    }
}
