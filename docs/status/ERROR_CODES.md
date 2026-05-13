# Catálogo oficial de erros HTTP do backend

| HTTP | Código | Categoria | Retry | Origem | Mensagem base |
|---|---|---|---|---|---|
| 400 | `VALIDATION_ERROR` | validation | não | ApiExceptionHandler | Dados inválidos. |
| 400 | `CONSTRAINT_VIOLATION` | validation | não | ApiExceptionHandler | Violação de restrição de entrada. |
| 401 | `UNAUTHORIZED` | auth | não | SecurityFilterChain | Autenticação obrigatória. |
| 403 | `FORBIDDEN` | auth | não | ApiExceptionHandler | Acesso negado. |
| 409 | `IDEMPOTENCY_IN_PROGRESS` | idempotency | sim | ApiExceptionHandler | Requisição idêntica em processamento. |
| 409 | `CONFLICT` | conflict | sim | ApiExceptionHandler | Conflito de operação. |
| 422 | `BUSINESS_RULE` | business | não | ApiExceptionHandler | Regra de negócio impedindo a operação. |
| 422 | `UNSUPPORTED_DOMAIN` | business | não | ApiExceptionHandler | Domínio não suportado para a operação. |
| 429 | `RATE_LIMIT_EXCEEDED` | throttling | sim | ApiExceptionHandler | Limite de requisições excedido. |
| 500 | `INTERNAL_ERROR` | system | sim | ApiExceptionHandler | Erro interno. |
| 503 | `EXTERNAL_INTEGRATION_UNAVAILABLE` | integration | sim | ApiExceptionHandler | Integração externa indisponível. |
