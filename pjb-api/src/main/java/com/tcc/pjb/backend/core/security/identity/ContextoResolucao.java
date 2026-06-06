package com.tcc.pjb.backend.core.security.identity;

public sealed interface ContextoResolucao permits ContextoResolvido, PendenteSelecao, ContextoNegado {
}
