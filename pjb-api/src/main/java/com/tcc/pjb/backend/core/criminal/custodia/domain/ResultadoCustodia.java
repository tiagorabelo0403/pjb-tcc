package com.tcc.pjb.backend.core.criminal.custodia.domain;

public record ResultadoCustodia(Long custodiaId,
                                String statusProcesso,
                                boolean mandadoAtivoBnmp,
                                String numeroMandado) {
}
