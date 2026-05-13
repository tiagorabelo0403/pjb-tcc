# Compile Recovery Probe

- Modo: **targeted**
- Arquivos seed analisados: **17**
- Arquivos main analisados após fechamento interno: **48**
- Arquivos Java totais varridos: **7847**
- Imports externos detectados: **598**
- Stubs transitórios gerados: **620**

## Resultado da compilação auxiliar dos stubs

- Erros detectados: **0**

## Resultado da compilação auxiliar do main

- Erros detectados: **178**

### Buckets

- `generic-shape-mismatch` -> 1
- `missing-symbol` -> 166
- `other` -> 11

### Pacotes externos ainda bloqueando a probe

- Nenhum pacote externo ausente detectado nesta execução.

### Arquivos mais tocados pelos erros

- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/JurisdictionEngine.java` -> 92
- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/Jurisdicao.java` -> 19
- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/service/jurisprudencia/search/JurisprudenceSearchEngine.java` -> 15
- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/enums/RitoProcessual.java` -> 14
- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/adapter/worker/PJeSubmissionWorker.java` -> 10
- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/tracker/UserActivitySocketHandler.java` -> 10
- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/core/procedural/ProceduralCatalogSupport.java` -> 6
- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/Processo.java` -> 4
- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/workflow/zeebe/ZeebeCompat.java` -> 3
- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/core/procedural/ProceduralRoutingReport.java` -> 2
- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/adapter/strategies/config/WebSocketConfig.java` -> 2
- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/adapter/factory/PJeAdapterFactory.java` -> 1

### Amostra inicial de erros

- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/enums/RitoProcessual.java:259` · `missing-symbol` · cannot find symbol
- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/enums/RitoProcessual.java:354` · `missing-symbol` · cannot find symbol
- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/core/procedural/ProceduralRoutingReport.java:32` · `missing-symbol` · cannot find symbol
- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/core/procedural/ProceduralRoutingReport.java:33` · `missing-symbol` · cannot find symbol
- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/Jurisdicao.java:66` · `missing-symbol` · cannot find symbol
- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/Jurisdicao.java:71` · `missing-symbol` · cannot find symbol
- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/Jurisdicao.java:76` · `missing-symbol` · cannot find symbol
- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/Jurisdicao.java:81` · `missing-symbol` · cannot find symbol
- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/Jurisdicao.java:86` · `missing-symbol` · cannot find symbol
- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/Jurisdicao.java:128` · `missing-symbol` · cannot find symbol
- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/Jurisdicao.java:228` · `missing-symbol` · cannot find symbol
- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/Jurisdicao.java:228` · `missing-symbol` · cannot find symbol
- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/Jurisdicao.java:229` · `missing-symbol` · cannot find symbol
- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/Jurisdicao.java:229` · `missing-symbol` · cannot find symbol
- `/tmp/pjb_recovery_08/pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/Jurisdicao.java:230` · `missing-symbol` · cannot find symbol

## Notas

- Probe heurística usada para recuperação quando o Maven Wrapper não consegue baixar o Maven no ambiente.
- Os stubs são transitórios e existem apenas durante a execução da probe.
- O objetivo é separar bloqueio de classpath externo de possível drift interno do repositório.
