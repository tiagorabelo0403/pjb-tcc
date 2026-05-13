package com.tcc.pjb.backend.service.rito.diagnostics;

import com.tcc.pjb.backend.model.entity.enums.processual.FaseProcessual;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.WorkItemType;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import com.tcc.pjb.backend.service.rito.RitoPackService;
import com.tcc.pjb.backend.service.rito.model.RitoDefinition;
import com.tcc.pjb.backend.service.rito.model.RitoStage;
import com.tcc.pjb.backend.service.rito.model.WorkTemplate;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class RitoPackDiagnosticsService {

    private final RitoPackService ritoPackService;
    private final ProceduralCatalogService proceduralCatalogService;

    public RitoPackDiagnosticsService(RitoPackService ritoPackService,
                                      ProceduralCatalogService proceduralCatalogService) {
        this.ritoPackService = ritoPackService;
        this.proceduralCatalogService = proceduralCatalogService;
    }

    @Cacheable(cacheNames = "ritos_pack_coverage")
    public RitoPackCoverageDto coverage() {
        Map<String, RitoDefinition> definitions = ritoPackService.definitions();
        Set<String> enumRitos = new TreeSet<>(proceduralCatalogService.catalogDrivenRitos().stream().map(Enum::name).toList());
        Set<String> packRitos = new TreeSet<>(definitions.keySet());

        List<String> missing = new ArrayList<>();
        for (String value : enumRitos) {
            if (!packRitos.contains(value)) {
                missing.add(value);
            }
        }

        List<String> extra = new ArrayList<>();
        for (String value : packRitos) {
            if (!enumRitos.contains(value)) {
                extra.add(value);
            }
        }

        Map<String, RitoPackCoverageDto.RitoStageSummary> summaries = new LinkedHashMap<>();
        LinkedHashSet<String> issues = new LinkedHashSet<>();

        for (String ritoName : packRitos) {
            RitoDefinition definition = definitions.get(ritoName);
            if (definition == null) {
                issues.add("Rito '" + ritoName + "' sem definição carregada");
                continue;
            }
            RitoProcessual rito = proceduralCatalogService.resolveRito(ritoName, definition.getRamoSugerido(), definition.getTitle());
            List<RitoStage> stages = definition.getStages() == null ? List.of() : definition.getStages();
            int totalWork = 0;
            List<String> fases = new ArrayList<>();
            boolean hasConhecimento = false;
            boolean hasExecucao = false;
            boolean hasRecursal = false;
            boolean externalParticipation = false;

            if (stages.isEmpty()) {
                issues.add("Rito '" + ritoName + "' não possui stages.");
            }

            for (RitoStage stage : stages) {
                if (stage == null || stage.getFase() == null || stage.getFase().isBlank()) {
                    issues.add("Rito '" + ritoName + "' possui stage sem fase.");
                    continue;
                }
                fases.add(stage.getFase());
                if ("CONHECIMENTO".equalsIgnoreCase(stage.getFase())) {
                    hasConhecimento = true;
                }
                if ("EXECUCAO".equalsIgnoreCase(stage.getFase()) || "CUMPRIMENTO_SENTENCA".equalsIgnoreCase(stage.getFase())) {
                    hasExecucao = true;
                }
                if ("RECURSAL".equalsIgnoreCase(stage.getFase())) {
                    hasRecursal = true;
                }
                try {
                    FaseProcessual.valueOf(stage.getFase().trim().toUpperCase());
                } catch (Exception ex) {
                    issues.add("Rito '" + ritoName + "' possui fase inválida: " + stage.getFase());
                }
                List<WorkTemplate> work = stage.getWork() == null ? List.of() : stage.getWork();
                totalWork += work.size();
                for (WorkTemplate item : work) {
                    if (item == null) {
                        continue;
                    }
                    try {
                        WorkItemType.valueOf(item.getType().trim().toUpperCase());
                    } catch (Exception ex) {
                        issues.add("Rito '" + ritoName + "' possui work type inválido: " + item.getType());
                    }
                    try {
                        TipoUsuario actor = TipoUsuario.valueOf(item.getActorRole().trim().toUpperCase());
                        if (actor.isAdvocacia() || actor.isMinisterioPublico() || actor.isDefensoriaPublica() || actor.isProcuradoria()) {
                            externalParticipation = true;
                        }
                    } catch (Exception ex) {
                        issues.add("Rito '" + ritoName + "' possui actorRole inválido: " + item.getActorRole());
                    }
                }
            }

            int requiredPartyRoles = 0;
            int requiredDocuments = 0;
            if (rito != null) {
                requiredPartyRoles = (int) proceduralCatalogService.requiredParties(rito).stream().filter(p -> p.required()).count();
                requiredDocuments = proceduralCatalogService.requiredDocuments(rito).size();
                if (!externalParticipation && requiredPartyRoles > 0) {
                    issues.add("Rito '" + ritoName + "' não possui work item externo coerente com o party schema.");
                }
            }

            summaries.put(ritoName, RitoPackCoverageDto.RitoStageSummary.builder()
                    .rito(ritoName)
                    .title(definition.getTitle())
                    .ramoSugerido(definition.getRamoSugerido())
                    .stageCount(stages.size())
                    .totalWorkItems(totalWork)
                    .requiredPartyRoles(requiredPartyRoles)
                    .requiredDocuments(requiredDocuments)
                    .externalParticipation(externalParticipation)
                    .fases(new ArrayList<>(new LinkedHashSet<>(fases)))
                    .hasConhecimento(hasConhecimento)
                    .hasExecucao(hasExecucao)
                    .hasRecursal(hasRecursal)
                    .build());
        }

        return RitoPackCoverageDto.builder()
                .generatedAt(Instant.now())
                .totalEnumRitos(enumRitos.size())
                .totalPackDefinitions(packRitos.size())
                .missingInPack(missing)
                .extraInPack(extra)
                .stageSummaries(summaries)
                .issues(List.copyOf(issues))
                .build();
    }
}
