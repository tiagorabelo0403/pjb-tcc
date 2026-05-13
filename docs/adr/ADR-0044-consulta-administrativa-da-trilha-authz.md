# ADR-0044 — consulta administrativa da trilha AUTHZ

## Status
Aceito

## Contexto

As rodadas anteriores endureceram o eixo ABAC do PJB com trilha explicável, hash auditável, risco, step-up e governança institucional. Apesar disso, a operação ainda não possuía uma superfície administrativa própria para consultar essas decisões de autorização por recurso, integração externa e capacidade institucional.

Isso criava uma assimetria: a trilha era emitida e selada no ledger, mas a leitura operacional dependia de parsing indireto ou de futuras integrações forenses, reduzindo a utilidade imediata para governança, suporte avançado, análise de incidentes e auditoria institucional de runtime.

## Decisão

Foi introduzida uma linha administrativa própria para materializar e consultar a trilha AUTHZ em runtime:

- `PjbAuthorizationTrailRegistry` passou a armazenar snapshots estruturados e bounded da trilha emitida pelo ABAC
- `PjbAuthorizationTrailSnapshot` normaliza a decisão em estrutura consultável, derivando campos operacionais para integração externa, unidade/caixa/capacidade institucional e expedição vinculada
- `PjbAuthorizationTrailAdminService` atua como orquestrador curto da superfície de consulta
- `PjbAuthorizationTrailProjectionAssembler` monta a resposta administrativa com resumo agregado e buckets operacionais
- `AdminAuthorizationTrailController` expõe `GET /api/v1/admin/security/authz-trails` com filtros por:
  - ação
  - tipo/id de recurso
  - ator
  - permitido/negado
  - nível de risco
  - requestId
  - integração externa
  - unidade/caixa/capacidade institucional
  - canal/escopo de governança
  - exigência de step-up

A gravação da trilha administrativa ocorre dentro de `PjbAuthorizationAuditFacade`, imediatamente antes do append no ledger, preservando coesão da auditoria e evitando espalhamento para o serviço principal.

## Consequências

### Positivas

- a trilha AUTHZ deixa de ser apenas emitida e passa a ser consultável em superfície administrativa própria
- investigação operacional e forense de runtime fica mais rápida para integrações sensíveis e capacidades institucionais
- o serviço principal de autorização continua curto e sem recontaminação por lógica de leitura/consulta
- a resposta administrativa já nasce com agrupamentos úteis para segurança operacional

### Negativas

- a materialização administrativa é de runtime e bounded, não substituindo um histórico forense de longo prazo em armazenamento soberano
- a nova superfície administrativa precisa continuar protegida por autorização forte, porque expõe trilha sensível de segurança
