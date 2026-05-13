package com.tcc.pjb.backend.core.peticionamento.saga.domain;

import java.util.List;

public record ValidacaoSagaPeticionamentoResult(boolean ok, List<String> erros) {
}
