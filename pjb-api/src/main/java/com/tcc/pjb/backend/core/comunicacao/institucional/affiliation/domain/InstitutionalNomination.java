package com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain;

import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryLandingPanel;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.enums.CapacidadeCaixaInstitucional;
import com.tcc.pjb.backend.model.entity.enums.FuncaoOperacionalInstitucional;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAccessLaneKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationStatus;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.time.Instant;
import java.util.Objects;
import java.util.Set;

public record InstitutionalNomination(
        String nominationId,
        String affiliationId,
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
        InstitutionalNominationStatus status,
        Instant ativaDe,
        Instant ativaAte,
        boolean requerStepUpMfa,
        boolean requerCertificadoICP,
        boolean requerRedeInstitucional,
        boolean permiteUsoRemotoAutorizado,
        String hashIntegridade,
        Instant createdAt,
        Instant updatedAt
) {
    public InstitutionalNomination {
        Objects.requireNonNull(nominationId);
        Objects.requireNonNull(affiliationId);
        Objects.requireNonNull(nominatedUserId);
        Objects.requireNonNull(nominationRole);
        Objects.requireNonNull(funcaoOperacional);
        Objects.requireNonNull(processProfile);
        Objects.requireNonNull(unidadeCodigo);
        Objects.requireNonNull(caixaCodigo);
        Objects.requireNonNull(status);
        capacidades = capacidades == null || capacidades.isEmpty()
                ? java.util.EnumSet.noneOf(CapacidadeCaixaInstitucional.class)
                : java.util.EnumSet.copyOf(capacidades);
        if (hashIntegridade == null || hashIntegridade.isBlank()) {
            hashIntegridade = computeHash(nominationId, affiliationId, nominatedUserId, accessLaneKind, nominationRole, funcaoOperacional, processProfile, unidadeCodigo, caixaCodigo, status, trustFloor, panelPreferencial, requerStepUpMfa, requerCertificadoICP, requerRedeInstitucional, permiteUsoRemotoAutorizado);
        }
    }

    public boolean ativaEm(Instant instant) {
        Instant now = instant == null ? Instant.now() : instant;
        return status.isAtiva()
                && (ativaDe == null || !ativaDe.isAfter(now))
                && (ativaAte == null || !ativaAte.isBefore(now));
    }

    public InstitutionalNomination withStatus(InstitutionalNominationStatus newStatus, Instant updatedAt) {
        return new InstitutionalNomination(
                nominationId,
                affiliationId,
                nominatedUserId,
                nominatedUserName,
                tipoUsuario,
                accessLaneKind,
                nominationRole,
                funcaoOperacional,
                processProfile,
                unidadeCodigo,
                caixaCodigo,
                capacidades,
                trustFloor,
                panelPreferencial,
                newStatus,
                ativaDe,
                ativaAte,
                requerStepUpMfa,
                requerCertificadoICP,
                requerRedeInstitucional,
                permiteUsoRemotoAutorizado,
                computeHash(nominationId, affiliationId, nominatedUserId, accessLaneKind, nominationRole, funcaoOperacional, processProfile, unidadeCodigo, caixaCodigo, newStatus, trustFloor, panelPreferencial, requerStepUpMfa, requerCertificadoICP, requerRedeInstitucional, permiteUsoRemotoAutorizado),
                createdAt,
                updatedAt
        );
    }

    private static String computeHash(Object... values) {
        StringBuilder sb = new StringBuilder("institutional_nomination");
        for (Object value : values) {
            sb.append('|').append(value == null ? '-' : value.toString());
        }
        return Hashes.sha256Hex(sb.toString());
    }
}
