package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessualLegacy;
import java.util.Locale;
import java.util.Objects;

public final class ProceduralRitoNames {

    private ProceduralRitoNames() {
    }

    public static String canonicalName(String raw) {
        RitoProcessual parsed = parse(raw);
        return parsed != null ? parsed.name() : null;
    }

    public static String resolveName(String ritoRaw, String ramoRaw, String classeTpu) {
        return ProceduralCatalogSupport.resolveRito(ritoRaw, ramoRaw, classeTpu).name();
    }

    public static RitoProcessual parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        return RitoProcessual.tryParse(raw)
                .or(() -> RitoProcessualLegacy.tryResolve(raw))
                .orElse(null);
    }

    public static RitoProcessual parseOrDefault(String raw, String fallback) {
        RitoProcessual parsed = parse(raw);
        if (parsed != null) {
            return parsed;
        }
        return Objects.requireNonNullElse(parse(fallback), RitoProcessual.COMUM_ORDINARIO);
    }

    public static boolean isPenal(String ritoName) {
        RitoProcessual rito = parse(ritoName);
        return rito != null && rito.isPenal();
    }

    public static boolean isTrabalhista(String ritoName) {
        RitoProcessual rito = parse(ritoName);
        return rito != null && rito.isTrabalhista();
    }

    public static boolean isPrevidenciario(String ritoName) {
        RitoProcessual rito = parse(ritoName);
        return rito != null && rito.isPrevidenciario();
    }

    public static boolean isTribFazenda(String ritoName) {
        RitoProcessual rito = parse(ritoName);
        return rito != null && rito.isTribFazenda();
    }

    public static boolean isEleitoral(String ritoName) {
        RitoProcessual rito = parse(ritoName);
        return rito != null && rito.isEleitoral();
    }

    public static boolean isMilitar(String ritoName) {
        RitoProcessual rito = parse(ritoName);
        return rito != null && rito.isMilitar();
    }

    public static boolean isEspecialConstitucional(String ritoName) {
        RitoProcessual rito = parse(ritoName);
        return rito != null && rito.isEspecialConstitucional();
    }

    public static boolean requiresSegredoByDefault(String ritoName) {
        RitoProcessual rito = parse(ritoName);
        return rito != null && rito.requiresSegredoByDefault();
    }

    public static String suggestedProtocolSystem(String ritoName, String esfera) {
        RitoProcessual rito = parse(ritoName);
        return rito != null ? rito.suggestedProtocolSystem(esfera) : defaultProtocolSystem(esfera);
    }

    public static boolean isOneOf(String ritoName, String... names) {
        if (ritoName == null || ritoName.isBlank() || names == null || names.length == 0) {
            return false;
        }
        String normalized = canonicalName(ritoName);
        String candidate = normalized != null ? normalized : ritoName.trim().toUpperCase(Locale.ROOT);
        for (String name : names) {
            if (name != null && candidate.equals(name.trim().toUpperCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public static boolean startsWith(String ritoName, String prefix) {
        if (ritoName == null || ritoName.isBlank() || prefix == null || prefix.isBlank()) {
            return false;
        }
        String candidate = canonicalName(ritoName);
        if (candidate == null) {
            candidate = ritoName.trim().toUpperCase(Locale.ROOT);
        }
        return candidate.startsWith(prefix.trim().toUpperCase(Locale.ROOT));
    }

    private static String defaultProtocolSystem(String esfera) {
        String e = esfera == null ? "" : esfera.trim().toUpperCase(Locale.ROOT);
        return switch (e) {
            case "FEDERAL" -> "PJe JF ou e-Proc TRF";
            case "TRABALHISTA" -> "PJe TRT";
            case "ELEITORAL" -> "PJe TSE / TRE";
            case "MILITAR" -> "Sistema Justiça Militar";
            default -> "PJe Estadual / eproc / ESAJ";
        };
    }
}
