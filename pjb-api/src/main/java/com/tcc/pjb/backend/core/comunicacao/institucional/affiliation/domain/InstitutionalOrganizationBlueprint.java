package com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain;

import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalEntryMode;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalOrganizationScope;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import com.tcc.pjb.backend.model.entity.enums.OrganizacaoExtraJudicialKind;
import java.util.List;

public record InstitutionalOrganizationBlueprint(
        String codigo,
        InstitutionalOrganizationScope scope,
        String nomeExibicao,
        DestinatarioInstitucionalKind destinatarioKind,
        OrganizacaoExtraJudicialKind organizacaoKind,
        InstitutionalEntryMode entryMode,
        InstitutionalTrustLevel trustFloorPadrao,
        boolean requerCertificadoICP,
        boolean restringeCertificadoRedeInstitucional,
        boolean permiteUsoRemotoComAutorizacao,
        boolean requerDuplaAprovacaoAdministrador,
        List<InstitutionalAccessLaneBlueprint> lanes,
        List<String> fundamentos
) {
    public InstitutionalOrganizationBlueprint {
        lanes = lanes == null ? List.of() : List.copyOf(lanes);
        fundamentos = fundamentos == null ? List.of() : List.copyOf(fundamentos);
    }
}
