package com.tcc.pjb.backend.integration.judicial.routing;

import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix;
import com.tcc.pjb.backend.core.procedural.CanonicalSanityGate;
import com.tcc.pjb.backend.core.procedural.CanonicalSanityGate.GateResult;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver;
import com.tcc.pjb.backend.core.procedural.ProceduralCanonicalResolver.CanonicalContext;
import com.tcc.pjb.backend.core.competence.NationalCompetenceMatrix.RamoJusticaNacional;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorRegistry;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorHomologationReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorHomologationService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorOperationalProfileReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorOperationalProfileService;
import com.tcc.pjb.backend.integration.judicial.JudicialMapSupport;
import com.tcc.pjb.backend.integration.judicial.JudicialProcessConnector;
import com.tcc.pjb.backend.integration.judicial.JudicialSubmissionCapability;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.integration.judicial.ProtocolSubmissionRequest;
import com.tcc.pjb.backend.tribunal.perfil.PerfilInstanciaTribunalService;
import com.tcc.pjb.backend.tribunal.perfil.PerfilInstanciaTribunalService.ResumoPerfil;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class TribunalProtocolRoutingService {

    public record RoutingDecision(
            String tribunalCodigo,
            String tribunalNome,
            JudicialSystem judicialSystem,
            JudicialSubmissionCapability capability,
            String competenceHint,
            boolean stepUpRequired,
            boolean certificateRequired,
            List<String> warnings,
            Map<String, Object> metadata,
            Instant resolvedAt
    ) {
        public RoutingDecision() {
            this(null, null, null, null, null, false, false, List.of(), Map.of(), Instant.now());
        }
    }

    private static final Set<String> PREFERRED_EPROC = Set.of("TJRS", "TJSC", "TRF4");
    private static final Set<String> PREFERRED_ESAJ = Set.of("TJSP", "TJMS", "TJAC");
    private static final Set<String> PREFERRED_PROJUDI = Set.of("TJPR", "TJRN", "TJRO");
    private static final Set<String> PREFERRED_CRETA = Set.of("TRF5", "JFCE", "JFPE", "JFAL", "JFRN", "JFPB", "JFSE");

    private final JudicialConnectorRegistry connectorRegistry;
    private final PerfilInstanciaTribunalService perfilInstanciaTribunalService;
    private final ProceduralCanonicalResolver proceduralCanonicalResolver;
    private final CanonicalSanityGate canonicalSanityGate;
    private final JudicialConnectorHomologationService homologationService;
    private final JudicialConnectorOperationalProfileService operationalProfileService;

    public TribunalProtocolRoutingService(JudicialConnectorRegistry connectorRegistry,
                                          PerfilInstanciaTribunalService perfilInstanciaTribunalService,
                                          ProceduralCanonicalResolver proceduralCanonicalResolver,
                                          CanonicalSanityGate canonicalSanityGate,
                                          JudicialConnectorHomologationService homologationService,
                                          JudicialConnectorOperationalProfileService operationalProfileService) {
        this.connectorRegistry = Objects.requireNonNull(connectorRegistry);
        this.perfilInstanciaTribunalService = Objects.requireNonNull(perfilInstanciaTribunalService);
        this.proceduralCanonicalResolver = Objects.requireNonNull(proceduralCanonicalResolver);
        this.canonicalSanityGate = Objects.requireNonNull(canonicalSanityGate);
        this.homologationService = Objects.requireNonNull(homologationService);
        this.operationalProfileService = Objects.requireNonNull(operationalProfileService);
    }

    public RoutingDecision resolve(Map<String, Object> payload,
                                   Enum<?> rito,
                                   String ramoDireito,
                                   String competencia,
                                   boolean recurso) {
        return resolve(payload, rito != null ? rito.name() : null, ramoDireito, competencia, recurso);
    }

    public RoutingDecision resolve(Map<String, Object> payload,
                                   String ritoName,
                                   String ramoDireito,
                                   String competencia,
                                   boolean recurso) {
        Map<String, Object> safePayload = payload == null ? Map.of() : payload;
        String normalizedRito = normalizeRitoName(ritoName);
        CanonicalContext canonicalContext = proceduralCanonicalResolver.resolve(mergeCanonicalPayload(safePayload, normalizedRito, ramoDireito, competencia));
        GateResult gateResult = canonicalSanityGate.evaluate(canonicalContext);
        String effectiveRito = canonicalContext.rito() != null ? canonicalContext.rito().name() : normalizedRito;
        String effectiveRamo = firstNonBlank(canonicalContext.ramoDireito(), ramoDireito);
        String effectiveCompetencia = firstNonBlank(canonicalContext.ramoJusticaNacional(), competencia);
        String tribunalCodigo = firstNonBlank(
                asText(safePayload.get("tribunalCodigo")),
                asText(safePayload.get("tribunal_codigo")),
                asText(safePayload.get("tribunal")),
                asText(safePayload.get("siglaTribunal")),
                canonicalContext.tribunalCodigo(),
                inferTribunalCodigoFromMatrix(canonicalContext, safePayload),
                inferTribunalCodigo(effectiveCompetencia, safePayload)
        );
        tribunalCodigo = tribunalCodigo == null ? "PJB_PADRAO" : tribunalCodigo.trim().toUpperCase(Locale.ROOT);
        ResumoPerfil perfil = normalizePerfilResumo(tribunalCodigo, perfilInstanciaTribunalService.resumo(tribunalCodigo));
        List<JudicialSystem> preferred = preferredSystems(safePayload, tribunalCodigo, effectiveRito, effectiveRamo, effectiveCompetencia, recurso, canonicalContext);
        List<String> warnings = new ArrayList<>();
        appendGateWarnings(gateResult, warnings);
        JudicialProcessConnector selectedConnector = null;
        JudicialSubmissionCapability selectedCapability = null;
        for (JudicialSystem system : preferred) {
            Optional<JudicialProcessConnector> candidate = connectorRegistry.find(system);
            if (candidate.isEmpty()) {
                warnings.add("Conector não registrado para " + system.name() + '.');
                continue;
            }
            JudicialSubmissionCapability capability = candidate.get().capability();
            if (capability.enabled()) {
                selectedConnector = candidate.get();
                selectedCapability = capability;
                if (capability.operational()) {
                    break;
                }
            }
        }
        if (selectedConnector == null) {
            selectedConnector = connectorRegistry.get(JudicialSystem.OUTRO);
            selectedCapability = selectedConnector.capability();
            warnings.add("TRIBUNAL_NOT_HOMOLOGATED: nenhum conector judicial homologado para o tribunal e rito resolvidos.");
        } else if (!selectedCapability.operational()) {
            warnings.add("WORKFLOW_BLUEPRINT_INCOMPLETE: conector selecionado sem operação completa de protocolo.");
        }
        ProtocolSubmissionRequest homologationProbe = new ProtocolSubmissionRequest(
                "ROUTING-" + tribunalCodigo,
                null,
                canonicalContext.classeTpuNome(),
                tribunalCodigo,
                null,
                null,
                effectiveRito,
                canonicalContext.classeTpuCodigo(),
                effectiveRamo,
                "{}",
                null,
                null,
                null,
                false,
                safePayload
        );
        JudicialConnectorHomologationReport homologation = homologationService.analyze(selectedConnector.system(), selectedCapability, homologationProbe);
        JudicialConnectorOperationalProfileReport operationalProfile = operationalProfileService.analyze(selectedConnector.system(), selectedCapability, homologationProbe);
        warnings.addAll(homologation.warnings());
        warnings.addAll(homologation.blockers());
        warnings.addAll(operationalProfile.warnings());
        warnings.addAll(operationalProfile.blockers());
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("preferredSystems", preferred.stream().map(Enum::name).toList());
        metadata.put("tribunalPerfil", JudicialMapSupport.compact(
                "codigo", perfil.tribunalCodigo(),
                "nome", perfil.tribunalNome(),
                "sigla", perfil.tribunalSigla(),
                "uf", perfil.uf(),
                "ramo", perfil.ramo() != null ? perfil.ramo().name() : null,
                "grau", perfil.grau() != null ? perfil.grau().name() : null
        ));
        metadata.put("canonicalContext", canonicalContext.toMap());
        metadata.put("sanityGate", gateResult.toMap());
        metadata.put("sanityStatus", gateResult.overallStatus());
        metadata.put("connectorRegistered", selectedConnector.system().name());
        metadata.put("connectorOperational", selectedCapability.operational());
        metadata.put("connectorBaseUrl", selectedCapability.baseUrl());
        metadata.put("connectorHomologation", homologation.toMap());
        metadata.put("connectorOperationalProfile", operationalProfile.toMap());
        NationalCompetenceMatrix.porCodigo(tribunalCodigo).ifPresent(matrix -> metadata.put("matrix", matrix.toMap()));
        return new RoutingDecision(
                perfil.tribunalCodigo(),
                perfil.tribunalNome(),
                selectedConnector.system(),
                selectedCapability,
                competenceHint(effectiveCompetencia, perfil.tribunalCodigo()),
                selectedCapability.requiresStepUpGovBr(),
                selectedCapability.requiresCertificate(),
                List.copyOf(new LinkedHashSet<>(warnings)),
                JudicialMapSupport.copyNonNull(metadata),
                Instant.now()
        );
    }


    private ResumoPerfil normalizePerfilResumo(String tribunalCodigo, ResumoPerfil perfil) {
        if (perfil == null) {
            return new ResumoPerfil(
                    tribunalCodigo == null || tribunalCodigo.isBlank() ? "PJB_PADRAO" : tribunalCodigo,
                    tribunalCodigo == null || tribunalCodigo.isBlank() ? "Plataforma Judicial Brasileira" : tribunalCodigo,
                    tribunalCodigo == null || tribunalCodigo.isBlank() ? "PJB" : tribunalCodigo,
                    null,
                    null,
                    null,
                    0,
                    false,
                    tribunalCodigo,
                    "America/Sao_Paulo",
                    false,
                    false,
                    false,
                    20
            );
        }
        if (tribunalCodigo == null || tribunalCodigo.isBlank() || !"PJB_PADRAO".equalsIgnoreCase(perfil.tribunalCodigo())) {
            return perfil;
        }
        return NationalCompetenceMatrix.porCodigo(tribunalCodigo)
                .map(matrix -> new ResumoPerfil(
                        matrix.codigo(),
                        matrix.nome(),
                        matrix.codigo(),
                        matrix.uf(),
                        mapPerfilRamo(matrix.ramo()),
                        mapPerfilGrau(matrix.ramo()),
                        perfil.totalTermosPersonalizados(),
                        false,
                        perfil.perfilFallbackUsado(),
                        perfil.fusoHorario(),
                        perfil.habilitaVideoAudiencia(),
                        perfil.habilitaNotificacaoWhatsApp(),
                        perfil.usaLogoEmDocumentos(),
                        perfil.itensPorPaginaPadrao()
                ))
                .orElse(perfil);
    }

    private PerfilInstanciaTribunalService.RamoJustica mapPerfilRamo(RamoJusticaNacional ramo) {
        if (ramo == null) {
            return null;
        }
        return switch (ramo) {
            case ELEITORAL, ELEITORAL_SUPERIOR -> PerfilInstanciaTribunalService.RamoJustica.ELEITORAL;
            case TRABALHO, TRABALHO_SUPERIOR -> PerfilInstanciaTribunalService.RamoJustica.TRABALHO;
            case FEDERAL -> PerfilInstanciaTribunalService.RamoJustica.FEDERAL;
            case MILITAR_ESTADUAL -> PerfilInstanciaTribunalService.RamoJustica.MILITAR_ESTADUAL;
            case MILITAR_SUPERIOR -> PerfilInstanciaTribunalService.RamoJustica.MILITAR_UNIAO;
            case SUPERIOR, SUPERIOR_STF -> PerfilInstanciaTribunalService.RamoJustica.SUPERIOR;
            default -> PerfilInstanciaTribunalService.RamoJustica.ESTADUAL;
        };
    }

    private PerfilInstanciaTribunalService.GrauInstancia mapPerfilGrau(RamoJusticaNacional ramo) {
        if (ramo == null) {
            return null;
        }
        return switch (ramo) {
            case SUPERIOR, ELEITORAL_SUPERIOR, TRABALHO_SUPERIOR, MILITAR_SUPERIOR -> PerfilInstanciaTribunalService.GrauInstancia.TRIBUNAL_SUPERIOR;
            case SUPERIOR_STF -> PerfilInstanciaTribunalService.GrauInstancia.STF;
            default -> PerfilInstanciaTribunalService.GrauInstancia.SEGUNDO_GRAU;
        };
    }

    private void appendGateWarnings(GateResult gateResult, List<String> warnings) {
        if (gateResult == null || warnings == null) {
            return;
        }
        for (var issue : gateResult.issues()) {
            warnings.add(issue.status().name() + ": " + issue.message());
        }
    }

    private String inferTribunalCodigoFromMatrix(CanonicalContext canonicalContext,
                                                 Map<String, Object> payload) {
        String uf = normalizeUf(firstNonBlank(asText(payload.get("uf")), asText(payload.get("ufAutor")), asText(payload.get("estado"))));
        if (uf == null) {
            return null;
        }
        RamoJusticaNacional ramo = inferRamo(
                firstNonBlank(canonicalContext.ramoJusticaNacional(), asText(payload.get("competencia")), asText(payload.get("esfera")), asText(payload.get("tipoJustica"))),
                firstNonBlank(canonicalContext.ramoDireito(), asText(payload.get("ramoDireito")), asText(payload.get("ramo"))),
                firstNonBlank(asText(payload.get("rito")), asText(payload.get("ritoProcessual")), asText(payload.get("procedimento")), canonicalContext.rito() != null ? canonicalContext.rito().name() : null)
        );
        if (ramo == null) {
            return null;
        }
        return NationalCompetenceMatrix.resolver(uf, ramo).map(NationalCompetenceMatrix::codigo).orElse(null);
    }

    private RamoJusticaNacional inferRamo(String competencia, String ramoDireito, String rito) {
        String combined = (((competencia == null ? "" : competencia) + ' ' + (ramoDireito == null ? "" : ramoDireito)).trim()).toUpperCase(Locale.ROOT);
        if (combined.contains("ELEITORAL") || isEleitoralRito(rito)) {
            return RamoJusticaNacional.ELEITORAL;
        }
        if (combined.contains("TRABALH") || isTrabalhistaRito(rito)) {
            return RamoJusticaNacional.TRABALHO;
        }
        if (combined.contains("MILITAR_SUPERIOR") || combined.contains("STM")) {
            return RamoJusticaNacional.MILITAR_SUPERIOR;
        }
        if (combined.contains("MILITAR") || isMilitarRito(rito)) {
            return RamoJusticaNacional.MILITAR_ESTADUAL;
        }
        if (combined.contains("FEDERAL") || combined.contains("PREVIDENCI") || isPrevidenciarioRito(rito)) {
            return RamoJusticaNacional.FEDERAL;
        }
        if (combined.contains("SUPERIOR") || combined.contains("STJ") || combined.contains("STF") || combined.contains("TSE") || combined.contains("STM") || combined.contains("TST")) {
            return RamoJusticaNacional.SUPERIOR;
        }
        return RamoJusticaNacional.ESTADUAL;
    }

    private List<JudicialSystem> preferredSystems(Map<String, Object> payload,
                                                  String tribunalCodigo,
                                                  String rito,
                                                  String ramoDireito,
                                                  String competencia,
                                                  boolean recurso,
                                                  CanonicalContext canonicalContext) {
        LinkedHashSet<JudicialSystem> systems = new LinkedHashSet<>();
        JudicialSystem explicit = parseSystem(firstNonBlank(
                asText(payload.get("sistemaProtocolo")),
                asText(payload.get("sistema_protocolo")),
                asText(payload.get("judicialSystem"))
        ));
        if (explicit != null) {
            systems.add(explicit);
        }
        if (canonicalContext.judicialSystemPreferido() != null) {
            JudicialSystem canonicalPreferred = parseSystem(canonicalContext.judicialSystemPreferido());
            if (canonicalPreferred != null) {
                systems.add(canonicalPreferred);
            }
        }
        NationalCompetenceMatrix.porCodigo(tribunalCodigo).ifPresent(matrix -> {
            if (matrix.connectorPreferido() != null) {
                systems.add(matrix.connectorPreferido());
            }
            if (matrix.sistemaJudicialFallback() != null) {
                systems.add(matrix.sistemaJudicialFallback());
            }
        });
        String tribunal = tribunalCodigo == null ? "" : tribunalCodigo.toUpperCase(Locale.ROOT);
        if (rito != null) {
            if (isTrabalhistaRito(rito) || isEleitoralRito(rito)) {
                systems.add(JudicialSystem.PJE);
                systems.add(JudicialSystem.PDPJ);
            } else if (isMilitarRito(rito)) {
                systems.add(JudicialSystem.PJE);
                systems.add(JudicialSystem.MNI);
                systems.add(JudicialSystem.PDPJ);
                systems.add(JudicialSystem.OUTRO);
            } else if (isPrevidenciarioRito(rito)) {
                systems.add(JudicialSystem.EPROC);
                systems.add(JudicialSystem.CRETA);
                systems.add(JudicialSystem.PJE);
                systems.add(JudicialSystem.PDPJ);
            } else if (isFazendaRito(rito)) {
                systems.add(JudicialSystem.PJE);
                systems.add(JudicialSystem.ESAJ);
                systems.add(JudicialSystem.EPROC);
                systems.add(JudicialSystem.PDPJ);
            }
        }
        if (tribunal.startsWith("TRE") || tribunal.startsWith("TSE") || containsNormalized(competencia, "eleitoral")) {
            systems.add(JudicialSystem.PJE);
            systems.add(JudicialSystem.PDPJ);
        }
        if (tribunal.startsWith("TRT") || containsNormalized(ramoDireito, "trabalh")) {
            systems.add(JudicialSystem.PJE);
            systems.add(JudicialSystem.PDPJ);
        }
        if (tribunal.startsWith("TRF") || containsNormalized(competencia, "federal")) {
            systems.add(JudicialSystem.EPROC);
            systems.add(JudicialSystem.CRETA);
            systems.add(JudicialSystem.PJE);
            systems.add(JudicialSystem.PDPJ);
        }
        if (PREFERRED_EPROC.contains(tribunal)) {
            systems.add(JudicialSystem.EPROC);
        }
        if (PREFERRED_ESAJ.contains(tribunal)) {
            systems.add(JudicialSystem.ESAJ);
        }
        if (PREFERRED_PROJUDI.contains(tribunal)) {
            systems.add(JudicialSystem.PROJUDI);
        }
        if (PREFERRED_CRETA.contains(tribunal)) {
            systems.add(JudicialSystem.CRETA);
        }
        if (recurso) {
            systems.add(JudicialSystem.PDPJ);
        }
        systems.add(JudicialSystem.PJE);
        systems.add(JudicialSystem.EPROC);
        systems.add(JudicialSystem.ESAJ);
        systems.add(JudicialSystem.PROJUDI);
        systems.add(JudicialSystem.CRETA);
        systems.add(JudicialSystem.PDPJ);
        systems.add(JudicialSystem.MNI);
        systems.add(JudicialSystem.OUTRO);
        return List.copyOf(systems);
    }

    private Map<String, Object> mergeCanonicalPayload(Map<String, Object> payload,
                                                       String ritoName,
                                                       String ramoDireito,
                                                       String competencia) {
        LinkedHashMap<String, Object> merged = new LinkedHashMap<>(payload == null ? Map.of() : payload);
        if (ritoName != null && !ritoName.isBlank()) {
            merged.putIfAbsent("rito", ritoName);
            merged.putIfAbsent("rito_processual", ritoName);
        }
        if (ramoDireito != null && !ramoDireito.isBlank()) {
            merged.putIfAbsent("ramo_direito", ramoDireito);
        }
        if (competencia != null && !competencia.isBlank()) {
            merged.putIfAbsent("competencia", competencia);
        }
        return JudicialMapSupport.copyNonNull(merged);
    }

    private String normalizeRitoName(String ritoName) {
        if (ritoName == null || ritoName.isBlank()) {
            return null;
        }
        String normalized = ritoName.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if (isEleitoralRito(normalized) || isTrabalhistaRito(normalized) || isMilitarRito(normalized) || isPrevidenciarioRito(normalized) || isFazendaRito(normalized)) {
            return normalized;
        }
        CanonicalContext canonicalContext = proceduralCanonicalResolver.resolve(Map.of("rito", ritoName));
        if (canonicalContext.rito() != null) {
            return canonicalContext.rito().name();
        }
        return normalized;
    }

    private String inferTribunalCodigo(String competencia, Map<String, Object> payload) {
        String uf = normalizeUf(firstNonBlank(asText(payload.get("uf")), asText(payload.get("ufAutor")), asText(payload.get("estado"))));
        String rito = firstNonBlank(asText(payload.get("rito")), asText(payload.get("ritoProcessual")));
        RamoJusticaNacional ramo = inferRamo(competencia, firstNonBlank(asText(payload.get("ramoDireito")), asText(payload.get("ramo"))), rito);
        if (uf != null && ramo != null) {
            String codigo = NationalCompetenceMatrix.resolver(uf, ramo).map(NationalCompetenceMatrix::codigo).orElse(null);
            if (codigo != null) {
                return codigo;
            }
        }
        String raw = firstNonBlank(asText(payload.get("esfera")), asText(payload.get("tipoJustica")), competencia);
        if (raw == null) {
            return null;
        }
        String value = raw.toUpperCase(Locale.ROOT);
        if (value.contains("ELEITORAL")) {
            return "TRE_PADRAO";
        }
        if (value.contains("TRABALH")) {
            return "TRT_PADRAO";
        }
        if (value.contains("MILITAR")) {
            return "JM_PADRAO";
        }
        if (value.contains("FEDERAL")) {
            return "TRF_PADRAO";
        }
        return "TJ_PADRAO";
    }

    private JudicialSystem parseSystem(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_').replace('/', '_');
        return switch (normalized) {
            case "PJE", "PJE_JF", "PJE_TRT", "PJE_TSE", "PJE_TRE" -> JudicialSystem.PJE;
            case "ESAJ", "E_SAJ" -> JudicialSystem.ESAJ;
            case "PROJUDI" -> JudicialSystem.PROJUDI;
            case "EPROC", "E_PROC" -> JudicialSystem.EPROC;
            case "CRETA" -> JudicialSystem.CRETA;
            case "PDPJ", "PDPJ_BR" -> JudicialSystem.PDPJ;
            case "MNI", "PJE_MNI" -> JudicialSystem.MNI;
            case "MP" -> JudicialSystem.MP;
            default -> {
                try {
                    yield JudicialSystem.valueOf(normalized);
                } catch (Exception ex) {
                    yield null;
                }
            }
        };
    }

    private String competenceHint(String competencia, String tribunalCodigo) {
        String normalized = firstNonBlank(competencia, tribunalCodigo, "competencia_a_confirmar");
        return normalized == null ? "competencia_a_confirmar" : normalized;
    }

    private boolean containsNormalized(String value, String token) {
        if (value == null || token == null) {
            return false;
        }
        return value.toLowerCase(Locale.ROOT).contains(token.toLowerCase(Locale.ROOT));
    }


    private boolean isEleitoralRito(Object rito) {
        return hasSegment(rito, "ELEITORAL");
    }

    private boolean isTrabalhistaRito(Object rito) {
        return hasSegment(rito, "TRABALH");
    }

    private boolean isMilitarRito(Object rito) {
        return hasSegment(rito, "MILITAR");
    }

    private boolean isPrevidenciarioRito(Object rito) {
        return hasSegment(rito, "PREVID") || hasSegment(rito, "JUIZADO_ESPECIAL_FEDERAL");
    }

    private boolean isFazendaRito(Object rito) {
        return hasSegment(rito, "FAZENDA") || hasSegment(rito, "TRIBUT") || hasSegment(rito, "EXECUCAO_FISCAL");
    }

    private boolean hasSegment(Object rito, String segment) {
        return rito != null && segment != null && String.valueOf(rito).toUpperCase(Locale.ROOT).contains(segment);
    }

    private String normalizeUf(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.length() > 2 ? normalized.substring(0, 2) : normalized;
    }

    private String asText(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
