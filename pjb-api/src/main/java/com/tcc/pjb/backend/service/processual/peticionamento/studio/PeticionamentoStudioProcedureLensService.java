package com.tcc.pjb.backend.service.processual.peticionamento.studio;

import com.tcc.pjb.backend.core.kernel.recursal.LegalAppealType;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.EmbargosGroundCode;
import com.tcc.pjb.backend.model.dto.intelligence.recursal.mesh.RecursalMeshSpeciesType;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.session.PeticionamentoSessaoRequest;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.service.processual.recursal.workspace.RecursalFilingBlueprintAssembler;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class PeticionamentoStudioProcedureLensService {

    public ProcedureLensReport resolve(PeticionamentoSessaoRequest request,
                                       Map<String, Object> procedure,
                                       Map<String, Object> workspace) {
        PeticionamentoSessaoRequest safe = request == null ? new PeticionamentoSessaoRequest() : request;
        Map<String, Object> safeProcedure = procedure == null ? Map.of() : procedure;
        Map<String, Object> safeWorkspace = workspace == null ? Map.of() : workspace;
        String rawText = joinSignals(safe, safeProcedure, safeWorkspace);
        boolean contraRazoes = containsAny(rawText, "CONTRARRAZOES", "CONTRARRAZOES_RECURSAIS", "CONTRA RAZOES", "CONTRA-RAZOES", "CONTRARRAZÕES");
        LegalAppealType appealType = resolveAppealType(safe, safeProcedure, safeWorkspace, rawText);
        String family = resolveFamily(safe, appealType, contraRazoes, rawText);
        RecursalMeshSpeciesType speciesType = resolveSpeciesType(safe, safeProcedure, safeWorkspace, appealType);
        List<String> embargosGrounds = resolveEmbargosGrounds(safe, appealType, rawText);
        Map<String, Object> blueprint = resolveBlueprint(safe, safeProcedure, appealType, family);
        List<String> alerts = resolveAlerts(family, appealType, contraRazoes, blueprint, embargosGrounds);
        List<String> checklist = resolveChecklist(family, contraRazoes, blueprint, embargosGrounds);
        List<String> nextSteps = resolveNextSteps(family, contraRazoes, blueprint, embargosGrounds);
        LinkedHashMap<String, Object> lensWorkspace = new LinkedHashMap<>();
        put(lensWorkspace, "petitionFamily", family);
        put(lensWorkspace, "recursalCounterReasons", contraRazoes);
        put(lensWorkspace, "canonicalAppealType", appealType == null || appealType == LegalAppealType.OUTRO ? null : appealType.name());
        put(lensWorkspace, "recursalSpeciesType", speciesType == null ? null : speciesType.name());
        put(lensWorkspace, "embargosGrounds", embargosGrounds);
        put(lensWorkspace, "recursalBlueprint", blueprint);
        return new ProcedureLensReport(
                family,
                resolveDraftingMode(family, contraRazoes),
                contraRazoes,
                appealType,
                speciesType,
                embargosGrounds,
                alerts,
                checklist,
                nextSteps,
                Map.copyOf(lensWorkspace)
        );
    }

    private Map<String, Object> resolveBlueprint(PeticionamentoSessaoRequest request,
                                                 Map<String, Object> procedure,
                                                 LegalAppealType appealType,
                                                 String family) {
        if (!("RECURSAL".equals(family) || "EMBARGOS".equals(family) || "CONTRARRAZOES_RECURSAIS".equals(family))) {
            return Map.of();
        }
        LegalAppealType effectiveType = appealType == null || appealType == LegalAppealType.OUTRO
                ? defaultAppealType(family)
                : appealType;
        Processo processo = new Processo();
        processo.setRamoDireito(RamoDireito.fromString(firstNonBlank(stringValue(procedure.get("ramoDireito"), null), request.getRamoDireito())));
        processo.setRito(RitoProcessual.fromString(firstNonBlank(stringValue(procedure.get("ritoProcessual"), null), request.getRitoProcessual())));
        processo.setClasseProcessual(firstNonBlank(stringValue(procedure.get("classeProcessual"), null), request.getClasseProcessual()));
        processo.setTribunal(firstNonBlank(stringValue(procedure.get("tribunal"), null), stringValue(request.getCtx() == null ? null : request.getCtx().get("tribunal"), null)));
        processo.setComarca(firstNonBlank(stringValue(procedure.get("comarca"), null), request.getCidadeProtocolo()));
        processo.setUf(firstNonBlank(stringValue(procedure.get("uf"), null), request.getUfProtocolo()));
        return RecursalFilingBlueprintAssembler.assemble(
                processo,
                effectiveType,
                null,
                Boolean.TRUE.equals(request.getRequerLiminar()) || Boolean.TRUE.equals(request.getTutelaUrgencia()),
                Boolean.TRUE.equals(resolveBoolean(request.getCtx(), "preparoDispensado"))
        );
    }

    private List<String> resolveAlerts(String family,
                                       LegalAppealType appealType,
                                       boolean contraRazoes,
                                       Map<String, Object> blueprint,
                                       List<String> embargosGrounds) {
        ArrayList<String> alerts = new ArrayList<>();
        if ("RECURSAL".equals(family) || "CONTRARRAZOES_RECURSAIS".equals(family) || "EMBARGOS".equals(family)) {
            alerts.add("Peça recursal exige fechamento de decisão atacada, ciência/intimação e técnica própria do ato impugnativo.");
        }
        if (contraRazoes) {
            alerts.add("Contrarrazões não repetem a petição inicial: devem enfrentar os capítulos do recurso adverso e sustentar a manutenção do julgado.");
        }
        if ("EMBARGOS".equals(family) && embargosGrounds.isEmpty()) {
            alerts.add("Embargos exigem individualização do vício da decisão: omissão, contradição, obscuridade ou erro material.");
        }
        alerts.addAll(listOfStrings(blueprint.get("avisosOperacionais")));
        return distinct(alerts);
    }

    private List<String> resolveChecklist(String family,
                                          boolean contraRazoes,
                                          Map<String, Object> blueprint,
                                          List<String> embargosGrounds) {
        ArrayList<String> checklist = new ArrayList<>();
        if ("RECURSAL".equals(family) || "EMBARGOS".equals(family) || contraRazoes) {
            checklist.add("Fechar a decisão ou acórdão de referência com data de ciência válida para a janela recursal.");
            checklist.add("Conferir legitimidade, representação, assinatura e cabimento específico da peça escolhida.");
        }
        if (contraRazoes) {
            checklist.add("Mapear os capítulos atacados pelo recurso adverso e rebater cada ponto de forma dialética.");
        }
        if ("EMBARGOS".equals(family) && !embargosGrounds.isEmpty()) {
            checklist.add("Delimitar o vício embargado sem ampliar indevidamente o objeto integrativo da decisão.");
        }
        checklist.addAll(listOfStrings(blueprint.get("travasDeValidacao")));
        checklist.addAll(documentLabels(blueprint.get("documentosObrigatorios")));
        return distinct(checklist);
    }

    private List<String> resolveNextSteps(String family,
                                          boolean contraRazoes,
                                          Map<String, Object> blueprint,
                                          List<String> embargosGrounds) {
        ArrayList<String> nextSteps = new ArrayList<>();
        if ("RECURSAL".equals(family)) {
            nextSteps.add("Validar cabimento, tempestividade, preparo e mesa institucional antes de consolidar a minuta recursal.");
        }
        if (contraRazoes) {
            nextSteps.add("Estruturar resposta ao recurso adverso por capítulos, preservando o núcleo favorável da decisão recorrida.");
        }
        if ("EMBARGOS".equals(family)) {
            nextSteps.add("Apontar com precisão o vício da decisão e o trecho concreto que precisa de integração ou correção.");
            if (!embargosGrounds.isEmpty()) {
                nextSteps.add("Vícios detectados para refinamento humano: " + String.join(", ", embargosGrounds) + ".");
            }
        }
        nextSteps.addAll(listOfStrings(blueprint.get("assistantPrompts")));
        return distinct(nextSteps);
    }

    private String resolveFamily(PeticionamentoSessaoRequest request,
                                 LegalAppealType appealType,
                                 boolean contraRazoes,
                                 String rawText) {
        String ctxFamily = stringValue(request.getCtx() == null ? null : request.getCtx().get("petitionFamily"), null);
        if (ctxFamily != null) {
            String token = normalize(ctxFamily);
            if (token.contains("EMBARGO")) {
                return "EMBARGOS";
            }
            if (token.contains("CONTRARRAZ")) {
                return "CONTRARRAZOES_RECURSAIS";
            }
            if (token.contains("RECURS")) {
                return "RECURSAL";
            }
        }
        if (contraRazoes) {
            return "CONTRARRAZOES_RECURSAIS";
        }
        if (appealType != null && appealType != LegalAppealType.OUTRO) {
            return appealType.name().startsWith("EMBARGOS") ? "EMBARGOS" : "RECURSAL";
        }
        if (containsAny(rawText, "EMBARGOS DE DECLARACAO", "EMBARGOS À EXECUCAO", "EMBARGOS A EXECUCAO", "EMBARGOS DE TERCEIRO")) {
            return "EMBARGOS";
        }
        if (containsAny(rawText, "APELACAO", "AGRAVO", "RECURSO", "CONTRARRAZOES", "CONTRARRAZÕES", "RAZOES RECURSAIS", "RAZÕES RECURSAIS")) {
            return "RECURSAL";
        }
        return "PETICAO_BASE";
    }

    private String resolveDraftingMode(String family, boolean contraRazoes) {
        if (contraRazoes) {
            return "CONTRARRAZOES_TECNICAS_ASSISTIDAS";
        }
        return switch (family) {
            case "RECURSAL" -> "RECURSAL_TECNICO_ASSISTIDO";
            case "EMBARGOS" -> "EMBARGOS_ASSISTIDOS";
            default -> "RAPIDO_ASSISTIDO";
        };
    }

    private LegalAppealType defaultAppealType(String family) {
        return "EMBARGOS".equals(family) ? LegalAppealType.EMBARGOS_DECLARACAO : LegalAppealType.APELACAO;
    }

    private LegalAppealType resolveAppealType(PeticionamentoSessaoRequest request,
                                              Map<String, Object> procedure,
                                              Map<String, Object> workspace,
                                              String rawText) {
        ArrayList<String> candidates = new ArrayList<>();
        add(candidates, stringValue(request.getCtx() == null ? null : request.getCtx().get("appealType"), null));
        add(candidates, stringValue(request.getCtx() == null ? null : request.getCtx().get("tipoRecursal"), null));
        add(candidates, stringValue(request.getCtx() == null ? null : request.getCtx().get("speciesType"), null));
        add(candidates, stringValue(procedure.get("classeProcessual"), null));
        add(candidates, request.getClasseProcessual());
        add(candidates, request.getTituloCaso());
        add(candidates, request.getTextoPeticaoLivre());
        add(candidates, request.getTextoFatosResumido());
        add(candidates, rawText);
        add(candidates, stringValue(workspace.get("nextAction"), null));
        for (String candidate : candidates) {
            LegalAppealType type = LegalAppealType.fromString(candidate);
            if (type != null && type != LegalAppealType.OUTRO) {
                return type;
            }
        }
        if (containsAny(rawText, "EMBARGOS DE DECLARACAO", "EMBARGOS DECLARATORIOS", "OMISSAO", "CONTRADICAO", "OBSCURIDADE", "ERRO MATERIAL")) {
            return LegalAppealType.EMBARGOS_DECLARACAO;
        }
        if (containsAny(rawText, "EMBARGOS A EXECUCAO", "EMBARGOS À EXECUCAO")) {
            return LegalAppealType.EMBARGOS_EXECUCAO;
        }
        if (containsAny(rawText, "EMBARGOS DE TERCEIRO")) {
            return LegalAppealType.EMBARGOS_TERCEIRO;
        }
        if (containsAny(rawText, "APELACAO", "APELAÇÃO")) {
            return LegalAppealType.APELACAO;
        }
        if (containsAny(rawText, "AGRAVO DE INSTRUMENTO")) {
            return LegalAppealType.AGRAVO_INSTRUMENTO;
        }
        if (containsAny(rawText, "AGRAVO INTERNO", "AGRAVO REGIMENTAL")) {
            return containsAny(rawText, "REGIMENTAL") ? LegalAppealType.AGRAVO_REGIMENTAL : LegalAppealType.AGRAVO_INTERNO;
        }
        if (containsAny(rawText, "RECURSO INOMINADO")) {
            return LegalAppealType.RECURSO_INOMINADO;
        }
        if (containsAny(rawText, "RECURSO DE REVISTA")) {
            return LegalAppealType.RECURSO_REVISTA;
        }
        if (containsAny(rawText, "AGRAVO DE PETICAO", "AGRAVO DE PETIÇÃO")) {
            return LegalAppealType.AGRAVO_PETICAO;
        }
        if (containsAny(rawText, "RESP", "RECURSO ESPECIAL")) {
            return LegalAppealType.RESP;
        }
        if (containsAny(rawText, "RECURSO EXTRAORDINARIO", "RECURSO EXTRAORDINÁRIO")) {
            return LegalAppealType.RE;
        }
        return LegalAppealType.OUTRO;
    }

    private RecursalMeshSpeciesType resolveSpeciesType(PeticionamentoSessaoRequest request,
                                                       Map<String, Object> procedure,
                                                       Map<String, Object> workspace,
                                                       LegalAppealType appealType) {
        ArrayList<String> candidates = new ArrayList<>();
        add(candidates, stringValue(request.getCtx() == null ? null : request.getCtx().get("speciesType"), null));
        add(candidates, stringValue(request.getCtx() == null ? null : request.getCtx().get("meshSpeciesType"), null));
        add(candidates, stringValue(workspace.get("meshSpeciesType"), null));
        add(candidates, stringValue(procedure.get("classeProcessual"), null));
        add(candidates, request.getClasseProcessual());
        for (String candidate : candidates) {
            RecursalMeshSpeciesType type = RecursalMeshSpeciesType.fromString(candidate);
            if (type != null) {
                return type;
            }
        }
        if (appealType == null) {
            return null;
        }
        return switch (appealType) {
            case APELACAO -> RecursalMeshSpeciesType.APCIV;
            case APELACAO_PENAL -> RecursalMeshSpeciesType.APCRIM;
            case AGRAVO_INSTRUMENTO -> RecursalMeshSpeciesType.AGINST;
            case AGRAVO_INTERNO -> RecursalMeshSpeciesType.AGINT;
            case AGRAVO_REGIMENTAL -> RecursalMeshSpeciesType.AGREG;
            case EMBARGOS_DECLARACAO -> RecursalMeshSpeciesType.EDCL;
            case RESP -> RecursalMeshSpeciesType.RESP;
            case RE -> RecursalMeshSpeciesType.RE;
            case RECURSO_INOMINADO -> RecursalMeshSpeciesType.RINOM;
            case PEDIDO_UNIFORMIZACAO -> RecursalMeshSpeciesType.PUILF;
            case RECURSO_ORDINARIO_CONSTITUCIONAL -> RecursalMeshSpeciesType.ROC;
            case RECURSO_ORDINARIO_TRABALHISTA -> RecursalMeshSpeciesType.ROT;
            case RECURSO_REVISTA -> RecursalMeshSpeciesType.RR;
            case AGRAVO_RECURSO_REVISTA -> RecursalMeshSpeciesType.AIRR;
            case AGRAVO_PETICAO -> RecursalMeshSpeciesType.AGPET;
            case EMBARGOS_EXECUCAO -> RecursalMeshSpeciesType.EEXEC;
            case EMBARGOS_EXECUCAO_FISCAL -> RecursalMeshSpeciesType.EEFISC;
            case EMBARGOS_TERCEIRO -> RecursalMeshSpeciesType.ETERC;
            case RECLAMACAO_CONSTITUCIONAL -> RecursalMeshSpeciesType.RCL;
            case CONFLITO_COMPETENCIA -> RecursalMeshSpeciesType.CC;
            case CORREICAO_PARCIAL -> RecursalMeshSpeciesType.CPARCIAL;
            case AGRAVO_RESP_RE -> RecursalMeshSpeciesType.ARESP;
            default -> null;
        };
    }

    private List<String> resolveEmbargosGrounds(PeticionamentoSessaoRequest request,
                                                LegalAppealType appealType,
                                                String rawText) {
        if (appealType == null || !(appealType == LegalAppealType.EMBARGOS_DECLARACAO
                || appealType == LegalAppealType.EMBARGOS_EXECUCAO
                || appealType == LegalAppealType.EMBARGOS_EXECUCAO_FISCAL
                || appealType == LegalAppealType.EMBARGOS_TERCEIRO
                || containsAny(rawText, "EMBARGOS"))) {
            return List.of();
        }
        LinkedHashSet<String> grounds = new LinkedHashSet<>();
        Object ctxGrounds = request.getCtx() == null ? null : request.getCtx().get("embargosGrounds");
        if (ctxGrounds instanceof List<?> list) {
            for (Object item : list) {
                String normalized = trimToNull(item == null ? null : String.valueOf(item));
                if (normalized != null) {
                    grounds.add(normalizeEmbargoGround(normalized));
                }
            }
        }
        if (containsAny(rawText, "OMISSAO", "OMISSÃO")) {
            grounds.add(EmbargosGroundCode.OMISSAO.name());
        }
        if (containsAny(rawText, "CONTRADICAO", "CONTRADIÇÃO")) {
            grounds.add(EmbargosGroundCode.CONTRADICAO.name());
        }
        if (containsAny(rawText, "OBSCURIDADE")) {
            grounds.add(EmbargosGroundCode.OBSCURIDADE.name());
        }
        if (containsAny(rawText, "ERRO MATERIAL")) {
            grounds.add(EmbargosGroundCode.ERRO_MATERIAL.name());
        }
        return List.copyOf(grounds);
    }

    private String normalizeEmbargoGround(String value) {
        String token = normalize(value);
        if (token.contains("OMISS")) {
            return EmbargosGroundCode.OMISSAO.name();
        }
        if (token.contains("CONTRAD")) {
            return EmbargosGroundCode.CONTRADICAO.name();
        }
        if (token.contains("OBSCUR")) {
            return EmbargosGroundCode.OBSCURIDADE.name();
        }
        if (token.contains("ERRO")) {
            return EmbargosGroundCode.ERRO_MATERIAL.name();
        }
        return token;
    }

    private List<String> documentLabels(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        ArrayList<String> labels = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map<?, ?> map) {
                String label = trimToNull(stringValue(map.get("label"), null));
                if (label != null) {
                    labels.add("Anexo obrigatório ou esperado: " + label + ".");
                }
            }
        }
        return labels;
    }

    private List<String> listOfStrings(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        ArrayList<String> out = new ArrayList<>();
        for (Object item : list) {
            String normalized = trimToNull(item == null ? null : String.valueOf(item));
            if (normalized != null) {
                out.add(normalized);
            }
        }
        return distinct(out);
    }

    private List<String> distinct(List<String> values) {
        LinkedHashSet<String> out = new LinkedHashSet<>();
        if (values != null) {
            for (String value : values) {
                String normalized = trimToNull(value);
                if (normalized != null) {
                    out.add(normalized);
                }
            }
        }
        return List.copyOf(out);
    }

    private String joinSignals(PeticionamentoSessaoRequest request,
                               Map<String, Object> procedure,
                               Map<String, Object> workspace) {
        ArrayList<String> parts = new ArrayList<>();
        add(parts, request.getTituloCaso());
        add(parts, request.getClasseProcessual());
        add(parts, request.getTextoPeticaoLivre());
        add(parts, request.getTextoFatosResumido());
        add(parts, request.getRitoProcessual());
        add(parts, request.getMateriaPrincipal());
        add(parts, stringValue(procedure.get("classeProcessual"), null));
        add(parts, stringValue(procedure.get("ritoProcessual"), null));
        add(parts, stringValue(workspace.get("nextAction"), null));
        if (request.getCtx() != null) {
            request.getCtx().forEach((key, value) -> {
                add(parts, key);
                if (value instanceof List<?> list) {
                    for (Object item : list) {
                        add(parts, item == null ? null : String.valueOf(item));
                    }
                } else {
                    add(parts, value == null ? null : String.valueOf(value));
                }
            });
        }
        return normalize(String.join(" | ", parts));
    }

    private void add(List<String> target, String value) {
        String normalized = trimToNull(value);
        if (normalized != null) {
            target.add(normalized);
        }
    }

    private Boolean resolveBoolean(Map<String, Object> ctx, String key) {
        if (ctx == null || key == null) {
            return null;
        }
        Object value = ctx.get(key);
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String text) {
            return "true".equalsIgnoreCase(text) || "sim".equalsIgnoreCase(text);
        }
        return null;
    }

    private boolean containsAny(String haystack, String... needles) {
        String normalizedHaystack = normalize(haystack);
        if (normalizedHaystack == null || needles == null) {
            return false;
        }
        for (String needle : needles) {
            String normalizedNeedle = normalize(needle);
            if (normalizedNeedle != null && normalizedHaystack.contains(normalizedNeedle)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        String trimmed = trimToNull(value);
        if (trimmed == null) {
            return null;
        }
        String normalized = java.text.Normalizer.normalize(trimmed, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toUpperCase(Locale.ROOT);
        return normalized;
    }

    private String stringValue(Object value, String fallback) {
        String normalized = trimToNull(value == null ? null : String.valueOf(value));
        return normalized == null ? fallback : normalized;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void put(Map<String, Object> target, String key, Object value) {
        if (target == null || key == null || key.isBlank() || value == null) {
            return;
        }
        if (value instanceof String text && text.isBlank()) {
            return;
        }
        if (value instanceof List<?> list && list.isEmpty()) {
            return;
        }
        if (value instanceof Map<?, ?> map && map.isEmpty()) {
            return;
        }
        target.put(key, value);
    }

    public record ProcedureLensReport(String petitionFamily,
                                      String draftingMode,
                                      boolean counterReasons,
                                      LegalAppealType appealType,
                                      RecursalMeshSpeciesType speciesType,
                                      List<String> embargosGrounds,
                                      List<String> alerts,
                                      List<String> checklist,
                                      List<String> nextSteps,
                                      Map<String, Object> workspace) {
        public ProcedureLensReport {
            petitionFamily = petitionFamily == null || petitionFamily.isBlank() ? "PETICAO_BASE" : petitionFamily;
            draftingMode = draftingMode == null || draftingMode.isBlank() ? "RAPIDO_ASSISTIDO" : draftingMode;
            embargosGrounds = embargosGrounds == null ? List.of() : List.copyOf(embargosGrounds);
            alerts = alerts == null ? List.of() : List.copyOf(alerts);
            checklist = checklist == null ? List.of() : List.copyOf(checklist);
            nextSteps = nextSteps == null ? List.of() : List.copyOf(nextSteps);
            workspace = workspace == null ? Map.of() : Map.copyOf(workspace);
        }
    }
}
