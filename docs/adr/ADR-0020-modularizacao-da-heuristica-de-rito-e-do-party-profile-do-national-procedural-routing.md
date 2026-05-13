# ADR-0020 — modularização da heurística de rito e do party profile do NationalProceduralRouting

## Contexto

Mesmo após a extração do perfil material da ação e da decisão de juizados, o `NationalProceduralRoutingService` ainda concentrava dois fechamentos estruturais demais para um serviço que deve atuar como orquestrador:

- a inferência heurística do rito-base quando o payload ainda não traz fechamento explícito
- a leitura do perfil das partes e dos sinais institucionais que condicionam a competência e as trilhas seguintes

Esses dois pontos influenciam diretamente o `CanonicalRitoSelector`, a leitura de competência, o teto processual e a decisão de aderência aos juizados. Mantê-los presos ao serviço principal ampliava o acoplamento e dificultava novas extrações seguras.

## Decisão

Separar esses subeixos em colaboradores próprios, preservando o contrato público do serviço principal:

- `NationalProceduralPartyProfileResolver`
- `NationalProceduralHeuristicRitoResolver`

O `NationalProceduralRoutingService` passa a delegar a leitura do perfil das partes e o fechamento heurístico do rito para resolvers especializados. A orquestração continua no serviço principal, mas a regra material preliminar sai do arquivo central.

## Consequências

### Positivas

- o serviço principal reduz mais uma zona de regra material concentrada
- o eixo de sinais das partes fica reutilizável e evolutivo para futuras análises de competência, sigilo e especialização
- a heurística de rito passa a ter ponto próprio para crescer sem contaminar o fluxo principal
- a base ganha melhor proteção para a próxima rodada de extrações do `NationalProceduralRoutingService`

### Custos

- aumento controlado do número de colaboradores do eixo procedural
- necessidade de manter governança para impedir regressão da concentração no serviço principal

## Status

Aceito.
