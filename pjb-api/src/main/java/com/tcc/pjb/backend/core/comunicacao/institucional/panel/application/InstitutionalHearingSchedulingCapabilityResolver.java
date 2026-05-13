package com.tcc.pjb.backend.core.comunicacao.institucional.panel.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAccessProfileCatalogEntry;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalOperationalProfileProjection;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

class InstitutionalHearingSchedulingCapabilityResolver {

    InstitutionalHearingSchedulingCapabilityProfile resolve(InstitutionalOperationalProfileProjection profile,
                                                            InstitutionalAccessProfileCatalogEntry catalogEntry) {
        Set<CapacidadeCaixaInstitucional> capacities = resolveCapacities(profile, catalogEntry);
        InstitutionalProcessProfile processProfile = resolveProcessProfile(profile, catalogEntry);
        InstitutionalNominationRole nominationRole = resolveNominationRole(profile, catalogEntry);
        String scope = firstNonBlank(profile == null ? null : profile.organizationScope(), catalogEntry == null ? null : catalogEntry.codigo());

        boolean legalInstitution = isLegalInstitutionProfile(processProfile);
        boolean secretariat = processProfile == InstitutionalProcessProfile.SECRETARIA_FORUM || processProfile == InstitutionalProcessProfile.DIRETOR_FORUM;
        boolean scheduler = processProfile == InstitutionalProcessProfile.AGENDADOR_AUDIENCIA
                || processProfile == InstitutionalProcessProfile.AGENDADOR_CONCILIACAO
                || processProfile == InstitutionalProcessProfile.CONCILIADOR
                || processProfile == InstitutionalProcessProfile.MEDIADOR
                || processProfile == InstitutionalProcessProfile.SECRETARIA_FORUM && scopeContains(scope, "CENTRAL_AUDIENCIA");
        boolean technicalSupport = processProfile == InstitutionalProcessProfile.ASSESSOR_INSTITUCIONAL
                || processProfile == InstitutionalProcessProfile.ANALISTA_INSTITUCIONAL
                || processProfile == InstitutionalProcessProfile.TECNICO_INSTITUCIONAL
                || processProfile == InstitutionalProcessProfile.SERVIDOR_TRIAGEM;
        boolean management = processProfile == InstitutionalProcessProfile.ADMINISTRADOR_INSTITUCIONAL
                || processProfile == InstitutionalProcessProfile.COORDENADOR_UNIDADE
                || nominationRole != null && nominationRole.isGestaoMestre();
        boolean prisonFlow = processProfile == InstitutionalProcessProfile.POLICIAL_PENAL
                || processProfile == InstitutionalProcessProfile.GESTOR_UNIDADE_PRISIONAL
                || processProfile == InstitutionalProcessProfile.OPERADOR_CUSTODIA_PRISIONAL;
        boolean hybridJudicial = processProfile == InstitutionalProcessProfile.MAGISTRADO_COOPERANTE;

        boolean sectionVisible = scheduler
                || secretariat
                || legalInstitution
                || technicalSupport
                || management
                || prisonFlow
                || hasAny(capacities,
                CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA,
                CapacidadeCaixaInstitucional.AGENDAR_AUDIENCIA,
                CapacidadeCaixaInstitucional.REMARCAR_AUDIENCIA,
                CapacidadeCaixaInstitucional.CANCELAR_AUDIENCIA,
                CapacidadeCaixaInstitucional.RESERVAR_SALA_AUDIENCIA,
                CapacidadeCaixaInstitucional.REGISTRAR_TERMO_AUDIENCIA,
                CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS,
                CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO,
                CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA);

        boolean canRequestHearing = sectionVisible
                && ((legalInstitution || technicalSupport || secretariat || management || prisonFlow || scheduler)
                && hasAny(capacities, CapacidadeCaixaInstitucional.SOLICITAR_AUDIENCIA, CapacidadeCaixaInstitucional.AGENDAR_AUDIENCIA)
                || legalInstitution || technicalSupport || secretariat || prisonFlow);
        boolean canSuggestSlot = sectionVisible && (scheduler || secretariat || technicalSupport || management);
        boolean canOrganizeDocket = sectionVisible && (hasAny(capacities, CapacidadeCaixaInstitucional.ORGANIZAR_DOCUMENTOS)
                || secretariat || technicalSupport || scheduler || management);
        boolean canOperationallySchedule = sectionVisible && (scheduler || secretariat || management)
                && hasAny(capacities,
                CapacidadeCaixaInstitucional.AGENDAR_AUDIENCIA,
                CapacidadeCaixaInstitucional.REMARCAR_AUDIENCIA,
                CapacidadeCaixaInstitucional.RESERVAR_SALA_AUDIENCIA);
        boolean canReschedule = canOperationallySchedule || hasAny(capacities, CapacidadeCaixaInstitucional.REMARCAR_AUDIENCIA);
        boolean canCancel = canOperationallySchedule && (management || secretariat || scheduler)
                || hasAny(capacities, CapacidadeCaixaInstitucional.CANCELAR_AUDIENCIA);
        boolean canReserveRoom = canOperationallySchedule || hasAny(capacities, CapacidadeCaixaInstitucional.RESERVAR_SALA_AUDIENCIA);
        boolean canManageVirtualRoom = canReserveRoom || scheduler || prisonFlow;
        boolean canConfirmAttendance = sectionVisible && (scheduler || secretariat || legalInstitution || prisonFlow || technicalSupport || management);
        boolean canRecordTerm = scheduler
                || secretariat
                || prisonFlow
                || hasAny(capacities, CapacidadeCaixaInstitucional.REGISTRAR_TERMO_AUDIENCIA);
        boolean canIssueHearingCommunications = sectionVisible && (secretariat || scheduler || management || prisonFlow
                || hasAny(capacities, CapacidadeCaixaInstitucional.RECEBER_COMUNICACAO, CapacidadeCaixaInstitucional.GERAR_CERTIDAO_CIENCIA));
        boolean canPrepareHearingBundle = sectionVisible && (canOrganizeDocket || technicalSupport || secretariat || scheduler || management || prisonFlow);
        boolean requiresJudicialAuthorization = canOperationallySchedule && !hybridJudicial;
        boolean requiresSecretariatCoordination = ((legalInstitution && nominationRole != InstitutionalNominationRole.TITULAR_INSTITUCIONAL) || technicalSupport || prisonFlow) && !scheduler;

        return new InstitutionalHearingSchedulingCapabilityProfile(
                capacities,
                processProfile,
                nominationRole,
                scope,
                legalInstitution,
                secretariat,
                scheduler,
                technicalSupport,
                management,
                prisonFlow,
                hybridJudicial,
                sectionVisible,
                canRequestHearing,
                canSuggestSlot,
                canOrganizeDocket,
                canOperationallySchedule,
                canReschedule,
                canCancel,
                canReserveRoom,
                canManageVirtualRoom,
                canConfirmAttendance,
                canRecordTerm,
                canIssueHearingCommunications,
                canPrepareHearingBundle,
                requiresJudicialAuthorization,
                requiresSecretariatCoordination
        );
    }

    private Set<CapacidadeCaixaInstitucional> resolveCapacities(InstitutionalOperationalProfileProjection profile,
                                                                InstitutionalAccessProfileCatalogEntry catalogEntry) {
        if (catalogEntry != null) {
            return copyCapacities(catalogEntry.capacidadesPadrao());
        }
        return parseCapacities(profile == null ? List.of() : profile.capacidades());
    }

    private Set<CapacidadeCaixaInstitucional> parseCapacities(Collection<String> source) {
        EnumSet<CapacidadeCaixaInstitucional> capacities = EnumSet.noneOf(CapacidadeCaixaInstitucional.class);
        if (source == null || source.isEmpty()) {
            return capacities;
        }
        for (String item : source) {
            if (item == null || item.isBlank()) {
                continue;
            }
            try {
                capacities.add(CapacidadeCaixaInstitucional.valueOf(item.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException ignored) {
            }
        }
        return capacities;
    }

    private InstitutionalProcessProfile resolveProcessProfile(InstitutionalOperationalProfileProjection profile,
                                                             InstitutionalAccessProfileCatalogEntry catalogEntry) {
        if (catalogEntry != null) {
            return catalogEntry.processProfile();
        }
        if (profile == null || profile.processProfile() == null || profile.processProfile().isBlank()) {
            return null;
        }
        try {
            return InstitutionalProcessProfile.valueOf(profile.processProfile().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private InstitutionalNominationRole resolveNominationRole(InstitutionalOperationalProfileProjection profile,
                                                              InstitutionalAccessProfileCatalogEntry catalogEntry) {
        if (catalogEntry != null) {
            return catalogEntry.nominationRole();
        }
        if (profile == null || profile.nominationRole() == null || profile.nominationRole().isBlank()) {
            return null;
        }
        try {
            return InstitutionalNominationRole.valueOf(profile.nominationRole().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private boolean isLegalInstitutionProfile(InstitutionalProcessProfile profile) {
        return profile == InstitutionalProcessProfile.PROMOTOR
                || profile == InstitutionalProcessProfile.DEFENSOR
                || profile == InstitutionalProcessProfile.PROCURADOR;
    }

    private Set<CapacidadeCaixaInstitucional> copyCapacities(Collection<CapacidadeCaixaInstitucional> source) {
        if (source == null || source.isEmpty()) {
            return EnumSet.noneOf(CapacidadeCaixaInstitucional.class);
        }
        return EnumSet.copyOf(source);
    }

    private boolean scopeContains(String scope, String token) {
        return scope != null && token != null && scope.toUpperCase(Locale.ROOT).contains(token);
    }

    private boolean hasAny(Set<CapacidadeCaixaInstitucional> current, CapacidadeCaixaInstitucional... expected) {
        if (current == null || current.isEmpty()) {
            return false;
        }
        for (CapacidadeCaixaInstitucional item : expected) {
            if (current.contains(Objects.requireNonNull(item))) {
                return true;
            }
        }
        return false;
    }

    private String firstNonBlank(String first, String second) {
        if (first != null && !first.isBlank()) {
            return first;
        }
        return second;
    }
}
