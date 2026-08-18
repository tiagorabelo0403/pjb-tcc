# Relatorio da Onda 2 - facades e ports oficiais

## 1. O que foi criado

- Estrategia oficial de facades e ports em `docs/architecture/facades_and_ports_strategy.md`.
- Relatorio inicial da Onda 2 em `docs/reports/modularization_wave2_facades_initial_report.md`.
- Records internos:
  - `UsuarioContextoAcordo`
  - `MovimentacaoAcordoCommand`
  - `AuditoriaAcordoCommand`
- Teste de adapters do acordo em `AcordoPortsAdaptersTest`.

## 2. O que foi alterado

- Ports do acordo foram ampliados.
- Adapters do acordo passaram a converter entity legada para record interno.
- `AcordoProcessualApplicationService` passou a registrar movimentacao e auditoria por comandos oficiais.
- ArchUnit e guard modular foram reforcados para bloquear retorno de entity JPA por port, repository em application e adapter fora de infrastructure.
- Documentacao do modulo `acordo` e plano de ondas foram atualizados.

## 3. Ports existentes

- `ProcessoAcordoPort`
- `UsuarioAcordoPort`
- `MovimentacaoAcordoPort`
- `AuditoriaAcordoPort`
- `AcordoProcessualStorePort`

## 4. Adapters existentes

- `PjbProcessoAcordoAdapter`
- `PjbUsuarioAcordoAdapter`
- `PjbMovimentacaoAcordoAdapter`
- `JpaAuditoriaAcordoAdapter`
- `JpaAcordoProcessualStoreAdapter`

## 5. Dependencias diretas removidas

Nao havia repository legado em `acordo.application`, `acordo.domain` ou `acordo.api` no inicio da onda. A mudanca removeu a chamada de rejeicao de homologacao que ainda usava movimentacao generica via `ProcessoAcordoPort` e centralizou o ato em `MovimentacaoAcordoPort`.

O fluxo interno de auditoria deixou de usar `AcordoAuditEntry` e passou a usar `AuditoriaAcordoCommand`, que expressa origem, hashes e sensibilidade do evento. O record antigo foi preservado por compatibilidade de contrato.

## 6. Dependencias diretas restantes

Permanecem apenas em `acordo.infrastructure`:

- `ProcessoRepository`
- `UsuarioRepository`
- `MovimentacaoProcessualRepository`
- `Processo`
- `Usuario`
- `MovimentacaoProcessual`

Essas dependencias permanecem porque os adapters sao a fronteira tecnica com o legado. Elas nao vazam para `application`, `domain` ou `api`.

## 7. Como isso reduz o megamonolito

Novos fluxos do acordo deixam de conversar por parametros soltos ou entities legadas. A aplicacao passa a depender de contratos pequenos e testaveis. O legado fica encapsulado nos adapters, o que permite migrar internamente por ondas sem quebrar a API do modulo.

## 8. Testes criados

- `AcordoPortsAdaptersTest`
  - Conversao de processo para `ProcessoAcordoContexto`.
  - Contexto de usuario sem entity legada.
  - Movimentacao por `MovimentacaoAcordoCommand`.
  - Auditoria sensivel por `AuditoriaAcordoCommand`.

## 9. Testes alterados

- `AcordoProcessualApplicationServiceTest`
- `AcordoProcessualChatBridgeServiceTest`
- `AcordoArchitectureTest`
- `ModularMonolithArchitectureTest`

## 10. Testes rodados

- `python -B scripts/modular_monolith_guard.py`
- `python -B scripts/architecture_hygiene_guard.py`
- `python -B scripts/constructor_injection_guard.py`
- `.\mvnw.cmd -B -pl pjb-api test-compile --no-transfer-progress`
- `.\mvnw.cmd -B -pl pjb-api test "-Dtest=*ArchitectureTest,*ArchUnit*,*Acordo*" "-DfailIfNoTests=false" --no-transfer-progress`

Resultado:

- Guard modular: `0` errors, `419` warnings.
- Guard de higiene arquitetural: OK.
- Guard de injecao por construtor: OK.
- `test-compile`: OK.
- Testes direcionados: `230` testes, `0` falhas, `0` erros, `2` ignorados.

## 11. Riscos restantes

- Documento processual ainda nao tem port de acordo porque a sala nao consome documento nesta onda.
- `magistradoId` no contexto processual permanece nulo ate haver fonte publicada segura no legado.
- Warnings legados do guard aumentaram para 419 porque a regra agora cataloga mais imports historicos, mas segue com `0` errors.
- Ciclos legados `advocacia/laiane/auditoria` continuam baselineados.
