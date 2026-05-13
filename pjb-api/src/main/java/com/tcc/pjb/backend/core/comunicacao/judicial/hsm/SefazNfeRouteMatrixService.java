package com.tcc.pjb.backend.core.comunicacao.judicial.hsm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.gov.GovServiceRegistry;
import com.tcc.pjb.backend.repository.gov.GovServiceRegistryRepository;

@Service
public class SefazNfeRouteMatrixService {

    public record ResolvedRoute(
            String uf,
            String tribunalCodigo,
            String endpoint,
            String fonte,
            List<String> marcadores
    ) {
    }

    private final GovServiceRegistryRepository govServiceRegistryRepository;
    private final SefazNfeProperties properties;

    public SefazNfeRouteMatrixService(GovServiceRegistryRepository govServiceRegistryRepository,
                                      SefazNfeProperties properties) {
        this.govServiceRegistryRepository = Objects.requireNonNull(govServiceRegistryRepository, "govServiceRegistryRepository");
        this.properties = Objects.requireNonNull(properties, "properties");
    }

    public Optional<ResolvedRoute> resolve(String ufHint, String tribunalCodigo) {
        String uf = normalizeUf(ufHint);
        String tribunal = upperTrim(tribunalCodigo);
        List<String> ufs = candidateUfs(uf, tribunal);
        List<GovServiceRegistry> entries = govServiceRegistryRepository.findEnabledByUfs(ufs).stream()
                .filter(this::isSefazNfeRoute)
                .sorted(Comparator.comparingInt(entry -> score(entry, uf, tribunal)))
                .toList();
        if (!entries.isEmpty()) {
            GovServiceRegistry entry = entries.getFirst();
            return Optional.of(new ResolvedRoute(
                    normalizeUf(entry.getUf()),
                    tribunal,
                    entry.getUrl(),
                    "GOV_SERVICE_REGISTRY:" + entry.getName(),
                    List.of(entry.getCategory(), entry.getServiceType() != null ? entry.getServiceType().name() : "UNKNOWN")
            ));
        }
        if (uf != null) {
            String configured = properties.consultaUrlPorUf().get(uf);
            if (configured != null && !configured.isBlank()) {
                return Optional.of(new ResolvedRoute(
                        uf,
                        tribunal,
                        configured,
                        "APPLICATION_PROPERTIES",
                        List.of("CONFIG", uf)
                ));
            }
        }
        return Optional.empty();
    }

    private boolean isSefazNfeRoute(GovServiceRegistry entry) {
        if (entry == null || !entry.isEnabled()) {
            return false;
        }
        String category = upperTrim(entry.getCategory());
        if (!Objects.equals(category, "FAZENDA")) {
            return false;
        }
        String name = normalizeToken(entry.getName());
        String url = normalizeToken(entry.getUrl());
        return name.contains("SEFAZ") || name.contains("NFE") || url.contains("SEFAZ") || url.contains("NFE");
    }

    private int score(GovServiceRegistry entry, String uf, String tribunal) {
        int score = 100;
        String entryUf = normalizeUf(entry.getUf());
        if (uf != null && Objects.equals(entryUf, uf)) {
            score -= 40;
        }
        if (entryUf == null || Objects.equals(entryUf, "BR")) {
            score += 10;
        }
        String name = normalizeToken(entry.getName());
        if (name.contains("SEFAZ")) {
            score -= 20;
        }
        if (name.contains("NFE")) {
            score -= 15;
        }
        if (tribunal != null && name.contains(tribunal)) {
            score -= 15;
        }
        return score;
    }

    private List<String> candidateUfs(String ufHint, String tribunalCodigo) {
        Set<String> ordered = new LinkedHashSet<>();
        String tribunalUf = inferUfFromTribunal(tribunalCodigo);
        if (tribunalUf != null) {
            ordered.add(tribunalUf);
        }
        if (ufHint != null) {
            ordered.add(ufHint);
        }
        ordered.add("BR");
        return new ArrayList<>(ordered);
    }

    private String inferUfFromTribunal(String tribunalCodigo) {
        String tribunal = upperTrim(tribunalCodigo);
        if (tribunal == null || tribunal.length() < 4) {
            return null;
        }
        if (tribunal.startsWith("TJ") || tribunal.startsWith("TRE") || tribunal.startsWith("TRT")) {
            String suffix = tribunal.substring(tribunal.length() - 2);
            return suffix.chars().allMatch(Character::isLetter) ? suffix : null;
        }
        return null;
    }

    private static String normalizeUf(String value) {
        String normalized = upperTrim(value);
        if (Objects.equals(normalized, "BR")) {
            return "BR";
        }
        return normalized != null && normalized.length() == 2 ? normalized : null;
    }

    private static String upperTrim(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizeToken(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.trim().toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace('/', '_')
                .replace(' ', '_');
    }
}
