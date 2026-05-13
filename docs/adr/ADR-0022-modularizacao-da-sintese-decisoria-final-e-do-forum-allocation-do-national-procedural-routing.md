# ADR-0022 — modularização da síntese decisória final e do forum allocation do NationalProceduralRouting

## Status

Aceito.

## Contexto

Mesmo após as extrações anteriores, o `NationalProceduralRoutingService` ainda concentrava dois blocos materiais demais para um serviço orquestrador:

- o fechamento do `forumAllocation`, com fallback de perfil de vara, acoplamento ao roteamento judicial, preflight estrutural e metadata operacional
- a síntese final de decisão, reunindo razões, bases legais, alertas, inputs faltantes, bloqueios, checklist, confiança, risco e revisão humana

Esse acúmulo mantinha o orquestrador principal com muita regra de domínio, aumentava o risco de regressões e dificultava a leitura institucional do eixo procedural.

## Decisão

A rodada separa esses blocos em colaboradores próprios:

- `NationalProceduralForumAllocationResolver`
- `NationalProceduralReviewSynthesisResolver`
- `NationalProceduralConfidenceResolver`
- objetos auxiliares de contexto e resultado para distribuição, forum allocation e síntese decisória

O `NationalProceduralRoutingService` permanece responsável por coordenar o fluxo macro:

- payload e corpus
- canonicalização e competência
- ação material, juizado e trilha procedural
- distribuição dinâmica
- delegação do forum allocation
- delegação da síntese decisória final
- montagem do relatório público e metadata estratégica

## Consequências

### Positivas

- o orquestrador principal fica significativamente mais curto e mais aderente ao papel de coordenação
- a política final de confiança/risco deixa de ficar acoplada à montagem de razões e bloqueios
- o fechamento de `forumAllocation` passa a ter fronteira própria para futuras evoluções de conectores, preflight e perfis de unidade
- reduz-se o risco de retorno de mensagens e regras materiais soltas ao serviço principal

### Custos

- aumento do número de classes internas do eixo procedural
- necessidade de manter testes de governança para preservar a disciplina arquitetural adotada

## Salvaguardas

- manter contratos externos de `ProceduralRoutingReport` e `ProceduralForumAllocationReport`
- impedir regressão com testes de governança específicos
- continuar evitando espalhamento de política de concorrência e mensagens operacionais fora de catálogos próprios
