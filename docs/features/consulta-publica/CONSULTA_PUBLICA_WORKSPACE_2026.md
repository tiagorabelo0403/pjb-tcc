# Consulta Pública 2026 — workspace unificado, governado e pronto para frontend

## Objetivo

Elevar a consulta pública do PJB para um modelo mais claro, mais acessível e mais previsível para frontend, separando com nitidez:

- trilha pessoal autenticada do titular do processo
- trilha pública resumida para terceiros
- resolução pública de página documental apenas quando a página for efetivamente pública

## O que entrou

### Novos endpoints

- `GET /api/v1/public/consultas-publicas/workspace`
- `GET /api/v1/public/consultas-publicas/search`
- `GET /api/v1/public/consultas-publicas/processos/{numero}`
- `GET /api/v1/public/consultas-publicas/pages/{pageId}`

## Contrato funcional

### Workspace

O workspace funciona como porta de entrada única para o frontend.

Ele já entrega:

- modo de entrada (`PUBLIC_ONLY` ou `PERSONAL_AND_PUBLIC`)
- configuração de filtros para busca pública
- rotas canônicas da jornada
- diretrizes de acessibilidade para UI
- cartões dos processos pessoais do usuário autenticado, com:
  - rito
  - fase
  - status
  - última movimentação
  - próximo prazo monitorado
  - total documental
  - rota para visão autenticada

### Busca pública

A busca pública continua resumida e não documental.

Melhorias aplicadas:

- score priorizando número do processo exato
- ordenação por score e última movimentação
- limitação de página com range seguro
- cache curto para absorver picos de leitura
- rate limit explícito por capability

### Detalhe público

A leitura por número do processo agora possui endpoint próprio orientado para frontend.

A resposta já retorna:

- resumo público consolidado
- trilha de acessibilidade
- ações recomendadas para UI
- warnings de acesso restrito
- `etag` e `refreshAfterSeconds`

### Resolução de página pública

`pages/{pageId}` deixou de ser stub.

Agora só resolve página quando:

- o processo é público
- o documento é público
- a página realmente existe

Quando o processo ou documento exigir credencial, a página não é exposta pela trilha pública.

## Segurança e desempenho

### Rate limit

Todos os endpoints públicos da consulta passaram a aplicar `CapabilityRateLimiter`.

### Cache

Foram adicionados caches dedicados:

- `consultaPublicaWorkspace`
- `consultaPublicaSearch`
- `consultaPublicaProcessDetail`
- `consultaPublicaPageResolve`

### Índices

Entrou a migration `V218__consulta_publica_workspace_hardening.sql` com reforço para:

- leitura pública por última movimentação
- lookup de processos do titular por CPF e `usuario_id`
- deadlines pendentes por processo
- resolução de `page_id`

## Observação de UX

A decisão arquitetural é deliberada:

- titular autenticado entra primeiro pela trilha pessoal
- terceiro entra pela trilha pública
- documento público é resolvido por chave de página, não por listagem integral aberta

Isso evita confusão entre publicidade processual, acesso pessoal e acesso profissional.
