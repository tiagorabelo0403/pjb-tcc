package com.tcc.pjb.backend.core.workflow;

import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport.DefinitionSnapshot;
import com.tcc.pjb.backend.core.procedural.ProceduralCatalogSupport.PartyRoleSpec;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.procedural.ProceduralCatalogService;
import com.tcc.pjb.backend.core.util.PayloadMaps;
import com.tcc.pjb.backend.service.rito.model.RitoStage;
import com.tcc.pjb.backend.service.rito.model.WorkTemplate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

@Component
public class BpmnWorkflowGenerator {

    public record BpmnOutput(
            RitoProcessual rito,
            String processId,
            String xml,
            String sha256,
            int externalTaskCount,
            int internalTaskCount,
            Instant generatedAt,
            Map<String, Object> blueprint
    ) {
        public BpmnOutput {
            xml = xml == null ? "" : xml;
            sha256 = sha256 == null ? "" : sha256;
            blueprint = PayloadMaps.deepCopyWithoutNulls(blueprint);
        }

        public boolean isEmpty() {
            return xml.isBlank();
        }

        public boolean hasExternalTasks() {
            return externalTaskCount > 0;
        }
    }

    public record WorkflowConsistencyReport(
            int totalRitos,
            int emptyBpmn,
            int missingExternalActor,
            List<String> emptyRitos,
            List<String> noExternalActorRitos,
            boolean passed,
            Instant generatedAt
    ) {}

    private static final Set<TipoUsuario> EXTERNAL_ROLES = EnumSet.of(
            TipoUsuario.ADVOGADO,
            TipoUsuario.MEMBRO_MINISTERIO_PUBLICO,
            TipoUsuario.DEFENSOR_PUBLICO,
            TipoUsuario.PROCURADOR
    );

    private final ProceduralCatalogService proceduralCatalogService;
    private final ConcurrentHashMap<String, BpmnOutput> cache = new ConcurrentHashMap<>();

    public BpmnWorkflowGenerator(ProceduralCatalogService proceduralCatalogService) {
        this.proceduralCatalogService = Objects.requireNonNull(proceduralCatalogService);
    }

    public BpmnOutput generate(RitoProcessual rito) {
        Objects.requireNonNull(rito, "rito");
        return cache.computeIfAbsent(rito.name(), ignored -> build(rito));
    }

    public BpmnOutput generate(CanonicalContext canonicalContext) {
        Objects.requireNonNull(canonicalContext, "canonicalContext");
        RitoProcessual rito = Objects.requireNonNull(canonicalContext.rito(), "canonicalContext.rito");
        return generate(rito);
    }

    public Map<String, BpmnOutput> generateAll() {
        for (RitoProcessual rito : proceduralCatalogService.catalogDrivenRitos()) {
            generate(rito);
        }
        return Map.copyOf(cache);
    }

    public WorkflowConsistencyReport consistencyGate() {
        List<RitoProcessual> ritos = proceduralCatalogService.catalogDrivenRitos();
        generateAll();
        List<String> emptyRitos = new ArrayList<>();
        List<String> noExternalActorRitos = new ArrayList<>();
        for (RitoProcessual rito : ritos) {
            BpmnOutput out = cache.get(rito.name());
            if (out == null || out.isEmpty()) {
                emptyRitos.add(rito.name());
                continue;
            }
            if (requiresExternalActor(rito) && !out.hasExternalTasks()) {
                noExternalActorRitos.add(rito.name());
            }
        }
        return new WorkflowConsistencyReport(
                ritos.size(),
                emptyRitos.size(),
                noExternalActorRitos.size(),
                List.copyOf(emptyRitos),
                List.copyOf(noExternalActorRitos),
                emptyRitos.isEmpty() && noExternalActorRitos.isEmpty(),
                Instant.now()
        );
    }

    public void evict(RitoProcessual rito) {
        if (rito != null) {
            cache.remove(rito.name());
        }
    }

    public void evictAll() {
        cache.clear();
    }

    private boolean requiresExternalActor(RitoProcessual rito) {
        DefinitionSnapshot snapshot = proceduralCatalogService.snapshot(rito);
        return snapshot.parties().stream().anyMatch(spec -> spec.required() && spec.external());
    }

    private BpmnOutput build(RitoProcessual rito) {
        DefinitionSnapshot snapshot = proceduralCatalogService.snapshot(rito);
        List<RitoStage> stages = snapshot.stages();
        if (stages == null || stages.isEmpty()) {
            return new BpmnOutput(rito, processId(rito), "", "", 0, 0, Instant.now(), Map.of());
        }
        String processId = processId(rito);
        int externalCount = 0;
        int internalCount = 0;
        StringBuilder xml = new StringBuilder(16384);
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<bpmn:definitions xmlns:bpmn=\"http://www.omg.org/spec/BPMN/20100524/MODEL\" ");
        xml.append("xmlns:bpmndi=\"http://www.omg.org/spec/BPMN/20100524/DI\" ");
        xml.append("xmlns:dc=\"http://www.omg.org/spec/DD/20100524/DC\" ");
        xml.append("xmlns:camunda=\"http://camunda.org/schema/1.0/bpmn\" ");
        xml.append("xmlns:pjb=\"https://pjb.gov.br/schema/workflow\" ");
        xml.append("targetNamespace=\"https://pjb.gov.br/schema/workflow\" ");
        xml.append("id=\"Definitions_").append(processId).append("\">\n");
        xml.append("  <bpmn:process id=\"").append(processId).append("\" name=\"")
                .append(xml(snapshot.title())).append("\" isExecutable=\"true\">\n");
        xml.append("    <bpmn:startEvent id=\"start_0\" name=\"Protocolo\"/>\n");
        for (int i = 0; i < stages.size(); i++) {
            RitoStage stage = stages.get(i);
            String stageId = stageId(i, stage.getFase());
            xml.append("    <bpmn:subProcess id=\"").append(stageId).append("\" name=\"")
                    .append(xml(stage.getFase())).append("\">\n");
            if (stage.getWork() != null) {
                int localIndex = 0;
                for (WorkTemplate work : stage.getWork()) {
                    if (work == null || work.getCode() == null || work.getCode().isBlank()) {
                        continue;
                    }
                    TipoUsuario role = parseRole(work.getActorRole());
                    boolean external = role != null && EXTERNAL_ROLES.contains(role);
                    String taskId = stageId + "_task_" + (++localIndex) + '_' + token(work.getCode());
                    if (external) {
                        externalCount++;
                        xml.append("      <bpmn:userTask id=\"").append(taskId).append("\" name=\"")
                                .append(xml(defaultText(work.getTitle(), work.getCode()))).append("\" camunda:candidateGroups=\"")
                                .append(role.name()).append("\">\n");
                    } else {
                        internalCount++;
                        xml.append("      <bpmn:serviceTask id=\"").append(taskId).append("\" name=\"")
                                .append(xml(defaultText(work.getTitle(), work.getCode()))).append("\" camunda:delegateExpression=\"${pjbServiceTaskDelegate}\">\n");
                    }
                    xml.append("        <bpmn:extensionElements>\n");
                    xml.append("          <camunda:properties>\n");
                    xml.append("            <camunda:property name=\"templateCode\" value=\"")
                            .append(xml(work.getCode())).append("\"/>\n");
                    if (role != null) {
                        xml.append("            <camunda:property name=\"actorRole\" value=\"")
                                .append(role.name()).append("\"/>\n");
                    }
                    if (Boolean.TRUE.equals(work.getBlocking())) {
                        xml.append("            <camunda:property name=\"blocking\" value=\"true\"/>\n");
                    }
                    if (work.getType() != null && !work.getType().isBlank()) {
                        xml.append("            <camunda:property name=\"type\" value=\"")
                                .append(xml(work.getType())).append("\"/>\n");
                    }
                    xml.append("          </camunda:properties>\n");
                    xml.append("        </bpmn:extensionElements>\n");
                    if (work.getDescription() != null && !work.getDescription().isBlank()) {
                        xml.append("        <bpmn:documentation>")
                                .append(xml(work.getDescription()))
                                .append("</bpmn:documentation>\n");
                    }
                    xml.append(external ? "      </bpmn:userTask>\n" : "      </bpmn:serviceTask>\n");
                }
            }
            xml.append("    </bpmn:subProcess>\n");
        }
        xml.append("    <bpmn:endEvent id=\"end_0\" name=\"Encerrado\"/>\n");
        xml.append("    <bpmn:sequenceFlow id=\"flow_start_0\" sourceRef=\"start_0\" targetRef=\"")
                .append(stageId(0, stages.get(0).getFase())).append("\"/>\n");
        for (int i = 0; i < stages.size() - 1; i++) {
            xml.append("    <bpmn:sequenceFlow id=\"flow_").append(i).append("\" sourceRef=\"")
                    .append(stageId(i, stages.get(i).getFase())).append("\" targetRef=\"")
                    .append(stageId(i + 1, stages.get(i + 1).getFase())).append("\"/>\n");
        }
        xml.append("    <bpmn:sequenceFlow id=\"flow_end_0\" sourceRef=\"")
                .append(stageId(stages.size() - 1, stages.get(stages.size() - 1).getFase()))
                .append("\" targetRef=\"end_0\"/>\n");
        xml.append("  </bpmn:process>\n");
        xml.append("</bpmn:definitions>\n");
        String finalXml = xml.toString();
        return new BpmnOutput(
                rito,
                processId,
                finalXml,
                sha256(finalXml),
                externalCount,
                internalCount,
                Instant.now(),
                buildBlueprint(snapshot, processId, externalCount, internalCount)
        );
    }

    private Map<String, Object> buildBlueprint(DefinitionSnapshot snapshot,
                                               String processId,
                                               int externalCount,
                                               int internalCount) {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("rito", snapshot.rito().name());
        out.put("processId", processId);
        out.put("title", snapshot.title());
        out.put("ramo", snapshot.ramo());
        out.put("competenceHints", snapshot.competenceHints());
        out.put("requiredDocuments", snapshot.documents().stream().filter(d -> d.required()).map(d -> d.code().name()).toList());
        out.put("requiredPartyRoles", snapshot.parties().stream().filter(PartyRoleSpec::required).map(PartyRoleSpec::code).toList());
        out.put("stageCount", snapshot.stages().size());
        out.put("externalTaskCount", externalCount);
        out.put("internalTaskCount", internalCount);
        out.put("catalogDriven", true);
        out.put("stages", snapshot.stages().stream().map(stage -> {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("fase", stage.getFase());
            entry.put("allowedNext", stage.getAllowedNext() == null ? List.of() : List.copyOf(stage.getAllowedNext()));
            LinkedHashSet<String> actors = new LinkedHashSet<>();
            List<Map<String, Object>> work = new ArrayList<>();
            if (stage.getWork() != null) {
                for (WorkTemplate item : stage.getWork()) {
                    if (item == null) {
                        continue;
                    }
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("code", item.getCode());
                    row.put("actorRole", item.getActorRole());
                    row.put("type", item.getType());
                    row.put("blocking", Boolean.TRUE.equals(item.getBlocking()));
                    work.add(Map.copyOf(row));
                    if (item.getActorRole() != null && !item.getActorRole().isBlank()) {
                        actors.add(item.getActorRole());
                    }
                }
            }
            entry.put("actors", List.copyOf(actors));
            entry.put("work", List.copyOf(work));
            return Map.copyOf(entry);
        }).toList());
        return Collections.unmodifiableMap(out);
    }

    private TipoUsuario parseRole(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return TipoUsuario.valueOf(raw.trim().toUpperCase());
        } catch (Exception ex) {
            return null;
        }
    }

    private String processId(RitoProcessual rito) {
        return "proc_" + token(rito.name().toLowerCase());
    }

    private String stageId(int index, String fase) {
        return "stage_" + index + '_' + token(fase == null ? "fase" : fase.toLowerCase());
    }

    private String token(String value) {
        return value == null ? "unknown" : value.replaceAll("[^A-Za-z0-9_]+", "_");
    }

    private String xml(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private String defaultText(String preferred, String fallback) {
        if (preferred != null && !preferred.isBlank()) {
            return preferred;
        }
        return fallback == null ? "Tarefa" : fallback;
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Falha ao gerar hash do BPMN.", ex);
        }
    }
}
