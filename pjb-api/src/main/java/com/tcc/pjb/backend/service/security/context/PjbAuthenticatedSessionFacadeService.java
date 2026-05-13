package com.tcc.pjb.backend.service.security.context;

import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalRequestContextKeys;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.application.InstitutionalEntryContextApplicationService;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryActivationDecision;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntrySummary;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalHorizontalDataPlanePlan;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.core.identity.govbr.application.GovBrIdentityAssuranceApplicationService;
import com.tcc.pjb.backend.core.identity.govbr.domain.GovBrIdentityAssuranceAggregate;
import com.tcc.pjb.backend.core.security.context.CurrentAuthenticationContext;
import com.tcc.pjb.backend.core.security.context.CurrentAuthenticationContextService;
import com.tcc.pjb.backend.model.dto.security.context.PjbAuthenticatedSessionResponse;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.state.NationalCommunicationInstitutionalStateBundle;
import com.tcc.pjb.backend.service.processual.comunicacao.institutional.state.NationalCommunicationInstitutionalStateBundleFacadeService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class PjbAuthenticatedSessionFacadeService {

    private final CurrentAuthenticationContextService currentAuthenticationContextService;
    private final GovBrIdentityAssuranceApplicationService govBrIdentityAssuranceApplicationService;
    private final InstitutionalEntryContextApplicationService entryContextApplicationService;
    private final NationalCommunicationInstitutionalStateBundleFacadeService stateBundleFacadeService;

    public PjbAuthenticatedSessionFacadeService(CurrentAuthenticationContextService currentAuthenticationContextService,
                                                GovBrIdentityAssuranceApplicationService govBrIdentityAssuranceApplicationService,
                                                InstitutionalEntryContextApplicationService entryContextApplicationService,
                                                NationalCommunicationInstitutionalStateBundleFacadeService stateBundleFacadeService) {
        this.currentAuthenticationContextService = Objects.requireNonNull(currentAuthenticationContextService);
        this.govBrIdentityAssuranceApplicationService = Objects.requireNonNull(govBrIdentityAssuranceApplicationService);
        this.entryContextApplicationService = Objects.requireNonNull(entryContextApplicationService);
        this.stateBundleFacadeService = Objects.requireNonNull(stateBundleFacadeService);
    }

    public PjbAuthenticatedSessionResponse atual() {
        InstitutionalEntrySummary summary = entryContextApplicationService.resolverEntradaAtual();
        return atual(summary, stateBundleFacadeService.carregar(summary));
    }

    public PjbAuthenticatedSessionResponse atual(InstitutionalEntrySummary summary,
                                                 NationalCommunicationInstitutionalStateBundle stateBundle) {
        InstitutionalEntrySummary safeSummary = summary == null ? entryContextApplicationService.resolverEntradaAtual() : summary;
        NationalCommunicationInstitutionalStateBundle safeStateBundle = stateBundle == null ? stateBundleFacadeService.carregar(safeSummary) : stateBundle;
        CurrentAuthenticationContext authentication = currentAuthenticationContextService.current();
        GovBrIdentityAssuranceAggregate govBr = govBrIdentityAssuranceApplicationService.atual();
        HttpServletRequest request = currentRequest();
        InstitutionalEntryActivationDecision activationDecision = safeStateBundle.entryActivationDecision();
        InstitutionalOperationalProfileProjection profile = safeStateBundle.operationalProfile();
        InstitutionalHorizontalDataPlanePlan dataPlanePlan = safeStateBundle.horizontalDataPlanePlan();
        String affiliationId = firstNonBlank(attr(request, "affiliationId"), safeStateBundle.affiliationId(), activationDecision == null ? null : activationDecision.affiliationId(), profile == null ? null : profile.affiliationId());
        String nominationId = firstNonBlank(attr(request, InstitutionalRequestContextKeys.ATTR_NOMINATION_ID), safeStateBundle.nominationId(), activationDecision == null ? null : activationDecision.nominationId(), profile == null ? null : profile.nominationId());
        String panelCode = firstNonBlank(attr(request, InstitutionalRequestContextKeys.ATTR_PANEL_CODE), activationDecision == null ? null : activationDecision.panelCode(), dataPlanePlan == null ? null : dataPlanePlan.panelCode(), profile == null ? null : profile.panelCode());
        String landingPath = firstNonBlank(attr(request, InstitutionalRequestContextKeys.ATTR_LANDING_PATH), activationDecision == null ? null : activationDecision.landingPath(), dataPlanePlan == null ? null : dataPlanePlan.landingPath(), profile == null ? null : profile.landingPath());
        String profileState = firstNonBlank(attr(request, InstitutionalRequestContextKeys.ATTR_PROFILE_STATE), activationDecision == null ? null : activationDecision.profileState(), profile == null ? null : profile.profileState());
        String targetEnvironment = firstNonBlank(attr(request, InstitutionalRequestContextKeys.ATTR_TARGET_ENVIRONMENT), activationDecision == null ? null : activationDecision.targetEnvironment());
        String readReplicaCode = firstNonBlank(attr(request, InstitutionalRequestContextKeys.ATTR_READ_REPLICA), activationDecision == null ? null : activationDecision.readReplicaCode(), dataPlanePlan == null ? null : dataPlanePlan.readReplicaCode(), profile == null ? null : profile.readReplicaCode());
        boolean panelProvisioningComplete = booleanAttr(request, InstitutionalRequestContextKeys.ATTR_PANEL_PROVISIONING_COMPLETE)
                || (activationDecision != null && activationDecision.panelProvisioningComplete());
        boolean sharedExperienceReady = booleanAttr(request, InstitutionalRequestContextKeys.ATTR_SHARED_EXPERIENCE_READY)
                || (activationDecision != null && activationDecision.sharedExperienceReady());
        boolean readyForInstitutionalPanel = !(activationDecision != null && activationDecision.requiresPanelProvisioningReview()) && (
                booleanAttr(request, InstitutionalRequestContextKeys.ATTR_READY_FOR_PANEL)
                        || panelProvisioningComplete
                        || (dataPlanePlan != null && dataPlanePlan.readyForInstitutionalPanel())
                        || (profile != null && profile.readyForInstitutionalPanel()));
        boolean institutionalProfileVisible = profile != null && profile.visibleInPjb();
        boolean activateInstitutionalContext = activationDecision != null && activationDecision.activateInstitutionalContext();
        LinkedHashSet<String> evidencias = new LinkedHashSet<>();
        if (authentication.jwtBacked()) evidencias.add("JWT_AUTENTICADO");
        if (authentication.mfaActive()) evidencias.add("MFA_ATIVO");
        if (govBr.contaGovBrVinculada()) evidencias.add("CONTA_GOVBR_VINCULADA");
        if (govBr.trustedDeviceAtivo()) evidencias.add("TRUSTED_DEVICE_ATIVO");
        if (govBr.contextoInstitucionalFechado()) evidencias.add("CONTEXTO_INSTITUCIONAL_FECHADO");
        if (institutionalProfileVisible) evidencias.add("PERFIL_OPERACIONAL_MATERIALIZADO");
        if (readyForInstitutionalPanel) evidencias.add("PAINEL_INSTITUCIONAL_PRONTO");
        if (panelProvisioningComplete) evidencias.add("PANEL_PROVISIONING_COMPLETE");
        if (sharedExperienceReady) evidencias.add("PANEL_SHARED_EXPERIENCE_READY");
        if (activateInstitutionalContext) evidencias.add("CONTEXTO_INSTITUCIONAL_ATIVO");
        if (safeSummary.contextoPreferencial() != null) evidencias.add("CONTEXTO_PREFERENCIAL_RESOLVIDO");
        if (targetEnvironment != null) evidencias.add("TARGET_ENVIRONMENT=" + targetEnvironment);
        if (panelCode != null) {
            evidencias.add("PANEL_CODE=" + panelCode);
            evidencias.add("PAINEL_CODE=" + panelCode);
        }
        return new PjbAuthenticatedSessionResponse(
                authentication.authenticated(),
                authentication.jwtBacked(),
                authentication.mfaActive(),
                authentication.authenticationType(),
                authentication.authenticationMethod(),
                authentication.principalName(),
                authentication.principalSubject(),
                authentication.principalIssuer(),
                authentication.principalUid(),
                authentication.principalCpf(),
                authentication.principalEmail(),
                authentication.acr(),
                authentication.amr(),
                authentication.authorities(),
                resolveActiveDeviceId(request),
                govBr.nivelGarantia(),
                govBr.contaGovBrVinculada(),
                govBr.trustedDeviceAtivo(),
                govBr.contextoInstitucionalFechado(),
                affiliationId,
                nominationId,
                panelCode,
                landingPath,
                profileState,
                targetEnvironment,
                readReplicaCode,
                institutionalProfileVisible,
                readyForInstitutionalPanel,
                panelProvisioningComplete,
                sharedExperienceReady,
                activateInstitutionalContext,
                List.copyOf(evidencias),
                Instant.now());
    }

    private String attr(HttpServletRequest request, String name) {
        if (request == null || name == null || name.isBlank()) {
            return null;
        }
        Object attr = request.getAttribute(name);
        if (attr != null) {
            String value = String.valueOf(attr).trim();
            if (!value.isEmpty()) {
                return value;
            }
        }
        String header = request.getHeader(name);
        if (header != null && !header.isBlank()) {
            return header.trim();
        }
        return null;
    }

    private boolean booleanAttr(HttpServletRequest request, String name) {
        String value = attr(request, name);
        return value != null && Boolean.parseBoolean(value);
    }

    private Long resolveActiveDeviceId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object attr = request.getAttribute("PJB_DEVICE_ID");
        if (attr instanceof Long value) {
            return value;
        }
        if (attr != null) {
            try {
                return Long.parseLong(String.valueOf(attr).trim());
            } catch (RuntimeException ignored) {
            }
        }
        String header = request.getHeader("X-Device-ID");
        if (header == null || header.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(header.trim());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private String firstNonBlank(String... values) {
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

    private HttpServletRequest currentRequest() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletRequestAttributes) {
            return servletRequestAttributes.getRequest();
        }
        return null;
    }
}
