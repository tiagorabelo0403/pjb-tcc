package com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessLaneBlueprint;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalOrganizationBlueprint;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalSecureEntrySummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalTrustAssessment;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalNominationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.application.InstitutionalEntryContextApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.application.InstitutionalIdentityBaseProfileResolverApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryContext;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryLandingPanel;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.FuncaoOperacionalInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAffiliationStatus;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationStatus;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalAffiliationApplicationService {

    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final InstitutionalNominationStateRepository nominationRepository;
    private final InstitutionalTrustAssessmentApplicationService trustAssessmentService;
    private final InstitutionalEntryContextApplicationService entryContextApplicationService;
    private final InstitutionalPublicRecognitionGateApplicationService publicRecognitionGateApplicationService;
    private final InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalogApplicationService;
    private final CurrentUserService currentUserService;
    private final InstitutionalIdentityBaseProfileResolverApplicationService identityBaseProfileResolver;

    public InstitutionalAffiliationApplicationService(InstitutionalAffiliationStateRepository affiliationRepository,
                                                      InstitutionalNominationStateRepository nominationRepository,
                                                      InstitutionalTrustAssessmentApplicationService trustAssessmentService,
                                                      InstitutionalEntryContextApplicationService entryContextApplicationService,
                                                      InstitutionalPublicRecognitionGateApplicationService publicRecognitionGateApplicationService,
                                                      InstitutionalOrganizationBlueprintCatalogApplicationService blueprintCatalogApplicationService,
                                                      CurrentUserService currentUserService,
                                                      InstitutionalIdentityBaseProfileResolverApplicationService identityBaseProfileResolver) {
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.nominationRepository = Objects.requireNonNull(nominationRepository);
        this.trustAssessmentService = Objects.requireNonNull(trustAssessmentService);
        this.entryContextApplicationService = Objects.requireNonNull(entryContextApplicationService);
        this.publicRecognitionGateApplicationService = Objects.requireNonNull(publicRecognitionGateApplicationService);
        this.blueprintCatalogApplicationService = Objects.requireNonNull(blueprintCatalogApplicationService);
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.identityBaseProfileResolver = Objects.requireNonNull(identityBaseProfileResolver);
    }

    public InstitutionalAffiliation solicitarAfiliacao(DestinatarioInstitucionalKind kind,
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
                                                       String emailContatoSeguranca,
                                                       InstitutionalNominationRole representativeRole,
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
                                                       List<String> fundamentos) {
        Usuario usuario = currentUserService.getRequired();
        Instant now = Instant.now();
        InstitutionalOrganizationScope resolvedScope = organizationScope != null
                ? organizationScope
                : blueprintCatalogApplicationService.inferScope(kind, unidadeCodigo, orgaoSigla, unidadeNome);
        InstitutionalOrganizationBlueprint blueprint = blueprintCatalogApplicationService.resolve(resolvedScope, kind).orElse(null);
        return affiliationRepository.save(new InstitutionalAffiliation(
                UUID.randomUUID().toString(),
                Objects.requireNonNullElse(kind, blueprint == null ? DestinatarioInstitucionalKind.ORGAO_TECNICO_CONVENIADO : blueprint.destinatarioKind()),
                normalize(orgaoSigla),
                require(orgaoNome),
                require(unidadeCodigo),
                unidadeNome == null || unidadeNome.isBlank() ? orgaoNome : unidadeNome.trim(),
                resolvedScope,
                blueprint == null ? null : blueprint.codigo(),
                blankToNull(uf),
                blankToNull(comarca),
                blankToNull(cnpj),
                blankToNull(esferaAdministrativa),
                sanitizeList(ramosMateriais),
                sanitizeList(abrangenciasTerritoriais),
                blankToNull(dominioInstitucional),
                blankToNull(autoridadeAderenteCargo),
                usuario.getId(),
                representativeRole == null ? InstitutionalNominationRole.ADMINISTRADOR_INSTITUCIONAL : representativeRole,
                blankToNull(emailContatoSeguranca),
                sanitizeList(canaisHabilitados),
                sanitizeList(politicaCiencia),
                sanitizeList(sla),
                sanitizeList(regrasFallback),
                sanitizeList(conveniosIntegracoes),
                trustFloor == null ? blueprintTrustFloor(blueprint) : trustFloor,
                orDefault(requerDuplaAprovacaoAdministrador, blueprint == null || blueprint.requerDuplaAprovacaoAdministrador()),
                orDefault(requerCertificadoICP, blueprint != null && blueprint.requerCertificadoICP()),
                orDefault(restringeCertificadoRedeInstitucional, blueprint == null || blueprint.restringeCertificadoRedeInstitucional()),
                orDefault(permiteUsoRemotoComAutorizacao, blueprint == null || blueprint.permiteUsoRemotoComAutorizacao()),
                InstitutionalAffiliationStatus.SOLICITADA,
                mergeFundamentos(usuario, resolvedScope, blueprint, fundamentos, esferaAdministrativa, ramosMateriais, abrangenciasTerritoriais, autoridadeAderenteCargo),
                now,
                now,
                null
        ));
    }

    public InstitutionalAffiliation homologarAfiliacao(String affiliationId, boolean homologar, List<String> fundamentos) {
        InstitutionalAffiliation current = affiliationRepository.findByAffiliationId(affiliationId)
                .orElseThrow(() -> new IllegalArgumentException("afiliacao_institucional_nao_encontrada"));
        var recognition = publicRecognitionGateApplicationService.avaliarAfiliacao(current);
        if (homologar && "NEGADA".equals(recognition.statusCode())) {
            throw new IllegalStateException("reconhecimento_publico_institucional_negado_para_homologacao");
        }
        InstitutionalAffiliationStatus target = homologar ? InstitutionalAffiliationStatus.HOMOLOGADA : InstitutionalAffiliationStatus.SUSPENSA;
        List<String> mergedFundamentos = appendRecognitionFundamentos(fundamentos, recognition, homologar);
        return affiliationRepository.save(current.withStatus(target, Instant.now(), mergedFundamentos));
    }

    public InstitutionalNomination nomearPessoa(String affiliationId,
                                                Long nominatedUserId,
                                                String nominatedUserName,
                                                TipoUsuario tipoUsuario,
                                                InstitutionalAccessLaneKind accessLaneKind,
                                                InstitutionalNominationRole nominationRole,
                                                FuncaoOperacionalInstitucional funcaoOperacional,
                                                InstitutionalProcessProfile processProfile,
                                                String unidadeCodigo,
                                                String caixaCodigo,
                                                Set<CapacidadeCaixaInstitucional> capacidades,
                                                InstitutionalTrustLevel trustFloor,
                                                InstitutionalEntryLandingPanel panelPreferencial,
                                                Instant ativaDe,
                                                Instant ativaAte,
                                                Boolean requerStepUpMfa,
                                                Boolean requerCertificadoICP,
                                                Boolean requerRedeInstitucional,
                                                Boolean permiteUsoRemotoAutorizado) {
        InstitutionalAffiliation affiliation = affiliationRepository.findByAffiliationId(affiliationId)
                .orElseThrow(() -> new IllegalArgumentException("afiliacao_institucional_nao_encontrada"));
        InstitutionalOrganizationBlueprint blueprint = blueprintCatalogApplicationService.resolve(affiliation.organizationScope(), affiliation.destinatarioKind()).orElse(null);
        InstitutionalAccessLaneBlueprint lane = resolveLaneBlueprint(affiliation, blueprint, accessLaneKind, nominationRole, funcaoOperacional, processProfile);
        FuncaoOperacionalInstitucional resolvedFuncao = funcaoOperacional != null ? funcaoOperacional : lane.funcaoOperacional();
        InstitutionalProcessProfile resolvedProfile = processProfile != null ? processProfile : lane.processProfile();
        InstitutionalNominationRole resolvedRole = nominationRole != null ? nominationRole : lane.nominationRole();
        InstitutionalAccessLaneKind resolvedLane = accessLaneKind != null ? accessLaneKind : lane.laneKind();
        Instant now = Instant.now();
        return nominationRepository.save(new InstitutionalNomination(
                UUID.randomUUID().toString(),
                affiliationId,
                nominatedUserId,
                nominatedUserName,
                tipoUsuario,
                resolvedLane,
                resolvedRole,
                resolvedFuncao,
                resolvedProfile,
                require(unidadeCodigo),
                require(caixaCodigo),
                capacidades == null ? defaultCapacidades(resolvedFuncao, lane) : capacitiesOrCopy(capacidades),
                trustFloor == null ? (lane == null || lane.trustFloor() == null ? affiliation.trustFloor() : lane.trustFloor()) : trustFloor,
                panelPreferencial == null ? defaultPanel(resolvedRole, resolvedFuncao, resolvedProfile, lane) : panelPreferencial,
                InstitutionalNominationStatus.ATIVA,
                ativaDe == null ? now : ativaDe,
                ativaAte,
                orDefault(requerStepUpMfa, lane != null && lane.requerStepUpMfa()),
                orDefault(requerCertificadoICP, lane == null ? affiliation.requerCertificadoICP() : lane.requerCertificadoICP()),
                orDefault(requerRedeInstitucional, lane == null ? affiliation.restringeCertificadoRedeInstitucional() : lane.requerRedeInstitucional()),
                orDefault(permiteUsoRemotoAutorizado, lane == null ? affiliation.permiteUsoRemotoComAutorizacao() : lane.permiteUsoRemotoAutorizado()),
                null,
                now,
                now
        ));
    }

    private List<String> appendRecognitionFundamentos(List<String> fundamentos,
                                                     com.tcc.pjb.backend.model.dto.admin.AdminInstitutionalPublicRecognitionResponse recognition,
                                                     boolean homologar) {
        List<String> extras = new ArrayList<>();
        if (fundamentos != null && !fundamentos.isEmpty()) {
            extras.addAll(fundamentos);
        }
        extras.add("reconhecimento_publico_status=" + recognition.statusCode());
        extras.add("reconhecimento_publico_reconhecida=" + recognition.recognized());
        extras.add("reconhecimento_publico_autoativavel=" + recognition.autoActivatable());
        if (homologar) {
            extras.add("homologacao_solicitada_com_politica_reconhecimento_publico");
        }
        extras.addAll(recognition.blockers().stream().map(item -> "reconhecimento_publico_blocker=" + item).toList());
        return extras;
    }

    public List<InstitutionalAffiliation> listarAfiliacoes() {
        return affiliationRepository.findAll().stream()
                .sorted(Comparator.comparing(InstitutionalAffiliation::updatedAt).reversed())
                .toList();
    }

    public List<InstitutionalNomination> listarNomeacoes(Long userId) {
        List<InstitutionalNomination> source = userId == null ? nominationRepository.findAll() : nominationRepository.findByNominatedUserId(userId);
        return source.stream().sorted(Comparator.comparing(InstitutionalNomination::updatedAt).reversed()).toList();
    }

    public InstitutionalSecureEntrySummary avaliarEntradaSeguraAtual() {
        Usuario usuario = currentUserService.getRequired();
        Instant now = Instant.now();
        List<InstitutionalNomination> nominations = nominationRepository.findByNominatedUserId(usuario.getId()).stream()
                .filter(item -> item.ativaEm(now))
                .toList();
        List<InstitutionalAffiliation> affiliations = affiliationRepository.findByAffiliationIds(nominations.stream()
                        .map(InstitutionalNomination::affiliationId)
                        .toList()).stream()
                .filter(InstitutionalAffiliation::ativa)
                .sorted(Comparator.comparing(InstitutionalAffiliation::updatedAt).reversed())
                .toList();
        InstitutionalNomination preferredNomination = nominations.stream()
                .sorted(Comparator.comparing((InstitutionalNomination item) -> item.trustFloor() == null ? 0 : item.trustFloor().ordem()).reversed()
                        .thenComparing(item -> item.nominationRole().isGestaoMestre() ? 1 : 0, Comparator.reverseOrder()))
                .findFirst()
                .orElse(null);
        InstitutionalAffiliation preferredAffiliation = preferredNomination == null ? null : affiliations.stream().filter(item -> item.affiliationId().equals(preferredNomination.affiliationId())).findFirst().orElse(null);
        InstitutionalTrustAssessment assessment = trustAssessmentService.avaliar(usuario, preferredAffiliation, preferredNomination);
        List<InstitutionalEntryContext> contexts = entryContextApplicationService.resolverContextosAtuais().stream()
                .filter(item -> preferredNomination == null || item.unidadeCodigo().equalsIgnoreCase(preferredNomination.unidadeCodigo()))
                .toList();
        return new InstitutionalSecureEntrySummary(identityBaseProfileResolver.resolve(usuario), assessment, affiliations, nominations, contexts, Instant.now());
    }

    private Set<CapacidadeCaixaInstitucional> defaultCapacidades(FuncaoOperacionalInstitucional funcao, InstitutionalAccessLaneBlueprint lane) {
        if (lane != null && lane.capacidadesPadrao() != null && !lane.capacidadesPadrao().isEmpty()) {
            return EnumSet.copyOf(lane.capacidadesPadrao());
        }
        return switch (funcao) {
            case COORDENADOR_UNIDADE, GESTOR_CAIXA -> EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE, CapacidadeCaixaInstitucional.ATRIBUIR_MEMBRO, CapacidadeCaixaInstitucional.REGISTRAR_SUBSTITUICAO);
            case SERVIDOR_TRIAGEM -> EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.REDISTRIBUIR_INTERNAMENTE, CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA);
            case ASSESSOR_INSTITUCIONAL -> EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.PREPARAR_MINUTA, CapacidadeCaixaInstitucional.ESCALAR_AO_TITULAR);
            case APOIO_TECNICO_SETORIAL -> EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA);
            case SUBSTITUTO, PLANTONISTA, MEMBRO_TITULAR -> EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.DAR_CIENCIA, CapacidadeCaixaInstitucional.PREPARAR_MINUTA, CapacidadeCaixaInstitucional.ASSINAR_MANIFESTACAO, CapacidadeCaixaInstitucional.PETICIONAR_EM_NOME_DO_ORGAO);
        };
    }

    private InstitutionalEntryLandingPanel defaultPanel(InstitutionalNominationRole role,
                                                        FuncaoOperacionalInstitucional funcao,
                                                        InstitutionalProcessProfile profile,
                                                        InstitutionalAccessLaneBlueprint lane) {
        if (lane != null && lane.panel() != null) {
            return lane.panel();
        }
        if (role == InstitutionalNominationRole.DIRETORIA_FORUM) return InstitutionalEntryLandingPanel.PAINEL_DIRETORIA_FORUM;
        if (role == InstitutionalNominationRole.SECRETARIA_FORUM) return InstitutionalEntryLandingPanel.PAINEL_SECRETARIA_FORUM;
        if (role == InstitutionalNominationRole.AGENDADOR_AUDIENCIA || role == InstitutionalNominationRole.AGENDADOR_CONCILIACAO) return InstitutionalEntryLandingPanel.PAINEL_AUDIENCIAS_CONCILIACAO;
        if (profile == InstitutionalProcessProfile.SECRETARIA_FORUM) return InstitutionalEntryLandingPanel.PAINEL_SECRETARIA_FORUM;
        if (profile == InstitutionalProcessProfile.AGENDADOR_AUDIENCIA || profile == InstitutionalProcessProfile.AGENDADOR_CONCILIACAO) return InstitutionalEntryLandingPanel.PAINEL_AUDIENCIAS_CONCILIACAO;
        if (profile == InstitutionalProcessProfile.GESTOR_DELEGACIA || profile == InstitutionalProcessProfile.DELEGADO) return InstitutionalEntryLandingPanel.PAINEL_DELEGACIA;
        if (profile == InstitutionalProcessProfile.GESTOR_UNIDADE_PRISIONAL || profile == InstitutionalProcessProfile.OPERADOR_CUSTODIA_PRISIONAL || profile == InstitutionalProcessProfile.POLICIAL_PENAL) return InstitutionalEntryLandingPanel.PAINEL_CUSTODIA_PRISIONAL;
        if (profile == InstitutionalProcessProfile.PERITO_JUDICIAL || profile == InstitutionalProcessProfile.PSICOLOGO_JUDICIAL || profile == InstitutionalProcessProfile.ASSISTENTE_SOCIAL_JUDICIAL || profile == InstitutionalProcessProfile.CONTADOR_JUDICIAL || profile == InstitutionalProcessProfile.ORGAO_TECNICO_CONVENIADO) {
            return InstitutionalEntryLandingPanel.PAINEL_TECNICO_JUDICIAL;
        }
        if (profile == InstitutionalProcessProfile.COOPERACAO_JUDICIAL) return InstitutionalEntryLandingPanel.PAINEL_COOPERACAO_JUDICIAL;
        return switch (funcao) {
            case COORDENADOR_UNIDADE, GESTOR_CAIXA -> InstitutionalEntryLandingPanel.PAINEL_ORGAO;
            case SERVIDOR_TRIAGEM -> InstitutionalEntryLandingPanel.PAINEL_TRIAGEM;
            case ASSESSOR_INSTITUCIONAL -> InstitutionalEntryLandingPanel.PAINEL_CAIXA;
            case APOIO_TECNICO_SETORIAL -> InstitutionalEntryLandingPanel.PAINEL_APOIO_TECNICO;
            case SUBSTITUTO, PLANTONISTA, MEMBRO_TITULAR -> InstitutionalEntryLandingPanel.PAINEL_TITULAR;
        };
    }

    private InstitutionalAccessLaneBlueprint resolveLaneBlueprint(InstitutionalAffiliation affiliation,
                                                                  InstitutionalOrganizationBlueprint blueprint,
                                                                  InstitutionalAccessLaneKind accessLaneKind,
                                                                  InstitutionalNominationRole role,
                                                                  FuncaoOperacionalInstitucional funcao,
                                                                  InstitutionalProcessProfile profile) {
        Optional<InstitutionalAccessLaneBlueprint> lane = blueprintCatalogApplicationService.resolveLane(
                affiliation.organizationScope(),
                accessLaneKind,
                role,
                funcao,
                profile);
        if (lane.isPresent()) {
            return lane.get();
        }
        if (blueprint != null && !blueprint.lanes().isEmpty()) {
            return blueprint.lanes().getFirst();
        }
        return new InstitutionalAccessLaneBlueprint(
                accessLaneKind == null ? InstitutionalAccessLaneKind.ADMINISTRACAO_MESTRA : accessLaneKind,
                "LANE_GENERICA",
                "Lane genérica institucional",
                role == null ? InstitutionalNominationRole.ADMINISTRADOR_INSTITUCIONAL : role,
                funcao == null ? FuncaoOperacionalInstitucional.COORDENADOR_UNIDADE : funcao,
                profile == null ? InstitutionalProcessProfile.ADMINISTRADOR_INSTITUCIONAL : profile,
                defaultPanel(role, funcao == null ? FuncaoOperacionalInstitucional.COORDENADOR_UNIDADE : funcao, profile == null ? InstitutionalProcessProfile.ADMINISTRADOR_INSTITUCIONAL : profile, null),
                affiliation.trustFloor(),
                EnumSet.of(CapacidadeCaixaInstitucional.VISUALIZAR),
                false,
                affiliation.requerCertificadoICP(),
                affiliation.restringeCertificadoRedeInstitucional(),
                affiliation.permiteUsoRemotoComAutorizacao(),
                List.of(),
                List.of("Fallback de lane genérica para não quebrar a operação.")
        );
    }

    private Set<CapacidadeCaixaInstitucional> capacitiesOrCopy(Set<CapacidadeCaixaInstitucional> capacidades) {
        return capacidades == null || capacidades.isEmpty() ? EnumSet.noneOf(CapacidadeCaixaInstitucional.class) : EnumSet.copyOf(capacidades);
    }

    private InstitutionalTrustLevel blueprintTrustFloor(InstitutionalOrganizationBlueprint blueprint) {
        return blueprint == null || blueprint.trustFloorPadrao() == null
                ? InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA
                : blueprint.trustFloorPadrao();
    }

    private List<String> mergeFundamentos(Usuario usuario,
                                          InstitutionalOrganizationScope scope,
                                          InstitutionalOrganizationBlueprint blueprint,
                                          List<String> fundamentos,
                                          String esferaAdministrativa,
                                          List<String> ramosMateriais,
                                          List<String> abrangenciasTerritoriais,
                                          String autoridadeAderenteCargo) {
        ArrayList<String> out = new ArrayList<>();
        out.add("solicitante=" + usuario.getId());
        out.add("tipo_usuario=" + (usuario.getTipoUsuario() == null ? "NAO_INFORMADO" : usuario.getTipoUsuario().name()));
        if (scope != null) out.add("scope=" + scope.name());
        if (blueprint != null) out.add("blueprint=" + blueprint.codigo());
        if (esferaAdministrativa != null && !esferaAdministrativa.isBlank()) out.add("esfera=" + esferaAdministrativa.trim());
        List<String> ramos = sanitizeList(ramosMateriais);
        if (!ramos.isEmpty()) out.add("ramos=" + String.join(",", ramos));
        List<String> abrangencias = sanitizeList(abrangenciasTerritoriais);
        if (!abrangencias.isEmpty()) out.add("abrangencia_territorial=" + String.join(",", abrangencias));
        if (autoridadeAderenteCargo != null && !autoridadeAderenteCargo.isBlank()) out.add("autoridade_aderente=" + autoridadeAderenteCargo.trim());
        out.add("modelo_responsabilidade=identidade_pessoal_raiz_com_contexto_institucional_delegado");
        if (fundamentos != null) out.addAll(fundamentos);
        return List.copyOf(out);
    }

    private List<String> sanitizeList(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .distinct()
                .toList();
    }

    private boolean orDefault(Boolean requested, boolean defaultValue) {
        return requested == null ? defaultValue : requested;
    }

    private String normalize(String value) {
        String token = require(value).trim().toUpperCase(java.util.Locale.ROOT).replace(' ', '_');
        return token.length() > 80 ? token.substring(0, 80) : token;
    }

    private String require(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("campo_obrigatorio");
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
