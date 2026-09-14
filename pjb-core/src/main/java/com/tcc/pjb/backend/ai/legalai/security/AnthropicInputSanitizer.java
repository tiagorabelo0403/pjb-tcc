package com.tcc.pjb.backend.ai.legalai.security;

import org.springframework.stereotype.Component;

@Component
public class AnthropicInputSanitizer {

    private final AiPromptEgressGuard guard = new AiPromptEgressGuard();

    public void validar(String input, String campo) {
        AiPromptInspection inspecao = guard.inspecionar(input);
        if (!inspecao.suspeito()) {
            return;
        }
        throw new PromptInjectionException(
                "Conteúdo bloqueado no campo '" + campo + "': padrão de injeção detectado ("
                        + motivo(inspecao) + ")");
    }

    private static String motivo(AiPromptInspection inspecao) {
        if (inspecao.sinais().isEmpty()) {
            return "marcador_de_protocolo";
        }
        return inspecao.sinaisConcatenados();
    }
}
