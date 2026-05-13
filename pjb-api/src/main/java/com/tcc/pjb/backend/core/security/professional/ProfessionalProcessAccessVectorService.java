package com.tcc.pjb.backend.core.security.professional;

import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.magistratura.delegation.DelegationCredential;
import com.tcc.pjb.backend.core.security.magistratura.delegation.DelegationScope;
import com.tcc.pjb.backend.core.security.sigilo.SigiloCredential;
import com.tcc.pjb.backend.core.security.sigilo.service.SigiloAccessService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.modules.laiane.model.LaianeProcuracaoStatus;
import com.tcc.pjb.backend.modules.laiane.repository.LaianeProcuracaoRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProfessionalProcessAccessVectorService {

    private final LaianeProcuracaoRepository procuracaoRepository;
    private final SigiloAccessService sigiloAccessService;
    private final ProfessionalInstitutionalAccessGrantService grantService;

    public ProfessionalProcessAccessVectorService(LaianeProcuracaoRepository procuracaoRepository,
                                                  SigiloAccessService sigiloAccessService,
                                                  ProfessionalInstitutionalAccessGrantService grantService) {
        this.procuracaoRepository = Objects.requireNonNull(procuracaoRepository);
        this.sigiloAccessService = Objects.requireNonNull(sigiloAccessService);
        this.grantService = Objects.requireNonNull(grantService);
    }

    public ProfessionalProcessAccessVector resolve(Usuario usuario, Processo processo) {
        if (usuario == null || !usuario.isAtivoESemanticoValido()) {
            return denied(ProfessionalActorClass.OUTRO, "Usuário profissional ausente, inativo ou semanticamente inválido.");
        }
        if (processo == null || processo.getId() == null) {
            return denied(actorClass(usuario), "Processo inexistente para avaliação de acesso profissional.");
        }

        ProfessionalActorClass actorClass = actorClass(usuario);
        NivelSigilo sigilo = sigilo(processo);
        boolean publicCase = !sigilo.exigeCredencial();
        boolean sameTerritory = sameTerritory(usuario, processo);
        boolean represented = isRepresented(usuario, processo);
        boolean sigiloCredential = hasValidSigiloCredential(usuario, processo);
        boolean liveDelegation = hasLiveDelegation(usuario);
        ProfessionalInstitutionalAccessGrantService.GrantResolution grants = grantService.resolveApplicable(usuario, processo, actorClass);

        LinkedHashSet<ProfessionalAccessBasis> bases = new LinkedHashSet<>();
        LinkedHashSet<ProfessionalCapability> capabilities = new LinkedHashSet<>();
        LinkedHashSet<ProfessionalDocumentVisibilityScope> scopes = new LinkedHashSet<>();
        LinkedHashSet<ProfessionalDocumentVisibilityScope> restricted = new LinkedHashSet<>();

        seedSearchCapabilities(capabilities);
        restrictDefaultScopes(restricted);

        if (isAdmin(usuario)) {
            bases.add(ProfessionalAccessBasis.ADMINISTRADOR_SISTEMA);
            capabilities.addAll(List.of(
                    ProfessionalCapability.VIEW_PUBLIC_SUMMARY,
                    ProfessionalCapability.VIEW_PUBLIC_ACTS,
                    ProfessionalCapability.VIEW_PUBLIC_DOCUMENTS,
                    ProfessionalCapability.VIEW_PROFESSIONAL_DOCUMENTS,
                    ProfessionalCapability.VIEW_RESTRICTED_PARTY_DOCUMENTS,
                    ProfessionalCapability.VIEW_CONFIDENTIAL_CASE,
                    ProfessionalCapability.VIEW_TIMELINE,
                    ProfessionalCapability.VIEW_DEADLINES,
                    ProfessionalCapability.VIEW_CALENDAR,
                    ProfessionalCapability.USE_JUDICIAL_CALCULATOR,
                    ProfessionalCapability.USE_AI_ASSIST,
                    ProfessionalCapability.WRITE_PRIVATE_NOTES,
                    ProfessionalCapability.VIEW_AUDIT_TRAIL
            ));
            scopes.addAll(List.of(ProfessionalDocumentVisibilityScope.values()));
            return allowed(actorClass, ProfessionalAccessBasis.ADMINISTRADOR_SISTEMA, bases, capabilities, scopes, restricted,
                    false, represented, false, "Acesso administrativo endurecido liberado com trilha obrigatória.");
        }

        return switch (actorClass) {
            case ADVOCACIA -> resolveAdvocacia(usuario, processo, publicCase, represented, sigiloCredential, bases, capabilities, scopes, restricted);
            case DEFENSORIA -> resolveDefensoria(processo, sigilo, publicCase, sameTerritory, grants, bases, capabilities, scopes, restricted);
            case PROCURADORIA -> resolveProcuradoria(processo, sigilo, publicCase, sameTerritory, grants, bases, capabilities, scopes, restricted);
            case MAGISTRATURA -> resolveMagistratura(usuario, processo, sigilo, publicCase, sameTerritory, grants, bases, capabilities, scopes, restricted);
            case APOIO_JUDICIAL -> resolveApoioJudicial(usuario, sigilo, publicCase, sameTerritory, grants, liveDelegation, bases, capabilities, scopes, restricted);
            default -> denied(actorClass, "Perfil profissional não habilitado para o painel forense avançado.");
        };
    }

    private ProfessionalProcessAccessVector resolveAdvocacia(Usuario usuario,
                                                             Processo processo,
                                                             boolean publicCase,
                                                             boolean represented,
                                                             boolean sigiloCredential,
                                                             LinkedHashSet<ProfessionalAccessBasis> bases,
                                                             LinkedHashSet<ProfessionalCapability> capabilities,
                                                             LinkedHashSet<ProfessionalDocumentVisibilityScope> scopes,
                                                             LinkedHashSet<ProfessionalDocumentVisibilityScope> restricted) {
        if (represented) {
            bases.add(ownerBasis(usuario, processo));
            capabilities.addAll(List.of(
                    ProfessionalCapability.VIEW_PUBLIC_SUMMARY,
                    ProfessionalCapability.VIEW_PUBLIC_ACTS,
                    ProfessionalCapability.VIEW_PUBLIC_DOCUMENTS,
                    ProfessionalCapability.VIEW_PROFESSIONAL_DOCUMENTS,
                    ProfessionalCapability.VIEW_RESTRICTED_PARTY_DOCUMENTS,
                    ProfessionalCapability.VIEW_TIMELINE,
                    ProfessionalCapability.VIEW_DEADLINES,
                    ProfessionalCapability.VIEW_CALENDAR,
                    ProfessionalCapability.USE_JUDICIAL_CALCULATOR,
                    ProfessionalCapability.USE_AI_ASSIST,
                    ProfessionalCapability.WRITE_PRIVATE_NOTES,
                    ProfessionalCapability.PETITION_PROTOCOL,
                    ProfessionalCapability.MANAGE_REPRESENTATION,
                    ProfessionalCapability.VIEW_AUDIT_TRAIL
            ));
            scopes.addAll(List.of(
                    ProfessionalDocumentVisibilityScope.PUBLIC_ACT,
                    ProfessionalDocumentVisibilityScope.PUBLIC_DOCUMENT,
                    ProfessionalDocumentVisibilityScope.PROFESSIONAL_NON_MANDATE_VIEW,
                    ProfessionalDocumentVisibilityScope.COUNSEL_REPRESENTED_PARTY,
                    ProfessionalDocumentVisibilityScope.EVIDENCE_RESTRICTED
            ));
            restricted.remove(ProfessionalDocumentVisibilityScope.COUNSEL_REPRESENTED_PARTY);
            restricted.remove(ProfessionalDocumentVisibilityScope.EVIDENCE_RESTRICTED);
            boolean requiresStepUp = sigilo(processo).exigeCredencial() && !sigiloCredential && !publicCase;
            if (sigiloCredential) {
                bases.add(ProfessionalAccessBasis.ADVOGADO_CREDENCIAL_SIGILO);
                capabilities.add(ProfessionalCapability.VIEW_CONFIDENTIAL_CASE);
            }
            return allowed(ProfessionalActorClass.ADVOCACIA, bases.iterator().next(), bases, capabilities, scopes, restricted,
                    requiresStepUp, true, false,
                    requiresStepUp ? "Representação ativa localizada; caso sigiloso exige credencial forte para leitura plena." : "Representação profissional ativa localizada no processo.");
        }
        if (publicCase) {
            bases.add(ProfessionalAccessBasis.PUBLICO_QUALIFICADO_ADVOCACIA);
            capabilities.addAll(List.of(
                    ProfessionalCapability.VIEW_PUBLIC_SUMMARY,
                    ProfessionalCapability.VIEW_PUBLIC_ACTS,
                    ProfessionalCapability.VIEW_PUBLIC_DOCUMENTS,
                    ProfessionalCapability.VIEW_PROFESSIONAL_DOCUMENTS,
                    ProfessionalCapability.VIEW_TIMELINE,
                    ProfessionalCapability.VIEW_AUDIT_TRAIL
            ));
            scopes.addAll(List.of(
                    ProfessionalDocumentVisibilityScope.PUBLIC_ACT,
                    ProfessionalDocumentVisibilityScope.PUBLIC_DOCUMENT,
                    ProfessionalDocumentVisibilityScope.PROFESSIONAL_NON_MANDATE_VIEW
            ));
            return allowed(ProfessionalActorClass.ADVOCACIA, ProfessionalAccessBasis.PUBLICO_QUALIFICADO_ADVOCACIA, bases, capabilities, scopes, restricted,
                    false, false, true,
                    "Advocacia autenticada com leitura qualificada de autos públicos e documentos profissionais não sigilosos.");
        }
        if (sigiloCredential) {
            bases.add(ProfessionalAccessBasis.ADVOGADO_CREDENCIAL_SIGILO);
            capabilities.addAll(List.of(
                    ProfessionalCapability.VIEW_PUBLIC_SUMMARY,
                    ProfessionalCapability.VIEW_PUBLIC_ACTS,
                    ProfessionalCapability.VIEW_PUBLIC_DOCUMENTS,
                    ProfessionalCapability.VIEW_PROFESSIONAL_DOCUMENTS,
                    ProfessionalCapability.VIEW_RESTRICTED_PARTY_DOCUMENTS,
                    ProfessionalCapability.VIEW_CONFIDENTIAL_CASE,
                    ProfessionalCapability.VIEW_TIMELINE,
                    ProfessionalCapability.VIEW_AUDIT_TRAIL
            ));
            scopes.addAll(List.of(
                    ProfessionalDocumentVisibilityScope.PUBLIC_ACT,
                    ProfessionalDocumentVisibilityScope.PUBLIC_DOCUMENT,
                    ProfessionalDocumentVisibilityScope.PROFESSIONAL_NON_MANDATE_VIEW,
                    ProfessionalDocumentVisibilityScope.COUNSEL_REPRESENTED_PARTY
            ));
            restricted.remove(ProfessionalDocumentVisibilityScope.COUNSEL_REPRESENTED_PARTY);
            return allowed(ProfessionalActorClass.ADVOCACIA, ProfessionalAccessBasis.ADVOGADO_CREDENCIAL_SIGILO, bases, capabilities, scopes, restricted,
                    true, false, false,
                    "Credencial temporária de sigilo aceita para leitura profissional controlada em caso não representado.");
        }
        return denied(ProfessionalActorClass.ADVOCACIA, "Autos sigilosos exigem procuração ativa, titularidade ou credencial de sigilo válida.");
    }

    private ProfessionalProcessAccessVector resolveDefensoria(Processo processo,
                                                              NivelSigilo sigilo,
                                                              boolean publicCase,
                                                              boolean sameTerritory,
                                                              ProfessionalInstitutionalAccessGrantService.GrantResolution grants,
                                                              LinkedHashSet<ProfessionalAccessBasis> bases,
                                                              LinkedHashSet<ProfessionalCapability> capabilities,
                                                              LinkedHashSet<ProfessionalDocumentVisibilityScope> scopes,
                                                              LinkedHashSet<ProfessionalDocumentVisibilityScope> restricted) {
        if (grants.hasFormalInstitutionalCoverage()) {
            bases.add(ProfessionalAccessBasis.DEFENSORIA_DESIGNACAO_FORMAL);
            capabilities.addAll(List.of(
                    ProfessionalCapability.VIEW_PUBLIC_SUMMARY,
                    ProfessionalCapability.VIEW_PUBLIC_ACTS,
                    ProfessionalCapability.VIEW_PUBLIC_DOCUMENTS,
                    ProfessionalCapability.VIEW_PROFESSIONAL_DOCUMENTS,
                    ProfessionalCapability.VIEW_RESTRICTED_PARTY_DOCUMENTS,
                    ProfessionalCapability.VIEW_CONFIDENTIAL_CASE,
                    ProfessionalCapability.VIEW_TIMELINE,
                    ProfessionalCapability.VIEW_DEADLINES,
                    ProfessionalCapability.VIEW_CALENDAR,
                    ProfessionalCapability.USE_JUDICIAL_CALCULATOR,
                    ProfessionalCapability.USE_AI_ASSIST,
                    ProfessionalCapability.WRITE_PRIVATE_NOTES,
                    ProfessionalCapability.VIEW_AUDIT_TRAIL
            ));
            scopes.addAll(List.of(
                    ProfessionalDocumentVisibilityScope.PUBLIC_ACT,
                    ProfessionalDocumentVisibilityScope.PUBLIC_DOCUMENT,
                    ProfessionalDocumentVisibilityScope.PROFESSIONAL_NON_MANDATE_VIEW,
                    ProfessionalDocumentVisibilityScope.INSTITUTIONAL_REPRESENTATION,
                    ProfessionalDocumentVisibilityScope.COUNSEL_REPRESENTED_PARTY,
                    ProfessionalDocumentVisibilityScope.EVIDENCE_RESTRICTED
            ));
            restricted.remove(ProfessionalDocumentVisibilityScope.INSTITUTIONAL_REPRESENTATION);
            restricted.remove(ProfessionalDocumentVisibilityScope.COUNSEL_REPRESENTED_PARTY);
            restricted.remove(ProfessionalDocumentVisibilityScope.EVIDENCE_RESTRICTED);
            boolean requiresStepUp = grants.requiresStepUp() || sigilo.getNivel() >= NivelSigilo.SIGILO_N3.getNivel();
            return allowed(ProfessionalActorClass.DEFENSORIA, ProfessionalAccessBasis.DEFENSORIA_DESIGNACAO_FORMAL, bases, capabilities, scopes, restricted,
                    requiresStepUp, true, false,
                    "Defensoria com designação institucional formal, trilha territorial e expansão contextual ligada ao processo.");
        }
        if (publicCase) {
            bases.add(ProfessionalAccessBasis.PUBLICO_QUALIFICADO_DEFENSORIA);
            capabilities.addAll(List.of(
                    ProfessionalCapability.VIEW_PUBLIC_SUMMARY,
                    ProfessionalCapability.VIEW_PUBLIC_ACTS,
                    ProfessionalCapability.VIEW_PUBLIC_DOCUMENTS,
                    ProfessionalCapability.VIEW_PROFESSIONAL_DOCUMENTS,
                    ProfessionalCapability.VIEW_TIMELINE,
                    ProfessionalCapability.VIEW_AUDIT_TRAIL
            ));
            scopes.addAll(List.of(
                    ProfessionalDocumentVisibilityScope.PUBLIC_ACT,
                    ProfessionalDocumentVisibilityScope.PUBLIC_DOCUMENT,
                    ProfessionalDocumentVisibilityScope.PROFESSIONAL_NON_MANDATE_VIEW
            ));
            if (sameTerritory) {
                bases.add(ProfessionalAccessBasis.DEFENSORIA_ATUACAO_TERRITORIAL);
                capabilities.addAll(List.of(
                        ProfessionalCapability.VIEW_DEADLINES,
                        ProfessionalCapability.VIEW_CALENDAR,
                        ProfessionalCapability.USE_JUDICIAL_CALCULATOR,
                        ProfessionalCapability.WRITE_PRIVATE_NOTES
                ));
                scopes.add(ProfessionalDocumentVisibilityScope.INSTITUTIONAL_REPRESENTATION);
                restricted.remove(ProfessionalDocumentVisibilityScope.INSTITUTIONAL_REPRESENTATION);
            }
            return allowed(ProfessionalActorClass.DEFENSORIA, bases.iterator().next(), bases, capabilities, scopes, restricted,
                    false, sameTerritory, !sameTerritory,
                    "Defensoria com painel qualificado para leitura pública e expansão territorial controlada.");
        }
        if (sameTerritory && sigilo.getNivel() < NivelSigilo.SIGILO_N3.getNivel()) {
            bases.add(ProfessionalAccessBasis.DEFENSORIA_ATUACAO_TERRITORIAL);
            capabilities.addAll(List.of(
                    ProfessionalCapability.VIEW_PUBLIC_SUMMARY,
                    ProfessionalCapability.VIEW_PUBLIC_ACTS,
                    ProfessionalCapability.VIEW_PROFESSIONAL_DOCUMENTS,
                    ProfessionalCapability.VIEW_TIMELINE,
                    ProfessionalCapability.VIEW_DEADLINES,
                    ProfessionalCapability.VIEW_CALENDAR,
                    ProfessionalCapability.USE_JUDICIAL_CALCULATOR,
                    ProfessionalCapability.VIEW_AUDIT_TRAIL
            ));
            scopes.addAll(List.of(
                    ProfessionalDocumentVisibilityScope.PUBLIC_ACT,
                    ProfessionalDocumentVisibilityScope.PROFESSIONAL_NON_MANDATE_VIEW,
                    ProfessionalDocumentVisibilityScope.INSTITUTIONAL_REPRESENTATION
            ));
            restricted.remove(ProfessionalDocumentVisibilityScope.INSTITUTIONAL_REPRESENTATION);
            return allowed(ProfessionalActorClass.DEFENSORIA, ProfessionalAccessBasis.DEFENSORIA_ATUACAO_TERRITORIAL, bases, capabilities, scopes, restricted,
                    true, true, false,
                    "Atuação territorial institucional liberada com step-up obrigatório e trilha reforçada.");
        }
        return denied(ProfessionalActorClass.DEFENSORIA, "Defensoria fora do território do feito ou sem designação institucional suficiente para processo sigiloso.");
    }

    private ProfessionalProcessAccessVector resolveProcuradoria(Processo processo,
                                                                NivelSigilo sigilo,
                                                                boolean publicCase,
                                                                boolean sameTerritory,
                                                                ProfessionalInstitutionalAccessGrantService.GrantResolution grants,
                                                                LinkedHashSet<ProfessionalAccessBasis> bases,
                                                                LinkedHashSet<ProfessionalCapability> capabilities,
                                                                LinkedHashSet<ProfessionalDocumentVisibilityScope> scopes,
                                                                LinkedHashSet<ProfessionalDocumentVisibilityScope> restricted) {
        if (grants.hasFormalInstitutionalCoverage()) {
            bases.add(ProfessionalAccessBasis.PROCURADORIA_REPRESENTACAO_FORMAL);
            capabilities.addAll(List.of(
                    ProfessionalCapability.VIEW_PUBLIC_SUMMARY,
                    ProfessionalCapability.VIEW_PUBLIC_ACTS,
                    ProfessionalCapability.VIEW_PUBLIC_DOCUMENTS,
                    ProfessionalCapability.VIEW_PROFESSIONAL_DOCUMENTS,
                    ProfessionalCapability.VIEW_RESTRICTED_PARTY_DOCUMENTS,
                    ProfessionalCapability.VIEW_CONFIDENTIAL_CASE,
                    ProfessionalCapability.VIEW_TIMELINE,
                    ProfessionalCapability.VIEW_DEADLINES,
                    ProfessionalCapability.VIEW_CALENDAR,
                    ProfessionalCapability.USE_JUDICIAL_CALCULATOR,
                    ProfessionalCapability.USE_AI_ASSIST,
                    ProfessionalCapability.WRITE_PRIVATE_NOTES,
                    ProfessionalCapability.VIEW_AUDIT_TRAIL
            ));
            scopes.addAll(List.of(
                    ProfessionalDocumentVisibilityScope.PUBLIC_ACT,
                    ProfessionalDocumentVisibilityScope.PUBLIC_DOCUMENT,
                    ProfessionalDocumentVisibilityScope.PROFESSIONAL_NON_MANDATE_VIEW,
                    ProfessionalDocumentVisibilityScope.INSTITUTIONAL_REPRESENTATION,
                    ProfessionalDocumentVisibilityScope.COUNSEL_REPRESENTED_PARTY,
                    ProfessionalDocumentVisibilityScope.EVIDENCE_RESTRICTED
            ));
            restricted.remove(ProfessionalDocumentVisibilityScope.INSTITUTIONAL_REPRESENTATION);
            restricted.remove(ProfessionalDocumentVisibilityScope.COUNSEL_REPRESENTED_PARTY);
            restricted.remove(ProfessionalDocumentVisibilityScope.EVIDENCE_RESTRICTED);
            boolean requiresStepUp = grants.requiresStepUp() || sigilo.getNivel() >= NivelSigilo.SIGILO_N3.getNivel();
            return allowed(ProfessionalActorClass.PROCURADORIA, ProfessionalAccessBasis.PROCURADORIA_REPRESENTACAO_FORMAL, bases, capabilities, scopes, restricted,
                    requiresStepUp, true, false,
                    "Procuradoria com representação formal do ente, recorte por lotação e costura segura do processo.");
        }
        if (publicCase) {
            bases.add(ProfessionalAccessBasis.PUBLICO_QUALIFICADO_PROCURADORIA);
            capabilities.addAll(List.of(
                    ProfessionalCapability.VIEW_PUBLIC_SUMMARY,
                    ProfessionalCapability.VIEW_PUBLIC_ACTS,
                    ProfessionalCapability.VIEW_PUBLIC_DOCUMENTS,
                    ProfessionalCapability.VIEW_PROFESSIONAL_DOCUMENTS,
                    ProfessionalCapability.VIEW_TIMELINE,
                    ProfessionalCapability.VIEW_AUDIT_TRAIL
            ));
            scopes.addAll(List.of(
                    ProfessionalDocumentVisibilityScope.PUBLIC_ACT,
                    ProfessionalDocumentVisibilityScope.PUBLIC_DOCUMENT,
                    ProfessionalDocumentVisibilityScope.PROFESSIONAL_NON_MANDATE_VIEW
            ));
            if (sameTerritory) {
                bases.add(ProfessionalAccessBasis.PROCURADORIA_ATUACAO_TERRITORIAL);
                capabilities.addAll(List.of(
                        ProfessionalCapability.VIEW_DEADLINES,
                        ProfessionalCapability.VIEW_CALENDAR,
                        ProfessionalCapability.USE_JUDICIAL_CALCULATOR,
                        ProfessionalCapability.WRITE_PRIVATE_NOTES
                ));
                scopes.add(ProfessionalDocumentVisibilityScope.INSTITUTIONAL_REPRESENTATION);
                restricted.remove(ProfessionalDocumentVisibilityScope.INSTITUTIONAL_REPRESENTATION);
            }
            return allowed(ProfessionalActorClass.PROCURADORIA, bases.iterator().next(), bases, capabilities, scopes, restricted,
                    false, sameTerritory, !sameTerritory,
                    "Procuradoria com leitura pública qualificada e expansão territorial institucional controlada.");
        }
        if (sameTerritory && sigilo.getNivel() < NivelSigilo.SIGILO_N3.getNivel()) {
            bases.add(ProfessionalAccessBasis.PROCURADORIA_ATUACAO_TERRITORIAL);
            capabilities.addAll(List.of(
                    ProfessionalCapability.VIEW_PUBLIC_SUMMARY,
                    ProfessionalCapability.VIEW_PUBLIC_ACTS,
                    ProfessionalCapability.VIEW_PROFESSIONAL_DOCUMENTS,
                    ProfessionalCapability.VIEW_TIMELINE,
                    ProfessionalCapability.VIEW_DEADLINES,
                    ProfessionalCapability.VIEW_CALENDAR,
                    ProfessionalCapability.USE_JUDICIAL_CALCULATOR,
                    ProfessionalCapability.VIEW_AUDIT_TRAIL
            ));
            scopes.addAll(List.of(
                    ProfessionalDocumentVisibilityScope.PUBLIC_ACT,
                    ProfessionalDocumentVisibilityScope.PROFESSIONAL_NON_MANDATE_VIEW,
                    ProfessionalDocumentVisibilityScope.INSTITUTIONAL_REPRESENTATION
            ));
            restricted.remove(ProfessionalDocumentVisibilityScope.INSTITUTIONAL_REPRESENTATION);
            return allowed(ProfessionalActorClass.PROCURADORIA, ProfessionalAccessBasis.PROCURADORIA_ATUACAO_TERRITORIAL, bases, capabilities, scopes, restricted,
                    true, true, false,
                    "Atuação fazendária territorial liberada com trilha reforçada.");
        }
        return denied(ProfessionalActorClass.PROCURADORIA, "Procuradoria fora do território do feito ou sem representação institucional suficiente.");
    }

    private ProfessionalProcessAccessVector resolveMagistratura(Usuario usuario,
                                                                Processo processo,
                                                                NivelSigilo sigilo,
                                                                boolean publicCase,
                                                                boolean sameTerritory,
                                                                ProfessionalInstitutionalAccessGrantService.GrantResolution grants,
                                                                LinkedHashSet<ProfessionalAccessBasis> bases,
                                                                LinkedHashSet<ProfessionalCapability> capabilities,
                                                                LinkedHashSet<ProfessionalDocumentVisibilityScope> scopes,
                                                                LinkedHashSet<ProfessionalDocumentVisibilityScope> restricted) {
        ProfessionalAccessBasis formalBasis = null;
        if (grants.relatoria()) {
            formalBasis = ProfessionalAccessBasis.MAGISTRATURA_RELATORIA_ATIVA;
        } else if (grants.colegiado()) {
            formalBasis = ProfessionalAccessBasis.MAGISTRATURA_COLEGIADO_ATIVO;
        } else if (grants.substituicao()) {
            formalBasis = ProfessionalAccessBasis.MAGISTRATURA_SUBSTITUICAO_ATIVA;
        } else if (grants.plantao()) {
            formalBasis = ProfessionalAccessBasis.MAGISTRATURA_PLANTAO_ATIVO;
        }
        if (formalBasis != null) {
            bases.add(formalBasis);
            capabilities.addAll(List.of(
                    ProfessionalCapability.VIEW_PUBLIC_SUMMARY,
                    ProfessionalCapability.VIEW_PUBLIC_ACTS,
                    ProfessionalCapability.VIEW_PUBLIC_DOCUMENTS,
                    ProfessionalCapability.VIEW_PROFESSIONAL_DOCUMENTS,
                    ProfessionalCapability.VIEW_RESTRICTED_PARTY_DOCUMENTS,
                    ProfessionalCapability.VIEW_CONFIDENTIAL_CASE,
                    ProfessionalCapability.VIEW_TIMELINE,
                    ProfessionalCapability.VIEW_DEADLINES,
                    ProfessionalCapability.VIEW_CALENDAR,
                    ProfessionalCapability.USE_JUDICIAL_CALCULATOR,
                    ProfessionalCapability.USE_AI_ASSIST,
                    ProfessionalCapability.WRITE_PRIVATE_NOTES,
                    ProfessionalCapability.SIGN_JUDICIAL_ACT,
                    ProfessionalCapability.VIEW_AUDIT_TRAIL
            ));
            scopes.addAll(List.of(
                    ProfessionalDocumentVisibilityScope.PUBLIC_ACT,
                    ProfessionalDocumentVisibilityScope.PUBLIC_DOCUMENT,
                    ProfessionalDocumentVisibilityScope.PROFESSIONAL_NON_MANDATE_VIEW,
                    ProfessionalDocumentVisibilityScope.COUNSEL_REPRESENTED_PARTY,
                    ProfessionalDocumentVisibilityScope.INSTITUTIONAL_REPRESENTATION,
                    ProfessionalDocumentVisibilityScope.COURT_INTERNAL,
                    ProfessionalDocumentVisibilityScope.EVIDENCE_RESTRICTED
            ));
            restricted.remove(ProfessionalDocumentVisibilityScope.COURT_INTERNAL);
            if (formalBasis == ProfessionalAccessBasis.MAGISTRATURA_COLEGIADO_ATIVO || isSecondInstanceMagistrature(usuario)) {
                scopes.add(ProfessionalDocumentVisibilityScope.CHAMBER_INTERNAL);
                restricted.remove(ProfessionalDocumentVisibilityScope.CHAMBER_INTERNAL);
            }
            boolean requiresStepUp = grants.requiresStepUp() && sigilo.getNivel() >= NivelSigilo.SIGILO_N2.getNivel();
            return allowed(ProfessionalActorClass.MAGISTRATURA, formalBasis, bases, capabilities, scopes, restricted,
                    requiresStepUp, false, false,
                    "Magistratura com competência formal vinculada ao processo, gabinete ou colegiado, sem depender apenas de território presumido.");
        }
        if (sameTerritory || publicCase) {
            ProfessionalAccessBasis basis = sameTerritory
                    ? ProfessionalAccessBasis.MAGISTRATURA_COMPETENCIA_TERRITORIAL
                    : ProfessionalAccessBasis.MAGISTRATURA_PUBLICO_AMPLIADO;
            bases.add(basis);
            capabilities.addAll(List.of(
                    ProfessionalCapability.VIEW_PUBLIC_SUMMARY,
                    ProfessionalCapability.VIEW_PUBLIC_ACTS,
                    ProfessionalCapability.VIEW_PUBLIC_DOCUMENTS,
                    ProfessionalCapability.VIEW_PROFESSIONAL_DOCUMENTS,
                    ProfessionalCapability.VIEW_TIMELINE,
                    ProfessionalCapability.VIEW_AUDIT_TRAIL,
                    ProfessionalCapability.USE_AI_ASSIST,
                    ProfessionalCapability.WRITE_PRIVATE_NOTES
            ));
            scopes.addAll(List.of(
                    ProfessionalDocumentVisibilityScope.PUBLIC_ACT,
                    ProfessionalDocumentVisibilityScope.PUBLIC_DOCUMENT,
                    ProfessionalDocumentVisibilityScope.PROFESSIONAL_NON_MANDATE_VIEW
            ));
            if (sameTerritory) {
                capabilities.addAll(List.of(
                        ProfessionalCapability.VIEW_RESTRICTED_PARTY_DOCUMENTS,
                        ProfessionalCapability.VIEW_CONFIDENTIAL_CASE,
                        ProfessionalCapability.VIEW_DEADLINES,
                        ProfessionalCapability.VIEW_CALENDAR,
                        ProfessionalCapability.USE_JUDICIAL_CALCULATOR,
                        ProfessionalCapability.SIGN_JUDICIAL_ACT
                ));
                scopes.addAll(List.of(
                        ProfessionalDocumentVisibilityScope.COUNSEL_REPRESENTED_PARTY,
                        ProfessionalDocumentVisibilityScope.INSTITUTIONAL_REPRESENTATION,
                        ProfessionalDocumentVisibilityScope.COURT_INTERNAL,
                        ProfessionalDocumentVisibilityScope.CHAMBER_INTERNAL,
                        ProfessionalDocumentVisibilityScope.EVIDENCE_RESTRICTED
                ));
                restricted.remove(ProfessionalDocumentVisibilityScope.COURT_INTERNAL);
                restricted.remove(ProfessionalDocumentVisibilityScope.CHAMBER_INTERNAL);
            }
            return allowed(ProfessionalActorClass.MAGISTRATURA, basis, bases, capabilities, scopes, restricted,
                    sigilo.exigeCredencial() && !sameTerritory, false, !sameTerritory,
                    sameTerritory ? "Magistratura com competência territorial presumida no feito." : "Magistratura com leitura pública ampliada fora da competência territorial presumida.");
        }
        return denied(ProfessionalActorClass.MAGISTRATURA, "Magistratura sem competência formal nem territorial suficiente para autos sigilosos.");
    }

    private ProfessionalProcessAccessVector resolveApoioJudicial(Usuario usuario,
                                                                 NivelSigilo sigilo,
                                                                 boolean publicCase,
                                                                 boolean sameTerritory,
                                                                 ProfessionalInstitutionalAccessGrantService.GrantResolution grants,
                                                                 boolean liveDelegation,
                                                                 LinkedHashSet<ProfessionalAccessBasis> bases,
                                                                 LinkedHashSet<ProfessionalCapability> capabilities,
                                                                 LinkedHashSet<ProfessionalDocumentVisibilityScope> scopes,
                                                                 LinkedHashSet<ProfessionalDocumentVisibilityScope> restricted) {
        if (liveDelegation || grants.delegatedCabinet()) {
            bases.add(ProfessionalAccessBasis.APOIO_JUDICIAL_DELEGACAO_FORMAL);
            capabilities.addAll(List.of(
                    ProfessionalCapability.VIEW_PUBLIC_SUMMARY,
                    ProfessionalCapability.VIEW_PUBLIC_ACTS,
                    ProfessionalCapability.VIEW_PUBLIC_DOCUMENTS,
                    ProfessionalCapability.VIEW_PROFESSIONAL_DOCUMENTS,
                    ProfessionalCapability.VIEW_TIMELINE,
                    ProfessionalCapability.VIEW_DEADLINES,
                    ProfessionalCapability.VIEW_CALENDAR,
                    ProfessionalCapability.WRITE_PRIVATE_NOTES,
                    ProfessionalCapability.VIEW_AUDIT_TRAIL
            ));
            scopes.addAll(List.of(
                    ProfessionalDocumentVisibilityScope.PUBLIC_ACT,
                    ProfessionalDocumentVisibilityScope.PUBLIC_DOCUMENT,
                    ProfessionalDocumentVisibilityScope.PROFESSIONAL_NON_MANDATE_VIEW,
                    ProfessionalDocumentVisibilityScope.COURT_INTERNAL
            ));
            restricted.remove(ProfessionalDocumentVisibilityScope.COURT_INTERNAL);
            if (allowsDrafts()) {
                scopes.add(ProfessionalDocumentVisibilityScope.PRIVATE_DRAFT);
                restricted.remove(ProfessionalDocumentVisibilityScope.PRIVATE_DRAFT);
                capabilities.add(ProfessionalCapability.USE_AI_ASSIST);
            }
            if (usuario.getTipoUsuario() == TipoUsuario.ASSESSOR_DESEMBARGADOR || usuario.getTipoUsuario() == TipoUsuario.ASSESSOR_MINISTRO || grants.colegiado()) {
                scopes.add(ProfessionalDocumentVisibilityScope.CHAMBER_INTERNAL);
                restricted.remove(ProfessionalDocumentVisibilityScope.CHAMBER_INTERNAL);
            }
            boolean requiresStepUp = grants.requiresStepUp() || (sigilo.getNivel() >= NivelSigilo.SIGILO_N2.getNivel() && !sameTerritory);
            return allowed(ProfessionalActorClass.APOIO_JUDICIAL, ProfessionalAccessBasis.APOIO_JUDICIAL_DELEGACAO_FORMAL, bases, capabilities, scopes, restricted,
                    requiresStepUp, false, false,
                    "Apoio judicial com delegação formal de gabinete, respeitando escopo de minuta e perímetro da unidade.");
        }
        if (publicCase && sameTerritory) {
            bases.add(ProfessionalAccessBasis.MAGISTRATURA_PUBLICO_AMPLIADO);
            capabilities.addAll(List.of(
                    ProfessionalCapability.VIEW_PUBLIC_SUMMARY,
                    ProfessionalCapability.VIEW_PUBLIC_ACTS,
                    ProfessionalCapability.VIEW_PUBLIC_DOCUMENTS,
                    ProfessionalCapability.VIEW_TIMELINE,
                    ProfessionalCapability.VIEW_AUDIT_TRAIL
            ));
            scopes.addAll(List.of(
                    ProfessionalDocumentVisibilityScope.PUBLIC_ACT,
                    ProfessionalDocumentVisibilityScope.PUBLIC_DOCUMENT
            ));
            return allowed(ProfessionalActorClass.APOIO_JUDICIAL, ProfessionalAccessBasis.MAGISTRATURA_PUBLICO_AMPLIADO, bases, capabilities, scopes, restricted,
                    false, false, true,
                    "Apoio judicial sem delegação formal limitado à camada pública qualificada da própria unidade territorial.");
        }
        return denied(ProfessionalActorClass.APOIO_JUDICIAL, "Apoio judicial exige delegação formal de gabinete para abrir documentação interna ou autos sigilosos.");
    }

    public ProfessionalActorClass actorClass(Usuario usuario) {
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return usuario != null && usuario.isAdvogado() ? ProfessionalActorClass.ADVOCACIA : ProfessionalActorClass.OUTRO;
        }
        TipoUsuario tipo = usuario.getTipoUsuario();
        if (tipo.isMagistratura()) {
            return ProfessionalActorClass.MAGISTRATURA;
        }
        if (tipo.isDefensoriaPublica()) {
            return ProfessionalActorClass.DEFENSORIA;
        }
        if (tipo.isProcuradoria()) {
            return ProfessionalActorClass.PROCURADORIA;
        }
        if (tipo.isServidorJudiciario() || tipo.isAssessor()) {
            return ProfessionalActorClass.APOIO_JUDICIAL;
        }
        if (tipo.isAdvocacia() || usuario.isAdvogado()) {
            return ProfessionalActorClass.ADVOCACIA;
        }
        return ProfessionalActorClass.OUTRO;
    }

    private ProfessionalAccessBasis ownerBasis(Usuario usuario, Processo processo) {
        if (processo.getUsuario() != null && Objects.equals(processo.getUsuario().getId(), usuario.getId())) {
            return ProfessionalAccessBasis.ADVOGADO_TITULAR_PROCESSO;
        }
        return ProfessionalAccessBasis.ADVOGADO_PROCURACAO_ATIVA;
    }

    private boolean isRepresented(Usuario usuario, Processo processo) {
        if (usuario == null || usuario.getId() == null || processo == null || processo.getId() == null) {
            return false;
        }
        if (processo.getUsuario() != null && Objects.equals(processo.getUsuario().getId(), usuario.getId())) {
            return true;
        }
        return procuracaoRepository.existsByAdvogadoIdAndProcessoIdAndStatus(usuario.getId(), processo.getId(), LaianeProcuracaoStatus.ATIVA);
    }

    private boolean hasValidSigiloCredential(Usuario usuario, Processo processo) {
        if (usuario == null || usuario.getId() == null || processo == null || processo.getId() == null) {
            return false;
        }
        SigiloCredential credential = RequestContext.getSigiloCredential().orElse(null);
        return credential != null && sigiloAccessService.validarCredencial(processo.getId(), usuario.getId(), credential);
    }

    private boolean hasLiveDelegation(Usuario usuario) {
        if (usuario == null || usuario.getId() == null) {
            return false;
        }
        DelegationCredential credential = RequestContext.getDelegationCredential().orElse(null);
        return credential != null && Objects.equals(credential.delegateId(), usuario.getId());
    }

    private boolean allowsDrafts() {
        DelegationCredential credential = RequestContext.getDelegationCredential().orElse(null);
        DelegationScope scope = credential == null ? null : credential.scope();
        return scope != null && scope.canDraft();
    }

    private boolean sameTerritory(Usuario usuario, Processo processo) {
        if (usuario == null || processo == null) {
            return false;
        }
        String userUf = normalize(usuario.getUf());
        String procUf = normalize(processo.getUf());
        String userComarca = normalize(usuario.getComarca());
        String procComarca = normalize(processo.getComarca());
        if (userUf == null || procUf == null) {
            return false;
        }
        if (!userUf.equals(procUf)) {
            return false;
        }
        return userComarca == null || procComarca == null || userComarca.equals(procComarca);
    }

    private NivelSigilo sigilo(Processo processo) {
        return processo == null || processo.getNivelSigilo() == null ? NivelSigilo.PUBLICO : processo.getNivelSigilo();
    }

    private boolean isAdmin(Usuario usuario) {
        return usuario != null && usuario.getTipoUsuario() != null && usuario.getTipoUsuario().isAdmin();
    }

    private boolean isSecondInstanceMagistrature(Usuario usuario) {
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return false;
        }
        return switch (usuario.getTipoUsuario()) {
            case DESEMBARGADOR, DESEMBARGADOR_FEDERAL, MINISTRO -> true;
            default -> false;
        };
    }

    private void seedSearchCapabilities(LinkedHashSet<ProfessionalCapability> capabilities) {
        capabilities.addAll(List.of(
                ProfessionalCapability.SEARCH_BY_NAME,
                ProfessionalCapability.SEARCH_BY_CPF,
                ProfessionalCapability.SEARCH_BY_PROCESS_NUMBER
        ));
    }

    private void restrictDefaultScopes(LinkedHashSet<ProfessionalDocumentVisibilityScope> restricted) {
        restricted.addAll(List.of(
                ProfessionalDocumentVisibilityScope.COUNSEL_REPRESENTED_PARTY,
                ProfessionalDocumentVisibilityScope.INSTITUTIONAL_REPRESENTATION,
                ProfessionalDocumentVisibilityScope.COURT_INTERNAL,
                ProfessionalDocumentVisibilityScope.CHAMBER_INTERNAL,
                ProfessionalDocumentVisibilityScope.PRIVATE_DRAFT,
                ProfessionalDocumentVisibilityScope.EVIDENCE_RESTRICTED
        ));
    }

    private ProfessionalProcessAccessVector allowed(ProfessionalActorClass actorClass,
                                                    ProfessionalAccessBasis primaryBasis,
                                                    LinkedHashSet<ProfessionalAccessBasis> bases,
                                                    LinkedHashSet<ProfessionalCapability> capabilities,
                                                    LinkedHashSet<ProfessionalDocumentVisibilityScope> scopes,
                                                    LinkedHashSet<ProfessionalDocumentVisibilityScope> restricted,
                                                    boolean requiresStepUp,
                                                    boolean represented,
                                                    boolean publicOnly,
                                                    String reason) {
        if (primaryBasis != null && !bases.contains(primaryBasis)) {
            bases.add(primaryBasis);
        }
        return new ProfessionalProcessAccessVector(true, actorClass.panelMode(), actorClass,
                primaryBasis == null ? ProfessionalAccessBasis.NENHUM : primaryBasis,
                List.copyOf(bases), List.copyOf(capabilities), List.copyOf(scopes), List.copyOf(restricted),
                requiresStepUp, represented, publicOnly, reason);
    }

    private ProfessionalProcessAccessVector denied(ProfessionalActorClass actorClass, String reason) {
        List<ProfessionalCapability> capabilities = List.of(
                ProfessionalCapability.SEARCH_BY_NAME,
                ProfessionalCapability.SEARCH_BY_CPF,
                ProfessionalCapability.SEARCH_BY_PROCESS_NUMBER
        );
        List<ProfessionalDocumentVisibilityScope> restricted = List.of(ProfessionalDocumentVisibilityScope.values());
        return new ProfessionalProcessAccessVector(false, actorClass.panelMode(), actorClass,
                ProfessionalAccessBasis.NENHUM, List.of(ProfessionalAccessBasis.NENHUM), capabilities,
                List.of(), restricted, false, false, false, reason);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }
}
