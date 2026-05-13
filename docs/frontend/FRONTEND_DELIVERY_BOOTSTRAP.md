# FRONTEND DELIVERY BOOTSTRAP

Superfície criada para acelerar integração do frontend com a base PJB sem depender de leitura manual dispersa.

## Rotas novas

- `GET /api/v1/frontend/delivery/summary`
- `GET /api/v1/frontend/delivery/routes`
- `GET /api/v1/frontend/delivery/domains`
- `GET /api/v1/frontend/delivery/blockers`
- `GET /api/v1/frontend/delivery/bootstrap`

## Objetivo

Entregar um ponto único para o frontend descobrir:

- resumo consolidado da prontidão de integração
- catálogo de rotas utilizáveis
- agrupamento por domínio funcional
- principais bloqueadores ainda existentes
- pacote inicial de bootstrap para squads de frontend

## Uso recomendado

1. Começar por `summary`
2. Ler `domains` para organizar telas e squads
3. Ler `routes` para gerar catálogo de integração
4. Ler `blockers` antes de prometer fluxo fechado
5. Usar `bootstrap` como pacote inicial de consumo
