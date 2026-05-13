package com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalNomination;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalTrustAssessment;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalSessionSecuritySignalService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.application.InstitutionalIdentityBaseProfileResolverApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalIdentityBaseProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalSessionSecuritySignalService.InstitutionalSessionSecuritySignal;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.application.InstitutionalRemoteCertificateAuthorizationApplicationService;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalEntryMode;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalSecurityFactor;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalTrustAssessmentApplicationService {

    private final InstitutionalSessionSecuritySignalService signalService;
    private final InstitutionalIdentityBaseProfileResolverApplicationService identityBaseProfileResolver;
    private final InstitutionalRemoteCertificateAuthorizationApplicationService remoteCertificateAuthorizationApplicationService;

    public InstitutionalTrustAssessmentApplicationService(InstitutionalSessionSecuritySignalService signalService,
                                                          InstitutionalIdentityBaseProfileResolverApplicationService identityBaseProfileResolver,
                                                          InstitutionalRemoteCertificateAuthorizationApplicationService remoteCertificateAuthorizationApplicationService) {
        this.signalService = Objects.requireNonNull(signalService);
        this.identityBaseProfileResolver = Objects.requireNonNull(identityBaseProfileResolver);
        this.remoteCertificateAuthorizationApplicationService = Objects.requireNonNull(remoteCertificateAuthorizationApplicationService);
    }

    public InstitutionalTrustAssessment avaliar(Usuario usuario,
                                                InstitutionalAffiliation affiliation,
                                                InstitutionalNomination nomination) {
        InstitutionalSessionSecuritySignal signal = signalService.collect(usuario);
        InstitutionalIdentityBaseProfile identityBase = identityBaseProfileResolver.resolve(usuario);
        EnumSet<InstitutionalSecurityFactor> factors = EnumSet.noneOf(InstitutionalSecurityFactor.class);
        ArrayList<String> reasons = new ArrayList<>();
        if (signal.govBrNivel() != null && signal.govBrNivel() != com.tcc.pjb.backend.model.entity.identity.IdentidadeJuridicaNacional.GovBrNivel.NAO_VINCULADO) {
            factors.add(InstitutionalSecurityFactor.LOGIN_GOVBR);
        }
        if (signal.loginInstitucionalGerenciado()) {
            factors.add(InstitutionalSecurityFactor.LOGIN_INSTITUCIONAL_GERENCIADO);
        }
        if (signal.govBrPrataOuOuro()) {
            factors.add(InstitutionalSecurityFactor.GOVBR_PRATA_OURO);
        } else {
            reasons.add("Conta gov.br abaixo de Prata/Ouro para contexto institucional mais sensível.");
        }
        if (signal.mfaAtivo()) {
            factors.add(InstitutionalSecurityFactor.MFA_ATIVO);
        } else if (nomination != null && nomination.requerStepUpMfa()) {
            reasons.add("Step-up/MFA ainda não confirmado na sessão atual.");
        }
        if (nomination != null && nomination.ativaEm(Instant.now())) {
            factors.add(InstitutionalSecurityFactor.NOMEACAO_ATIVA);
        } else if (nomination != null || affiliation != null) {
            reasons.add("Não há nomeação institucional ativa para o usuário no contexto solicitado.");
        }
        if (affiliation != null && affiliation.ativa()) {
            factors.add(InstitutionalSecurityFactor.AFILIACAO_HOMOLOGADA);
        } else if (nomination != null || affiliation != null) {
            reasons.add("Afiliação institucional ainda não homologada pelo PJB.");
        }
        if (signal.certificadoICPDetectado()) {
            factors.add(InstitutionalSecurityFactor.CERTIFICADO_ICP_BRASIL);
        } else if ((affiliation != null && affiliation.requerCertificadoICP()) || (nomination != null && nomination.requerCertificadoICP())) {
            reasons.add("Certificado ICP-Brasil não detectado na sessão atual.");
        }
        if (signal.redeInstitucionalConfiavel()) {
            factors.add(InstitutionalSecurityFactor.REDE_INSTITUCIONAL_CONFIAVEL);
        }
        boolean remoteAuthorizationActive = signal.autorizacaoRemotaCertificado()
                || (usuario != null && affiliation != null && remoteCertificateAuthorizationApplicationService.possuiAutorizacaoAtiva(usuario.getId(), affiliation.affiliationId()));
        if (remoteAuthorizationActive) {
            factors.add(InstitutionalSecurityFactor.AUTORIZACAO_REMOTA_CERTIFICADO);
        }
        if (signal.dispositivoHomologado()) {
            factors.add(InstitutionalSecurityFactor.DISPOSITIVO_HOMOLOGADO);
        }
        InstitutionalTrustLevel trustLevel = calcularNivel(signal, nomination, affiliation, remoteAuthorizationActive);
        InstitutionalTrustLevel min = nomination != null && nomination.trustFloor() != null
                ? nomination.trustFloor()
                : affiliation != null && affiliation.trustFloor() != null
                    ? affiliation.trustFloor()
                    : InstitutionalTrustLevel.NIVEL_1_IDENTIDADE_FEDERADA;
        boolean certificadoPermitido = !((affiliation != null && affiliation.restringeCertificadoRedeInstitucional()) || (nomination != null && nomination.requerRedeInstitucional()))
                || signal.redeInstitucionalConfiavel()
                || remoteAuthorizationActive;
        if (!certificadoPermitido) {
            reasons.add("Uso do certificado fora da rede institucional depende de autorização remota válida.");
        }
        boolean directMode = nomination == null && affiliation == null;
        boolean directFlowAllowed = identityBase != null && identityBase.possuiFluxoDireto();
        if (!directMode && !factors.contains(InstitutionalSecurityFactor.LOGIN_GOVBR)) {
            reasons.add("Contexto institucional exige identidade pessoal raiz validada via gov.br.");
        }
        if (!directMode && !factors.contains(InstitutionalSecurityFactor.GOVBR_PRATA_OURO)) {
            reasons.add("Contexto institucional exige conta gov.br em nível prata ou ouro para trilha de responsabilização forte.");
        }
        boolean allowed = directMode
                ? directFlowAllowed && (factors.contains(InstitutionalSecurityFactor.LOGIN_GOVBR) || factors.contains(InstitutionalSecurityFactor.CERTIFICADO_ICP_BRASIL))
                : trustLevel.atende(min)
                    && factors.contains(InstitutionalSecurityFactor.LOGIN_GOVBR)
                    && factors.contains(InstitutionalSecurityFactor.GOVBR_PRATA_OURO)
                    && factors.contains(InstitutionalSecurityFactor.NOMEACAO_ATIVA)
                    && factors.contains(InstitutionalSecurityFactor.AFILIACAO_HOMOLOGADA)
                    && certificadoPermitido;
        if (directMode && !directFlowAllowed) {
            reasons.add("O tipo de usuário autenticado exige vínculo institucional homologado para atuação contextual.");
        }
        if (!allowed && reasons.isEmpty()) {
            reasons.add("O contexto institucional não atingiu o piso mínimo de confiabilidade exigido.");
        }
        return new InstitutionalTrustAssessment(
                usuario == null ? null : usuario.getId(),
                usuario == null ? null : usuario.getNome(),
                nomination != null || affiliation != null
                        ? InstitutionalEntryMode.INSTITUCIONAL_AFILIADO
                        : identityBase == null || identityBase.entryModePreferencial() == null
                            ? InstitutionalEntryMode.DIRETO_PESSOA
                            : identityBase.entryModePreferencial(),
                affiliation == null ? null : affiliation.affiliationId(),
                nomination == null ? null : nomination.nominationId(),
                trustLevel,
                Set.copyOf(factors),
                signal.redeInstitucionalConfiavel(),
                signal.loginInstitucionalGerenciado(),
                remoteAuthorizationActive,
                certificadoPermitido,
                signal.mfaAtivo(),
                allowed,
                nomination == null ? null : nomination.panelPreferencial(),
                List.copyOf(reasons),
                Instant.now()
        );
    }

    private InstitutionalTrustLevel calcularNivel(InstitutionalSessionSecuritySignal signal,
                                                  InstitutionalNomination nomination,
                                                  InstitutionalAffiliation affiliation,
                                                  boolean remoteAuthorizationActive) {
        boolean nomeacaoAtiva = nomination != null && nomination.ativaEm(Instant.now());
        boolean afiliacaoAtiva = affiliation != null && affiliation.ativa();
        if (signal.certificadoICPDetectado() && nomeacaoAtiva && afiliacaoAtiva
                && (signal.redeInstitucionalConfiavel() || remoteAuthorizationActive)) {
            return InstitutionalTrustLevel.NIVEL_4_AMBIENTE_INSTITUCIONAL_RESTRITO;
        }
        if (signal.certificadoICPDetectado() && nomeacaoAtiva && afiliacaoAtiva) {
            return InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO;
        }
        if (nomeacaoAtiva && afiliacaoAtiva && signal.govBrPrataOuOuro()) {
            return InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA;
        }
        return InstitutionalTrustLevel.NIVEL_1_IDENTIDADE_FEDERADA;
    }
}
