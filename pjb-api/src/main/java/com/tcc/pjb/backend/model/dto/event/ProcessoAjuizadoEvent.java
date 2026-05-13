package com.tcc.pjb.backend.model.dto.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import com.tcc.pjb.backend.model.dto.Attachment;
import com.tcc.pjb.backend.model.entity.Processo;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProcessoAjuizadoEvent {

    private final Long processoId;
    private final String numeroUnificado;
    private final String ramoDireito;
    private final String rito;
    private final String tipoJustica;
    private final String classeProcessual;
    private final String assunto;
    private final BigDecimal valorCausa;
    private final String comarca;
    private final String uf;
    private final String tribunalCodigo;
    private final String unidadeJudiciariaCodigo;
    private final LocalDateTime criadoEm;
    private final List<Attachment> anexos;
    private final boolean juizo100Digital;

    public static ProcessoAjuizadoEvent from(Processo p, List<Attachment> anexos) {
        return from(p, anexos, false);
    }

    public static ProcessoAjuizadoEvent from(Processo p, List<Attachment> anexos, boolean juizo100Digital) {
        if (p == null) {
            return ProcessoAjuizadoEvent.builder().processoId(null).anexos(List.of()).juizo100Digital(juizo100Digital).build();
        }
        List<Attachment> safe = anexos == null ? List.of() : Collections.unmodifiableList(anexos);
        return ProcessoAjuizadoEvent.builder()
                .processoId(p.getId())
                .numeroUnificado(p.getNumeroUnificado())
                .ramoDireito(p.getRamoDireito() == null ? null : p.getRamoDireito().name())
                .rito(p.getRito() == null ? null : p.getRito().name())
                .tipoJustica(p.getTipoJustica() == null ? null : p.getTipoJustica().name())
                .classeProcessual(p.getClasseProcessual())
                .assunto(p.getAssunto())
                .valorCausa(p.getValorCausa())
                .comarca(p.getComarca())
                .uf(p.getUf())
                .tribunalCodigo(p.getTribunalCodigoRoteado())
                .unidadeJudiciariaCodigo(p.getUnidadeJudiciariaCodigo())
                .criadoEm(p.getDataCriacao())
                .anexos(safe)
                .juizo100Digital(juizo100Digital)
                .build();
    }
}
