# ADR-0012 — Modularização interna do assembler da mesa operacional

## Contexto

Após a redução do `InstitutionalOperationalDeskGovernanceApplicationService` para orquestração curta, a maior concentração residual passou a existir em `InstitutionalOperationalDeskGovernanceAssembler`.

O assembler reunia, ao mesmo tempo:

- base estrutural da mesa operacional
- resolução de filas, fronteiras e contrapartes
- catálogo de atos por papel
- augmentação por tipo de unidade
- augmentação por eixo judicial
- fechamento final do agregado

Essa concentração reduzia legibilidade, elevava risco de regressão em alterações pequenas e deixava a trilha institucional dependente de um único arquivo grande.

## Decisão

A montagem interna da governança da mesa operacional passa a ser segmentada em colaboradores package-private especializados:

- `InstitutionalOperationalDeskGovernanceDraft`
- `InstitutionalOperationalDeskBaselineAssembler`
- `InstitutionalOperationalDeskCounterpartScopeResolver`
- `InstitutionalOperationalDeskRoleActsAssembler`
- `InstitutionalOperationalDeskUnitAugmenter`
- `InstitutionalOperationalDeskJudicialAxisAugmenter`

O `InstitutionalOperationalDeskGovernanceAssembler` permanece como coordenador da sequência de montagem e do fechamento final do agregado.

## Consequências

### Positivas

- redução do acoplamento estrutural do assembler principal
- segmentação clara por responsabilidade operacional
- menor risco de regressão ao evoluir filas, atos, contrapartes ou fluxos especializados
- preparação mais segura para extrações futuras no eixo `core/comunicacao/institucional/panel`

### Negativas

- aumento do número de classes internas da trilha
- necessidade maior de disciplina de package organization para evitar dispersão sem critério

## Guard rails

- nenhuma política de virtual threads pode ser introduzida nesses colaboradores
- o contrato público do `InstitutionalOperationalDeskGovernanceApplicationService` permanece intacto
- o estado mutável de montagem permanece concentrado no draft, evitando espalhar coleções mutáveis entre múltiplos serviços públicos
