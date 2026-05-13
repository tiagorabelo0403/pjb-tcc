package com.tcc.pjb.backend.modules.laiane.service;

import com.tcc.pjb.backend.ai.juridica.v3.core.AjuizamentoIntentEngine;
import com.tcc.pjb.backend.ai.juridica.v3.core.AjuizamentoIntent;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport.PartyRoleSpec;
import com.tcc.pjb.backend.modules.laiane.dto.legal.LaianePeticaoValidateResponse;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import com.tcc.pjb.backend.tribunal.perfil.PerfilInstanciaTribunalService;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class LaianePeticaoValidatorService {

    private static final Pattern PLACEHOLDER = Pattern.compile("\\[[^\\]]+\\]");
    private static final Pattern HEADER_SPLIT = Pattern.compile("(?m)^\\s*(?:dos|das|da|do)?\\s*[A-ZÁÀÂÃÉÈÊÍÌÎÓÒÔÕÚÙÛÇ0-9 _-]{3,}\\s*:?$");

    private final PerfilInstanciaTribunalService perfilInstanciaTribunalService;
    private final AjuizamentoIntentEngine ajuizamentoIntentEngine;
    private final ProceduralCatalogService proceduralCatalogService;
    private final ProceduralCanonicalResolver proceduralCanonicalResolver;

    public LaianePeticaoValidatorService(PerfilInstanciaTribunalService perfilInstanciaTribunalService,
                                         AjuizamentoIntentEngine ajuizamentoIntentEngine,
                                         ProceduralCatalogService proceduralCatalogService,
                                         ProceduralCanonicalResolver proceduralCanonicalResolver) {
        this.perfilInstanciaTribunalService = perfilInstanciaTribunalService;
        this.ajuizamentoIntentEngine = ajuizamentoIntentEngine;
        this.proceduralCatalogService = proceduralCatalogService;
        this.proceduralCanonicalResolver = proceduralCanonicalResolver;
    }

    public LaianePeticaoValidateResponse validate(String content) {
        return validate(content, null, null, null);
    }

    public LaianePeticaoValidateResponse validate(String content, String ritoRaw, String classeTpu, String ramoDireito) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        String text = content == null ? "" : content;
        if (text.isBlank()) {
            errors.add("Conteúdo vazio.");
            return LaianePeticaoValidateResponse.builder().ok(false).errors(errors).warnings(warnings).build();
        }

        CanonicalContext canonical = resolveCanonical(text, ritoRaw, classeTpu, ramoDireito);
        var rito = canonical.rito() != null ? canonical.rito() : proceduralCatalogService.resolveRito(ritoRaw, ramoDireito, classeTpu);
        String ritoName = rito != null ? rito.name() : "";
        List<PartyRoleSpec> partySchema = proceduralCatalogService.requiredParties(rito);
        Map<String, Boolean> presence = detectPartyPresence(text, partySchema);

        for (PartyRoleSpec role : partySchema) {
            if (!role.required()) {
                continue;
            }
            if (!presence.getOrDefault(role.code(), Boolean.FALSE)) {
                errors.add("Parte obrigatória não identificada: " + humanize(role.code()) + '.');
            }
        }

        if (!containsSemanticSection(text, List.of("fundamentos", "fundamentação", "fundamentacao", "do direito", "mérito", "merito"))) {
            warnings.add("Fundamentação jurídica ausente ou incompleta.");
        }
        if (!containsSemanticSection(text, List.of("pedidos", "dos pedidos", "requerimentos", "requer", "pedido final"))) {
            warnings.add("Pedidos não foram encontrados em seção própria.");
        }
        if (!containsSemanticSection(text, List.of("fatos", "dos fatos", "narrativa fática", "narrativa fatica", "síntese fática", "sintese fatica"))) {
            warnings.add("Exposição fática não foi encontrada em seção própria.");
        }
        if (!containsTribunalHeader(text)) {
            warnings.add("Endereçamento do órgão julgador não foi identificado de forma clara.");
        }
        if (canonical.tribunalCodigo() == null || canonical.tribunalCodigo().isBlank()) {
            warnings.add("Tribunal não foi resolvido com segurança; revise a competência antes do protocolo.");
        }
        if (ritoName.startsWith("ELEITORAL") && !containsAnyNormalized(text, List.of("prazo", "eleição", "eleicao", "campanha", "pleito", "zona eleitoral", "tre", "tse"))) {
            warnings.add("A peça eleitoral não evidenciou contexto temporal ou jurisdicional do pleito.");
        }
        if (ritoName.startsWith("MILITAR") && !containsAnyNormalized(text, List.of("auditoria militar", "organização militar", "organizacao militar", "posto", "graduação", "graduacao", "ipm", "cppm", "cpm"))) {
            warnings.add("A peça militar não evidenciou elementos funcionais ou orgânicos típicos da Justiça Militar.");
        }

        List<String> placeholders = detectMissingFields(text);
        if (!placeholders.isEmpty()) {
            warnings.add("Há campos em placeholder ainda não preenchidos: " + String.join(", ", placeholders));
        }

        return LaianePeticaoValidateResponse.builder()
                .ok(errors.isEmpty())
                .errors(List.copyOf(new LinkedHashSet<>(errors)))
                .warnings(List.copyOf(new LinkedHashSet<>(warnings)))
                .build();
    }

    public static List<String> detectMissingFields(String content) {
        if (content == null || content.isBlank()) {
            return List.of();
        }
        Matcher matcher = PLACEHOLDER.matcher(content);
        List<String> out = new ArrayList<>();
        int guard = 0;
        while (matcher.find() && guard++ < 50) {
            String token = matcher.group();
            token = token.substring(1, token.length() - 1).trim();
            if (!token.isBlank() && !out.contains(token)) {
                out.add(token);
            }
        }
        return List.copyOf(out);
    }

    private CanonicalContext resolveCanonical(String text, String ritoRaw, String classeTpu, String ramoDireito) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("rito", ritoRaw);
        payload.put("ramoDireito", ramoDireito);
        payload.put("ramo_direito", ramoDireito);
        payload.put("classe", classeTpu);
        payload.put("classeTpu", classeTpu);
        payload.put("assunto", classeTpu);
        payload.put("narrativa", text);
        payload.put("texto", text);
        CanonicalContext canonical = proceduralCanonicalResolver.resolve(payload);
        if (canonical.rito() != null && (ritoRaw != null || classeTpu != null || ramoDireito != null)) {
            return canonical;
        }
        try {
            AjuizamentoIntent intent = ajuizamentoIntentEngine.inferir(payload);
            if (intent != null && intent.rito() != null) {
                payload.put("rito", intent.rito());
                if (intent.ramoDireito() != null && !intent.ramoDireito().isBlank()) {
                    payload.put("ramoDireito", intent.ramoDireito());
                }
                return proceduralCanonicalResolver.resolve(payload);
            }
        } catch (Exception ignored) {
        }
        return canonical;
    }

    private Map<String, Boolean> detectPartyPresence(String text, List<PartyRoleSpec> partySchema) {
        Map<String, Boolean> out = new LinkedHashMap<>();
        String normalized = normalize(text);
        for (PartyRoleSpec role : partySchema) {
            boolean found = role.aliases().stream()
                    .filter(Objects::nonNull)
                    .map(this::expandAliases)
                    .flatMap(List::stream)
                    .distinct()
                    .anyMatch(alias -> containsRoleReference(text, normalized, alias));
            out.put(role.code(), found);
        }
        return out;
    }

    private List<String> expandAliases(String alias) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        out.add(alias);
        String normalized = normalizeToken(alias);
        if (normalized.equals("AUTOR")) {
            out.add(perfilInstanciaTribunalService.termo(PerfilInstanciaTribunalService.TermoPadrao.AUTOR));
            out.add(perfilInstanciaTribunalService.termoPlural(PerfilInstanciaTribunalService.TermoPadrao.AUTOR));
            out.add("parte autora");
            out.add("polo ativo");
        }
        if (normalized.equals("REU")) {
            out.add(perfilInstanciaTribunalService.termo(PerfilInstanciaTribunalService.TermoPadrao.REU));
            out.add(perfilInstanciaTribunalService.termoPlural(PerfilInstanciaTribunalService.TermoPadrao.REU));
            out.add("parte ré");
            out.add("parte re");
            out.add("polo passivo");
        }
        return List.copyOf(out);
    }

    private boolean containsRoleReference(String originalText, String normalizedText, String alias) {
        String normalizedAlias = normalize(alias);
        if (normalizedAlias.isBlank()) {
            return false;
        }
        String regex = "(?im)^\\s*(?:dos|das|da|do)?\\s*" + Pattern.quote(alias) + "\\s*[:\\-].*$";
        if (Pattern.compile(regex).matcher(originalText).find()) {
            return true;
        }
        String normalizedRegex = "(?im)^\\s*(?:dos|das|da|do)?\\s*" + Pattern.quote(normalizedAlias) + "\\s*[:\\-].*$";
        if (Pattern.compile(normalizedRegex).matcher(normalizedText).find()) {
            return true;
        }
        return normalizedText.contains(" " + normalizedAlias + " ")
                || normalizedText.startsWith(normalizedAlias + " ")
                || normalizedText.endsWith(" " + normalizedAlias)
                || normalizedText.contains("\n" + normalizedAlias + " ")
                || normalizedText.contains("\n" + normalizedAlias + ":");
    }

    private boolean containsSemanticSection(String text, List<String> aliases) {
        String normalized = normalize(text);
        for (String alias : aliases) {
            String normalizedAlias = normalize(alias);
            if (normalized.contains("\n" + normalizedAlias + "\n")
                    || normalized.contains("\n" + normalizedAlias + ":")
                    || normalized.contains("\n" + normalizedAlias + " -")
                    || normalized.startsWith(normalizedAlias + "\n")) {
                return true;
            }
        }
        return HEADER_SPLIT.matcher(text).find() && aliases.stream().anyMatch(alias -> normalized.contains(normalize(alias)));
    }

    private boolean containsTribunalHeader(String text) {
        return containsAnyNormalized(text, List.of(
                "excelentissimo senhor doutor juiz",
                "excelentíssima senhora doutora juíza",
                "excelentissima senhora doutora juiza",
                "vara",
                "juizado",
                "tribunal regional",
                "tribunal de justiça",
                "tribunal de justica",
                "zona eleitoral",
                "auditoria militar",
                "superior tribunal",
                "tribunal superior eleitoral"
        ));
    }

    private boolean containsAnyNormalized(String text, List<String> aliases) {
        String normalized = normalize(text);
        return aliases.stream().map(this::normalize).anyMatch(normalized::contains);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .replaceAll("[^A-Za-z0-9\\n ]", " ")
                .replaceAll(" +", " ")
                .toLowerCase(Locale.ROOT)
                .trim();
    }

    private String normalizeToken(String value) {
        return normalize(value).replace(' ', '_').toUpperCase(Locale.ROOT);
    }

    private String humanize(String token) {
        return Arrays.stream(token.split("_"))
                .filter(s -> !s.isBlank())
                .map(s -> s.substring(0, 1) + s.substring(1).toLowerCase(Locale.ROOT))
                .reduce((a, b) -> a + " " + b)
                .orElse(token);
    }
}
