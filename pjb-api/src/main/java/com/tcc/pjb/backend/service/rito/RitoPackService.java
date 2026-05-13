package com.tcc.pjb.backend.service.rito;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.catalog.CatalogVersion;
import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.core.catalog.CatalogVersionService;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import com.tcc.pjb.backend.service.rito.diagnostics.RitoPackStatus;
import com.tcc.pjb.backend.service.rito.model.RitoDefinition;
import com.tcc.pjb.backend.service.rito.model.RitoPack;
import com.tcc.pjb.backend.service.rito.model.RitoStage;
import com.tcc.pjb.backend.service.rito.model.WorkTemplate;
import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class RitoPackService {

    private static final String PACK_PATH = "ritos/rito_pack_2026.json";

    private final ObjectMapper objectMapper;
    private final CatalogVersionService catalogVersionService;
    private final RitoPackStatus status;
    private final ProceduralCatalogService proceduralCatalogService;
    private RitoPack pack;

    public RitoPackService(ObjectMapper objectMapper,
                           CatalogVersionService catalogVersionService,
                           RitoPackStatus status,
                           ProceduralCatalogService proceduralCatalogService) {
        this.objectMapper = objectMapper;
        this.catalogVersionService = catalogVersionService;
        this.status = status;
        this.proceduralCatalogService = proceduralCatalogService;
    }

    @PostConstruct
    public void load() {
        CatalogVersion catalogVersion = null;
        try {
            catalogVersion = catalogVersionService.resolveCurrentRitosPack();
        } catch (Exception ignored) {
        }

        try (InputStream in = new ClassPathResource(PACK_PATH).getInputStream()) {
            RitoPack raw = objectMapper.readValue(in, RitoPack.class);
            Map<String, RitoDefinition> sourceDefinitions = raw != null && raw.getDefinitions() != null
                    ? raw.getDefinitions()
                    : Map.of();
            Map<String, RitoDefinition> merged = new LinkedHashMap<>();
            for (RitoProcessual rito : proceduralCatalogService.catalogDrivenRitos()) {
                merged.put(rito.name(), proceduralCatalogService.enrichDefinition(rito, sourceDefinitions.get(rito.name())));
            }
            for (RitoProcessual rito : proceduralCatalogService.allKnownRitos()) {
                merged.putIfAbsent(rito.name(), proceduralCatalogService.enrichDefinition(rito, sourceDefinitions.get(rito.name())));
            }
            for (Map.Entry<String, RitoDefinition> entry : sourceDefinitions.entrySet()) {
                String key = entry.getKey();
                if (key == null || key.isBlank()) {
                    continue;
                }
                RitoProcessual resolved = RitoProcessual.tryParse(key).orElse(null);
                if (resolved != null) {
                    merged.put(key, proceduralCatalogService.enrichDefinition(resolved, entry.getValue()));
                } else if (!merged.containsKey(key)) {
                    merged.put(key, entry.getValue());
                }
            }
            this.pack = RitoPack.builder().definitions(Map.copyOf(merged)).build();
            List<String> issues = validate(this.pack);
            status.markLoaded(
                    catalogVersion != null ? catalogVersion.getVersion() : null,
                    catalogVersion != null ? catalogVersion.getChecksum() : null,
                    issues
            );
        } catch (Exception e) {
            this.pack = RitoPack.builder().definitions(Map.of()).build();
            status.markFailed(
                    catalogVersion != null ? catalogVersion.getVersion() : null,
                    catalogVersion != null ? catalogVersion.getChecksum() : null,
                    "Falha ao carregar rito pack: " + e.getMessage()
            );
        }
    }

    public Optional<RitoDefinition> get(RitoProcessual rito) {
        if (rito == null || pack == null || pack.getDefinitions() == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(pack.getDefinitions().get(rito.name()));
    }

    public List<RitoProcessual> catalogDrivenRitos() {
        return proceduralCatalogService.catalogDrivenRitos();
    }

    public Map<String, RitoDefinition> definitions() {
        return pack == null || pack.getDefinitions() == null
                ? Map.of()
                : Map.copyOf(pack.getDefinitions());
    }

    private List<String> validate(RitoPack loaded) {
        List<String> issues = new ArrayList<>();
        Set<String> canonicalRitos = new LinkedHashSet<>();
        for (RitoProcessual rito : proceduralCatalogService.catalogDrivenRitos()) {
            canonicalRitos.add(rito.name());
        }
        Set<String> compatibilityRitos = new LinkedHashSet<>();
        for (RitoProcessual rito : proceduralCatalogService.allKnownRitos()) {
            compatibilityRitos.add(rito.name());
        }
        Set<String> keys = loaded.getDefinitions() == null ? Set.of() : loaded.getDefinitions().keySet();

        for (String key : keys) {
            if (key == null || key.isBlank()) {
                issues.add("definitions contém chave vazia");
                continue;
            }
            if (!compatibilityRitos.contains(key) && RitoProcessual.tryParse(key).isEmpty()) {
                issues.add("definitions contém rito desconhecido: '" + key + "'");
            }
        }

        for (String ritoCanonico : canonicalRitos) {
            if (!keys.contains(ritoCanonico)) {
                issues.add("Rito pack não contém definição para rito canônico: '" + ritoCanonico + "'");
            }
        }

        for (String ritoName : keys) {
            RitoDefinition definition = loaded.getDefinitions().get(ritoName);
            if (definition == null) {
                issues.add("Rito '" + ritoName + "' está nulo");
                continue;
            }
            if (definition.getStages() == null || definition.getStages().isEmpty()) {
                issues.add("Rito '" + ritoName + "' não possui stages");
                continue;
            }

            RitoProcessual rito = RitoProcessual.tryParse(ritoName).orElse(null);
            Set<String> externalRoles = new LinkedHashSet<>();
            if (rito != null) {
                proceduralCatalogService.requiredParties(rito).stream()
                        .filter(p -> p.external())
                        .forEach(p -> externalRoles.add(p.code()));
            }

            boolean hasExternalTask = false;
            for (RitoStage stage : definition.getStages()) {
                if (stage == null) {
                    continue;
                }
                String fase = stage.getFase();
                if (fase == null || fase.isBlank()) {
                    issues.add("Rito '" + ritoName + "' possui stage sem fase");
                    continue;
                }
                try {
                    FaseProcessual.valueOf(fase.trim().toUpperCase());
                } catch (Exception ex) {
                    issues.add("Rito '" + ritoName + "' possui fase inválida: '" + fase + "'");
                }
                if (stage.getAllowedNext() != null) {
                    for (String next : stage.getAllowedNext()) {
                        if (next == null || next.isBlank()) {
                            continue;
                        }
                        try {
                            FaseProcessual.valueOf(next.trim().toUpperCase());
                        } catch (Exception ex) {
                            issues.add("Rito '" + ritoName + "' possui allowedNext inválido: '" + next + "' (fase=" + fase + ")");
                        }
                    }
                }
                if (stage.getWork() == null || stage.getWork().isEmpty()) {
                    issues.add("Rito '" + ritoName + "' possui stage sem work: '" + fase + "'");
                    continue;
                }
                Set<String> workCodes = new LinkedHashSet<>();
                for (WorkTemplate work : stage.getWork()) {
                    if (work == null) {
                        continue;
                    }
                    if (work.getCode() == null || work.getCode().isBlank()) {
                        issues.add("Rito '" + ritoName + "' possui workItem sem code (fase=" + fase + ")");
                        continue;
                    }
                    if (!workCodes.add(work.getCode().trim().toUpperCase())) {
                        issues.add("Rito '" + ritoName + "' possui workItem duplicado: '" + work.getCode() + "' (fase=" + fase + ")");
                    }
                    if (work.getType() == null || work.getType().isBlank()) {
                        issues.add("Rito '" + ritoName + "' workItem sem type: '" + work.getCode() + "'");
                    } else {
                        try {
                            WorkItemType.valueOf(work.getType().trim().toUpperCase());
                        } catch (Exception ex) {
                            issues.add("Rito '" + ritoName + "' workItem com type inválido: '" + work.getType() + "' (code=" + work.getCode() + ")");
                        }
                    }
                    if (work.getActorRole() == null || work.getActorRole().isBlank()) {
                        issues.add("Rito '" + ritoName + "' workItem sem actorRole: '" + work.getCode() + "'");
                    } else {
                        try {
                            TipoUsuario actor = TipoUsuario.valueOf(work.getActorRole().trim().toUpperCase());
                            if (actor.isAdvocacia() || actor.isMinisterioPublico() || actor.isDefensoriaPublica() || actor.isProcuradoria()) {
                                hasExternalTask = true;
                            }
                        } catch (Exception ex) {
                            issues.add("Rito '" + ritoName + "' workItem com actorRole inválido: '" + work.getActorRole() + "' (code=" + work.getCode() + ")");
                        }
                    }
                }
            }
            if (!externalRoles.isEmpty() && !hasExternalTask) {
                issues.add("Rito '" + ritoName + "' não possui work item externo compatível com participação das partes");
            }
            if (rito != null && proceduralCatalogService.requiredDocuments(rito).isEmpty()) {
                issues.add("Rito '" + ritoName + "' está sem documentação essencial catalogada");
            }
        }
        return List.copyOf(new LinkedHashSet<>(issues));
    }
}
