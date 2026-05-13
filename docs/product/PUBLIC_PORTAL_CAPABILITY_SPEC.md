# Public Portal Capability Specification

O portal público do PJB deve entregar a experiência externa que usuários esperam de e-SAJ, eproc, PJe, Creta e Projudi, preservando sigilo, LGPD e trilha auditável.

## Jornadas mínimas

| Jornada | Capacidade |
|---|---|
| Consulta por número | Localizar processo público ou autorizado por chave. |
| Consulta por OAB | Localizar processos públicos vinculados a profissional. |
| Consulta por parte | Desambiguar nomes e limitar dados pessoais. |
| Linha do tempo pública | Exibir movimentações públicas com linguagem compreensível. |
| Conferência documental | Validar documento por código, hash ou QR. |
| Chave de acesso | Permitir acesso temporário, revogável e auditado. |
| Push processual | Acompanhar eventos públicos ou autorizados. |
| Linguagem simples | Explicar próximo passo, prazo, audiência e pendência para parte leiga. |

## Regras de segurança

- Processo sigiloso não se torna público por chave genérica.
- Chave de acesso carrega escopo, expiração, titular, trilha e revogação.
- Documento sigiloso exige canal autenticado ou chave de resposta específica.
- Consulta pública deve mascarar dado pessoal sensível.
- Verificação documental pode confirmar integridade sem expor teor sigiloso.

## Implementação incremental

A especificação é materializada em:

```text
pjb-api/src/main/java/com/tcc/pjb/backend/core/frontend/publicaccess
pjb-api/src/main/java/com/tcc/pjb/backend/core/security/accesskey
```

Esses pacotes complementam `core.frontend.delivery`, `core.processo.busca`, `core.security`, `core.lgpd`, `core.icp` e o domínio documental existente.
