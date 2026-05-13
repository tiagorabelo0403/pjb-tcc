package com.tcc.pjb.backend.integration.datajud.feed;

import com.tcc.pjb.backend.model.entity.Processo;
import org.springframework.stereotype.Component;

@Component
public class DataJudFeedPayloadAssembler {

    public DataJudFeedEntry toEntry(Processo processo) {
        return new DataJudFeedEntry(
                processo.getId(),
                processo.getNumeroUnificado(),
                processo.getTribunal(),
                processo.getClasseTpuCodigo(),
                processo.getAssunto(),
                processo.getStatusProcesso() == null ? null : processo.getStatusProcesso().name()
        );
    }
}
