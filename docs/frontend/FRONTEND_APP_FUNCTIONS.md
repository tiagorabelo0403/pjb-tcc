# Frontend app functions

Esta trilha consolida funções finais para consumo direto do frontend.

## Endpoints

- `GET /api/v1/frontend/app/me`
- `GET /api/v1/frontend/app/me/capabilities`
- `GET /api/v1/frontend/app/me/context`
- `GET /api/v1/frontend/app/me/menu`
- `GET /api/v1/frontend/app/support/catalogs`
- `GET /api/v1/frontend/app/bootstrap`

## Objetivo

Entregar ao frontend:

- identidade do usuário atual
- capacidades do perfil autenticado
- contexto de sessão e step-up Gov.br
- menu base por persona
- catálogos de apoio para formulários e filtros
- bootstrap consolidado para montagem inicial da aplicação


## Criação de escritório próprio

O frontend já pode permitir que o advogado crie o próprio escritório e assuma o papel de patrono fundador com contexto automaticamente ativado.

## Catálogo amplo de ramos

Use `GET /api/v1/frontend/app/support/catalogs/ramos-direito` para obter o catálogo completo de ramos materiais e processuais suportados pelo backend para política de escritório e filtros de telas.

## Governança de escritório pronta para frontend

Além da sessão, modo escritório, fila patronal, transferências e juntada governada, o frontend app agora também expõe:

- upload governado por processo e workspace
- consulta de fingerprint do lote de upload
- finalização governada do lote com verificação de fingerprint
- workspace multimídia contextualizado pelo escritório ativo

Endpoints novos:

- `POST /api/v1/frontend/app/offices/workspace/processes/{processoId}/uploads/batches`
- `GET /api/v1/frontend/app/offices/workspace/processes/{processoId}/uploads/batches/{batchId}`
- `POST /api/v1/frontend/app/offices/workspace/processes/{processoId}/uploads/batches/{batchId}/items`
- `PUT /api/v1/frontend/app/offices/workspace/processes/{processoId}/uploads/direct/{batchId}/{itemId}`
- `POST /api/v1/frontend/app/offices/workspace/processes/{processoId}/uploads/batches/{batchId}/finalize`
- `POST /api/v1/frontend/app/offices/workspace/processes/{processoId}/multimedia/workspace`
