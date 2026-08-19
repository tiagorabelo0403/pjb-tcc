package com.tcc.pjb.backend.core.procedural;

import com.tcc.pjb.backend.integration.judicial.JudicialConnectorRegistry;
import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import com.tcc.pjb.backend.integration.judicial.JudicialProcessConnector;
import com.tcc.pjb.backend.integration.judicial.JudicialSubmissionCapability;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.integration.judicial.ProtocolSubmissionRequest;
import com.tcc.pjb.backend.integration.judicial.ProtocolSubmissionResult;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class ProceduralSubmissionBlueprintService {

    private final NationalProceduralRoutingService nationalProceduralRoutingService;
    private final JudicialConnectorRegistry judicialConnectorRegistry;
    private final ProcessoRepository processoRepository;

    public ProceduralSubmissionBlueprintService(NationalProceduralRoutingService nationalProceduralRoutingService,
                                                JudicialConnectorRegistry judicialConnectorRegistry,
                                                ProcessoRepository processoRepository) {
        this.nationalProceduralRoutingService = Objects.requireNonNull(nationalProceduralRoutingService);
        this.judicialConnectorRegistry = Objects.requireNonNull(judicialConnectorRegistry);
        this.processoRepository = Objects.requireNonNull(processoRepository);
    }

    public ProceduralSubmissionBlueprintReport analyzeProcess(Processo processo) {
        return analyzeProcess(processo, null);
    }

    public ProceduralSubmissionBlueprintReport analyzeProcess(Processo processo,
                                                              ProceduralRoutingReport routing) {
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        if (processo != null) {
            payload.put("processoId", processo.getId());
            payload.put("numeroUnificado", processo.getNumeroUnificado());
            payload.put("numeroProcesso", processo.getNumeroProcesso());
            payload.put("classe", processo.getClasseProcessual());
            payload.put("classeProcessual", processo.getClasseProcessual());
            payload.put("assunto", processo.getAssunto());
            payload.put("objetoProcessual", processo.getObjetoProcessual());
            payload.put("pedidoPrincipal", processo.getPedidoPrincipal());
            payload.put("pedidos", processo.getPedidosConsolidados());
            payload.put("provas", processo.getMaterialProbatorioResumo());
            payload.put("ramoDireito", processo.getRamoDireito() != null ? processo.getRamoDireito().name() : null);
            payload.put("rito", processo.getRito() != null ? processo.getRito().name() : null);
            payload.put("tipoJustica", processo.getTipoJustica() != null ? processo.getTipoJustica().name() : null);
            payload.put("valorCausa", processo.getValorCausa());
            payload.put("parteAutoraNome", processo.getParteAutoraNome());
            payload.put("parteAutoraCpf", processo.getParteAutoraCpf());
            payload.put("parteReuNome", processo.getParteReuNome());
            payload.put("parteReuCpf", processo.getParteReuCpf());
            if (processo.getJurisdicao() != null) {
                payload.put("tribunalCodigo", processo.getJurisdicao().getCodigo());
                payload.put("foro", processo.getJurisdicao().getNome());
                payload.put("comarcaAutor", processo.getJurisdicao().getCidade());
                payload.put("ufAutor", processo.getJurisdicao().getUf());
            }
        }
        return analyzeContext(payload, routing, processo != null ? processo.getId() : null);
    }

    public ProceduralSubmissionBlueprintReport analyzeContext(Map<String, Object> payload,
                                                              Long currentProcessId) {
        return analyzeContext(payload, null, currentProcessId);
    }

    public ProceduralSubmissionBlueprintReport analyzeContext(Map<String, Object> payload,
                                                              ProceduralRoutingReport routing,
                                                              Long currentProcessId) {
        LinkedHashMap<String, Object> safePayload = new LinkedHashMap<>();
        if (payload != null) {
            safePayload.putAll(payload);
        }
        ProceduralRoutingReport resolvedRouting = routing != null ? routing : nationalProceduralRoutingService.analyzeContext(safePayload);
        LocalCorrelation localCorrelation = resolveLocalCorrelation(safePayload, currentProcessId, resolvedRouting);
        JudicialSystem system = resolveJudicialSystem(resolvedRouting);
        JudicialProcessConnector connector = judicialConnectorRegistry.find(system).orElseGet(() -> judicialConnectorRegistry.get(JudicialSystem.OUTRO));
        JudicialSubmissionCapability capability = connector.capability();
        String tribunalCodigo = firstNonBlank(
                resolvedRouting != null && resolvedRouting.forumAllocation() != null ? resolvedRouting.forumAllocation().tribunalCodigo() : null,
                resolvedRouting != null ? resolvedRouting.tribunalCodigo() : null,
                text(safePayload.get("tribunalCodigo"))
        );
        String tribunalNome = firstNonBlank(
                resolvedRouting != null && resolvedRouting.forumAllocation() != null ? resolvedRouting.forumAllocation().tribunalNome() : null,
                resolvedRouting != null ? resolvedRouting.tribunalNome() : null,
                tribunalCodigo
        );
        String unidadeCodigo = firstNonBlank(
                resolvedRouting != null && resolvedRouting.forumAllocation() != null ? resolvedRouting.forumAllocation().unidadeJudiciariaCodigo() : null,
                resolvedRouting != null ? resolvedRouting.varaSugerida() : null,
                text(safePayload.get("varaPretendida"))
        );
        String unidadeNome = firstNonBlank(
                resolvedRouting != null && resolvedRouting.forumAllocation() != null ? resolvedRouting.forumAllocation().varaSugerida() : null,
                unidadeCodigo
        );
        String classeTpuCodigo = firstNonBlank(
                resolvedRouting != null && resolvedRouting.forumAllocation() != null ? resolvedRouting.forumAllocation().classeTpuCodigo() : null,
                text(safePayload.get("classeTpu")),
                text(safePayload.get("classe"))
        );
        String classeTpuNome = firstNonBlank(
                resolvedRouting != null && resolvedRouting.forumAllocation() != null ? resolvedRouting.forumAllocation().classeTpuNome() : null,
                text(safePayload.get("assunto")),
                text(safePayload.get("objetoProcessual"))
        );
        String rito = firstNonBlank(resolvedRouting != null ? resolvedRouting.ritoSugerido() : null, text(safePayload.get("rito")));
        String requestId = buildRequestId(currentProcessId, tribunalCodigo, classeTpuCodigo, rito, safePayload);
        Map<String, Object> requestPreview = buildRequestPreview(requestId, safePayload, resolvedRouting, system, tribunalCodigo, tribunalNome, unidadeCodigo, unidadeNome, classeTpuCodigo, classeTpuNome, rito);
        ProtocolSubmissionRequest request = new ProtocolSubmissionRequest(
                requestId,
                firstNonBlank(text(safePayload.get("numeroUnificado")), text(safePayload.get("numeroProcesso"))),
                firstNonBlank(text(safePayload.get("pedidoPrincipal")), text(safePayload.get("assunto")), text(safePayload.get("objetoProcessual")), classeTpuNome),
                tribunalCodigo,
                unidadeCodigo,
                unidadeNome,
                rito,
                classeTpuCodigo,
                firstNonBlank(text(safePayload.get("ramoDireito")), resolvedRouting != null ? resolvedRouting.actionFamily() : null),
                requestPreview.toString(),
                integrityHash(requestPreview),
                null,
                null,
                true,
                requestPreview
        );
        ProtocolSubmissionResult dryRun = connector.submit(request);

        LinkedHashSet<String> blockingIssues = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        if (resolvedRouting != null) {
            addAll(blockingIssues, resolvedRouting.blockingIssues());
            addAll(reviewChecklist, resolvedRouting.reviewChecklist());
            addAll(warnings, resolvedRouting.alerts());
            if (resolvedRouting.forumAllocation() != null) {
                addAll(blockingIssues, resolvedRouting.forumAllocation().incompatibilities());
                addAll(reviewChecklist, resolvedRouting.forumAllocation().reviewChecklist());
                addAll(warnings, resolvedRouting.forumAllocation().warnings());
            }
        }
        addAll(blockingIssues, localCorrelation.blockingIssues());
        addAll(reviewChecklist, localCorrelation.reviewChecklist());
        addAll(warnings, localCorrelation.warnings());
        if (dryRun != null && !dryRun.accepted() && capability.supportsDryRun()) {
            warnings.add(firstNonBlank(dryRun.message(), "Dry-run de protocolo não aceito pelo conector judicial selecionado."));
            reviewChecklist.add("Revalidar metadados do protocolo e disponibilidade operacional do conector antes do envio real.");
            if (capability.operational()) {
                blockingIssues.add("Dry-run do conector operacional foi rejeitado e exige saneamento antes do protocolo real.");
            }
        }
        if (capability.requiresCertificate()) {
            reviewChecklist.add("Garantir certificado compatível com o sistema judicial selecionado.");
        }
        if (capability.requiresStepUpGovBr()) {
            reviewChecklist.add("Executar step-up Gov.br antes do protocolo eletrônico final.");
        }
        if (!capability.operational()) {
            warnings.add("Conector judicial ainda sem homologação operacional completa para protocolo real neste ambiente.");
        }

        boolean routingReady = resolvedRouting != null
                && resolvedRouting.confidence() >= 0.68d
                && resolvedRouting.blockingIssues().isEmpty()
                && (resolvedRouting.forumAllocation() == null || resolvedRouting.forumAllocation().preProtocoloApto());
        boolean dryRunAccepted = dryRun != null && dryRun.accepted();
        boolean readyForAssisted = routingReady && blockingIssues.isEmpty() && (!capability.supportsDryRun() || dryRunAccepted);
        boolean readyForRealConnector = readyForAssisted && capability.operational() && capability.supportsProtocol() && dryRunAccepted;
        String blueprintStatus = resolveBlueprintStatus(readyForAssisted, readyForRealConnector, blockingIssues, capability, dryRun, localCorrelation);

        LinkedHashMap<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("routing", resolvedRouting != null ? resolvedRouting.toMap() : Map.of());
        metadata.put("localCorrelation", localCorrelation.toMap());
        metadata.put("connectorCapability", JudicialMapSupport.compact(
                "system", capability.system() != null ? capability.system().name() : null,
                "enabled", capability.enabled(),
                "supportsProtocol", capability.supportsProtocol(),
                "supportsDryRun", capability.supportsDryRun(),
                "operational", capability.operational(),
                "requiresGovBrStepUp", capability.requiresStepUpGovBr(),
                "requiresCertificate", capability.requiresCertificate(),
                "baseUrl", capability.baseUrl()
        ));
        if (dryRun != null) {
            metadata.put("dryRun", JudicialMapSupport.compact(
                    "accepted", dryRun.accepted(),
                    "status", dryRun.status(),
                    "message", dryRun.message(),
                    "protocolReference", dryRun.protocolReference(),
                    "processedAt", dryRun.processedAt() != null ? dryRun.processedAt().toString() : null,
                    "raw", dryRun.raw()
            ));
        }
        metadata.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);

        return new ProceduralSubmissionBlueprintReport(
                Instant.now(),
                requestId,
                blueprintStatus,
                readyForAssisted,
                readyForRealConnector,
                dryRunAccepted,
                system,
                tribunalCodigo,
                tribunalNome,
                classeTpuCodigo,
                classeTpuNome,
                unidadeCodigo,
                unidadeNome,
                rito,
                resolvedRouting != null ? resolvedRouting.actionNature() : null,
                resolvedRouting != null && resolvedRouting.forumAllocation() != null ? resolvedRouting.forumAllocation().competenciaTerritorialModo() : null,
                resolvedRouting != null && resolvedRouting.forumAllocation() != null ? resolvedRouting.forumAllocation().preventionMode() : null,
                resolvedRouting != null && resolvedRouting.forumAllocation() != null ? resolvedRouting.forumAllocation().linkageMode() : null,
                localCorrelation.mode(),
                List.copyOf(localCorrelation.relatedProcessNumbers()),
                capability.operational(),
                capability.requiresStepUpGovBr(),
                capability.requiresCertificate(),
                dryRun != null ? dryRun.status() : null,
                dryRun != null ? dryRun.protocolReference() : null,
                List.copyOf(blockingIssues),
                List.copyOf(reviewChecklist),
                List.copyOf(warnings),
                Map.copyOf(requestPreview),
                Collections.unmodifiableMap(metadata)
        );
    }

    private String resolveBlueprintStatus(boolean readyForAssisted,
                                         boolean readyForRealConnector,
                                         Set<String> blockingIssues,
                                         JudicialSubmissionCapability capability,
                                         ProtocolSubmissionResult dryRun,
                                         LocalCorrelation localCorrelation) {
        if (!blockingIssues.isEmpty()) {
            return "BLOCKED";
        }
        if (localCorrelation.duplicateNumberDetected()) {
            return "DUPLICATE_GUARD_REVIEW";
        }
        if (readyForRealConnector) {
            return "READY_FOR_REAL_CONNECTOR_PROTOCOL";
        }
        if (readyForAssisted) {
            return capability.operational() ? "READY_FOR_ASSISTED_PROTOCOL" : "READY_WITH_CONNECTOR_GAP";
        }
        if (dryRun != null && !dryRun.accepted()) {
            return "DRY_RUN_REVIEW_REQUIRED";
        }
        return "STRUCTURAL_REVIEW_REQUIRED";
    }

    private LocalCorrelation resolveLocalCorrelation(Map<String, Object> payload,
                                                     Long currentProcessId,
                                                     ProceduralRoutingReport routing) {
        LinkedHashSet<Processo> candidates = new LinkedHashSet<>();
        String autorCpf = normalizeCpf(text(payload.get("parteAutoraCpf")));
        String reuCpf = normalizeCpf(text(payload.get("parteReuCpf")));
        if (autorCpf != null) {
            candidates.addAll(processoRepository.findAllByPartesCpf(autorCpf));
        }
        if (reuCpf != null && !Objects.equals(reuCpf, autorCpf)) {
            candidates.addAll(processoRepository.findAllByPartesCpf(reuCpf));
        }
        candidates.removeIf(p -> p == null || (currentProcessId != null && Objects.equals(currentProcessId, p.getId())));

        LinkedHashSet<String> relatedNumbers = new LinkedHashSet<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> reviewChecklist = new LinkedHashSet<>();
        LinkedHashSet<String> blockingIssues = new LinkedHashSet<>();
        boolean duplicateNumberDetected = false;
        boolean sameTrackDetected = false;
        String currentNumber = firstNonBlank(text(payload.get("numeroUnificado")), text(payload.get("numeroProcesso")));
        String currentTribunal = firstNonBlank(
                routing != null && routing.forumAllocation() != null ? routing.forumAllocation().tribunalCodigo() : null,
                routing != null ? routing.tribunalCodigo() : null,
                text(payload.get("tribunalCodigo"))
        );
        String currentClasse = firstNonBlank(
                routing != null && routing.forumAllocation() != null ? routing.forumAllocation().classeTpuCodigo() : null,
                text(payload.get("classe")),
                text(payload.get("classeProcessual"))
        );
        for (Processo candidate : candidates) {
            relatedNumbers.add(firstNonBlank(candidate.getNumeroUnificado(), candidate.getNumeroProcesso(), candidate.getClasseProcessual()));
            if (currentNumber != null && currentNumber.equalsIgnoreCase(firstNonBlank(candidate.getNumeroUnificado(), candidate.getNumeroProcesso()))) {
                duplicateNumberDetected = true;
            }
            String candidateTribunal = candidate.getJurisdicao() != null ? candidate.getJurisdicao().getCodigo() : null;
            if (currentTribunal != null && currentTribunal.equalsIgnoreCase(candidateTribunal)
                    && currentClasse != null && currentClasse.equalsIgnoreCase(candidate.getClasseProcessual())) {
                sameTrackDetected = true;
            }
        }
        if (duplicateNumberDetected) {
            blockingIssues.add("Já existe processo local com o mesmo número informado; revisar prevenção, dependência e identidade do protocolo.");
        }
        if (sameTrackDetected) {
            warnings.add("Há sinais locais de prevenção ou dependência na mesma trilha tribunal/classe.");
            reviewChecklist.add("Conferir prevenção, conexão, continência e eventual distribuição por dependência antes do protocolo real.");
        }
        String mode;
        if (duplicateNumberDetected) {
            mode = "LOCAL_DUPLICATE_NUMBER";
        } else if (sameTrackDetected) {
            mode = "LOCAL_PREVENTION_SIGNAL";
        } else if (!relatedNumbers.isEmpty()) {
            mode = "LOCAL_CONNECTION_SIGNAL";
        } else {
            mode = "NO_LOCAL_SIGNAL";
        }
        return new LocalCorrelation(mode, relatedNumbers, warnings, reviewChecklist, blockingIssues, duplicateNumberDetected);
    }

    private Map<String, Object> buildRequestPreview(String requestId,
                                                    Map<String, Object> payload,
                                                    ProceduralRoutingReport routing,
                                                    JudicialSystem judicialSystem,
                                                    String tribunalCodigo,
                                                    String tribunalNome,
                                                    String unidadeCodigo,
                                                    String unidadeNome,
                                                    String classeTpuCodigo,
                                                    String classeTpuNome,
                                                    String rito) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        out.put("requestId", requestId);
        out.put("numeroUnificado", firstNonBlank(text(payload.get("numeroUnificado")), text(payload.get("numeroProcesso"))));
        out.put("title", firstNonBlank(text(payload.get("pedidoPrincipal")), text(payload.get("assunto")), text(payload.get("objetoProcessual")), classeTpuNome));
        out.put("judicialSystem", judicialSystem != null ? judicialSystem.name() : null);
        out.put("tribunalCodigo", tribunalCodigo);
        out.put("tribunalNome", tribunalNome);
        out.put("unidadeJudiciariaCodigo", unidadeCodigo);
        out.put("unidadeJudiciariaNome", unidadeNome);
        out.put("classeTpu", classeTpuCodigo);
        out.put("classeTpuNome", classeTpuNome);
        out.put("rito", rito);
        out.put("ramoDireito", firstNonBlank(text(payload.get("ramoDireito")), routing != null ? routing.actionFamily() : null));
        out.put("tipoJustica", routing != null ? routing.tipoJusticaSugerida() : text(payload.get("tipoJustica")));
        out.put("valorCausa", payload.get("valorCausa"));
        out.put("parteAutoraCpf", normalizeCpf(text(payload.get("parteAutoraCpf"))));
        out.put("parteReuCpf", normalizeCpf(text(payload.get("parteReuCpf"))));
        if (routing != null) {
            out.put("routing", routing.toMap());
        }
        out.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
        return Collections.unmodifiableMap(out);
    }

    private JudicialSystem resolveJudicialSystem(ProceduralRoutingReport routing) {
        String systemName = firstNonBlank(
                routing != null && routing.forumAllocation() != null ? routing.forumAllocation().connectorSystem() : null,
                routing != null ? routing.judicialSystem() : null,
                JudicialSystem.OUTRO.name()
        );
        try {
            return JudicialSystem.valueOf(systemName.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            return JudicialSystem.OUTRO;
        }
    }

    private String buildRequestId(Long currentProcessId,
                                  String tribunalCodigo,
                                  String classeTpuCodigo,
                                  String rito,
                                  Map<String, Object> payload) {
        String seed = firstNonBlank(
                currentProcessId != null ? "P" + currentProcessId : null,
                text(payload.get("numeroUnificado")),
                text(payload.get("numeroProcesso")),
                UUID.randomUUID().toString()
        );
        String hash = integrityHash(seed + "|" + String.valueOf(tribunalCodigo) + "|" + String.valueOf(classeTpuCodigo) + "|" + String.valueOf(rito) + "|" + String.valueOf(text(payload.get("pedidoPrincipal"))));
        return "SUB-" + hash.substring(0, 12).toUpperCase(Locale.ROOT);
    }

    private String integrityHash(Object value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] raw = digest.digest(String.valueOf(value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(raw);
        } catch (Exception ex) {
            return Integer.toHexString(String.valueOf(value).hashCode());
        }
    }

    private static String normalizeCpf(String cpf) {
        if (cpf == null) {
            return null;
        }
        String digits = cpf.replaceAll("\\D+", "").trim();
        return digits.isBlank() ? null : digits;
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String out = String.valueOf(value).trim();
        return out.isEmpty() ? null : out;
    }

    private static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private static void addAll(Set<String> target, java.util.Collection<String> values) {
        if (target == null || values == null) {
            return;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                target.add(value.trim());
            }
        }
    }

    private record LocalCorrelation(
            String mode,
            Set<String> relatedProcessNumbers,
            Set<String> warnings,
            Set<String> reviewChecklist,
            Set<String> blockingIssues,
            boolean duplicateNumberDetected
    ) {
        Map<String, Object> toMap() {
            LinkedHashMap<String, Object> out = new LinkedHashMap<>();
            out.put("mode", mode);
            out.put("relatedProcessNumbers", relatedProcessNumbers == null ? List.of() : List.copyOf(relatedProcessNumbers));
            out.put("warnings", warnings == null ? List.of() : List.copyOf(warnings));
            out.put("reviewChecklist", reviewChecklist == null ? List.of() : List.copyOf(reviewChecklist));
            out.put("blockingIssues", blockingIssues == null ? List.of() : List.copyOf(blockingIssues));
            out.put("duplicateNumberDetected", duplicateNumberDetected);
            out.entrySet().removeIf(e -> e.getKey() == null || e.getValue() == null);
            return Collections.unmodifiableMap(out);
        }
    }
}
