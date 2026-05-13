# ADR-0019 — modularização do perfil material e da decisão de juizados do NationalProceduralRouting

## Contexto

Após as rodadas anteriores, o `NationalProceduralRoutingService` ainda concentrava duas zonas de regra material sensíveis demais:

- o fechamento do perfil material da ação, com detecção de famílias procedimentais e ritos-base
- a decisão de aderência ao sistema dos juizados, com alertas, checklists e fechamento de override de rito

Além da concentração excessiva de regra, esses trechos ainda carregavam mensagens operacionais diretamente no corpo da lógica, o que contrariava a diretriz de retirar mensagens verdes soltas dos serviços e dificultava a evolução segura do eixo procedural.

## Decisão

Separar o subeixo em colaboradores próprios, preservando o contrato público do `NationalProceduralRoutingService`:

- `NationalProceduralActionProfileResolver`
- `NationalProceduralJuizadoDecisionResolver`
- `NationalProceduralActionProfileMessages`
- `NationalProceduralJuizadoDecisionMessages`
- `NationalProceduralPartyProfile`
- `NationalProceduralActionProfile`
- `NationalProceduralJuizadoDecision`

O serviço principal permanece como orquestrador da análise procedural, delegando a classificação material e a aderência aos juizados para resolvers especializados. As mensagens operacionais desses dois subeixos deixam de ficar soltas nos serviços e passam a morar em catálogos dedicados.

## Consequências

### Positivas

- o `NationalProceduralRoutingService` fica menor e mais próximo de uma orquestração procedural real
- regras materiais e regras de juizado passam a evoluir em pontos separados, reduzindo risco de regressão cruzada
- mensagens operacionais desse eixo ficam centralizadas fora do serviço principal e fora dos resolvers
- os contratos internos do eixo procedural ficam mais explícitos com records dedicados para partido material, perfil da ação e decisão de juizado
- a base ganha melhor apoio para futuras extrações adicionais sem refactor cosmético

### Custos

- aumento controlado do número de classes do eixo procedural
- necessidade de manter testes de governança protegendo a centralização de mensagens

## Status

Aceito.
