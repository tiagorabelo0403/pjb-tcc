package com.tcc.pjb.backend.platform.jusos.v2.colegiado;

record ContagemVotos(int favor,
                     int contra,
                     int abstencao,
                     com.tcc.pjb.backend.model.entity.julgamento.enums.TipoVotoColegiado resultado) {
}
