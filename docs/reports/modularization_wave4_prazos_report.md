# Relatorio da Onda 4 de modularizacao

## 1. O que foi implementado

- Novo modulo `com.tcc.pjb.backend.modules.prazos`.
- Contrato `PrazoProcessualPort`.
- Records internos para calculo de prazo e dia forense.
- Policy de dominio `PrazoProcessualBoundaryPolicy`.
- Application service `PrazoProcessualApplicationService`.
- Adapter `LegacyPrazoProcessualAdapter` para o service legado.
- Testes de dominio, application, adapter e arquitetura.

## 2. O que foi preservado

- `PrazoProcessualNacionalService`.
- `NationalPrazoEngine`.
- `CalendarioForenseTribunalService`.
- Controllers e DTOs HTTP legados.
- Migrations e persistencia existentes.

## 3. Dependencias isoladas

Novos modulos podem consumir o contrato modular de prazos sem importar diretamente:

- `PrazoProcessualNacionalService`.
- `NationalPrazoEngine.TipoPrazo`.
- `RamoDireito`.
- `GrauJurisdicao`.
- DTOs de `model.dto.processual.prazo`.
- Repositories ou entities legadas.

## 4. Arquivos criados

- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/prazos/api/PrazoProcessualPort.java`.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/prazos/api/PrazoProcessualCalculoCommand.java`.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/prazos/api/PrazoProcessualCalculoResult.java`.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/prazos/api/PrazoDiaForenseCommand.java`.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/prazos/api/PrazoDiaForenseResult.java`.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/prazos/domain/PrazoProcessualBoundaryPolicy.java`.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/prazos/domain/PrazoProcessualParametros.java`.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/prazos/domain/PrazoProcessualDomainException.java`.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/prazos/application/PrazoProcessualApplicationService.java`.
- `pjb-api/src/main/java/com/tcc/pjb/backend/modules/prazos/infrastructure/LegacyPrazoProcessualAdapter.java`.
- `pjb-api/src/test/java/com/tcc/pjb/backend/modules/prazos/PrazosArchitectureTest.java`.
- `pjb-api/src/test/java/com/tcc/pjb/backend/modules/prazos/domain/PrazoProcessualBoundaryPolicyTest.java`.
- `pjb-api/src/test/java/com/tcc/pjb/backend/modules/prazos/application/PrazoProcessualApplicationServiceTest.java`.
- `pjb-api/src/test/java/com/tcc/pjb/backend/modules/prazos/infrastructure/LegacyPrazoProcessualAdapterTest.java`.
- `docs/architecture/modules/prazos.md`.
- `docs/reports/modularization_wave4_prazos_initial_report.md`.
- `docs/reports/modularization_wave4_prazos_report.md`.

## 5. Arquivos alterados

- `docs/architecture/facades_and_ports_strategy.md`.
- `docs/architecture/modules/README.md`.
- `docs/architecture/modularization_wave_plan.md`.

## 6. Testes rodados

- `.\mvnw.cmd -B -pl pjb-api test-compile --no-transfer-progress`: aprovado.
- `scripts/modular_monolith_guard.py`: aprovado com 0 errors, 419 warnings e 0 baseline issues.
- `scripts/architecture_hygiene_guard.py`: aprovado.
- `scripts/constructor_injection_guard.py`: aprovado.
- `.\mvnw.cmd -B -pl pjb-api test "-Dtest=PrazoProcessualBoundaryPolicyTest,PrazoProcessualApplicationServiceTest,LegacyPrazoProcessualAdapterTest,PrazosArchitectureTest,ModularMonolithArchitectureTest" "-DfailIfNoTests=false" --no-transfer-progress`: aprovado com 30 testes, 0 falhas, 0 erros e 0 ignorados.

Nesta sessao, `python` nao estava no PATH. Os guards foram executados com o Python local encontrado em `C:\Program Files\PostgreSQL\18\pgAdmin 4\python\python.exe`, preservando os scripts e parametro `-B`.

## 7. Riscos restantes

- O modulo ainda nao calcula prazo a partir de `processoId`.
- Notificacoes ainda nao possuem port modular proprio.
- Auditoria global ficou fora desta rodada.
- O adapter ainda depende do service legado, como esperado nesta etapa.

## 8. Proxima etapa recomendada

Criar `modules.notificacoes` ou uma facade de notificacoes pequena, consumindo o resultado de `modules.prazos` sem acessar repositories legados. Depois, migrar um alerta de prazo isolado.
