package com.tcc.pjb.backend.core.processo.runtime.domain;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.Objects;

public record ProcessoRuntimeContext(
        Processo processo,
        Long processoId,
        String numeroProcesso,
        String numeroUnificado,
        RamoDireito ramoDireito,
        RitoProcessual ritoProcessual,
        TipoUsuario papelPrincipal,
        String tribunal,
        String vara,
        String comarca,
        String uf,
        boolean sigiloReforcado
) {
    public ProcessoRuntimeContext {
        processo = Objects.requireNonNull(processo);
        processoId = processoId == null ? 0L : processoId;
        numeroProcesso = numeroProcesso == null ? "" : numeroProcesso.trim();
        numeroUnificado = numeroUnificado == null ? "" : numeroUnificado.trim();
        tribunal = tribunal == null ? "" : tribunal.trim();
        vara = vara == null ? "" : vara.trim();
        comarca = comarca == null ? "" : comarca.trim();
        uf = uf == null ? "" : uf.trim();
        papelPrincipal = papelPrincipal == null ? TipoUsuario.CIDADAO : papelPrincipal;
    }

    public String numeroReferencia() {
        return !numeroProcesso.isBlank() ? numeroProcesso : numeroUnificado;
    }

    public RamoDireito ramoEfetivo(RamoDireito preferencial) {
        return preferencial != null ? preferencial : ramoDireito;
    }
}
