# Backend Public Contract Freeze

Esta camada congela dentro do próprio PJB o contrato público que o frontend deve consumir
antes do freeze de integração.

## Surface administrativa

- `GET /api/v1/admin/frontend-readiness/public-contract/summary`
- `GET /api/v1/admin/frontend-readiness/public-contract/routes`
- `GET /api/v1/admin/frontend-readiness/public-contract/dtos`
- `GET /api/v1/admin/frontend-readiness/public-contract/freeze`

## O que esta surface expõe

- catálogo de rotas públicas e autenticadas relevantes ao frontend
- modo de autenticação por rota (`PUBLIC`, `AUTHENTICATED`, `STEP_UP`)
- tipo de request body quando detectável
- tipo de resposta retornado pelo controller
- catálogo mínimo de DTOs/envelopes estáveis para o frontend
- pacote de freeze consolidado com auth, erros, rotas e DTOs

## Uso recomendado

1. O frontend deve começar por `public-contract/freeze`.
2. As telas devem usar apenas rotas marcadas como estáveis para frontend.
3. O consumo inicial deve priorizar DTOs do catálogo desta surface.
4. Antes do freeze final, cruzar esta surface com `/v3/api-docs`.
