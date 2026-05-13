# ADR-0016 — modularização do NationalProcessRouting e centralização das mensagens operacionais

## Contexto

O `NationalProcessRoutingService` concentrava validação de entrada, inferência de justiça/competência, política de roteamento, construção de narrativas operacionais e montagem de metadata. Além disso, mantinha mensagens operacionais relevantes diretamente no corpo do serviço.

## Decisão

A trilha foi reorganizada em colaboradores internos:

- `NationalProcessRoutingSupport`
- `NationalProcessRoutingDecisionPolicy`
- `NationalProcessRoutingNarrativeFactory`
- `NationalProcessRoutingMetadataFactory`
- `NationalProcessRoutingMessages`

O serviço principal permanece como orquestrador curto, mantendo o contrato externo e delegando a lógica estrutural para peças menores.

## Consequências

- política de roteamento mais legível
- metadata separada da narrativa operacional
- mensagens verdes realocadas para classe canônica
- menor risco de regressão ao evoluir `core/processual/routing`
