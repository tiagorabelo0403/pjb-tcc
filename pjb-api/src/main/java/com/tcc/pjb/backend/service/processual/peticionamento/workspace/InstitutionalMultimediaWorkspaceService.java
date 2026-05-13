package com.tcc.pjb.backend.service.processual.peticionamento.workspace;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.model.dto.processual.peticionamento.media.PeticionamentoMediaBlocoRequest;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.service.ui.branding.InstitutionalBrandingPolicyService;
import com.tcc.pjb.backend.service.ui.branding.InstitutionalBrandingResolverService;
import com.tcc.pjb.backend.service.ui.branding.InstitutionalPieceVisualComposerService;
import com.tcc.pjb.backend.service.upload.UploadCapacityGovernanceService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.service.processual.peticionamento.PeticionamentoPericiaEvidenceIntelligenceService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoMediaPublicationGateService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoMediaSecurityPipelineService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoMediaStorageShieldService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoMultimidiaComposerService;
import com.tcc.pjb.backend.service.processual.peticionamento.media.PeticionamentoThreatSentinelService;

@Service
public class InstitutionalMultimediaWorkspaceService {

    private final ObjectMapper objectMapper;
    private final PeticionamentoMultimidiaComposerService multimidiaComposerService;
    private final PeticionamentoMediaSecurityPipelineService mediaSecurityPipelineService;
    private final PeticionamentoThreatSentinelService threatSentinelService;
    private final PeticionamentoMediaStorageShieldService mediaStorageShieldService;
    private final PeticionamentoPericiaEvidenceIntelligenceService periciaEvidenceIntelligenceService;
    private final PeticionamentoMediaPublicationGateService mediaPublicationGateService;
    private final UploadCapacityGovernanceService uploadCapacityGovernanceService;
    private final InstitutionalBrandingResolverService institutionalBrandingResolverService;
    private final InstitutionalBrandingPolicyService institutionalBrandingPolicyService;
    private final InstitutionalPieceVisualComposerService institutionalPieceVisualComposerService;

    public InstitutionalMultimediaWorkspaceService(ObjectMapper objectMapper,
                                                   PeticionamentoMultimidiaComposerService multimidiaComposerService,
                                                   PeticionamentoMediaSecurityPipelineService mediaSecurityPipelineService,
                                                   PeticionamentoThreatSentinelService threatSentinelService,
                                                   PeticionamentoMediaStorageShieldService mediaStorageShieldService,
                                                   PeticionamentoPericiaEvidenceIntelligenceService periciaEvidenceIntelligenceService,
                                                   PeticionamentoMediaPublicationGateService mediaPublicationGateService,
                                                   UploadCapacityGovernanceService uploadCapacityGovernanceService,
                                                   InstitutionalBrandingResolverService institutionalBrandingResolverService,
                                                   InstitutionalBrandingPolicyService institutionalBrandingPolicyService,
                                                   InstitutionalPieceVisualComposerService institutionalPieceVisualComposerService) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper");
        this.multimidiaComposerService = Objects.requireNonNull(multimidiaComposerService, "multimidiaComposerService");
        this.mediaSecurityPipelineService = Objects.requireNonNull(mediaSecurityPipelineService, "mediaSecurityPipelineService");
        this.threatSentinelService = Objects.requireNonNull(threatSentinelService, "threatSentinelService");
        this.mediaStorageShieldService = Objects.requireNonNull(mediaStorageShieldService, "mediaStorageShieldService");
        this.periciaEvidenceIntelligenceService = Objects.requireNonNull(periciaEvidenceIntelligenceService, "periciaEvidenceIntelligenceService");
        this.mediaPublicationGateService = Objects.requireNonNull(mediaPublicationGateService, "mediaPublicationGateService");
        this.uploadCapacityGovernanceService = Objects.requireNonNull(uploadCapacityGovernanceService, "uploadCapacityGovernanceService");
        this.institutionalBrandingResolverService = Objects.requireNonNull(institutionalBrandingResolverService, "institutionalBrandingResolverService");
        this.institutionalBrandingPolicyService = Objects.requireNonNull(institutionalBrandingPolicyService, "institutionalBrandingPolicyService");
        this.institutionalPieceVisualComposerService = Objects.requireNonNull(institutionalPieceVisualComposerService, "institutionalPieceVisualComposerService");
    }

    public Map<String, Object> enrich(ResolveRequest request) {
        ResolveRequest safe = request == null ? ResolveRequest.empty() : request;
        Map<String, Object> requestMap = normalizeRequest(safe.rawRequest());
        List<PeticionamentoMediaBlocoRequest> inlineMedia = resolveInlineMedia(requestMap);
        List<String> provasDocumentais = resolveStrings(requestMap, List.of("provasDocumentais", "provasDocumentaisReferenciadas", "anexosProbatorios"));
        List<String> documentosPessoais = resolveStrings(requestMap, List.of("documentosPessoais", "documentosIdentificacao"));
        List<String> documentosRepresentacao = resolveStrings(requestMap, List.of("documentosRepresentacao", "documentosRepresentativos", "instrumentosRepresentacao"));
        List<String> documentosAnexados = resolveStrings(requestMap, List.of("documentosAnexados", "documentosGerais", "anexos", "anexosGerais"));

        PeticionamentoMultimidiaComposerService.CompositionReport composition = multimidiaComposerService.compose(
                new PeticionamentoMultimidiaComposerService.ResolveRequest(
                        inlineMedia,
                        provasDocumentais,
                        documentosPessoais,
                        documentosRepresentacao,
                        documentosAnexados
                )
        );
        PeticionamentoMediaSecurityPipelineService.SecurityReport mediaSecurity = mediaSecurityPipelineService.assess(
                new PeticionamentoMediaSecurityPipelineService.ResolveRequest(
                        inlineMedia,
                        safe.tipoUsuario(),
                        safe.sigiloSensivel()
                )
        );
        PeticionamentoThreatSentinelService.ThreatSentinelReport threatSentinel = threatSentinelService.plan(
                new PeticionamentoThreatSentinelService.ResolveRequest(
                        sessionKey(safe),
                        inlineMedia,
                        safe.sigiloSensivel()
                )
        );
        PeticionamentoMediaStorageShieldService.StorageShieldReport mediaStorage = mediaStorageShieldService.plan(
                new PeticionamentoMediaStorageShieldService.ResolveRequest(
                        inlineMedia,
                        provasDocumentais,
                        documentosPessoais,
                        documentosRepresentacao,
                        documentosAnexados,
                        safe.preparingProtocolPackage()
                )
        );
        PeticionamentoPericiaEvidenceIntelligenceService.PericiaEvidenceReport periciaEvidence = periciaEvidenceIntelligenceService.analyze(
                new PeticionamentoPericiaEvidenceIntelligenceService.ResolveRequest(
                        inlineMedia,
                        provasDocumentais,
                        safe.sigiloSensivel() || safe.tecnicoPericial()
                )
        );
        PeticionamentoMediaPublicationGateService.PublicationGateReport publication = mediaPublicationGateService.resolve(
                new PeticionamentoMediaPublicationGateService.ResolveRequest(
                        inlineMedia,
                        composition,
                        mediaSecurity,
                        mediaStorage,
                        periciaEvidence,
                        safe.preparingProtocolPackage()
                )
        );
        Map<String, Object> uploadGovernance = uploadCapacityGovernanceService.governanceSummary();
        Map<String, Object> institutionalBranding = institutionalBrandingResolverService.resolveProfile(
                new InstitutionalBrandingResolverService.ResolveRequest(
                        safe.actorLane(),
                        safe.pieceKind(),
                        safe.tipoUsuario(),
                        requestMap
                )
        );
        Map<String, Object> brandingGovernance = institutionalBrandingPolicyService.governanceSummary();
        Map<String, Object> pieceVisualIdentity = institutionalPieceVisualComposerService.compose(
                new InstitutionalPieceVisualComposerService.ResolveRequest(
                        resolvePieceLabel(safe),
                        institutionalBranding
                )
        );

        ArrayList<String> blockers = new ArrayList<>();
        blockers.addAll(rewriteMessages(composition.blockers(), safe));
        blockers.addAll(rewriteMessages(mediaSecurity.blockers(), safe));
        ArrayList<String> alerts = new ArrayList<>();
        alerts.addAll(rewriteMessages(composition.alerts(), safe));
        alerts.addAll(rewriteMessages(mediaSecurity.alerts(), safe));
        alerts.addAll(rewriteMessages(periciaEvidence.alerts(), safe));
        if (publication.blocking()) {
            alerts.add(resolvePieceLabel(safe) + " multimídia retida por gate de publicação controlada.");
        }

        LinkedHashMap<String, Object> workspace = new LinkedHashMap<>();
        workspace.put("actorLane", safe.actorLane());
        workspace.put("pieceKind", safe.pieceKind());
        workspace.put("pieceLabel", resolvePieceLabel(safe));
        workspace.put("technicalTrack", safe.tecnicoPericial());
        workspace.put("inlineNarrativeAllowedTypes", List.of("IMAGEM", "AUDIO", "VIDEO"));
        workspace.put("inlineDocumentForbidden", true);
        workspace.put("postPieceAttachmentBlock", composition.workspace().getOrDefault("sections", Map.of()));
        workspace.put("multimediaComposition", composition.workspace());
        workspace.put("mediaSecurityStatus", mediaSecurity.workspace());
        workspace.put("threatSentinel", threatSentinel.workspace());
        workspace.put("mediaStorageShield", mediaStorage.workspace());
        workspace.put("periciaEvidence", periciaEvidence.workspace());
        workspace.put("mediaPublicationStatus", publication.workspace());
        workspace.put("institutionalBranding", institutionalBranding);
        workspace.put("pieceVisualIdentity", pieceVisualIdentity);
        workspace.put("brandingGovernance", brandingGovernance);
        workspace.put("uploadGovernance", uploadGovernance);
        workspace.put("capabilities", capabilitiesFor(safe));
        workspace.put("nextAction", resolveNextAction(blockers, publication, safe));
        workspace.put("blockers", List.copyOf(blockers));
        workspace.put("alerts", List.copyOf(alerts));

        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("multimediaEnabled", !inlineMedia.isEmpty() || !provasDocumentais.isEmpty() || !documentosPessoais.isEmpty() || !documentosRepresentacao.isEmpty() || !documentosAnexados.isEmpty());
        out.put("institutionalWorkspace", Map.copyOf(workspace));
        out.put("multimediaComposition", composition.protocolSection());
        out.put("mediaSecurityStatus", mediaSecurity.protocolSection());
        out.put("threatSentinel", threatSentinel.workspace());
        out.put("mediaStorageShield", mediaStorage.protocolSection());
        out.put("periciaEvidence", periciaEvidence.protocolSection());
        out.put("mediaPublicationStatus", publication.protocolSection());
        out.put("institutionalBranding", institutionalBranding);
        out.put("pieceVisualIdentity", pieceVisualIdentity);
        out.put("brandingGovernance", brandingGovernance);
        out.put("uploadGovernance", uploadGovernance);
        out.put("blockers", List.copyOf(blockers));
        out.put("alerts", List.copyOf(alerts));
        out.put("nextAction", workspace.get("nextAction"));
        out.put("pieceProfile", resolveProfile(blockers, publication, safe, inlineMedia));
        return Collections.unmodifiableMap(out);
    }

    private Map<String, Object> normalizeRequest(Object request) {
        if (request == null) {
            return Map.of();
        }
        Object raw = objectMapper.convertValue(request, Object.class);
        if (raw instanceof Map<?, ?> map) {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            map.forEach((key, value) -> out.put(String.valueOf(key), value));
            return Collections.unmodifiableMap(out);
        }
        return Map.of("value", raw);
    }

    private List<PeticionamentoMediaBlocoRequest> resolveInlineMedia(Map<String, Object> requestMap) {
        for (String key : List.of("midiaInline", "inlineMediaBlocks", "inlineMedia", "blocosMidia")) {
            Object raw = requestMap.get(key);
            if (!(raw instanceof Iterable<?> iterable)) {
                continue;
            }
            ArrayList<PeticionamentoMediaBlocoRequest> out = new ArrayList<>();
            for (Object item : iterable) {
                if (item == null) {
                    continue;
                }
                try {
                    PeticionamentoMediaBlocoRequest converted = objectMapper.convertValue(item, PeticionamentoMediaBlocoRequest.class);
                    if (converted != null) {
                        out.add(converted);
                    }
                } catch (IllegalArgumentException ignored) {
                }
            }
            if (!out.isEmpty()) {
                return List.copyOf(out);
            }
        }
        return List.of();
    }

    private List<String> resolveStrings(Map<String, Object> requestMap, List<String> keys) {
        ArrayList<String> out = new ArrayList<>();
        for (String key : keys) {
            Object raw = requestMap.get(key);
            if (raw instanceof Iterable<?> iterable) {
                for (Object item : iterable) {
                    String normalized = normalizeString(item);
                    if (normalized != null && !out.contains(normalized)) {
                        out.add(normalized);
                    }
                }
            }
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private static String normalizeString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private static List<String> rewriteMessages(List<String> messages, ResolveRequest request) {
        if (messages == null || messages.isEmpty()) {
            return List.of();
        }
        String lower = resolvePieceLabel(request).toLowerCase(Locale.ROOT);
        String upper = resolvePieceLabel(request);
        ArrayList<String> out = new ArrayList<>();
        for (String message : messages) {
            if (message == null || message.isBlank()) {
                continue;
            }
            String rewritten = message
                    .replace("petição", lower)
                    .replace("Petição", upper)
                    .replace("pós-petição", "pós-peça");
            out.add(rewritten);
        }
        return out.isEmpty() ? List.of() : List.copyOf(out);
    }

    private static String resolvePieceLabel(ResolveRequest request) {
        return switch (Objects.requireNonNullElse(request.pieceKind(), "PETICAO_INSTITUCIONAL")) {
            case "MANIFESTACAO_MINISTERIAL" -> "Manifestação ministerial";
            case "PARECER_MINISTERIAL" -> "Parecer ministerial";
            case "CONTESTACAO_PROCURADORIA" -> "Contestação da procuradoria";
            case "PARECER_PROCURADORIA" -> "Parecer da procuradoria";
            case "LAUDO_PERICIAL" -> "Laudo pericial";
            case "RESPOSTA_A_QUESITOS" -> "Resposta a quesitos";
            case "HONORARIOS_PERICIAIS" -> "Solicitação de honorários periciais";
            case "CERTIDAO_OFICIAL_JUSTICA" -> "Certidão do oficial de justiça";
            case "AVALIACAO_OFICIAL_JUSTICA" -> "Avaliação do oficial de justiça";
            case "OFICIO_OFICIAL_JUSTICA" -> "Ofício do oficial de justiça";
            case "RESPOSTA_OFICIO_OFICIAL_JUSTICA" -> "Resposta a ofício do oficial de justiça";
            case "PARECER_PSICOSSOCIAL" -> "Parecer psicossocial";
            case "RELATORIO_PSICOSSOCIAL" -> "Relatório psicossocial";
            case "RELATORIO_INQUERITO" -> "Relatório de inquérito";
            case "REPRESENTACAO_POLICIAL" -> "Representação policial";
            case "CERTIDAO_CARTORIO_POLICIAL" -> "Certidão cartorária policial";
            default -> "Petição institucional";
        };
    }

    private static List<String> capabilitiesFor(ResolveRequest request) {
        ArrayList<String> out = new ArrayList<>();
        out.add("NARRATIVA_MULTIMIDIA_INLINE");
        out.add("ANEXOS_POS_PECA_SEPARADOS");
        out.add("TRIPLO_ESCUDO_DE_VALIDACAO");
        out.add("PUBLICACAO_CONTROLADA_DERIVADOS");
        if (request.tecnicoPericial()) {
            out.add("TRILHA_PERICIAL_MULTIMIDIA");
        }
        if ("MINISTERIO_PUBLICO".equals(request.actorLane())) {
            out.add("MANIFESTACAO_MINISTERIAL_MULTIMIDIA");
            if ("PARECER_MINISTERIAL".equals(request.pieceKind())) {
                out.add("PARECER_MINISTERIAL_MULTIMIDIA");
            }
        }
        if ("DEFENSORIA".equals(request.actorLane())) {
            out.add("PETICIONAMENTO_DEFENSORIAL_MULTIMIDIA");
        }
        if ("PROCURADORIA".equals(request.actorLane())) {
            out.add("ATUACAO_PROCURADORIA_MULTIMIDIA");
            if ("PARECER_PROCURADORIA".equals(request.pieceKind())) {
                out.add("PARECER_PROCURADORIA_MULTIMIDIA");
            }
            if ("CONTESTACAO_PROCURADORIA".equals(request.pieceKind())) {
                out.add("CONTESTACAO_PROCURADORIA_MULTIMIDIA");
            }
        }
        if ("PERICIA".equals(request.actorLane())) {
            out.add("LAUDO_TECNICO_MULTIMIDIA");
        }
        if ("OFICIAL_JUSTICA".equals(request.actorLane())) {
            out.add("CERTIDAO_MULTIMIDIA_OFICIAL");
            out.add("CUMPRIMENTO_TELEMATIZADO_GOVERNADO");
            if ("OFICIO_OFICIAL_JUSTICA".equals(request.pieceKind())) {
                out.add("OFICIO_MULTIMIDIA_OFICIAL");
                out.add("ENCAMINHAMENTO_CARTORARIO_POS_OFICIO");
                out.add("CATALOGO_TIPOS_OFICIO_GOVERNADO");
                out.add("DESTINATARIO_INSTITUCIONAL_ESTRUTURADO");
                out.add("TRACEABLE_DELIVERY_OFICIO");
            }
            if ("RESPOSTA_OFICIO_OFICIAL_JUSTICA".equals(request.pieceKind())) {
                out.add("RESPOSTA_OFICIO_MULTIMIDIA_OFICIAL");
                out.add("RETORNO_CARTORARIO_POS_RESPOSTA");
                out.add("MINUTA_GOVERNADA_RESPOSTA_OFICIO");
                out.add("TRACEABLE_DELIVERY_OFICIO");
            }
        }
        if ("PSICOSSOCIAL".equals(request.actorLane())) {
            out.add("PARECER_PSICOSSOCIAL_MULTIMIDIA");
            out.add("RELATORIO_PSICOSSOCIAL_MULTIMIDIA");
        }
        if ("POLICIA_CIVIL".equals(request.actorLane()) || "POLICIA_FEDERAL".equals(request.actorLane())) {
            out.add("INQUERITO_MULTIMIDIA_ESTREITO");
            out.add("NARRATIVA_INVESTIGATIVA_MULTIMIDIA");
            out.add("EVIDENCIA_AUDIO_VIDEO_IMAGEM_INLINE");
            if ("REPRESENTACAO_POLICIAL".equals(request.pieceKind())) {
                out.add("REPRESENTACAO_POLICIAL_MULTIMIDIA");
            }
        }
        return List.copyOf(out);
    }

    private static String resolveNextAction(List<String> blockers,
                                            PeticionamentoMediaPublicationGateService.PublicationGateReport publication,
                                            ResolveRequest request) {
        if (blockers != null && !blockers.isEmpty()) {
            return "REORGANIZAR_ANEXOS_E_MIDIA_DA_PECA";
        }
        if (publication != null && publication.blocking()) {
            return "REGULARIZAR_PUBLICACAO_CONTROLADA";
        }
        if (publication != null && publication.pendingPublication()) {
            return request.tecnicoPericial() ? "ACOMPANHAR_QUARENTENA_E_TRILHA_PERICIAL" : "ACOMPANHAR_QUARENTENA_E_CANONICALIZACAO";
        }
        if ("POLICIA_CIVIL".equals(request.actorLane()) || "POLICIA_FEDERAL".equals(request.actorLane())) {
            return "SUBMETER_PECA_INVESTIGATIVA";
        }
        if ("OFICIAL_JUSTICA".equals(request.actorLane())) {
            return switch (request.pieceKind()) {
                case "OFICIO_OFICIAL_JUSTICA" -> "SUBMETER_OFICIO_DO_OFICIAL";
                case "RESPOSTA_OFICIO_OFICIAL_JUSTICA" -> "SUBMETER_RESPOSTA_A_OFICIO_DO_OFICIAL";
                default -> "SUBMETER_CERTIDAO_OU_AVALIACAO_DO_OFICIAL";
            };
        }
        return request.tecnicoPericial() ? "SUBMETER_LAUDO_OU_RESPOSTA_TECNICA" : "SUBMETER_PECA_INSTITUCIONAL";
    }

    private static String resolveProfile(List<String> blockers,
                                         PeticionamentoMediaPublicationGateService.PublicationGateReport publication,
                                         ResolveRequest request,
                                         List<PeticionamentoMediaBlocoRequest> inlineMedia) {
        if (blockers != null && !blockers.isEmpty()) {
            return "PECA_MULTIMIDIA_BLOQUEADA";
        }
        if (publication != null && publication.pendingPublication()) {
            return request.tecnicoPericial() ? "TRILHA_TECNICA_EM_PROCESSAMENTO" : "PECA_MULTIMIDIA_EM_PROCESSAMENTO";
        }
        if (inlineMedia == null || inlineMedia.isEmpty()) {
            if ("POLICIA_CIVIL".equals(request.actorLane()) || "POLICIA_FEDERAL".equals(request.actorLane())) {
                return "PECA_INVESTIGATIVA_DOCUMENTAL";
            }
            if ("OFICIAL_JUSTICA".equals(request.actorLane())) {
                return switch (request.pieceKind()) {
                    case "OFICIO_OFICIAL_JUSTICA" -> "OFICIO_DOCUMENTAL_OFICIAL";
                    case "RESPOSTA_OFICIO_OFICIAL_JUSTICA" -> "RESPOSTA_OFICIO_DOCUMENTAL_OFICIAL";
                    case "AVALIACAO_OFICIAL_JUSTICA" -> "AVALIACAO_DOCUMENTAL_OFICIAL";
                    default -> "CERTIDAO_DOCUMENTAL_OFICIAL";
                };
            }
            return request.tecnicoPericial() ? "PECA_TECNICA_DOCUMENTAL" : "PECA_INSTITUCIONAL_DOCUMENTAL";
        }
        if ("POLICIA_CIVIL".equals(request.actorLane()) || "POLICIA_FEDERAL".equals(request.actorLane())) {
            return "PECA_INVESTIGATIVA_MULTIMIDIA";
        }
        if ("OFICIAL_JUSTICA".equals(request.actorLane())) {
            return switch (request.pieceKind()) {
                case "OFICIO_OFICIAL_JUSTICA" -> "OFICIO_MULTIMIDIA_OFICIAL";
                case "RESPOSTA_OFICIO_OFICIAL_JUSTICA" -> "RESPOSTA_OFICIO_MULTIMIDIA_OFICIAL";
                case "AVALIACAO_OFICIAL_JUSTICA" -> "AVALIACAO_MULTIMIDIA_OFICIAL";
                default -> "CERTIDAO_MULTIMIDIA_OFICIAL";
            };
        }
        return request.tecnicoPericial() ? "PECA_TECNICA_MULTIMIDIA" : "PECA_INSTITUCIONAL_MULTIMIDIA";
    }

    private static String sessionKey(ResolveRequest request) {
        return request.actorLane().toLowerCase(Locale.ROOT) + ":" + request.pieceKind().toLowerCase(Locale.ROOT) + ":" + Objects.toString(request.processoId(), "sem-processo");
    }

    public record ResolveRequest(String actorLane,
                                 String pieceKind,
                                 Long processoId,
                                 TipoUsuario tipoUsuario,
                                 Object rawRequest,
                                 boolean preparingProtocolPackage,
                                 boolean sigiloSensivel,
                                 boolean tecnicoPericial) {
        public ResolveRequest {
            actorLane = normalize(actorLane, "INSTITUCIONAL");
            pieceKind = normalize(pieceKind, "PETICAO_INSTITUCIONAL");
        }

        public static ResolveRequest empty() {
            return new ResolveRequest("INSTITUCIONAL", "PETICAO_INSTITUCIONAL", null, null, null, false, false, false);
        }

        private static String normalize(String value, String fallback) {
            String text = value == null ? null : value.trim();
            return text == null || text.isEmpty() ? fallback : text;
        }
    }
}
