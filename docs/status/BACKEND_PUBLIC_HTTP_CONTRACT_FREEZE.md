# Backend Public HTTP Contract Freeze

Surface administrativa adicionada para estabilizar o contrato HTTP consumido pelo frontend.

## Endpoints
- `GET /api/v1/admin/frontend-readiness/public-contract/envelopes`
- `GET /api/v1/admin/frontend-readiness/public-contract/validation`
- `GET /api/v1/admin/frontend-readiness/public-contract/error-catalog`
- `GET /api/v1/admin/frontend-readiness/public-contract/http-freeze`

## Conteúdo
- envelopes padrão (`ApiQueryResponse`, `ApiCommandResponse`)
- validação e 422 padronizado
- catálogo mínimo de erros HTTP para o frontend
- freeze consolidado do contrato HTTP
