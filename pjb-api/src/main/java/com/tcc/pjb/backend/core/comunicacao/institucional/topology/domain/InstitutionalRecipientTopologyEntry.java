package com.tcc.pjb.backend.core.comunicacao.institucional.topology.domain;

import java.util.Set;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.NationalCommunicationRecipientKind;
import com.tcc.pjb.backend.model.entity.enums.OrganizacaoExtraJudicialKind;

public record InstitutionalRecipientTopologyEntry(
        DestinatarioInstitucionalKind destinatarioInstitucionalKind,
        OrganizacaoExtraJudicialKind organizacaoExtraJudicialKind,
        Set<NationalCommunicationRecipientKind> legadosCompativeis,
        boolean instituicaoEssencialJustica,
        boolean apoioTecnicoOuAuxiliar,
        boolean admiteCanalNacionalPessoal
) {
}
