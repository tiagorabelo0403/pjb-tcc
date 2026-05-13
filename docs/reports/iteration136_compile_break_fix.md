# Round 136 — correção de compile breaks reais

## Arquivos corrigidos

- `pjb-api/src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralActionProfile.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/procedural/NationalProceduralPartyProfile.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/processo/juizado/procedural/NationalProceduralJuizadoDecisionResolver.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/core/processo/juizado/procedural/NationalProceduralJuizadoDecisionContext.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/operations/NationalCommunicationInstitutionalOperationsController.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/controller/processual/comunicacao/institutional/panel/NationalCommunicationInstitutionalFinalController.java`
- `pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/AudienciaContexto.java`

## Correções materiais

- records nacionais usados fora do pacote tornados públicos;
- imports explícitos adicionados no fluxo decisório do juizado;
- imports Spring MVC adicionados em duas controllers institucionais;
- imports explícitos corretos de enums em `AudienciaContexto`.

## Evidência usada

- log de compile real enviado pelo usuário com falhas em `NationalProceduralJuizadoDecisionResolver`, `NationalProceduralJuizadoDecisionContext`, `NationalCommunicationInstitutionalOperationsController`, `NationalCommunicationInstitutionalFinalController` e `AudienciaContexto`.

## Validação honesta

- `runtime_concurrency_guard.py`: passou
- `architecture_hygiene_guard.py`: passou
- `constructor_injection_guard.py`: passou
- `config_taxonomy_guard.py`: passou
- `transactional_hotspot_guard.py --fail-on-missing-budgets`: passou
- `repository_layout_guard.py`: passou
- sem afirmação de build Maven global verde
- sem afirmação de compile total do `pjb-api`
