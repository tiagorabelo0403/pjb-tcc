package com.tcc.pjb.backend.service.criminal;

import com.tcc.pjb.backend.model.dto.profile.operational.DelegadoInqueritoMultimidiaRequest;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.processual.peticionamento.workspace.InstitutionalMultimediaWorkspaceService;
import com.tcc.pjb.backend.service.ui.branding.InstitutionalPanelBrandingService;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class InqueritoMultimidiaWorkspaceService {

    private final InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService;
    private final InstitutionalPanelBrandingService institutionalPanelBrandingService;
    private final PoliceInvestigationSystemLandscapeService policeInvestigationSystemLandscapeService;
    private final PoliceInteroperabilityAdapterBlueprintService policeInteroperabilityAdapterBlueprintService;
    private final PjbPoliceNativeToolbeltService pjbPoliceNativeToolbeltService;
    private final PoliceTransactionalAdapterMeshService policeTransactionalAdapterMeshService;
    private final PoliceSovereignOperationalWorkbenchService policeSovereignOperationalWorkbenchService;
    private final PjbPoliceNativeExecutionService pjbPoliceNativeExecutionService;
    private final PoliceTraceableExecutionLedgerService policeTraceableExecutionLedgerService;

    public InqueritoMultimidiaWorkspaceService(InstitutionalMultimediaWorkspaceService institutionalMultimediaWorkspaceService,
                                              InstitutionalPanelBrandingService institutionalPanelBrandingService,
                                              PoliceInvestigationSystemLandscapeService policeInvestigationSystemLandscapeService,
                                              PoliceInteroperabilityAdapterBlueprintService policeInteroperabilityAdapterBlueprintService,
                                              PjbPoliceNativeToolbeltService pjbPoliceNativeToolbeltService,
                                              PoliceTransactionalAdapterMeshService policeTransactionalAdapterMeshService,
                                              PoliceSovereignOperationalWorkbenchService policeSovereignOperationalWorkbenchService,
                                              PjbPoliceNativeExecutionService pjbPoliceNativeExecutionService,
                                              PoliceTraceableExecutionLedgerService policeTraceableExecutionLedgerService) {
        this.institutionalMultimediaWorkspaceService = Objects.requireNonNull(institutionalMultimediaWorkspaceService, "institutionalMultimediaWorkspaceService");
        this.institutionalPanelBrandingService = Objects.requireNonNull(institutionalPanelBrandingService, "institutionalPanelBrandingService");
        this.policeInvestigationSystemLandscapeService = Objects.requireNonNull(policeInvestigationSystemLandscapeService, "policeInvestigationSystemLandscapeService");
        this.policeInteroperabilityAdapterBlueprintService = Objects.requireNonNull(policeInteroperabilityAdapterBlueprintService, "policeInteroperabilityAdapterBlueprintService");
        this.pjbPoliceNativeToolbeltService = Objects.requireNonNull(pjbPoliceNativeToolbeltService, "pjbPoliceNativeToolbeltService");
        this.policeTransactionalAdapterMeshService = Objects.requireNonNull(policeTransactionalAdapterMeshService, "policeTransactionalAdapterMeshService");
        this.policeSovereignOperationalWorkbenchService = Objects.requireNonNull(policeSovereignOperationalWorkbenchService, "policeSovereignOperationalWorkbenchService");
        this.pjbPoliceNativeExecutionService = Objects.requireNonNull(pjbPoliceNativeExecutionService, "pjbPoliceNativeExecutionService");
        this.policeTraceableExecutionLedgerService = Objects.requireNonNull(policeTraceableExecutionLedgerService, "policeTraceableExecutionLedgerService");
    }

    public Map<String, Object> compose(Long inqueritoId, TipoUsuario tipoUsuario, DelegadoInqueritoMultimidiaRequest request) {
        DelegadoInqueritoMultimidiaRequest safe = request == null
                ? new DelegadoInqueritoMultimidiaRequest("RELATORIO_INQUERITO", "Narrativa investigativa não informada", null, null, null, null, null, null, Boolean.FALSE, Boolean.TRUE)
                : request;
        String actorLane = actorLane(tipoUsuario);
        String pieceKind = resolvePieceKind(safe.tipoPeca());
        Map<String, Object> institutional = institutionalMultimediaWorkspaceService.enrich(
                new InstitutionalMultimediaWorkspaceService.ResolveRequest(
                        actorLane,
                        pieceKind,
                        inqueritoId,
                        tipoUsuario,
                        safe,
                        safe.prepararPacoteProtocoloResolvido(),
                        safe.sigiloSensivelResolvido(),
                        false
                )
        );
        Map<String, Object> panelBranding = institutionalPanelBrandingService.resolve(actorLane, "PAINEL_INQUERITO_MULTIMIDIA", tipoUsuario);
        Map<String, Object> policeSystemsLandscape = policeInvestigationSystemLandscapeService.landscapeFor(tipoUsuario);
        Map<String, Object> adapterOperationalMesh = policeInteroperabilityAdapterBlueprintService.operationalMesh(tipoUsuario);
        Map<String, Object> nativeToolbelt = pjbPoliceNativeToolbeltService.nativeWorkbench(tipoUsuario);
        Map<String, Object> transactionalAdapterMesh = policeTransactionalAdapterMeshService.sovereignMesh(tipoUsuario);
        Map<String, Object> investigativeWorkstation = policeSovereignOperationalWorkbenchService.compose(tipoUsuario);
        Map<String, Object> nativeExecutionWorkbench = pjbPoliceNativeExecutionService.nativeExecutionWorkbench(tipoUsuario);
        Map<String, Object> traceableOperationalLedger = policeTraceableExecutionLedgerService.operationalLedgerBlueprint(tipoUsuario);
        LinkedHashMap<String, Object> page = new LinkedHashMap<>();
        page.put("pageMode", "INQUERITO_MULTIMIDIA_ESTREITO");
        page.put("columnMode", "NARRATIVA_ESTREITA_COM_EVIDENCIAS");
        page.put("supportsInlineText", Boolean.TRUE);
        page.put("supportsInlineImage", Boolean.TRUE);
        page.put("supportsInlineAudio", Boolean.TRUE);
        page.put("supportsInlineVideo", Boolean.TRUE);
        page.put("documentsAfterNarrative", Boolean.TRUE);
        page.put("pieceKind", pieceKind);
        page.put("actorLane", actorLane);
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.putAll(institutional);
        out.put("inqueritoMultimediaPage", Map.copyOf(page));
        out.put("policeSystemsLandscape", policeSystemsLandscape);
        out.put("adapterOperationalMesh", adapterOperationalMesh);
        out.put("nativeToolbelt", nativeToolbelt);
        out.put("transactionalAdapterMesh", transactionalAdapterMesh);
        out.put("pjbOwnTools", nativeToolbelt);
        out.put("nativeExecutionWorkbench", nativeExecutionWorkbench);
        out.put("traceableOperationalLedger", traceableOperationalLedger);
        out.put("recentTraceableExecutions", policeTraceableExecutionLedgerService.recentExecutions(tipoUsuario, 6));
        out.put("investigativeWorkstation", investigativeWorkstation);
        out.put("interoperabilityBlueprint", investigativeWorkstation.get("interoperabilityTargets"));
        out.put("documentAuthenticityPolicy", investigativeWorkstation.get("signatureAndAuthenticity"));
        out.put("allFunctionsNecessary", investigativeWorkstation.get("mandatoryFunctionFamilies"));
        out.putAll(panelBranding);
        return Collections.unmodifiableMap(out);
    }

    private static String actorLane(TipoUsuario tipoUsuario) {
        if (tipoUsuario == TipoUsuario.DELEGADO_POLICIA_FEDERAL) {
            return "POLICIA_FEDERAL";
        }
        return "POLICIA_CIVIL";
    }

    private static String resolvePieceKind(String tipoPeca) {
        String normalized = tipoPeca == null ? "RELATORIO_INQUERITO" : tipoPeca.trim().toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "REPRESENTACAO_POLICIAL", "REPRESENTACAO" -> "REPRESENTACAO_POLICIAL";
            case "DESPACHO_CARTORIO_POLICIAL", "CERTIDAO_CARTORIO_POLICIAL" -> "CERTIDAO_CARTORIO_POLICIAL";
            default -> "RELATORIO_INQUERITO";
        };
    }
}
