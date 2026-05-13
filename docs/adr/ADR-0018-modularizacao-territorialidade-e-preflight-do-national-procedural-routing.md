# ADR-0018 — modularização da territorialidade e do pré-protocolo do NationalProceduralRouting

## Contexto

Mesmo após a rodada anterior, o `NationalProceduralRoutingService` ainda concentrava partes sensíveis demais do eixo procedimental:

- territorialidade e âncora de foro/comarca
- sinais de prevenção, conexão, continência e dependência
- montagem do payload usado por `TribunalProtocolRoutingService` e `ProceduralPreflightEngine`
- classificação de bloqueios estruturais do pré-protocolo
- textos operacionais ainda ligados a territorialidade e revisão de distribuição

Esse acoplamento mantinha o serviço principal extenso e dificultava a evolução segura da malha territorial e do pré-protocolo nacional.

## Decisão

Separar esses subeixos em colaboradores próprios, preservando o contrato público do serviço principal:

- `NationalProceduralTerritorialAnalysisFactory`
- `NationalProceduralLinkageAnalysisFactory`
- `NationalProceduralPreflightPayloadFactory`
- `NationalProceduralTerritorialAnchor`
- `NationalProceduralLinkageAnalysis`
- `NationalProceduralRoutingSupport`

Também foi ampliado o catálogo `NationalProceduralRoutingMessages` para absorver mensagens operacionais ligadas a:

- revisão de dependência/prevenção/conexão/continência
- intervenção operacional na distribuição final
- fundamentos textuais da âncora territorial
- razões textuais da análise de vinculação entre feitos

## Consequências

### Positivas

- territorialidade e vinculação processual deixam de ficar embutidas no serviço principal
- pré-protocolo ganha payload factory própria, reduzindo risco de regressão quando evoluir documentação, assinatura e malha de conectores
- mensagens verdes desse eixo saem do corpo do serviço e passam a ficar centralizadas em catálogo dedicado
- o serviço principal fica mais perto de uma orquestração procedural real, com menos detalhe técnico preso em um único arquivo
- a base ganha um ponto melhor para evoluir prevenção, conexão e distribuição dependente sem poluir o fluxo principal

### Custos

- mais classes auxiliares dentro do eixo procedural
- necessidade de proteger a centralização das mensagens com teste de governança adicional

## Status

Aceito.
