package com.tcc.pjb.backend.core.comunicacao.institucional.topology.application;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.comunicacao.institucional.topology.domain.InstitutionalRecipientTopologyEntry;
import com.tcc.pjb.backend.model.entity.enums.DestinatarioInstitucionalKind;
import com.tcc.pjb.backend.model.entity.enums.NationalCommunicationRecipientKind;

@Service
public class InstitutionalRecipientTopologyApplicationService {

    public List<InstitutionalRecipientTopologyEntry> list() {
        return Arrays.stream(DestinatarioInstitucionalKind.values())
                .map(this::toEntry)
                .toList();
    }

    private InstitutionalRecipientTopologyEntry toEntry(DestinatarioInstitucionalKind kind) {
        Set<NationalCommunicationRecipientKind> legados = EnumSet.noneOf(NationalCommunicationRecipientKind.class);
        kind.toNationalCommunicationRecipientKind().ifPresent(legados::add);
        if (kind == DestinatarioInstitucionalKind.ADVOCACIA_PUBLICA
                || kind == DestinatarioInstitucionalKind.PROCURADORIA_ESTADO
                || kind == DestinatarioInstitucionalKind.PROCURADORIA_MUNICIPIO
                || kind == DestinatarioInstitucionalKind.AGU) {
            legados.add(NationalCommunicationRecipientKind.FAZENDA_PUBLICA);
        }
        if (kind == DestinatarioInstitucionalKind.PERICIA_JUDICIAL) {
            legados.add(NationalCommunicationRecipientKind.PERITO_JUDICIAL);
        }
        if (kind == DestinatarioInstitucionalKind.EQUIPE_PSICOSSOCIAL) {
            legados.add(NationalCommunicationRecipientKind.EQUIPE_PSICOSSOCIAL);
        }
        return new InstitutionalRecipientTopologyEntry(
                kind,
                kind.toOrganizacaoExtraJudicialKind(),
                Set.copyOf(legados),
                kind.isInstituicaoEssencialJustica(),
                kind.isApoioTecnicoOuAuxiliar(),
                kind.admiteCanalNacionalPessoal()
        );
    }
}
