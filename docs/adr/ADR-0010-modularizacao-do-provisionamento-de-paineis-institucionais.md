# ADR-0010 — modularização do provisionamento de painéis institucionais

## Contexto

`InstitutionalPanelProvisioningReadinessApplicationService` concentrava resolução de dependências, heurísticas de sinais, agregação de blueprint/workspace e montagem final do payload de prontidão. Isso aumentava custo de manutenção, dificultava testes dirigidos e tornava a evolução do eixo institucional mais arriscada.

## Decisão

A trilha de provisionamento foi quebrada em componentes menores e estáveis:

- `InstitutionalPanelProvisioningDependenciesResolver`
- `InstitutionalPanelProvisioningSnapshotAccumulator`
- `InstitutionalPanelProvisioningOutcomeFactory`
- `InstitutionalPanelProvisioningSupport`
- `InstitutionalPanelProvisioningContext`
- `InstitutionalPanelProvisioningSnapshot`

`InstitutionalPanelProvisioningReadinessApplicationService` passa a atuar como orquestrador curto. A geração do timestamp de atualização também foi centralizada em `InstantSource`, permitindo snapshot determinístico em teste sem alterar o contrato público principal.

## Consequências

### Positivas

- redução da classe principal de provisionamento para orquestração curta
- separação entre resolução, sinais, agregação e montagem do resultado
- menor risco de regressão ao evoluir readiness, shared surfaces e governança de desk/hearing
- teste mais estável para o instante do snapshot

### Custos

- aumento do número de classes no pacote `panel.application`
- necessidade de preservar a coesão entre os helpers na mesma trilha institucional

## Status

Aceito.
