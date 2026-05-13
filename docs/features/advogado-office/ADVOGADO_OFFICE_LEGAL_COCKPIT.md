# ADVOGADO OFFICE LEGAL COCKPIT

Esta rodada liga o workspace institucional do advogado aos blocos jurídicos já existentes do PJB, sem criar trilhas paralelas.

## O que passou a ficar integrado no frontend app

- resumo institucional do escritório ativo
- carteira processual visível no workspace
- cores processuais por ramo, status e sigilo
- movimentação em modo leitura por processo
- calendário de prazos dentro do contexto do advogado
- calculadora judicial dentro do workspace institucional
- funcionamento equivalente para afiliado, patrono e advogado autônomo com escritório pessoal

## Endpoints novos

- `GET /api/v1/frontend/app/offices/workspace/legal-cockpit`
- `GET /api/v1/frontend/app/offices/workspace/processes/{processoId}/reading-mode`

## Observação técnica

O `ProcessoRepository` foi endurecido com os métodos usados pelo escopo do workspace:

- `searchWorkspaceVisible(...)`
- `findWorkspaceScopedById(...)`
