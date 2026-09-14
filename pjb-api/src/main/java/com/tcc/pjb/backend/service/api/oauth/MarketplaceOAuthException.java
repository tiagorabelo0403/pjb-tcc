package com.tcc.pjb.backend.service.api.oauth;

import org.springframework.http.HttpStatus;

/**
 * Falha de autenticação/autorização OAuth2 do marketplace (token ausente/inválido/expirado/inativo,
 * escopo insuficiente, cliente não habilitado). Antes desta exceção, esses casos usavam
 * {@code IllegalStateException} genérica, que não tinha handler dedicado em {@code ApiExceptionHandler}
 * e caía no catch-all — respondendo 500 (erro interno) em vez de 401/403. Não era falha de segurança
 * (a requisição era rejeitada do mesmo jeito, sem vazar detalhe), mas o cliente da API não conseguia
 * distinguir "seu token expirou" de "o servidor quebrou". Carrega o status HTTP correto para cada caso.
 */
public class MarketplaceOAuthException extends RuntimeException {

    private final HttpStatus status;

    public MarketplaceOAuthException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
