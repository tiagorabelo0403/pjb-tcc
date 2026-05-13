package com.tcc.pjb.backend.core.kernel.recursal.context;

import java.util.Objects;
import com.tcc.pjb.backend.core.kernel.recursal.InstanceLevel;
import com.tcc.pjb.backend.core.kernel.recursal.LegalIntegrationSystem;
import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.MateriaJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;

public record ProceduralContext(
        Long processoId,
        String numeroUnificado,
        TipoJustica tipoJustica,
        RamoDireito ramoDireito,
        MateriaJurisdicao materia,
        RitoProcessual rito,
        String uf,
        String tribunal,
        InstanceLevel currentInstance,
        NivelSigilo currentSecrecy,
        LegalIntegrationSystem originSystem
) {

    public ProceduralContext {
        Objects.requireNonNull(processoId, "processoId é obrigatório");
        numeroUnificado = Objects.toString(numeroUnificado, "").trim();
        Objects.requireNonNull(tipoJustica, "tipoJustica é obrigatório");
        Objects.requireNonNull(ramoDireito, "ramoDireito é obrigatório");
        Objects.requireNonNull(materia, "materia é obrigatório");
        Objects.requireNonNull(rito, "rito é obrigatório");
        uf = Objects.toString(uf, "").trim();
        tribunal = Objects.toString(tribunal, "").trim();
        if (currentInstance == null) currentInstance = InstanceLevel.FIRST_INSTANCE;
        if (currentSecrecy == null) currentSecrecy = NivelSigilo.PUBLICO;
        if (originSystem == null) originSystem = LegalIntegrationSystem.OTHER;
    }
}
