package com.tcc.pjb.backend.modules.acordo.application;

/**
 * Recurso existe e o ator tem permissão, mas a ação é rejeitada pelo estado atual (sala terminal/expirada,
 * proposta já com termo) ou por regra de negócio sobre o próprio conteúdo do pedido (campo obrigatório,
 * valor negativo, janela processual fechada). Mapeia para 422, mesmo padrão já usado por
 * {@code RecursalTransitionRejectedException} no {@code ApiExceptionHandler}.
 */
public class AcordoConflictException extends AcordoApplicationException {

    public AcordoConflictException(String message) {
        super(message);
    }
}
