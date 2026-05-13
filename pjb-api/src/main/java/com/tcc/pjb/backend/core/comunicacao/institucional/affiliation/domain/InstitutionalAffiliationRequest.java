package com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain;

import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalAffiliationRequestStatus;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalNominationRole;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record InstitutionalAffiliationRequest(
        String requestId,
        DestinatarioInstitucionalKind destinatarioKind,
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
        Long representanteUsuarioId,
        String representanteNome,
        InstitutionalNominationRole representativeRole,
        Map<Long, String> bootstrapAdministrators,
        InstitutionalTrustLevel trustFloorProposto,
        boolean requerDuplaAprovacaoAdministrador,
        boolean requerCertificadoICP,
        boolean restringeCertificadoRedeInstitucional,
        boolean permiteUsoRemotoComAutorizacao,
        List<String> canaisHabilitados,
        List<String> politicaCiencia,
        List<String> sla,
        List<String> regrasFallback,
        List<String> conveniosIntegracoes,
        List<InstitutionalAffiliationDocument> documentos,
        InstitutionalAffiliationRequestStatus status,
        String materializedAffiliationId,
        List<String> fundamentos,
        Instant createdAt,
        Instant decidedAt,
        Instant updatedAt,
        String hashIntegridade
) {
    public InstitutionalAffiliationRequest {
        Objects.requireNonNull(requestId);
        Objects.requireNonNull(destinatarioKind);
        Objects.requireNonNull(orgaoSigla);
        Objects.requireNonNull(orgaoNome);
        Objects.requireNonNull(unidadeCodigo);
        Objects.requireNonNull(representanteUsuarioId);
        Objects.requireNonNull(status);
        bootstrapAdministrators = bootstrapAdministrators == null || bootstrapAdministrators.isEmpty()
                ? Map.of(representanteUsuarioId, representanteNome == null || representanteNome.isBlank() ? "Representante institucional" : representanteNome.trim())
                : Map.copyOf(new LinkedHashMap<>(bootstrapAdministrators));
        ramosMateriais = sanitize(ramosMateriais);
        abrangenciasTerritoriais = sanitize(abrangenciasTerritoriais);
        canaisHabilitados = sanitize(canaisHabilitados);
        politicaCiencia = sanitize(politicaCiencia);
        sla = sanitize(sla);
        regrasFallback = sanitize(regrasFallback);
        conveniosIntegracoes = sanitize(conveniosIntegracoes);
        documentos = documentos == null ? List.of() : List.copyOf(documentos);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
        if (hashIntegridade == null || hashIntegridade.isBlank()) {
            hashIntegridade = computeHash(requestId, destinatarioKind, organizationScope, orgaoSigla, unidadeCodigo, representanteUsuarioId,
                    esferaAdministrativa, ramosMateriais, abrangenciasTerritoriais, dominioInstitucional, autoridadeAderenteCargo,
                    trustFloorProposto, requerDuplaAprovacaoAdministrador, requerCertificadoICP,
                    restringeCertificadoRedeInstitucional, permiteUsoRemotoComAutorizacao,
                    canaisHabilitados, politicaCiencia, sla, regrasFallback, conveniosIntegracoes, status, materializedAffiliationId);
        }
    }

    public InstitutionalAffiliationRequest withStatus(InstitutionalAffiliationRequestStatus newStatus,
                                                      String materializedAffiliationId,
                                                      List<String> extraFundamentos,
                                                      Instant decidedAt,
                                                      Instant updatedAt) {
        return new InstitutionalAffiliationRequest(
                requestId,
                destinatarioKind,
                organizationScope,
                orgaoSigla,
                orgaoNome,
                unidadeCodigo,
                unidadeNome,
                uf,
                comarca,
                cnpj,
                esferaAdministrativa,
                ramosMateriais,
                abrangenciasTerritoriais,
                dominioInstitucional,
                autoridadeAderenteCargo,
                representanteUsuarioId,
                representanteNome,
                representativeRole,
                bootstrapAdministrators,
                trustFloorProposto,
                requerDuplaAprovacaoAdministrador,
                requerCertificadoICP,
                restringeCertificadoRedeInstitucional,
                permiteUsoRemotoComAutorizacao,
                canaisHabilitados,
                politicaCiencia,
                sla,
                regrasFallback,
                conveniosIntegracoes,
                documentos,
                newStatus,
                materializedAffiliationId,
                mergeFundamentos(extraFundamentos),
                createdAt,
                decidedAt,
                updatedAt,
                computeHash(requestId, destinatarioKind, organizationScope, orgaoSigla, unidadeCodigo, representanteUsuarioId,
                        esferaAdministrativa, ramosMateriais, abrangenciasTerritoriais, dominioInstitucional, autoridadeAderenteCargo,
                        trustFloorProposto, requerDuplaAprovacaoAdministrador, requerCertificadoICP,
                        restringeCertificadoRedeInstitucional, permiteUsoRemotoComAutorizacao,
                        canaisHabilitados, politicaCiencia, sla, regrasFallback, conveniosIntegracoes, newStatus, materializedAffiliationId)
        );
    }

    private List<String> mergeFundamentos(List<String> extras) {
        if (extras == null || extras.isEmpty()) {
            return fundamentos;
        }
        java.util.ArrayList<String> out = new java.util.ArrayList<>(fundamentos);
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
        StringBuilder sb = new StringBuilder("institutional_affiliation_request");
        for (Object value : values) {
            sb.append('|').append(value == null ? '-' : value.toString());
        }
        return Hashes.sha256Hex(sb.toString());
    }
}
