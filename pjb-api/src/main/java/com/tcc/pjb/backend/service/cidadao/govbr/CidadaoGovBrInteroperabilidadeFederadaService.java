package com.tcc.pjb.backend.service.cidadao.govbr;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.integration.govbr.oidc.GovBrOidcProperties;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorRegistry;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorRuntimePostureReport;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorRuntimePostureService;
import com.tcc.pjb.backend.integration.judicial.JudicialConnectorRuntimePostureSystemReport;
import com.tcc.pjb.backend.integration.judicial.JudicialProcessConnector;
import com.tcc.pjb.backend.integration.judicial.JudicialSubmissionCapability;
import com.tcc.pjb.backend.integration.judicial.JudicialSystem;
import com.tcc.pjb.backend.integration.judicial.security.JudicialCertificateRevocationMode;
import com.tcc.pjb.backend.integration.judicial.security.JudicialConnectorSecurityPackReport;
import com.tcc.pjb.backend.integration.judicial.security.JudicialConnectorSecurityPackService;
import com.tcc.pjb.backend.integration.judicial.security.JudicialConnectorTlsMode;
import com.tcc.pjb.backend.model.dto.cidadao.govbr.CidadaoGovBrAcessoFederadoRequest;
import com.tcc.pjb.backend.model.dto.cidadao.govbr.CidadaoGovBrAcessoFederadoResponse;
import com.tcc.pjb.backend.model.dto.cidadao.govbr.CidadaoGovBrInteroperabilidadeFederadaResponse;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.identity.IdentidadeJuridicaNacional;
import com.tcc.pjb.backend.service.identity.IdentidadeJuridicaNacionalService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class CidadaoGovBrInteroperabilidadeFederadaService {

    private final CurrentUserService currentUserService;
    private final GovBrOidcProperties govBrOidcProperties;
    private final IdentidadeJuridicaNacionalService identidadeService;
    private final JudicialConnectorRegistry connectorRegistry;
    private final JudicialConnectorRuntimePostureService runtimePostureService;
    private final JudicialConnectorSecurityPackService securityPackService;

    public CidadaoGovBrInteroperabilidadeFederadaService(CurrentUserService currentUserService,
                                                         GovBrOidcProperties govBrOidcProperties,
                                                         IdentidadeJuridicaNacionalService identidadeService,
                                                         JudicialConnectorRegistry connectorRegistry,
                                                         JudicialConnectorRuntimePostureService runtimePostureService,
                                                         JudicialConnectorSecurityPackService securityPackService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.govBrOidcProperties = Objects.requireNonNull(govBrOidcProperties);
        this.identidadeService = Objects.requireNonNull(identidadeService);
        this.connectorRegistry = Objects.requireNonNull(connectorRegistry);
        this.runtimePostureService = Objects.requireNonNull(runtimePostureService);
        this.securityPackService = Objects.requireNonNull(securityPackService);
    }

    public CidadaoGovBrInteroperabilidadeFederadaResponse panorama() {
        IdentityState identity = resolveIdentity();
        JudicialConnectorRuntimePostureReport runtime = runtimePostureService.nationalReport();
        Map<JudicialSystem, JudicialConnectorRuntimePostureSystemReport> postureBySystem = indexRuntime(runtime);
        ArrayList<CidadaoGovBrInteroperabilidadeFederadaResponse.ConnectorCoverage> connectors = new ArrayList<>();
        for (JudicialSystem system : orderedSystems()) {
            connectors.add(toCoverage(system, connectorRegistry.find(system).orElse(null), postureBySystem.get(system), identity));
        }
        connectors.sort(Comparator
                .comparing(CidadaoGovBrInteroperabilidadeFederadaResponse.ConnectorCoverage::discoveryReady, Comparator.reverseOrder())
                .thenComparing(CidadaoGovBrInteroperabilidadeFederadaResponse.ConnectorCoverage::accessReady, Comparator.reverseOrder())
                .thenComparing(CidadaoGovBrInteroperabilidadeFederadaResponse.ConnectorCoverage::documentReady, Comparator.reverseOrder())
                .thenComparing(CidadaoGovBrInteroperabilidadeFederadaResponse.ConnectorCoverage::proxySoberanoElegivel, Comparator.reverseOrder())
                .thenComparing(CidadaoGovBrInteroperabilidadeFederadaResponse.ConnectorCoverage::runtimeBloqueado)
                .thenComparing(CidadaoGovBrInteroperabilidadeFederadaResponse.ConnectorCoverage::sistemaOrigem));

        int discoveryReady = 0;
        int accessReady = 0;
        int documentReady = 0;
        int blocked = 0;
        int quarantined = 0;
        int stepUp = 0;
        int mtls = 0;
        int proxy = 0;
        LinkedHashSet<String> alerts = new LinkedHashSet<>();
        ArrayList<CidadaoGovBrInteroperabilidadeFederadaResponse.GapItem> gaps = new ArrayList<>();
        for (CidadaoGovBrInteroperabilidadeFederadaResponse.ConnectorCoverage item : connectors) {
            if (item.discoveryReady()) {
                discoveryReady++;
            }
            if (item.accessReady()) {
                accessReady++;
            }
            if (item.documentReady()) {
                documentReady++;
            }
            if (item.runtimeBloqueado()) {
                blocked++;
            }
            if ("QUARANTINED".equals(item.runtimeStatus())) {
                quarantined++;
            }
            if (item.exigeStepUp()) {
                stepUp++;
            }
            if ("MTLS".equals(item.tlsMode())) {
                mtls++;
            }
            if (item.proxySoberanoElegivel()) {
                proxy++;
            }
            alerts.addAll(item.alertas());
            alerts.addAll(item.bloqueios());
            gaps.addAll(toGaps(item));
        }

        if (!identity.govBrLinked) {
            gaps.add(new CidadaoGovBrInteroperabilidadeFederadaResponse.GapItem(
                    "GOVBR_LINK_REQUIRED",
                    "CRITICO",
                    "Vínculo gov.br ausente",
                    "A descoberta soberana por CPF e o acesso federado restrito exigem vínculo formal da identidade do cidadão ao ecossistema gov.br."
            ));
        }
        if (!identity.discoveryReady) {
            gaps.add(new CidadaoGovBrInteroperabilidadeFederadaResponse.GapItem(
                    "ASSURANCE_DISCOVERY_LOW",
                    "ALTO",
                    "Assurance insuficiente para descoberta ampliada",
                    "A conta atual não atinge o nível mínimo recomendado para descoberta ampliada em múltiplas fontes judiciais."
            ));
        }

        return new CidadaoGovBrInteroperabilidadeFederadaResponse(
                LocalDateTime.now(),
                new CidadaoGovBrInteroperabilidadeFederadaResponse.IdentitySummary(
                        identity.govBrEnabled,
                        identity.govBrLinked,
                        identity.govBrNivel.name(),
                        identity.discoveryReady,
                        identity.restrictedReady,
                        identity.stepUpDisponivel,
                        toLocalDateTime(identity.ultimaSincronizacao)
                ),
                new CidadaoGovBrInteroperabilidadeFederadaResponse.Summary(
                        connectors.size(),
                        discoveryReady,
                        accessReady,
                        documentReady,
                        blocked,
                        quarantined,
                        stepUp,
                        mtls,
                        proxy
                ),
                List.copyOf(connectors),
                List.copyOf(gaps),
                List.copyOf(alerts),
                new CidadaoGovBrInteroperabilidadeFederadaResponse.Links(
                        "/api/v1/cidadao/govbr/acervo-unificado",
                        "/api/v1/cidadao/govbr/acesso-federado/avaliar",
                        "/api/v1/auth/govbr/assurance-level",
                        "/api/v1/auth/govbr/stepup/start",
                        "/api/v1/processual/integration/external/diagnostic"
                )
        );
    }

    public CidadaoGovBrAcessoFederadoResponse avaliarAcesso(CidadaoGovBrAcessoFederadoRequest request) {
        Objects.requireNonNull(request);
        IdentityState identity = resolveIdentity();
        JudicialSystem system = parseSystem(request.sistemaOrigem());
        JudicialProcessConnector connector = connectorRegistry.find(system).orElse(null);
        JudicialSubmissionCapability capability = connector != null ? connector.capability() : null;
        JudicialConnectorRuntimePostureSystemReport runtime = indexRuntime(runtimePostureService.nationalReport()).get(system);
        JudicialConnectorSecurityPackReport security = securityPackService.effectivePack(system, normalizeTribunal(request.tribunalCodigo()));

        boolean secureTransportReady = isSecureTransportReady(security);
        boolean blockedByRuntime = runtime != null && isBlockedRuntime(runtime.runtimeStatus());
        boolean requiresStepUp = (capability != null && capability.requiresStepUpGovBr())
                || request.processoSigiloso()
                || request.exigeCiencia()
                || request.exigeAtuacao();
        boolean requiresInstitutionalCredential = request.processoSigiloso()
                && request.nivelSigilo() != null
                && request.nivelSigilo().trim().toUpperCase(Locale.ROOT).contains("N4");

        boolean discoveryAllowed = identity.discoveryReady && connector != null && capability.enabled() && capability.supportsSnapshotSync();
        boolean capaAllowed = discoveryAllowed && !blockedByRuntime;
        boolean timelineAllowed = capaAllowed && capability.supportsEventSync();
        boolean identityStrongEnough = !requiresStepUp || identity.restrictedReady;
        boolean documentsAllowed = timelineAllowed
                && request.possuiDocumentos()
                && secureTransportReady
                && identityStrongEnough
                && !requiresInstitutionalCredential;
        boolean mediaAllowed = documentsAllowed && request.possuiMidiaExterna() && capability.supportsExternalMedia();
        boolean proxySoberanoElegivel = documentsAllowed && capability.supportsExternalMedia() && security.tlsMode() == JudicialConnectorTlsMode.MTLS;

        String documentMode = resolveDocumentMode(documentsAllowed, mediaAllowed, proxySoberanoElegivel, request.possuiDocumentos());
        String accessMode = capaAllowed
                ? (timelineAllowed ? GovBrFederatedInteropLabels.ACESSO_CAPA_TIMELINE : GovBrFederatedInteropLabels.ACESSO_CAPA_MINIMA)
                : GovBrFederatedInteropLabels.ACESSO_CAPA_MINIMA;
        String syncMode = runtime != null && runtime.readOnlyProjectionRecommended()
                ? GovBrFederatedInteropLabels.SINCRONIZACAO_PROJECAO_LOCAL
                : GovBrFederatedInteropLabels.SINCRONIZACAO_INCREMENTAL;
        String decision = resolveDecision(requiresStepUp, identityStrongEnough, blockedByRuntime, discoveryAllowed, documentsAllowed, requiresInstitutionalCredential);

        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        if (!identity.govBrLinked) {
            blockers.add("Identidade do cidadão ainda não está vinculada ao gov.br.");
        }
        if (!discoveryAllowed) {
            blockers.add("Conector ou descoberta por CPF canônico ainda não estão prontos para esta fonte.");
        }
        if (blockedByRuntime) {
            blockers.add("Fonte judicial está bloqueada ou em quarentena operacional.");
        }
        if (!secureTransportReady) {
            blockers.add("Envelope criptográfico do conector ainda não atende o mínimo soberano de transporte seguro.");
        }
        if (requiresInstitutionalCredential) {
            blockers.add("Nível de sigilo exige credencial institucional específica além do login gov.br do cidadão.");
        }
        if (requiresStepUp && !identity.restrictedReady) {
            warnings.add("Acesso exige step-up para liberar timeline/documentos sensíveis.");
        }
        if (runtime != null && runtime.readOnlyProjectionRecommended()) {
            warnings.add("A fonte está em leitura degradada por projeção local com reconciliação posterior.");
        }
        if (request.possuiMidiaExterna() && !capability.supportsExternalMedia()) {
            warnings.add("A fonte não expõe mídia externa federada; o PJB deve operar por link controlado ou capa reduzida.");
        }
        if (request.possuiDocumentos() && documentsAllowed && !proxySoberanoElegivel) {
            warnings.add("Documento pode ser entregue por link federado controlado; proxy soberano pleno ainda depende de MTLS e política documental da fonte.");
        }

        return new CidadaoGovBrAcessoFederadoResponse(
                LocalDateTime.now(),
                system.name(),
                GovBrFederatedInteropLabels.systemLabel(system),
                normalizeTribunal(request.tribunalCodigo()),
                request.numeroProcesso(),
                connector != null && capability.supportsSnapshotSync() ? GovBrFederatedInteropLabels.DESCOBERTA_CPF_CNJ : GovBrFederatedInteropLabels.DESCOBERTA_CPF_FONTES_HIBRIDAS,
                accessMode,
                documentMode,
                syncMode,
                discoveryAllowed,
                capaAllowed,
                timelineAllowed,
                documentsAllowed,
                mediaAllowed,
                requiresStepUp,
                requiresInstitutionalCredential,
                secureTransportReady,
                proxySoberanoElegivel,
                decision,
                List.copyOf(warnings),
                List.copyOf(blockers),
                new CidadaoGovBrAcessoFederadoResponse.Links(
                        "/api/v1/cidadao/govbr/acervo-unificado",
                        "/api/v1/processual/integration/external/diagnostic",
                        capability != null ? capability.baseUrl() : null,
                        "/api/v1/auth/govbr/stepup/start",
                        "/api/v1/processual/recursal/documental/authenticity"
                )
        );
    }

    private List<CidadaoGovBrInteroperabilidadeFederadaResponse.GapItem> toGaps(CidadaoGovBrInteroperabilidadeFederadaResponse.ConnectorCoverage item) {
        ArrayList<CidadaoGovBrInteroperabilidadeFederadaResponse.GapItem> gaps = new ArrayList<>();
        if (!item.discoveryReady()) {
            gaps.add(new CidadaoGovBrInteroperabilidadeFederadaResponse.GapItem(
                    item.sistemaOrigem() + "_DISCOVERY",
                    "ALTO",
                    "Descoberta incompleta em " + item.sistemaLabel(),
                    "A fonte ainda não oferece envelope suficiente para localizar processos por CPF canônico com descoberta governada."
            ));
        }
        if (!item.accessReady()) {
            gaps.add(new CidadaoGovBrInteroperabilidadeFederadaResponse.GapItem(
                    item.sistemaOrigem() + "_ACCESS",
                    GovBrFederatedInteropLabels.gapSeverity(item.runtimeStatus()),
                    "Acesso federado incompleto em " + item.sistemaLabel(),
                    "A fonte ainda não fecha capa, timeline e runtime seguro suficientes para leitura soberana dentro do PJB."
            ));
        }
        if (!item.documentReady()) {
            gaps.add(new CidadaoGovBrInteroperabilidadeFederadaResponse.GapItem(
                    item.sistemaOrigem() + "_DOCUMENT",
                    item.transporteSeguroPronto() ? "MEDIO" : "ALTO",
                    "Ponte documental incompleta em " + item.sistemaLabel(),
                    "Ainda falta proxy seguro, espelho autorizado ou envelope documental equivalente para uso pleno dentro do PJB."
            ));
        }
        return gaps;
    }

    private CidadaoGovBrInteroperabilidadeFederadaResponse.ConnectorCoverage toCoverage(JudicialSystem system,
                                                                                           JudicialProcessConnector connector,
                                                                                           JudicialConnectorRuntimePostureSystemReport runtime,
                                                                                           IdentityState identity) {
        JudicialSubmissionCapability capability = connector != null ? connector.capability() : null;
        JudicialConnectorSecurityPackReport security = securityPackService.effectivePack(system, null);
        boolean connectorRegistered = connector != null;
        boolean secureTransportReady = isSecureTransportReady(security);
        boolean blockedRuntime = runtime != null && isBlockedRuntime(runtime.runtimeStatus());
        boolean discoveryReady = connectorRegistered && capability.enabled() && capability.supportsSnapshotSync() && identity.discoveryReady;
        boolean accessReady = discoveryReady && capability.supportsEventSync() && !blockedRuntime;
        boolean documentReady = accessReady && secureTransportReady && capability.supportsExternalMedia() && identity.restrictedReady;
        boolean proxySoberanoElegivel = documentReady && security.tlsMode() == JudicialConnectorTlsMode.MTLS;
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashSet<String> blockers = new LinkedHashSet<>();
        if (!connectorRegistered) {
            blockers.add("Conector ainda não registrado na malha federada do PJB.");
        }
        if (runtime != null) {
            if (runtime.warnings() != null) {
                warnings.addAll(runtime.warnings());
            }
            if (runtime.blockers() != null) {
                blockers.addAll(runtime.blockers());
            }
            if (runtime.readOnlyProjectionRecommended()) {
                warnings.add("Leitura governada deve usar projeção local com reconciliação incremental nesta fonte.");
            }
        }
        if (!secureTransportReady) {
            blockers.add("Envelope criptográfico do conector ainda não fecha transporte seguro soberano.");
        }
        if (capability != null && capability.requiresStepUpGovBr() && !identity.restrictedReady) {
            warnings.add("Step-up gov.br será exigido para acesso restrito nesta fonte.");
        }
        return new CidadaoGovBrInteroperabilidadeFederadaResponse.ConnectorCoverage(
                system.name(),
                GovBrFederatedInteropLabels.systemLabel(system),
                connectorRegistered,
                connectorRegistered && capability.supportsSnapshotSync() ? GovBrFederatedInteropLabels.DESCOBERTA_CPF_CNJ : GovBrFederatedInteropLabels.DESCOBERTA_CPF_FONTES_HIBRIDAS,
                connectorRegistered && capability.supportsEventSync() ? GovBrFederatedInteropLabels.ACESSO_CAPA_TIMELINE : GovBrFederatedInteropLabels.ACESSO_CAPA_MINIMA,
                resolveDocumentMode(accessReady && secureTransportReady, accessReady && capability.supportsExternalMedia(), proxySoberanoElegivel, true),
                runtime != null && runtime.readOnlyProjectionRecommended() ? GovBrFederatedInteropLabels.SINCRONIZACAO_PROJECAO_LOCAL : GovBrFederatedInteropLabels.SINCRONIZACAO_INCREMENTAL,
                runtime != null ? runtime.runtimeStatus() : "UNMAPPED",
                GovBrFederatedInteropLabels.tlsLabel(security.tlsMode()),
                discoveryReady,
                accessReady,
                documentReady,
                capability != null && capability.requiresStepUpGovBr(),
                capability != null && capability.requiresCertificate(),
                secureTransportReady,
                proxySoberanoElegivel,
                blockedRuntime,
                List.copyOf(warnings),
                List.copyOf(blockers),
                "/api/v1/processual/integration/external/diagnostic"
        );
    }

    private IdentityState resolveIdentity() {
        Usuario usuario = currentUserService.getRequired();
        if (usuario.getTipoUsuario() != TipoUsuario.CIDADAO) {
            throw new IllegalStateException("role");
        }
        Optional<IdentidadeJuridicaNacional> identidadeOpt = identidadeService.buscarPorDocumento(usuario.getCpf());
        IdentidadeJuridicaNacional identidade = identidadeOpt.orElse(null);
        IdentidadeJuridicaNacional.GovBrNivel nivel = identidade != null ? identidade.getGovBrNivel() : IdentidadeJuridicaNacional.GovBrNivel.NAO_VINCULADO;
        boolean discoveryReady = switch (nivel) {
            case BRONZE, PRATA, OURO -> true;
            case NAO_VINCULADO -> false;
        };
        boolean restrictedReady = switch (nivel) {
            case PRATA, OURO -> true;
            case BRONZE, NAO_VINCULADO -> false;
        };
        return new IdentityState(
                govBrOidcProperties.enabled(),
                identidade != null && nivel != IdentidadeJuridicaNacional.GovBrNivel.NAO_VINCULADO,
                nivel,
                discoveryReady,
                restrictedReady,
                govBrOidcProperties.enabled(),
                identidade != null ? identidade.getUltimaSincronizacaoEm() : null
        );
    }

    private Map<JudicialSystem, JudicialConnectorRuntimePostureSystemReport> indexRuntime(JudicialConnectorRuntimePostureReport report) {
        LinkedHashMap<JudicialSystem, JudicialConnectorRuntimePostureSystemReport> index = new LinkedHashMap<>();
        if (report != null && report.systems() != null) {
            report.systems().forEach(item -> index.put(item.system(), item));
        }
        return Map.copyOf(index);
    }

    private List<JudicialSystem> orderedSystems() {
        return List.of(JudicialSystem.PJE, JudicialSystem.ESAJ, JudicialSystem.EPROC, JudicialSystem.CRETA, JudicialSystem.PROJUDI, JudicialSystem.PDPJ, JudicialSystem.MNI, JudicialSystem.MP, JudicialSystem.OUTRO);
    }

    private boolean isSecureTransportReady(JudicialConnectorSecurityPackReport security) {
        return security != null
                && security.enabled()
                && security.tlsMode() != null
                && security.hostnameVerification()
                && security.requireTrustStoreForPathValidation()
                && security.revocationMode() != null
                && security.revocationMode() != JudicialCertificateRevocationMode.DISABLED;
    }

    private boolean isBlockedRuntime(String runtimeStatus) {
        if (runtimeStatus == null) {
            return false;
        }
        String normalized = runtimeStatus.trim().toUpperCase(Locale.ROOT);
        return "BLOCKED".equals(normalized) || "QUARANTINED".equals(normalized);
    }

    private JudicialSystem parseSystem(String raw) {
        if (raw == null || raw.isBlank()) {
            return JudicialSystem.OUTRO;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        try {
            return JudicialSystem.valueOf(normalized);
        } catch (Exception ex) {
            return JudicialSystem.OUTRO;
        }
    }

    private String normalizeTribunal(String tribunalCodigo) {
        if (tribunalCodigo == null || tribunalCodigo.isBlank()) {
            return null;
        }
        return tribunalCodigo.trim().toUpperCase(Locale.ROOT);
    }

    private String resolveDocumentMode(boolean documentsAllowed,
                                       boolean mediaAllowed,
                                       boolean proxySoberanoElegivel,
                                       boolean hasDocuments) {
        if (!hasDocuments) {
            return GovBrFederatedInteropLabels.DOCUMENTO_INDISPONIVEL;
        }
        if (!documentsAllowed) {
            return GovBrFederatedInteropLabels.DOCUMENTO_LINK_FEDERADO;
        }
        if (proxySoberanoElegivel) {
            return mediaAllowed ? GovBrFederatedInteropLabels.DOCUMENTO_ESPELHO_AUTORIZADO : GovBrFederatedInteropLabels.DOCUMENTO_PROXY_SOBERANO;
        }
        return GovBrFederatedInteropLabels.DOCUMENTO_LINK_FEDERADO;
    }

    private String resolveDecision(boolean requiresStepUp,
                                   boolean identityStrongEnough,
                                   boolean blockedByRuntime,
                                   boolean discoveryAllowed,
                                   boolean documentsAllowed,
                                   boolean requiresInstitutionalCredential) {
        if (requiresInstitutionalCredential || blockedByRuntime || !discoveryAllowed) {
            return GovBrFederatedInteropLabels.DECISAO_ACESSO_NEGADO;
        }
        if (requiresStepUp && !identityStrongEnough) {
            return GovBrFederatedInteropLabels.DECISAO_ACESSO_STEP_UP;
        }
        if (!documentsAllowed) {
            return GovBrFederatedInteropLabels.DECISAO_ACESSO_DEGRADADO;
        }
        return GovBrFederatedInteropLabels.DECISAO_ACESSO_LIBERADO;
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    private record IdentityState(
            boolean govBrEnabled,
            boolean govBrLinked,
            IdentidadeJuridicaNacional.GovBrNivel govBrNivel,
            boolean discoveryReady,
            boolean restrictedReady,
            boolean stepUpDisponivel,
            Instant ultimaSincronizacao
    ) {
    }
}
