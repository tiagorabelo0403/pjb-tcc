# Round 137 — Varredura de imports e visibilidade top-level

## Objetivo
Executar uma sonda reaproveitável para identificar classes com risco semelhante ao lote que quebrou no compile real do usuário:
- annotations Spring MVC usadas sem import correspondente
- tipos de domínio conhecidos usados sem import correto
- import cross-package de tipos top-level não públicos

## Resultado
A sonda foi executada sobre `pjb-api/src/main/java`. Ela identificou mais dois pontos reais no eixo de juizado procedural, ambos corrigidos nesta rodada, e não encontrou novos problemas nas demais regras cobertas após a correção. A sonda foi endurecida para ignorar menções textuais a annotations em analisadores estáticos, reduzindo falso positivo.

## Ajustes preservados do round 136
- controllers institucionais de comunicação com imports Spring MVC corrigidos
- `AudienciaContexto` com imports corretos de enums processuais/jurisdicionais
- perfis procedimentais nacionais visíveis para consumo cross-package no eixo de juizado

## Limitação honesta
A sonda não substitui o compile completo do `pjb-api`. Ela antecipa um subconjunto útil dos erros de organização que já apareceram no projeto.


## Correções aplicadas nesta rodada
- `NationalProceduralJuizadoExclusionResolver` recebeu import explícito de `NationalProceduralActionProfile`
- `NationalProceduralJuizadoTrackClassifier` recebeu imports explícitos de `NationalProceduralActionProfile` e `NationalProceduralPartyProfile`
- a própria sonda foi endurecida para não marcar falsos positivos em classes que apenas analisam strings de annotations
