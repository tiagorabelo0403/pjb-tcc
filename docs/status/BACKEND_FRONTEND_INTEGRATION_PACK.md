# Backend integration pack para frontend

Artefatos colocados dentro do projeto para acelerar a integração do frontend:

- `docs/openapi/public-api.yaml`
- `docs/openapi/admin-api.yaml`
- `docs/postman/PJB_Frontend_Integration.postman_collection.json`
- `docs/postman/PJB_Frontend_Environment.postman_environment.json`
- `docs/ERROR_CODES.md`
- `docs/reports/error_code_catalog.json`
- `pjb-api/src/main/resources/application-frontend-dev.yml`
- `pjb-api/src/main/resources/frontend-dev/seed-users.json`
- `pjb-api/src/main/resources/frontend-dev/seed-processos.json`
- `pjb-api/src/main/resources/frontend-dev/seed-custas.json`
- `pjb-api/src/main/resources/frontend-dev/seed-integrations.json`
- `pjb-api/src/test/java/com/tcc/pjb/backend/smoke/FrontendPrimaryFlowsSmokeTest.java`

A surface administrativa correspondente ficou disponível em:

- `GET /api/v1/admin/frontend-readiness/integration-pack/summary`
- `GET /api/v1/admin/frontend-readiness/integration-pack/artifacts`
- `GET /api/v1/admin/frontend-readiness/integration-pack/seeds`
- `GET /api/v1/admin/frontend-readiness/integration-pack/profile`
- `GET /api/v1/admin/frontend-readiness/integration-pack/smoke`
