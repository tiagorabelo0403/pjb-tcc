package com.tcc.pjb.backend.service.ui.accessibility;

import com.tcc.pjb.backend.model.dto.ui.accessibility.UiPlainLanguagePreviewResponseDto;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class UiPlainLanguageService {

    private static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

    public UiPlainLanguagePreviewResponseDto preview(String text) {
        String original = Objects.toString(text, "").trim();
        String simplified = simplifyInternal(original);
        return new UiPlainLanguagePreviewResponseDto(
                original,
                simplified,
                Map.copyOf(appliedRules(original, simplified))
        );
    }

    private String simplifyInternal(String text) {
        if (text.isBlank()) {
            return text;
        }
        String out = " " + text + " ";
        out = replacePhrase(out, "intima-se", "avise-se oficialmente");
        out = replacePhrase(out, "intimem-se", "avisem-se oficialmente");
        out = replacePhrase(out, "manifeste-se", "apresente sua posição");
        out = replacePhrase(out, "se manifeste", "apresente sua posição");
        out = replacePhrase(out, "manifestação", "posição apresentada");
        out = replacePhrase(out, "no prazo de", "dentro do prazo de");
        out = replacePhrase(out, "expeça-se", "emita-se");
        out = replacePhrase(out, "certifique-se", "registre-se oficialmente");
        out = replacePhrase(out, "autue-se", "registre-se o caso");
        out = replacePhrase(out, "junte-se", "anexe-se");
        out = replacePhrase(out, "conclusos", "prontos para decisão");
        out = replacePhrase(out, "decorrido o prazo", "depois que o prazo terminar");
        out = replacePhrase(out, "parte autora", "quem entrou com o pedido");
        out = replacePhrase(out, "parte ré", "quem está sendo demandado");
        out = replacePhrase(out, "parte re", "quem está sendo demandado");
        out = replacePhrase(out, "requerente", "quem fez o pedido");
        out = replacePhrase(out, "requerido", "quem recebeu o pedido");
        out = replacePhrase(out, "cumpra-se", "faça-se o cumprimento");
        out = replacePhrase(out, "remetam-se os autos", "envie-se o processo");
        out = WHITESPACE_PATTERN.matcher(out).replaceAll(" ").trim();
        return normalizeSentenceCase(out);
    }

    private Map<String, Integer> appliedRules(String original, String simplified) {
        LinkedHashMap<String, Integer> out = new LinkedHashMap<>();
        out.put("originalLength", original.length());
        out.put("simplifiedLength", simplified.length());
        out.put("sentenceCount", simplified.isBlank() ? 0 : simplified.split("[.!?]+").length);
        out.put("wordCount", simplified.isBlank() ? 0 : WHITESPACE_PATTERN.split(simplified.trim()).length);
        out.put("reductionDelta", Math.max(0, original.length() - simplified.length()));
        return out;
    }

    private String replacePhrase(String source, String target, String replacement) {
        Pattern pattern = Pattern.compile("(?i)(^|\\b)" + Pattern.quote(target) + "(?=\\b)");
        return pattern.matcher(source)
                .replaceAll(matchResult -> {
                    String boundary = Objects.toString(matchResult.group(1), "");
                    return Matcher.quoteReplacement(boundary + replacement.toLowerCase(Locale.ROOT));
                });
    }

    private String normalizeSentenceCase(String value) {
        if (value.isBlank()) {
            return value;
        }
        char first = value.charAt(0);
        return Character.toUpperCase(first) + value.substring(1);
    }
}
