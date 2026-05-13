# Compile Recovery Probe

- Arquivos main analisados: **6821**
- Arquivos Java totais varridos: **7847**
- Imports externos detectados: **598**
- Stubs transitórios gerados: **619**

## Resultado da compilação auxiliar dos stubs

- Erros detectados: **2**

## Resultado da compilação auxiliar do main

- Erros detectados: **250**

### Buckets

- `missing-package` -> 140
- `missing-symbol` -> 110

### Pacotes externos ainda bloqueando a probe

- `jakarta.persistence` -> 24
- `org.springframework.stereotype` -> 23
- `lombok` -> 21
- `com.fasterxml.jackson.annotation` -> 7
- `jakarta.validation.constraints` -> 6
- `org.hibernate.annotations` -> 5
- `org.springframework.data.domain` -> 5
- `org.slf4j` -> 4
- `org.springframework.data.annotation` -> 4
- `org.springframework.beans.factory.annotation` -> 4
- `org.springframework.web.socket` -> 3
- `org.springframework.data.jpa.domain.support` -> 3
- `org.springframework.scheduling.annotation` -> 2
- `org.springframework.context.annotation` -> 2
- `org.springframework.boot.autoconfigure.condition` -> 2
- `com.fasterxml.jackson.databind` -> 2
- `org.springframework.boot` -> 1
- `org.springframework.boot.autoconfigure` -> 1
- `org.springframework.boot.context.properties` -> 1
- `org.springframework.cache.annotation` -> 1
- `org.springframework.context` -> 1
- `org.springframework.kafka.core` -> 1
- `org.springframework.web.socket.handler` -> 1
- `org.springframework.web.socket.config.annotation` -> 1
- `io.camunda.zeebe.client.api.response` -> 1

### Arquivos mais tocados pelos erros

- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/Processo.java` -> 31
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/tracker/UserActivitySocketHandler.java` -> 22
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/adapter/worker/PJeSubmissionWorker.java` -> 14
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/Jurisdicao.java` -> 14
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/JurisdictionEngine.java` -> 12
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/jurisprudencia/Precedente.java` -> 12
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/Usuario.java` -> 10
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/service/jurisprudencia/search/JurisprudenceSearchEngine.java` -> 10
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/BackendApplication.java` -> 9
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/model/repository/PrecedenteRepository.java` -> 9
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/ai/agentic/api/AgenticController.java` -> 9
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/adapter/strategies/config/WebSocketConfig.java` -> 8
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/Equipe.java` -> 8
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/ai/audit/AiAuditLedger.java` -> 8
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/shared/dto/PJeAndamentoResponse.java` -> 6
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/MembroEquipe.java` -> 6
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/adapter/factory/PJeAdapterFactory.java` -> 4
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/shared/dto/PJeAutenticacaoResponse.java` -> 4
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/shared/dto/PJeSubmissaoResponse.java` -> 4
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/platform/runtime/execution/PjbExecutionOrchestrator.java` -> 3
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/platform/runtime/PjbBoundedExecutorProvider.java` -> 3
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/ai/academy/CurriculumKnowledgeService.java` -> 2
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/enums/RamoDireito.java` -> 2
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/ai/agentic/core/AgenticRunRequest.java` -> 2
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/ai/agentic/core/AgenticDomain.java` -> 2

### Amostra inicial de erros

- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/BackendApplication.java:3` · `missing-package` · package org.springframework.boot does not exist
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/BackendApplication.java:4` · `missing-package` · package org.springframework.boot.autoconfigure does not exist
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/BackendApplication.java:5` · `missing-package` · package org.springframework.boot.context.properties does not exist
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/BackendApplication.java:6` · `missing-package` · package org.springframework.cache.annotation does not exist
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/BackendApplication.java:7` · `missing-package` · package org.springframework.scheduling.annotation does not exist
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/BackendApplication.java:9` · `missing-symbol` · cannot find symbol
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/BackendApplication.java:10` · `missing-symbol` · cannot find symbol
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/BackendApplication.java:11` · `missing-symbol` · cannot find symbol
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/BackendApplication.java:12` · `missing-symbol` · cannot find symbol
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/adapter/factory/PJeAdapterFactory.java:6` · `missing-package` · package org.springframework.context does not exist
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/adapter/factory/PJeAdapterFactory.java:7` · `missing-package` · package org.springframework.stereotype does not exist
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/adapter/factory/PJeAdapterFactory.java:10` · `missing-symbol` · cannot find symbol
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/adapter/factory/PJeAdapterFactory.java:15` · `missing-symbol` · cannot find symbol
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/shared/dto/PJeAutenticacaoResponse.java:5` · `missing-package` · package lombok does not exist
- `/tmp/pjb07/pjb-api/src/main/java/com/tcc/pjb/backend/shared/dto/PJeAutenticacaoResponse.java:6` · `missing-package` · package lombok does not exist

## Notas

- Probe heurística usada para recuperação quando o Maven Wrapper não consegue baixar o Maven no ambiente.
- Os stubs são transitórios e existem apenas durante a execução da probe.
- O objetivo é separar bloqueio de classpath externo de possível drift interno do repositório.
