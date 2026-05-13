package com.tcc.pjb.backend.service.advogado;

import com.tcc.pjb.backend.model.dto.advogado.LaianePeticaoInicialEstruturarRequest;
import java.util.Objects;

public final class LaianePeticaoInicialDraftRequestMapper {

    private LaianePeticaoInicialDraftRequestMapper() {
    }

    public static LaianePeticaoInicialDraftService.EstruturarRequest toServiceRequest(LaianePeticaoInicialEstruturarRequest request) {
        LaianePeticaoInicialEstruturarRequest safe = Objects.requireNonNull(request, "request");
        return new LaianePeticaoInicialDraftService.EstruturarRequest(
                safe.processoId(),
                safe.tituloCaso(),
                safe.parteAutora(),
                safe.parteRe(),
                safe.ramoDireito(),
                safe.ritoProcessual(),
                safe.classeProcessual(),
                safe.fatos(),
                safe.fundamentosJuridicos(),
                safe.pedidos(),
                safe.provasIndicadas(),
                safe.valorCausa(),
                safe.tutelaUrgencia(),
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
