# Round 73 — Dashboards institucionais por órgão, unidade e núcleo

## O que entrou

- Dashboard organizacional profissional unificado:
  - `/api/v1/frontend/app/professional/workspace/organizational-executive-dashboard`
- Dashboards específicos:
  - `/api/v1/frontend/app/professional/workspace/magistrature-organ-dashboard`
  - `/api/v1/frontend/app/professional/workspace/defensoria-organ-dashboard`
  - `/api/v1/frontend/app/professional/workspace/procuradoria-organ-dashboard`

## Reaproveitamento real

- reutiliza o dashboard executivo profissional já existente
- reutiliza o dashboard executivo específico por papel
- reutiliza grants institucionais já persistidos
- reutiliza galeria de perfis, board operacional, spotlight processual e tema Brasil
- reutiliza trilha de auditoria do `PjbFrontendAppApplicationService`

## Superfícies novas

- unidades organizacionais ranqueadas por criticidade e grants ativos
- cobertura institucional por gabinete, colegiado, unidade ou ente
- fila crítica institucional
- perfil de grants e competência

## Conexões feitas

- menu do frontend
- workspace híbrido da consulta pública por rota organizacional profissional
- quick routes do dashboard profissional geral
