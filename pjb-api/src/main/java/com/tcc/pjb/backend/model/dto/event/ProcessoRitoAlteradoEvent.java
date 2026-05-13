package com.tcc.pjb.backend.model.dto.event;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProcessoRitoAlteradoEvent {

    private final Long processoId;
    private final String numeroUnificado;
    private final String ritoAnterior;
    private final String ritoNovo;
    private final String grupoAnterior;
    private final String grupoNovo;
    private final String fundamentacao;
    private final LocalDateTime alteradoEm;

    public static ProcessoRitoAlteradoEvent from(Processo processo,
                                                  RitoProcessual anterior,
                                                  RitoProcessual novo,
                                                  String fundamentacao) {
        return ProcessoRitoAlteradoEvent.builder()
                .processoId(processo.getId())
                .numeroUnificado(processo.getNumeroUnificado())
                .ritoAnterior(anterior == null ? null : anterior.name())
                .ritoNovo(novo == null ? null : novo.name())
                .grupoAnterior(anterior == null ? null : anterior.getGrupoPrincipal().name())
                .grupoNovo(novo == null ? null : novo.getGrupoPrincipal().name())
                .fundamentacao(fundamentacao)
                .alteradoEm(LocalDateTime.now())
                .build();
    }
}
