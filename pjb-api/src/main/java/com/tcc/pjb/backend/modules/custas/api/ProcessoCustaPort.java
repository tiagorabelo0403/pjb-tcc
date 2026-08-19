package com.tcc.pjb.backend.modules.custas.api;

import java.util.Optional;

public interface ProcessoCustaPort {

    Optional<ProcessoCustaContexto> obterContexto(Long processoId);
}
