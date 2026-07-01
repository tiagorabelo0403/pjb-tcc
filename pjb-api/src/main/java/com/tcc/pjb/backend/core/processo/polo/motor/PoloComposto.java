package com.tcc.pjb.backend.core.processo.polo.motor;

import com.tcc.pjb.backend.model.entity.enums.TipoParte;
import com.tcc.pjb.backend.model.entity.enums.TipoPolo;

public record PoloComposto(
        TipoPolo tipoPolo,
        TipoParte tipoParte,
        String nome,
        String cpf,
        String ufDomicilio,
        String comarcaDomicilio,
        String municipioDomicilio) {
}
