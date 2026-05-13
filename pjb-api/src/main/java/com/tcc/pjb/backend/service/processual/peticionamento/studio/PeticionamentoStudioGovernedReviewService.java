package com.tcc.pjb.backend.service.processual.peticionamento.studio;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class PeticionamentoStudioGovernedReviewService {

    public ReviewGovernanceReport build(ResolveRequest request) {
        ResolveRequest safe = request == null ? ResolveRequest.empty() : request;
        ArrayList<Map<String, Object>> lanes = new ArrayList<>();
        ArrayList<String> blockers = new ArrayList<>();
        ArrayList<String> warnings = new ArrayList<>();
        ArrayList<String> nextActions = new ArrayList<>();

        String governanceMode = resolveGovernanceMode(safe);
        boolean patronalRequired = requiresPatronalReview(safe, governanceMode);
        boolean institutionalReview = requiresInstitutionalReview(safe, governanceMode);
        boolean finalSignerRequired = patronalRequired || institutionalReview || safe.forceSignerReview();

        lanes.add(lane("AUTOR_DA_MINUTA", "AUTOR", "ACTIVE", "Responsável pela consolidação fática, normativa e documental da primeira versão da peça."));
        if (patronalRequired) {
            lanes.add(lane("REVISAO_PATRONAL", safe.signerName() == null ? "PATRONO_RESPONSAVEL" : safe.signerName(), safe.reviewAccepted() ? "READY" : "PENDING", "Revisão patronal obrigatória antes da assinatura e do protocolo em modo escritório."));
        }
        if (institutionalReview) {
            lanes.add(lane("REVISAO_INSTITUCIONAL", safe.organizationalAnchor() == null ? "UNIDADE_INSTITUCIONAL" : safe.organizationalAnchor(), safe.reviewAccepted() ? "READY" : "PENDING", "Trilha hierárquica institucional exigida para controle do ato processual."));
        }
        lanes.add(lane("ASSINATURA", safe.signerName() == null ? "ASSINANTE_FINAL" : safe.signerName(), finalSignerRequired && !safe.reviewAccepted() ? "WAITING_REVIEW" : "READY", "Assinatura governada conforme o perfil, a lotação ou a política patronal ativa."));
        lanes.add(lane("PROTOCOLO", "PACOTE_DE_PROTOCOLO", safe.protocolReady() ? "READY" : "BLOCKED", "Protocolo assistido condicionado a checklist, risco e integridade documental."));

        if (safe.riskBlocking()) {
            blockers.add("A matriz de risco mantém bloqueios ativos; a revisão governada não pode liberar assinatura imediata.");
        }
        if (!safe.protocolReady()) {
            blockers.add("Checklist e lacunas documentais ainda impedem o pacote de protocolo final.");
        }
        if (finalSignerRequired && !safe.reviewAccepted()) {
            blockers.add("A trilha governada exige revisão/aceite formal antes da assinatura final.");
        }
        if ("EMBARGOS".equals(safe.petitionFamily())) {
            warnings.add("Embargos devem permanecer na moldura integrativa própria da espécie durante a revisão governada.");
        }
        if (safe.isRecursalFamily()) {
            warnings.add("A revisão governada deve conferir decisão impugnada, ciência/intimação e técnica dialética da peça recursal.");
        }
        if (patronalRequired) {
            nextActions.add("Encaminhar a minuta para revisão patronal com diff, checklist e matriz prova x pedido já consolidados.");
        }
        if (institutionalReview) {
            nextActions.add("Submeter a peça à trilha institucional com validação de competência, assinatura e documentação de suporte.");
        }
        if (!patronalRequired && !institutionalReview && !safe.forceSignerReview()) {
            nextActions.add("Executar revisão final do próprio subscritor e seguir para assinatura controlada.");
        }
        if (!safe.protocolReady()) {
            nextActions.add("Resolver primeiro as pendências críticas do checklist e das lacunas documentais antes de solicitar aceite final.");
        }
        if (nextActions.isEmpty()) {
            nextActions.add("Peça pronta para aceite final, assinatura governada e protocolo assistido.");
        }

        LinkedHashMap<String, Object> workspace = new LinkedHashMap<>();
        workspace.put("governanceMode", governanceMode);
        workspace.put("reviewRequired", finalSignerRequired);
        workspace.put("reviewAccepted", safe.reviewAccepted());
        workspace.put("signerName", safe.signerName());
        workspace.put("lanes", List.copyOf(lanes));
        workspace.put("blockers", List.copyOf(blockers));
        workspace.put("warnings", List.copyOf(warnings));
        workspace.put("nextActions", List.copyOf(nextActions));
        workspace.put("organizationalAnchor", safe.organizationalAnchor());
        workspace.put("petitionFamily", safe.petitionFamily());
        return new ReviewGovernanceReport(
                governanceMode,
                List.copyOf(lanes),
                List.copyOf(blockers),
                List.copyOf(warnings),
                List.copyOf(nextActions),
                Map.copyOf(workspace)
        );
    }

    private String resolveGovernanceMode(ResolveRequest request) {
        if (request.explicitGovernanceMode() != null && !request.explicitGovernanceMode().isBlank()) {
            return request.explicitGovernanceMode().trim().toUpperCase(Locale.ROOT);
        }
        if (request.actorProfile() != null) {
            String normalized = request.actorProfile().toUpperCase(Locale.ROOT);
            if (normalized.contains("INSTITUCIONAL") || normalized.contains("DEFENSOR") || normalized.contains("PROCURADOR") || normalized.contains("MAGISTR")) {
                return "INSTITUCIONAL_HIERARQUICO";
            }
            if (normalized.contains("ESCRITORIO") || normalized.contains("OFFICE") || normalized.contains("PATRONO")) {
                return "ESCRITORIO_PATRONAL";
            }
        }
        return "SOLO_AUTOGOVERNADO";
    }

    private boolean requiresPatronalReview(ResolveRequest request, String governanceMode) {
        if (request.forcePatronalReview()) {
            return true;
        }
        return "ESCRITORIO_PATRONAL".equals(governanceMode) && request.signerName() != null && !request.signerName().isBlank();
    }

    private boolean requiresInstitutionalReview(ResolveRequest request, String governanceMode) {
        if (request.forceInstitutionalReview()) {
            return true;
        }
        return "INSTITUCIONAL_HIERARQUICO".equals(governanceMode);
    }

    private Map<String, Object> lane(String code, String actor, String status, String summary) {
        LinkedHashMap<String, Object> lane = new LinkedHashMap<>();
        lane.put("code", code);
        lane.put("actor", actor);
        lane.put("status", status);
        lane.put("summary", summary);
        return Map.copyOf(lane);
    }

    public record ResolveRequest(String petitionFamily,
                                 String actorProfile,
                                 String draftingMode,
                                 String explicitGovernanceMode,
                                 String signerName,
                                 String organizationalAnchor,
                                 boolean protocolReady,
                                 boolean riskBlocking,
                                 boolean reviewAccepted,
                                 boolean forcePatronalReview,
                                 boolean forceInstitutionalReview,
                                 boolean forceSignerReview) {
        public ResolveRequest {
            petitionFamily = petitionFamily == null || petitionFamily.isBlank() ? "PETICAO_BASE" : petitionFamily.trim();
        }

        public boolean isRecursalFamily() {
            return petitionFamily != null && !petitionFamily.isBlank() && !"PETICAO_BASE".equals(petitionFamily);
        }

        public static ResolveRequest empty() {
            return new ResolveRequest("PETICAO_BASE", null, null, null, null, null, false, true, false, false, false, false);
        }
    }

    public record ReviewGovernanceReport(String governanceMode,
                                         List<Map<String, Object>> lanes,
                                         List<String> blockers,
                                         List<String> warnings,
                                         List<String> nextActions,
                                         Map<String, Object> workspace) {
        public ReviewGovernanceReport {
            governanceMode = governanceMode == null || governanceMode.isBlank() ? "SOLO_AUTOGOVERNADO" : governanceMode.trim().toUpperCase(Locale.ROOT);
            lanes = lanes == null ? List.of() : List.copyOf(lanes);
            blockers = blockers == null ? List.of() : List.copyOf(blockers);
            warnings = warnings == null ? List.of() : List.copyOf(warnings);
            nextActions = nextActions == null ? List.of() : List.copyOf(nextActions);
            workspace = workspace == null ? Map.of() : Map.copyOf(new LinkedHashMap<>(workspace));
        }
    }
}
