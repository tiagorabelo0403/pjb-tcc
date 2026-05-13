# Backend ready for frontend

Este documento congela o que o frontend precisa considerar como pronto no backend antes da integracao pesada.

## Surface operacional

- `GET /api/v1/admin/frontend-readiness/summary`
- `GET /api/v1/admin/frontend-readiness/checklist`
- `GET /api/v1/admin/frontend-readiness/auth`
- `GET /api/v1/admin/frontend-readiness/errors`
- `GET /api/v1/admin/frontend-readiness/public-routes`
- `GET /api/v1/admin/frontend-readiness/blockers`
- `GET /api/v1/admin/frontend-readiness/bootstrap`

## O que esta surface mede

- gate estrutural de build
- limpeza da superficie HTTP
- prontidao do contrato de autenticacao
- consistencia do envelope de erro
- catalogo de rotas publicas para consumo do frontend
- blockers de integracao ainda abertos

## Regras de uso

1. O frontend nao deve consumir rotas administrativas.
2. O frontend deve priorizar apenas o catalogo retornado por `public-routes`.
3. O frontend deve ler `auth` e `errors` antes de fechar login, sessao e tratamentos 401/403/409/422.
4. O frontend deve tratar `blockers` como impeditivos reais de freeze quando vierem com severidade alta ou critica.
5. O build global continua sendo bloqueador formal ate execucao end-to-end fora deste ambiente.


## Freeze do contrato público

- `GET /api/v1/admin/frontend-readiness/public-contract/summary`
- `GET /api/v1/admin/frontend-readiness/public-contract/routes`
- `GET /api/v1/admin/frontend-readiness/public-contract/dtos`
- `GET /api/v1/admin/frontend-readiness/public-contract/freeze`


## Freeze HTTP
- `GET /api/v1/admin/frontend-readiness/public-contract/envelopes`
- `GET /api/v1/admin/frontend-readiness/public-contract/validation`
- `GET /api/v1/admin/frontend-readiness/public-contract/error-catalog`
- `GET /api/v1/admin/frontend-readiness/public-contract/http-freeze`

## Integration pack adicional

Entrou uma sub-surface nova de readiness para integração acelerada do frontend:

- `GET /api/v1/admin/frontend-readiness/integration-pack/summary`
- `GET /api/v1/admin/frontend-readiness/integration-pack/artifacts`
- `GET /api/v1/admin/frontend-readiness/integration-pack/seeds`
- `GET /api/v1/admin/frontend-readiness/integration-pack/profile`
- `GET /api/v1/admin/frontend-readiness/integration-pack/smoke`

Essa camada cobre:

- OpenAPI exportada (`docs/openapi/public-api.yaml` e `docs/openapi/admin-api.yaml`)
- coleção Postman oficial (`docs/postman/...`)
- perfil `frontend-dev`
- seed pack de usuários, processos, custas e integrações mockadas
- smoke pack dos fluxos primários do frontend
- catálogo oficial de erros (`docs/ERROR_CODES.md`)
