package com.tcc.pjb.backend.ai.legalai.security;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Component
public class AnthropicInputSanitizer {

    private static final List<Pattern> PATTERNS = List.of(
            Pattern.compile("(?i)ignore\\s+(all\\s+)?previous\\s+instructions?"),
            Pattern.compile("(?i)disregard\\s+(all\\s+)?previous"),
            Pattern.compile("(?i)you\\s+are\\s+now\\s+"),
            Pattern.compile("(?i)\\bsystem\\s*:\\s"),
            Pattern.compile("(?i)\\[INST\\]"),
            Pattern.compile("(?i)<\\|.*?\\|>"),
            Pattern.compile("(?i)developer\\s+mode"),
            Pattern.compile("(?i)jailbreak"),
            Pattern.compile("(?i)\\bDAN\\b"),
            Pattern.compile("(?i)override\\s+(safety|guidelines|rules|constraints)"),
            Pattern.compile("(?i)reveal\\s+(your\\s+)?(system\\s+)?(prompt|instructions?)"),
            Pattern.compile("(?i)forget\\s+(all\\s+)?previous\\s+(context|instructions?)"),
            Pattern.compile("(?i)act\\s+as\\s+(if\\s+)?you\\s+(have\\s+no|are\\s+without)"),
            Pattern.compile("(?i)\\bpretend\\s+you\\s+are\\b")
    );

    public void validar(String input, String campo) {
        if (input == null || input.isBlank()) {
            return;
        }
        for (Pattern p : PATTERNS) {
            if (p.matcher(input).find()) {
                throw new PromptInjectionException(
                        "Conteúdo bloqueado no campo '" + campo + "': padrão de injeção detectado");
            }
        }
    }
}
