package com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain;

import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAffiliationStatus;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public record InstitutionalAffiliation(
        String affiliationId,
        DestinatarioInstitucionalKind destinatarioKind,
        String orgaoSigla,
        String orgaoNome,
        String unidadeCodigo,
        String unidadeNome,
        InstitutionalOrganizationScope organizationScope,
        String blueprintCode,
        String uf,
        String comarca,
        String cnpj,
        String esferaAdministrativa,
        List<String> ramosMateriais,
        List<String> abrangenciasTerritoriais,
        String dominioInstitucional,
        String autoridadeAderenteCargo,
        Long representanteUsuarioId,
        InstitutionalNominationRole representativeRole,
        String emailContatoSeguranca,
        List<String> canaisHabilitados,
        List<String> politicaCiencia,
        List<String> sla,
        List<String> regrasFallback,
        List<String> conveniosIntegracoes,
        InstitutionalTrustLevel trustFloor,
        boolean requerDuplaAprovacaoAdministrador,
        boolean requerCertificadoICP,
        boolean restringeCertificadoRedeInstitucional,
        boolean permiteUsoRemotoComAutorizacao,
        InstitutionalAffiliationStatus status,
        List<String> fundamentos,
        Instant createdAt,
        Instant updatedAt,
        String hashIntegridade
) {
    public InstitutionalAffiliation {
        Objects.requireNonNull(affiliationId);
        Objects.requireNonNull(destinatarioKind);
        Objects.requireNonNull(orgaoSigla);
        Objects.requireNonNull(orgaoNome);
        Objects.requireNonNull(unidadeCodigo);
        Objects.requireNonNull(status);
        ramosMateriais = sanitize(ramosMateriais);
        abrangenciasTerritoriais = sanitize(abrangenciasTerritoriais);
        canaisHabilitados = sanitize(canaisHabilitados);
        politicaCiencia = sanitize(politicaCiencia);
        sla = sanitize(sla);
        regrasFallback = sanitize(regrasFallback);
        conveniosIntegracoes = sanitize(conveniosIntegracoes);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        if (hashIntegridade == null || hashIntegridade.isBlank()) {
            hashIntegridade = computeHash(
                    affiliationId,
                    destinatarioKind,
                    orgaoSigla,
                    unidadeCodigo,
                    organizationScope,
                    blueprintCode,
                    esferaAdministrativa,
                    ramosMateriais,
                    abrangenciasTerritoriais,
                    dominioInstitucional,
                    autoridadeAderenteCargo,
                    representanteUsuarioId,
                    status,
                    trustFloor,
                    requerDuplaAprovacaoAdministrador,
                    requerCertificadoICP,
                    restringeCertificadoRedeInstitucional,
                    permiteUsoRemotoComAutorizacao,
                    canaisHabilitados,
                    politicaCiencia,
                    sla,
                    regrasFallback,
                    conveniosIntegracoes);
        }
    }

    public boolean ativa() {
        return status.isAtiva();
    }

    public InstitutionalAffiliation withStatus(InstitutionalAffiliationStatus newStatus, Instant updatedAt, List<String> extraFundamentos) {
        return new InstitutionalAffiliation(
                affiliationId,
                destinatarioKind,
                orgaoSigla,
                orgaoNome,
                unidadeCodigo,
                unidadeNome,
                organizationScope,
                blueprintCode,
                uf,
                comarca,
                cnpj,
                esferaAdministrativa,
                ramosMateriais,
                abrangenciasTerritoriais,
                dominioInstitucional,
                autoridadeAderenteCargo,
                representanteUsuarioId,
                representativeRole,
                emailContatoSeguranca,
                canaisHabilitados,
                politicaCiencia,
                sla,
                regrasFallback,
                conveniosIntegracoes,
                trustFloor,
                requerDuplaAprovacaoAdministrador,
                requerCertificadoICP,
                restringeCertificadoRedeInstitucional,
                permiteUsoRemotoComAutorizacao,
                newStatus,
                mergeFundamentos(extraFundamentos),
                createdAt,
                updatedAt,
                computeHash(
                        affiliationId,
                        destinatarioKind,
                        orgaoSigla,
                        unidadeCodigo,
                        organizationScope,
                        blueprintCode,
                        esferaAdministrativa,
                        ramosMateriais,
                        abrangenciasTerritoriais,
                        dominioInstitucional,
                        autoridadeAderenteCargo,
                        representanteUsuarioId,
                        newStatus,
                        trustFloor,
                        requerDuplaAprovacaoAdministrador,
                        requerCertificadoICP,
                        restringeCertificadoRedeInstitucional,
                        permiteUsoRemotoComAutorizacao,
                        canaisHabilitados,
                        politicaCiencia,
                        sla,
                        regrasFallback,
                        conveniosIntegracoes));
    }

    private List<String> mergeFundamentos(List<String> extras) {
        if (extras == null || extras.isEmpty()) {
            return fundamentos;
        }
        ArrayList<String> out = new ArrayList<>(fundamentos);
        out.addAll(extras);
        return List.copyOf(out);
    }

    private static List<String> sanitize(List<String> values) {
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

    private static String computeHash(Object... values) {
        StringBuilder sb = new StringBuilder("institutional_affiliation");
        for (Object value : values) {
            sb.append('|').append(value == null ? '-' : value.toString());
        }
        return Hashes.sha256Hex(sb.toString());
    }
}
