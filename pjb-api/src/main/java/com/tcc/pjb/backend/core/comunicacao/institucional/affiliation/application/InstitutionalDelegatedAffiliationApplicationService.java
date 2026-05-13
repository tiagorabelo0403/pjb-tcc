package com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalScopeResolutionSupport;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessLaneBlueprint;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliationDocument;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliationRequest;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationRequestStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryLandingPanel;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalAffiliationApprovalTrailApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalAffiliationValidationApplicationService;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.FuncaoOperacionalInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAffiliationRequestStatus;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalDelegatedAffiliationApplicationService {

    private final CurrentUserService currentUserService;
    private final InstitutionalAffiliationApplicationService affiliationApplicationService;
    private final InstitutionalAffiliationRequestStateRepository requestRepository;
    private final InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalogApplicationService;
    private final InstitutionalAffiliationValidationApplicationService validationApplicationService;
    private final InstitutionalPublicRecognitionGateApplicationService publicRecognitionGateApplicationService;
    private final InstitutionalAffiliationApprovalTrailApplicationService approvalTrailApplicationService;

    public InstitutionalDelegatedAffiliationApplicationService(CurrentUserService currentUserService,
                                                               InstitutionalAffiliationApplicationService affiliationApplicationService,
                                                               InstitutionalAffiliationRequestStateRepository requestRepository,
                                                               InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalogApplicationService,
                                                               InstitutionalAffiliationValidationApplicationService validationApplicationService,
                                                               InstitutionalPublicRecognitionGateApplicationService publicRecognitionGateApplicationService,
                                                               InstitutionalAffiliationApprovalTrailApplicationService approvalTrailApplicationService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.affiliationApplicationService = Objects.requireNonNull(affiliationApplicationService);
        this.requestRepository = Objects.requireNonNull(requestRepository);
        this.blueprintCatalogApplicationService = Objects.requireNonNull(blueprintCatalogApplicationService);
        this.validationApplicationService = Objects.requireNonNull(validationApplicationService);
        this.publicRecognitionGateApplicationService = Objects.requireNonNull(publicRecognitionGateApplicationService);
        this.approvalTrailApplicationService = Objects.requireNonNull(approvalTrailApplicationService);
    }

    public InstitutionalAffiliationRequest solicitarAdesao(DestinatarioInstitucionalKind destinatarioKind,
                                                           InstitutionalOrganizationScope organizationScope,
                                                           String orgaoSigla,
                                                           String orgaoNome,
                                                           String unidadeCodigo,
                                                           String unidadeNome,
                                                           String uf,
                                                           String comarca,
                                                           String cnpj,
                                                           String esferaAdministrativa,
                                                           List<String> ramosMateriais,
                                                           List<String> abrangenciasTerritoriais,
                                                           String dominioInstitucional,
                                                           String autoridadeAderenteCargo,
                                                           String representanteNome,
                                                           InstitutionalNominationRole representativeRole,
                                                           Map<Long, String> bootstrapAdministrators,
                                                           InstitutionalTrustLevel trustFloor,
                                                           Boolean requerDuplaAprovacaoAdministrador,
                                                           Boolean requerCertificadoICP,
                                                           Boolean restringeCertificadoRedeInstitucional,
                                                           Boolean permiteUsoRemotoComAutorizacao,
                                                           List<String> canaisHabilitados,
                                                           List<String> politicaCiencia,
                                                           List<String> sla,
                                                           List<String> regrasFallback,
                                                           List<String> conveniosIntegracoes,
                                                           List<InstitutionalAffiliationDocument> documentos,
                                                           List<String> fundamentos) {
        Usuario usuario = currentUserService.getRequired();
        InstitutionalOrganizationScope scope = organizationScope != null ? organizationScope : blueprintCatalogApplicationService.inferScope(destinatarioKind, unidadeCodigo, orgaoSigla, unidadeNome);
        validateNoActiveCollision(orgaoSigla, unidadeCodigo, null);
        Instant now = Instant.now();
        var blueprint = blueprintCatalogApplicationService.findByScope(InstitutionalScopeResolutionSupport.fallback(scope)).orElse(null);
        Map<Long, String> administrators = bootstrapAdministrators == null || bootstrapAdministrators.isEmpty()
                ? Map.of(usuario.getId(), representanteNome == null || representanteNome.isBlank() ? usuario.getNome() : representanteNome.trim())
                : sanitizedAdministrators(bootstrapAdministrators, usuario);
        boolean duplaAdministracao = requerDuplaAprovacaoAdministrador != null
                ? requerDuplaAprovacaoAdministrador
                : blueprint != null && blueprint.requerDuplaAprovacaoAdministrador();
        validateBootstrapAdministrators(administrators, duplaAdministracao);
        InstitutionalAffiliationRequest request = new InstitutionalAffiliationRequest(
                UUID.randomUUID().toString(),
                require(destinatarioKind),
                scope,
                require(orgaoSigla),
                require(orgaoNome),
                require(unidadeCodigo),
                unidadeNome,
                uf,
                comarca,
                cnpj,
                esferaAdministrativa,
                sanitize(ramosMateriais),
                sanitize(abrangenciasTerritoriais),
                dominioInstitucional,
                autoridadeAderenteCargo,
                usuario.getId(),
                representanteNome == null || representanteNome.isBlank() ? usuario.getNome() : representanteNome.trim(),
                representativeRole == null ? InstitutionalNominationRole.ADMINISTRADOR_INSTITUCIONAL : representativeRole,
                administrators,
                trustFloor == null ? defaultTrust(scope) : trustFloor,
                duplaAdministracao,
                requerCertificadoICP != null ? requerCertificadoICP : blueprint != null && blueprint.requerCertificadoICP(),
                restringeCertificadoRedeInstitucional != null ? restringeCertificadoRedeInstitucional : blueprint != null && blueprint.restringeCertificadoRedeInstitucional(),
                permiteUsoRemotoComAutorizacao != null ? permiteUsoRemotoComAutorizacao : blueprint != null && blueprint.permiteUsoRemotoComAutorizacao(),
                sanitize(canaisHabilitados),
                sanitize(politicaCiencia),
                sanitize(sla),
                sanitize(regrasFallback),
                sanitize(conveniosIntegracoes),
                documentos == null ? List.of() : List.copyOf(documentos),
                InstitutionalAffiliationRequestStatus.PENDENTE_VALIDACAO,
                null,
                appendFundamentos(fundamentos,
                        "adesao_delegada_pelo_orgao",
                        "representante_usuario_id=" + usuario.getId(),
                        "representative_role=" + (representativeRole == null ? InstitutionalNominationRole.ADMINISTRADOR_INSTITUCIONAL.name() : representativeRole.name()),
                        "scope=" + (scope == null ? "GENERICO_INSTITUCIONAL" : scope.name()),
                        authorityCargoFundamento(autoridadeAderenteCargo),
                        metadataFundamento("esfera", esferaAdministrativa),
                        metadataFundamento("ramos", sanitize(ramosMateriais)),
                        metadataFundamento("abrangencia_territorial", sanitize(abrangenciasTerritoriais))),
                now,
                null,
                now,
                null
        );
        InstitutionalAffiliationRequest saved = requestRepository.save(request);
        validationApplicationService.validar(saved);
        approvalTrailApplicationService.registrarSubmissao(saved, usuario);
        return saved;
    }

    public InstitutionalAffiliationRequest homologarSolicitacao(String requestId,
                                                                boolean aprovar,
                                                                List<String> fundamentos) {
        InstitutionalAffiliationRequest request = requestRepository.findByRequestId(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação de adesão institucional não encontrada."));
        if (request.status().isTerminal()) {
            return request;
        }
        Instant now = Instant.now();
        if (!aprovar) {
            InstitutionalAffiliationRequest rejected = request.withStatus(
                    InstitutionalAffiliationRequestStatus.REJEITADA,
                    null,
                    appendFundamentos(fundamentos, "homologacao_pjb_rejeitada"),
                    now,
                    now);
            InstitutionalAffiliationRequest persisted = requestRepository.save(rejected);
            approvalTrailApplicationService.registrarDecisao(persisted, currentUserService.getRequired(), false, fundamentos);
            return persisted;
        }
        validationApplicationService.assertAptaParaHomologacao(request);
        var recognition = publicRecognitionGateApplicationService.avaliarSolicitacao(request);
        if ("NEGADA".equals(recognition.statusCode())) {
            throw new IllegalStateException("reconhecimento_publico_institucional_negado_para_homologacao_delegada");
        }
        validateNoActiveCollision(request.orgaoSigla(), request.unidadeCodigo(), request.requestId());
        InstitutionalAffiliation materialized = affiliationApplicationService.solicitarAfiliacao(
                request.destinatarioKind(),
                request.organizationScope(),
                request.orgaoSigla(),
                request.orgaoNome(),
                request.unidadeCodigo(),
                request.unidadeNome(),
                request.uf(),
                request.comarca(),
                request.cnpj(),
                request.esferaAdministrativa(),
                request.ramosMateriais(),
                request.abrangenciasTerritoriais(),
                request.dominioInstitucional(),
                request.autoridadeAderenteCargo(),
                resolveContactEmail(request),
                request.representativeRole(),
                request.trustFloorProposto(),
                request.requerDuplaAprovacaoAdministrador(),
                request.requerCertificadoICP(),
                request.restringeCertificadoRedeInstitucional(),
                request.permiteUsoRemotoComAutorizacao(),
                request.canaisHabilitados(),
                request.politicaCiencia(),
                request.sla(),
                request.regrasFallback(),
                request.conveniosIntegracoes(),
                appendFundamentos(request.fundamentos(),
                        "solicitacao_delegada=" + request.requestId(),
                        "homologacao_pjb",
                        "reconhecimento_publico_status=" + recognition.statusCode(),
                        "reconhecimento_publico_reconhecida=" + recognition.recognized()));
        InstitutionalAffiliation active = affiliationApplicationService.homologarAfiliacao(materialized.affiliationId(), true,
                appendFundamentos(fundamentos,
                        "materializada_a_partir_da_solicitacao=" + request.requestId(),
                        "reconhecimento_publico_status=" + recognition.statusCode(),
                        "reconhecimento_publico_reconhecida=" + recognition.recognized()));
        seedBootstrapAdministrators(request, active);
        InstitutionalAffiliationRequest approved = request.withStatus(
                InstitutionalAffiliationRequestStatus.HOMOLOGADA,
                active.affiliationId(),
                appendFundamentos(fundamentos,
                        "homologacao_pjb_aprovada",
                        "afiliacao_materializada=" + active.affiliationId(),
                        "reconhecimento_publico_status=" + recognition.statusCode(),
                        "reconhecimento_publico_reconhecida=" + recognition.recognized()),
                now,
                now);
        InstitutionalAffiliationRequest persisted = requestRepository.save(approved);
        approvalTrailApplicationService.registrarDecisao(persisted, currentUserService.getRequired(), true, fundamentos);
        return persisted;
    }

    public List<InstitutionalAffiliationRequest> listarSolicitacoes() {
        return requestRepository.findAll().stream()
                .sorted(Comparator.comparing(InstitutionalAffiliationRequest::updatedAt).reversed())
                .toList();
    }

    private void seedBootstrapAdministrators(InstitutionalAffiliationRequest request,
                                             InstitutionalAffiliation affiliation) {
        InstitutionalAccessLaneBlueprint lane = resolveMasterLane(affiliation.organizationScope());
        List<Map.Entry<Long, String>> administrators = request.bootstrapAdministrators().entrySet().stream().toList();
        for (Map.Entry<Long, String> admin : administrators) {
            affiliationApplicationService.nomearPessoa(
                    affiliation.affiliationId(),
                    admin.getKey(),
                    admin.getValue(),
                    TipoUsuario.ADMINISTRADOR,
                    lane == null ? InstitutionalAccessLaneKind.ADMINISTRACAO_MESTRA : lane.laneKind(),
                    lane == null ? InstitutionalNominationRole.ADMINISTRADOR_INSTITUCIONAL : lane.nominationRole(),
                    lane == null ? FuncaoOperacionalInstitucional.COORDENADOR_UNIDADE : lane.funcaoOperacional(),
                    lane == null ? InstitutionalProcessProfile.ADMINISTRADOR_INSTITUCIONAL : lane.processProfile(),
                    affiliation.unidadeCodigo(),
                    caixaGestoraCodigo(affiliation, lane),
                    lane == null ? null : lane.capacidadesPadrao(),
                    lane == null ? affiliation.trustFloor() : lane.trustFloor(),
                    lane == null ? InstitutionalEntryLandingPanel.PAINEL_ADMINISTRATIVO : lane.panel(),
                    Instant.now(),
                    null,
                    lane == null || lane.requerStepUpMfa(),
                    lane == null ? affiliation.requerCertificadoICP() : lane.requerCertificadoICP(),
                    lane == null ? affiliation.restringeCertificadoRedeInstitucional() : lane.requerRedeInstitucional(),
                    lane == null ? affiliation.permiteUsoRemotoComAutorizacao() : lane.permiteUsoRemotoAutorizado()
            );
        }
    }


    private void validateBootstrapAdministrators(Map<Long, String> administrators, boolean duplaAdministracao) {
        if (!duplaAdministracao) {
            return;
        }
        if (administrators == null || administrators.size() < 2) {
            throw new IllegalArgumentException("dupla_administracao_institucional_exige_dois_administradores_iniciais");
        }
    }

    private InstitutionalAccessLaneBlueprint resolveMasterLane(InstitutionalOrganizationScope scope) {
        return blueprintCatalogApplicationService.findByScope(InstitutionalScopeResolutionSupport.fallback(scope))
                .map(blueprint -> blueprint.lanes().stream()
                        .filter(item -> item.nominationRole() != null && item.nominationRole().isGestaoMestre())
                        .findFirst()
                        .orElseGet(() -> blueprint.lanes().stream().findFirst().orElse(null)))
                .orElse(null);
    }

    private String caixaGestoraCodigo(InstitutionalAffiliation affiliation,
                                      InstitutionalAccessLaneBlueprint lane) {
        String suffix = lane == null || lane.laneKind() == null ? "ADMINISTRACAO_MESTRA" : lane.laneKind().name();
        return affiliation.unidadeCodigo() + "::" + suffix + "::CAIXA";
    }

    private void validateNoActiveCollision(String orgaoSigla, String unidadeCodigo, String ignoreRequestId) {
        boolean existing = affiliationApplicationService.listarAfiliacoes().stream()
                .filter(InstitutionalAffiliation::ativa)
                .anyMatch(item -> item.orgaoSigla().equalsIgnoreCase(require(orgaoSigla))
                        && item.unidadeCodigo().equalsIgnoreCase(require(unidadeCodigo)));
        if (existing) {
            throw new IllegalStateException("Já existe afiliação ativa para o órgão e unidade informados.");
        }
        boolean pending = requestRepository.findByUnidadeCodigo(require(unidadeCodigo)).stream()
                .filter(item -> item.status().isAtiva())
                .filter(item -> ignoreRequestId == null || !item.requestId().equals(ignoreRequestId))
                .anyMatch(item -> item.orgaoSigla().equalsIgnoreCase(require(orgaoSigla)));
        if (pending) {
            throw new IllegalStateException("Já existe solicitação ativa para o órgão e unidade informados.");
        }
    }

    private String resolveContactEmail(InstitutionalAffiliationRequest request) {
        String dominio = request.dominioInstitucional();
        if (dominio == null || dominio.isBlank()) {
            return null;
        }
        String sanitized = dominio.trim().replace("https://", "").replace("http://", "").replace("www.", "");
        int slash = sanitized.indexOf('/');
        if (slash >= 0) {
            sanitized = sanitized.substring(0, slash);
        }
        return "seguranca@" + sanitized;
    }

    private InstitutionalTrustLevel defaultTrust(InstitutionalOrganizationScope scope) {
        return blueprintCatalogApplicationService.findByScope(InstitutionalScopeResolutionSupport.fallback(scope))
                .map(item -> item.trustFloorPadrao())
                .orElse(InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA);
    }

    private Map<Long, String> sanitizedAdministrators(Map<Long, String> bootstrapAdministrators, Usuario fallback) {
        LinkedHashMap<Long, String> out = new LinkedHashMap<>();
        bootstrapAdministrators.forEach((id, name) -> {
            if (id != null) {
                out.put(id, name == null || name.isBlank() ? (Objects.equals(id, fallback.getId()) ? fallback.getNome() : "Administrador institucional") : name.trim());
            }
        });
        if (out.isEmpty()) {
            out.put(fallback.getId(), fallback.getNome());
        }
        return Collections.unmodifiableMap(out);
    }

    private static List<String> sanitize(List<String> items) {
        if (items == null || items.isEmpty()) {
            return List.of();
        }
        ArrayList<String> out = new ArrayList<>();
        for (String item : items) {
            if (item != null && !item.isBlank()) {
                out.add(item.trim());
            }
        }
        return List.copyOf(out);
    }

    private static List<String> appendFundamentos(List<String> fundamentos, String... extras) {
        ArrayList<String> out = new ArrayList<>();
        if (fundamentos != null) out.addAll(fundamentos);
        if (extras != null) {
            for (String extra : extras) {
                if (extra != null && !extra.isBlank()) {
                    out.add(extra.trim());
                }
            }
        }
        return List.copyOf(out);
    }

    private String authorityCargoFundamento(String autoridadeAderenteCargo) {
        return metadataFundamento("autoridade_aderente", autoridadeAderenteCargo);
    }

    private String metadataFundamento(String key, String value) {
        return value == null || value.isBlank() ? null : key + "=" + value.trim();
    }

    private String metadataFundamento(String key, List<String> values) {
        List<String> sanitized = sanitize(values);
        return sanitized.isEmpty() ? null : key + "=" + String.join(",", sanitized);
    }

    private static <T> T require(T value) {
        return Objects.requireNonNull(value);
    }

    private static String require(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Valor obrigatório não informado.");
        }
        return value.trim();
    }
}
