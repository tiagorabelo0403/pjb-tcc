# Modularização de `custas` — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Levar o bounded context `custas` (motor de custas judiciais — GRU, PIX, isenção por rito) da estrutura legada espalhada (`core/financeiro/custas/`, `model/entity/financeiro/`, `model/repository/`, `controller/admin/`) para a estrutura completa de 5 camadas já validada em `com.tcc.pjb.backend.modules.acordo`, sem mudar nenhuma regra de negócio.

**Architecture:** ver `docs/architecture/specs/2026-08-07-custas-modularizacao-design.md` para o desenho completo e o porquê de cada decisão (por que `custas` e não outro candidato, por que `@PjbDataOwnership` não é tocado, por que `GruCodigoBarrasGenerator` vai pra `api/` e `PixPayloadGenerator` fica em `domain/`).

**Tech Stack:** Java 21, Spring Boot 3, `git mv` (preserva histórico), `sed`/find-replace mecânico de package/import (nunca recriação manual de arquivo), ArchUnit (`com.tngtech.archunit`, já é dependência do projeto — ver `PrazosArchitectureTest`), `scripts/modular_monolith_guard.py` (guard Python já existente, `maxErrors: 0`).

## Global Constraints

- **Nenhuma regra de negócio muda.** Todo método, toda assinatura, toda lógica de `CustaJudicialService`/`CustasApplicationService`/`CustaIsencaoPolicy`/`CustaIsencaoPorRitoPolicy`/os 46 arquivos de `domain` permanece byte-a-byte idêntica — só `package` e `import` mudam.
- **Usar `git mv`, nunca recriar arquivo do zero.** Preserva histórico git, elimina risco de erro de transcrição em arquivos grandes.
- **Cada task compila (`./mvnw test-compile -pl pjb-api`) antes de prosseguir pra próxima.** Uma task que não compila não é "completa", mesmo que os arquivos já estejam no lugar certo.
- **Verificar com `grep` que zero referências ao pacote antigo restam** ao final de cada task que move arquivos — critério de conclusão objetivo, não visual.
- **`@PjbDataOwnership` não é tocado nesta fatia** — nem adicionado nem removido de `CustaJudicial`. Ver spec para o porquê (é metadado morto, nem `acordo` nem `prazos` o usam).
- Todos os 13 testes de `custas` devem continuar verdes com as mesmas asserções — só pacote/import mudam nos testes também.
- `scripts/modular_monolith_guard.py` deve terminar com **0 errors** e sem estourar nenhum orçamento de warning do baseline atual (`docs/architecture/modular_monolith_guard_baseline.json`) — essa é a governança que realmente bloqueia build, não `@PjbDataOwnership`.

---

### Task 1: Mover `domain/` (46 arquivos) pra `modules.custas.domain`

**Files:**
- Move (diretório inteiro): `pjb-api/src/main/java/com/tcc/pjb/backend/core/financeiro/custas/domain/` → `pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/domain/` (44 arquivos)
- Move: `pjb-api/src/main/java/com/tcc/pjb/backend/core/financeiro/custas/CustaIsencaoPolicy.java` → `pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/domain/CustaIsencaoPolicy.java`
- Move: `pjb-api/src/main/java/com/tcc/pjb/backend/core/financeiro/custas/PixPayloadGenerator.java` → `pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/domain/PixPayloadGenerator.java`

**Interfaces:**
- Produces: todo tipo hoje em `com.tcc.pjb.backend.core.financeiro.custas.domain.*` passa a viver em `com.tcc.pjb.backend.modules.custas.domain.*` — mesmos nomes de classe, mesma API pública. Tasks 2-6 consomem essa nova localização.

Confirmado por grep antes desta task: nenhum arquivo dentro de `core/financeiro/custas/domain/*.java` importa outro arquivo do próprio pacote `custas` (zero `import com.tcc.pjb.backend.core.financeiro.custas` dentro de `domain/*.java`) — a única mudança necessária nesses 44 arquivos é a linha `package`. `CustaIsencaoPolicy.java` importa `domain.IsencaoCustaResult`/`domain.TipoCusta` (mesmo pacote depois do move, sem import necessário) e `model.entity.Processo` (fora do escopo desta migração, não muda). `PixPayloadGenerator.java` importa só `domain.PixResult` (idem) e `java.math.BigDecimal`.

- [ ] **Step 1: `git mv` do diretório `domain/` inteiro**

```bash
git mv pjb-api/src/main/java/com/tcc/pjb/backend/core/financeiro/custas/domain pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/domain
```

- [ ] **Step 2: `git mv` dos 2 arquivos soltos**

```bash
git mv pjb-api/src/main/java/com/tcc/pjb/backend/core/financeiro/custas/CustaIsencaoPolicy.java pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/domain/CustaIsencaoPolicy.java
git mv pjb-api/src/main/java/com/tcc/pjb/backend/core/financeiro/custas/PixPayloadGenerator.java pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/domain/PixPayloadGenerator.java
```

- [ ] **Step 3: Corrigir a linha `package` nos 46 arquivos movidos**

```bash
grep -rl "^package com\.tcc\.pjb\.backend\.core\.financeiro\.custas\.domain;" pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/domain/ | xargs sed -i 's/^package com\.tcc\.pjb\.backend\.core\.financeiro\.custas\.domain;/package com.tcc.pjb.backend.modules.custas.domain;/'
```

Isso corrige os 44 arquivos que já tinham `package ....custas.domain;`. Os 2 arquivos soltos (`CustaIsencaoPolicy.java`, `PixPayloadGenerator.java`) tinham `package com.tcc.pjb.backend.core.financeiro.custas;` (sem `.domain`) — corrigir manualmente com Edit, trocando essa linha específica pra `package com.tcc.pjb.backend.modules.custas.domain;` em cada um dos 2 arquivos.

- [ ] **Step 4: Verificar que não sobrou nenhuma referência ao pacote antigo nos arquivos movidos**

```bash
grep -rn "core\.financeiro\.custas" pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/domain/
```

Expected: nenhuma linha (saída vazia). Se aparecer algo, é um import que precisa da mesma correção do Step 3.

- [ ] **Step 5: Compilar só pra confirmar que este passo isolado não quebrou nada de óbvio**

Run: `./mvnw test-compile -pl pjb-api` — **esperado FALHAR** neste ponto, porque `CustaJudicialService`, `CustasApplicationService`, `CustaIsencaoPorRitoPolicy`, `CustasConfiguration`, `GruCodigoBarrasGenerator` (ainda não movidos) importam `com.tcc.pjb.backend.core.financeiro.custas.domain.*`, que não existe mais. Confirmar que os únicos erros de compilação são exatamente "cannot find symbol" apontando pros imports desses arquivos ainda não migrados — nenhum outro tipo de erro. Isso prova que o move em si foi limpo; os erros restantes são esperados e fechados pelas próximas tasks.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor(custas): move domain/ (46 arquivos) pra modules.custas.domain"
```

---

### Task 2: Mover `GruCodigoBarrasGenerator` pra `modules.custas.api`

**Files:**
- Move: `pjb-api/src/main/java/com/tcc/pjb/backend/core/financeiro/custas/GruCodigoBarrasGenerator.java` → `pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/api/GruCodigoBarrasGenerator.java`

**Interfaces:**
- Consumes: `modules.custas.domain.GruResult` (Task 1).
- Produces: `com.tcc.pjb.backend.modules.custas.api.GruCodigoBarrasGenerator` — interface funcional `GruResult gerar(String tipoCusta, BigDecimal valor, String uf)`. Task 3 (infrastructure, que implementa via bean) e Task 6 (os 2 consumidores externos em `core/financeiro/trabalhista/`) consomem essa nova localização.

- [ ] **Step 1: `git mv`**

```bash
git mv pjb-api/src/main/java/com/tcc/pjb/backend/core/financeiro/custas/GruCodigoBarrasGenerator.java pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/api/GruCodigoBarrasGenerator.java
```

- [ ] **Step 2: Corrigir `package` e o import de `GruResult`**

O arquivo tem `package com.tcc.pjb.backend.core.financeiro.custas;` e `import com.tcc.pjb.backend.core.financeiro.custas.domain.GruResult;`. Trocar por:

```java
package com.tcc.pjb.backend.modules.custas.api;

import com.tcc.pjb.backend.modules.custas.domain.GruResult;
```

- [ ] **Step 3: Compilar**

Run: `./mvnw test-compile -pl pjb-api` — mesmos erros esperados da Task 1 (menos um, já que este arquivo está resolvido), nenhum erro novo introduzido por este arquivo especificamente.

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "refactor(custas): move GruCodigoBarrasGenerator pra modules.custas.api"
```

---

### Task 3: Mover `infrastructure/` (entidade + repositório + configuração — 3 arquivos) pra `modules.custas.infrastructure`

**Files:**
- Move: `pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/financeiro/CustaJudicial.java` → `pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/infrastructure/persistence/CustaJudicial.java`
- Move: `pjb-api/src/main/java/com/tcc/pjb/backend/model/repository/CustaJudicialRepository.java` → `pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/infrastructure/persistence/CustaJudicialRepository.java`
- Move: `pjb-api/src/main/java/com/tcc/pjb/backend/core/financeiro/custas/CustasConfiguration.java` → `pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/infrastructure/CustasConfiguration.java`

**Interfaces:**
- Consumes: `modules.custas.domain.TipoCusta` (Task 1, usado pela entidade), `modules.custas.api.GruCodigoBarrasGenerator` (Task 2), `modules.custas.domain.{GruResult,PixResult}` (Task 1).
- Produces: `com.tcc.pjb.backend.modules.custas.infrastructure.persistence.CustaJudicial` (entidade JPA) e `.CustaJudicialRepository` — Task 4 (application) consome. `com.tcc.pjb.backend.modules.custas.infrastructure.CustasConfiguration` — sem consumidor direto fora do Spring context scan, só precisa compilar e continuar registrando os beans `gruCodigoBarrasGenerator`/`pixPayloadGenerator`.

- [ ] **Step 1: `git mv` dos 3 arquivos**

```bash
git mv pjb-api/src/main/java/com/tcc/pjb/backend/model/entity/financeiro/CustaJudicial.java pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/infrastructure/persistence/CustaJudicial.java
git mv pjb-api/src/main/java/com/tcc/pjb/backend/model/repository/CustaJudicialRepository.java pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/infrastructure/persistence/CustaJudicialRepository.java
git mv pjb-api/src/main/java/com/tcc/pjb/backend/core/financeiro/custas/CustasConfiguration.java pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/infrastructure/CustasConfiguration.java
```

- [ ] **Step 2: Corrigir `CustaJudicial.java`**

Trocar a linha `package com.tcc.pjb.backend.model.entity.financeiro;` por `package com.tcc.pjb.backend.modules.custas.infrastructure.persistence;`. Trocar `import com.tcc.pjb.backend.core.financeiro.custas.domain.TipoCusta;` por `import com.tcc.pjb.backend.modules.custas.domain.TipoCusta;`. As importações de `com.tcc.pjb.backend.core.modularity.PjbModuleId`, `com.tcc.pjb.backend.core.ownership.PjbDataOwnership`, `com.tcc.pjb.backend.core.ownership.PjbOwnershipMode` e a anotação `@PjbDataOwnership(module = PjbModuleId.PROCESSO_LIFECYCLE, ...)` na classe **não mudam** — constraint global desta fatia, não mexer nessa anotação.

- [ ] **Step 3: Corrigir `CustaJudicialRepository.java`**

Trocar `package com.tcc.pjb.backend.model.repository;` por `package com.tcc.pjb.backend.modules.custas.infrastructure.persistence;`. Trocar `import com.tcc.pjb.backend.model.entity.financeiro.CustaJudicial;` por nada (mesmo pacote depois do move, sem import necessário) ou manter como import explícito se preferir clareza — ambos compilam; **usar sem import** (mesmo pacote) pra bater com o padrão do resto do arquivo.

- [ ] **Step 4: Corrigir `CustasConfiguration.java`**

Trocar `package com.tcc.pjb.backend.core.financeiro.custas;` por `package com.tcc.pjb.backend.modules.custas.infrastructure;`. Trocar:
```java
import com.tcc.pjb.backend.core.financeiro.custas.domain.GruResult;
import com.tcc.pjb.backend.core.financeiro.custas.domain.PixResult;
```
por:
```java
import com.tcc.pjb.backend.modules.custas.api.GruCodigoBarrasGenerator;
import com.tcc.pjb.backend.modules.custas.domain.GruResult;
import com.tcc.pjb.backend.modules.custas.domain.PixPayloadGenerator;
import com.tcc.pjb.backend.modules.custas.domain.PixResult;
```

`GruCodigoBarrasGenerator` e `PixPayloadGenerator` (os tipos de retorno dos métodos `@Bean`) precisam de import explícito agora porque saíram do pacote onde `CustasConfiguration` vivia antes — antes eram mesmo pacote (`core.financeiro.custas`), sem import necessário; agora `CustasConfiguration` está em `infrastructure` e as interfaces estão em `api`/`domain`, pacotes diferentes.

- [ ] **Step 5: Verificar que não sobrou referência ao pacote antigo nos 3 arquivos**

```bash
grep -n "core\.financeiro\.custas\|model\.entity\.financeiro\.CustaJudicial\|model\.repository\.CustaJudicialRepository" pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/infrastructure/persistence/CustaJudicial.java pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/infrastructure/persistence/CustaJudicialRepository.java pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/infrastructure/CustasConfiguration.java
```

Expected: nenhuma linha.

- [ ] **Step 6: Compilar**

Run: `./mvnw test-compile -pl pjb-api` — erros restantes devem ser só em `CustaJudicialService.java`/`CustasApplicationService.java`/`CustaIsencaoPorRitoPolicy.java`/`AdminCustasController.java` (ainda não movidos) apontando pros caminhos antigos de `CustaJudicial`/`CustaJudicialRepository`/pacote `core.financeiro.custas`.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor(custas): move entidade, repositorio e configuracao pra modules.custas.infrastructure"
```

---

### Task 4: Mover `application/` (3 arquivos) pra `modules.custas.application`

**Files:**
- Move: `pjb-api/src/main/java/com/tcc/pjb/backend/core/financeiro/custas/CustaJudicialService.java` → `pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/application/CustaJudicialService.java`
- Move: `pjb-api/src/main/java/com/tcc/pjb/backend/core/financeiro/custas/CustasApplicationService.java` → `pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/application/CustasApplicationService.java`
- Move: `pjb-api/src/main/java/com/tcc/pjb/backend/core/financeiro/custas/CustaIsencaoPorRitoPolicy.java` → `pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/application/CustaIsencaoPorRitoPolicy.java`

**Interfaces:**
- Consumes: `modules.custas.domain.*` (Task 1), `modules.custas.infrastructure.persistence.{CustaJudicial,CustaJudicialRepository}` (Task 3).
- Produces: `com.tcc.pjb.backend.modules.custas.application.{CustaJudicialService,CustasApplicationService,CustaIsencaoPorRitoPolicy}` — Task 5 (web) consome `CustasApplicationService`.

- [ ] **Step 1: `git mv`**

```bash
git mv pjb-api/src/main/java/com/tcc/pjb/backend/core/financeiro/custas/CustaJudicialService.java pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/application/CustaJudicialService.java
git mv pjb-api/src/main/java/com/tcc/pjb/backend/core/financeiro/custas/CustasApplicationService.java pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/application/CustasApplicationService.java
git mv pjb-api/src/main/java/com/tcc/pjb/backend/core/financeiro/custas/CustaIsencaoPorRitoPolicy.java pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/application/CustaIsencaoPorRitoPolicy.java
```

- [ ] **Step 2: Corrigir `CustaJudicialService.java`**

Trocar `package com.tcc.pjb.backend.core.financeiro.custas;` por `package com.tcc.pjb.backend.modules.custas.application;`.

Trocar todas as 21 linhas `import com.tcc.pjb.backend.core.financeiro.custas.domain.X;` (X = `CustaJudicialResult`, `RegistrarPagamentoCustaResult`, `RegistrarPagamentoCustaCommand`, `GerarCustaJudicialCommand`, `GruResult`, `PixResult`, `CustaConsultaCommand`, `CustaConsultaResult`, `CustaPagamentoCommandSnapshot`, `CustaPagamentoAuditSnapshot`, `GruEmissaoSnapshot`, `PixPayloadSnapshot`, `CustaTimelineEntry`, `CustaConsultaTimelineCommand`, `CustaConsultaTimelineResult`, `CustaStatusSnapshot`, `CustaVencimentoSnapshot`, `TipoCusta`, `CustaJudicialView`, `PixCobrancaView`) por `import com.tcc.pjb.backend.modules.custas.domain.X;` — mesma lista de nomes, só o prefixo do pacote muda. Forma mais segura de fazer isso num arquivo só:

```bash
sed -i 's/com\.tcc\.pjb\.backend\.core\.financeiro\.custas\.domain\./com.tcc.pjb.backend.modules.custas.domain./g' pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/application/CustaJudicialService.java
```

Trocar também:
```java
import com.tcc.pjb.backend.model.entity.financeiro.CustaJudicial;
import com.tcc.pjb.backend.model.repository.CustaJudicialRepository;
```
por:
```java
import com.tcc.pjb.backend.modules.custas.infrastructure.persistence.CustaJudicial;
import com.tcc.pjb.backend.modules.custas.infrastructure.persistence.CustaJudicialRepository;
```

`import com.tcc.pjb.backend.model.entity.Processo;` e `import com.tcc.pjb.backend.model.repository.ProcessoRepository;` **não mudam** — `Processo` fica onde está, fora do escopo desta migração.

- [ ] **Step 3: Corrigir `CustasApplicationService.java`**

Trocar `package com.tcc.pjb.backend.core.financeiro.custas;` por `package com.tcc.pjb.backend.modules.custas.application;`. Trocar os imports `com.tcc.pjb.backend.core.financeiro.custas.domain.{CustaConsultaCommand,CustaConsultaTimelineCommand,CustaHealthQuery,GerarCustaJudicialCommand,TipoCusta}` pro prefixo `modules.custas.domain.` (mesmo padrão `sed` do Step 2). As referências totalmente qualificadas inline no corpo do método (ex.: `com.tcc.pjb.backend.core.financeiro.custas.domain.CustaJudicialResult`) também precisam da mesma troca de prefixo — o mesmo comando `sed` acima cobre essas ocorrências também, já que ele substitui a string em qualquer lugar do arquivo, não só nas linhas de import:

```bash
sed -i 's/com\.tcc\.pjb\.backend\.core\.financeiro\.custas\.domain\./com.tcc.pjb.backend.modules.custas.domain./g' pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/application/CustasApplicationService.java
```

- [ ] **Step 4: Corrigir `CustaIsencaoPorRitoPolicy.java`**

Trocar `package com.tcc.pjb.backend.core.financeiro.custas;` por `package com.tcc.pjb.backend.modules.custas.application;`. Trocar os imports de `domain.IsencaoCustaResult`/`domain.TipoCusta` pro novo prefixo (mesmo `sed`). `import com.tcc.pjb.backend.model.entity.Processo;`, `import com.tcc.pjb.backend.model.entity.enums.RamoDireito;`, `import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;` **não mudam**.

- [ ] **Step 5: Verificar que não sobrou referência ao pacote antigo nos 3 arquivos**

```bash
grep -n "core\.financeiro\.custas" pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/application/*.java
```

Expected: nenhuma linha.

- [ ] **Step 6: Compilar**

Run: `./mvnw test-compile -pl pjb-api` — erros restantes devem ser só em `AdminCustasController.java` e nos 2 arquivos externos de `core/financeiro/trabalhista/`.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor(custas): move application/ (CustaJudicialService, CustasApplicationService, CustaIsencaoPorRitoPolicy) pra modules.custas.application"
```

---

### Task 5: Mover `AdminCustasController` pra `modules.custas.web`

**Files:**
- Move: `pjb-api/src/main/java/com/tcc/pjb/backend/controller/admin/AdminCustasController.java` → `pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/web/AdminCustasController.java`

**Interfaces:**
- Consumes: `modules.custas.application.CustasApplicationService` (Task 4), `modules.custas.domain.TipoCusta` (Task 1).

- [ ] **Step 1: `git mv`**

```bash
git mv pjb-api/src/main/java/com/tcc/pjb/backend/controller/admin/AdminCustasController.java pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/web/AdminCustasController.java
```

- [ ] **Step 2: Corrigir `package` e imports**

Trocar `package com.tcc.pjb.backend.controller.admin;` por `package com.tcc.pjb.backend.modules.custas.web;`. Trocar `import com.tcc.pjb.backend.core.financeiro.custas.CustasApplicationService;` por `import com.tcc.pjb.backend.modules.custas.application.CustasApplicationService;` e `import com.tcc.pjb.backend.core.financeiro.custas.domain.TipoCusta;` por `import com.tcc.pjb.backend.modules.custas.domain.TipoCusta;`. Ler o arquivo primeiro pra confirmar se há mais algum import de `core.financeiro.custas.*` além desses dois antes de fechar a task — não presumir que a lista está completa sem checar o arquivo real.

- [ ] **Step 3: Verificar**

```bash
grep -n "core\.financeiro\.custas\|controller\.admin" pjb-api/src/main/java/com/tcc/pjb/backend/modules/custas/web/AdminCustasController.java
```

Expected: nenhuma linha (a segunda parte do grep é só pra garantir que a classe não ficou com auto-referência ao pacote antigo por engano).

- [ ] **Step 4: Compilar**

Run: `./mvnw test-compile -pl pjb-api` — erros restantes devem ser só nos 2 arquivos externos de `core/financeiro/trabalhista/` e nos 13 arquivos de teste.

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor(custas): move AdminCustasController pra modules.custas.web"
```

---

### Task 6: Atualizar os 2 consumidores externos (`core/financeiro/trabalhista/`)

**Files:**
- Modify: `pjb-api/src/main/java/com/tcc/pjb/backend/core/financeiro/trabalhista/WorkflowTrabalhistaService.java`
- Modify: `pjb-api/src/main/java/com/tcc/pjb/backend/core/financeiro/trabalhista/TrabalhistaWorkflowConfiguration.java`

**Interfaces:**
- Consumes: `modules.custas.api.GruCodigoBarrasGenerator` (Task 2), `modules.custas.domain.GruResult` (Task 1). Estes são os ÚNICOS 2 arquivos de todo o repositório, fora do módulo `custas`, que dependem de algo do módulo — confirmado por grep exaustivo na investigação desta fatia.

Estes 2 arquivos **não movem** — continuam em `core/financeiro/trabalhista/`, fora de qualquer módulo. Só o import muda.

- [ ] **Step 1: Corrigir `WorkflowTrabalhistaService.java`**

Trocar `import com.tcc.pjb.backend.core.financeiro.custas.GruCodigoBarrasGenerator;` por `import com.tcc.pjb.backend.modules.custas.api.GruCodigoBarrasGenerator;`.

- [ ] **Step 2: Corrigir `TrabalhistaWorkflowConfiguration.java`**

Trocar:
```java
import com.tcc.pjb.backend.core.financeiro.custas.GruCodigoBarrasGenerator;
import com.tcc.pjb.backend.core.financeiro.custas.domain.GruResult;
```
por:
```java
import com.tcc.pjb.backend.modules.custas.api.GruCodigoBarrasGenerator;
import com.tcc.pjb.backend.modules.custas.domain.GruResult;
```

- [ ] **Step 3: Verificar**

```bash
grep -rn "core\.financeiro\.custas" pjb-api/src/main/java/com/tcc/pjb/backend/core/financeiro/trabalhista/
```

Expected: nenhuma linha.

- [ ] **Step 4: Confirmar que este é o fim das referências ao pacote antigo em TODO `src/main`**

```bash
grep -rn "com\.tcc\.pjb\.backend\.core\.financeiro\.custas" pjb-api/src/main/java/
```

Expected: nenhuma linha em lugar nenhum de `src/main`. Se algo aparecer, é um consumidor que a investigação original não pegou — parar e investigar antes de prosseguir, não presumir que está tudo certo.

- [ ] **Step 5: Compilar**

Run: `./mvnw test-compile -pl pjb-api` — erros restantes devem ser só nos 13 arquivos de teste (Task 7).

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "refactor(custas): atualiza os 2 consumidores externos em core/financeiro/trabalhista pra modules.custas.api"
```

---

### Task 7: Mover e corrigir os 13 arquivos de teste

**Files:**
- Move (mantendo o mesmo layout de subpacote que os arquivos principais, espelhado em `src/test`):
  - `pjb-api/src/test/java/com/tcc/pjb/backend/core/financeiro/custas/CustaIsencaoPorRitoPolicyTest.java` → `.../modules/custas/application/CustaIsencaoPorRitoPolicyTest.java`
  - `.../core/financeiro/custas/CustaJudicialFlowIT.java` → `.../modules/custas/application/CustaJudicialFlowIT.java`
  - `.../core/financeiro/custas/CustaJudicialRepositoryIT.java` → `.../modules/custas/infrastructure/persistence/CustaJudicialRepositoryIT.java`
  - `.../core/financeiro/custas/CustaJudicialServiceCommandHelpersTest.java` → `.../modules/custas/application/CustaJudicialServiceCommandHelpersTest.java`
  - `.../core/financeiro/custas/CustaJudicialServiceConsultaTest.java` → `.../modules/custas/application/CustaJudicialServiceConsultaTest.java`
  - `.../core/financeiro/custas/CustaJudicialServiceIsencaoTest.java` → `.../modules/custas/application/CustaJudicialServiceIsencaoTest.java`
  - `.../core/financeiro/custas/CustaJudicialServicePagamentoTest.java` → `.../modules/custas/application/CustaJudicialServicePagamentoTest.java`
  - `.../core/financeiro/custas/CustaJudicialServiceSnapshotsTest.java` → `.../modules/custas/application/CustaJudicialServiceSnapshotsTest.java`
  - `.../core/financeiro/custas/CustaJudicialServiceTest.java` → `.../modules/custas/application/CustaJudicialServiceTest.java`
  - `.../core/financeiro/custas/CustaJudicialServiceViewsTest.java` → `.../modules/custas/application/CustaJudicialServiceViewsTest.java`
  - `.../core/financeiro/custas/CustasApplicationServiceTest.java` → `.../modules/custas/application/CustasApplicationServiceTest.java`
  - `.../core/financeiro/custas/domain/TipoCustaTest.java` → `.../modules/custas/domain/TipoCustaTest.java`
  - `.../controller/admin/AdminCustasControllerTest.java` → `.../modules/custas/web/AdminCustasControllerTest.java`

**Interfaces:**
- Consumes: todas as classes principais já movidas nas Tasks 1-5.

Regra pra decidir o subpacote de teste de cada arquivo: mesmo subpacote da classe principal testada (`CustaJudicialServiceXxxTest`/`CustaJudicialFlowIT`/`CustaIsencaoPorRitoPolicyTest`/`CustasApplicationServiceTest` testam classes de `application/`, então vão pra `modules/custas/application/`; `CustaJudicialRepositoryIT` testa a classe de `infrastructure/persistence/`, vai pra lá; `TipoCustaTest` testa um record de `domain/`, vai pra lá; `AdminCustasControllerTest` testa o controller de `web/`, vai pra lá).

- [ ] **Step 1: `git mv` dos 13 arquivos** (um comando por arquivo, usando os caminhos exatos listados em Files acima)

- [ ] **Step 2: Corrigir `package` e imports em cada um dos 13 arquivos**

Para cada arquivo: a linha `package` precisa apontar pro novo subpacote de teste (espelhando o Files acima). Os imports que referenciam `core.financeiro.custas.*` (incluindo `.domain.*`) precisam do mesmo tratamento `sed` das tasks anteriores:

```bash
sed -i 's/com\.tcc\.pjb\.backend\.core\.financeiro\.custas\.domain\./com.tcc.pjb.backend.modules.custas.domain./g; s/com\.tcc\.pjb\.backend\.core\.financeiro\.custas\./com.tcc.pjb.backend.modules.custas.application./g' <arquivo>
```

Atenção: esse comando genérico assume que a classe testada mudou pra `application` — não é verdade pra `CustaJudicialRepositoryIT` (infrastructure) nem `AdminCustasControllerTest` (web) nem `TipoCustaTest` (domain, mas esse já não tem esse padrão de import já que é só o record). Ler cada um dos 13 arquivos antes de aplicar qualquer substituição automática e ajustar o segundo padrão do `sed` (`.application.`) pro pacote de destino correto de cada arquivo específico — não copiar o comando cegamente pros 13 sem checar. Também corrigir manualmente os imports de `model.entity.financeiro.CustaJudicial`/`model.repository.CustaJudicialRepository` pra `modules.custas.infrastructure.persistence.*` onde aparecerem (provavelmente em `CustaJudicialRepositoryIT`, `CustaJudicialFlowIT`, e os testes de `CustaJudicialService*`), e `controller.admin.AdminCustasController` pra `modules.custas.web.AdminCustasController` em `AdminCustasControllerTest`.

- [ ] **Step 3: Verificar que não sobrou referência a nenhum pacote antigo em nenhum dos 13**

```bash
grep -rln "core\.financeiro\.custas\|model\.entity\.financeiro\.CustaJudicial\|model\.repository\.CustaJudicialRepository\|controller\.admin\.AdminCustasController" pjb-api/src/test/java/com/tcc/pjb/backend/modules/custas/
```

Expected: nenhuma linha.

- [ ] **Step 4: Compilar**

Run: `./mvnw test-compile -pl pjb-api` — deve compilar limpo agora (0 erros restantes, esta é a última peça).

- [ ] **Step 5: Rodar os testes unitários dos 13 (excluindo os 2 IT, que exigem Postgres real via Testcontainers)**

Run: `./mvnw test -pl pjb-api -Dtest=CustaIsencaoPorRitoPolicyTest,CustaJudicialServiceCommandHelpersTest,CustaJudicialServiceConsultaTest,CustaJudicialServiceIsencaoTest,CustaJudicialServicePagamentoTest,CustaJudicialServiceSnapshotsTest,CustaJudicialServiceTest,CustaJudicialServiceViewsTest,CustasApplicationServiceTest,TipoCustaTest,AdminCustasControllerTest`

Expected: mesmo número de testes que passava antes da migração (comparar com a contagem original de cada arquivo se possível), 0 falhas, 0 erros.

- [ ] **Step 6: Rodar os 2 ITs** (`CustaJudicialFlowIT`, `CustaJudicialRepositoryIT` — exigem Testcontainers Postgres, ~15-25min neste ambiente por causa da suíte unitária completa rodar antes)

Run: `./mvnw verify -pl pjb-api -Dsurefire.skip=true -Dit.test=CustaJudicialFlowIT,CustaJudicialRepositoryIT -DfailIfNoTests=true`

Expected: BUILD SUCCESS, 0 falhas.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor(custas): move os 13 testes pra modules.custas, espelhando o layout de producao"
```

---

### Task 8: Teste de arquitetura + guard + atualização da estratégia + verificação final

**Files:**
- Create: `pjb-api/src/test/java/com/tcc/pjb/backend/modules/custas/CustasArchitectureTest.java`
- Modify: `docs/architecture/monolith_to_modular_strategy.md`

**Interfaces:**
- Consumes: nenhuma classe de produção diretamente — o teste analisa o pacote `com.tcc.pjb.backend.modules.custas` como um todo via ArchUnit.

- [ ] **Step 1: Criar `CustasArchitectureTest.java`, espelhando `PrazosArchitectureTest.java` linha por linha**

```java
package com.tcc.pjb.backend.modules.custas;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

@AnalyzeClasses(packages = "com.tcc.pjb.backend.modules.custas", importOptions = {ImportOption.DoNotIncludeTests.class, ImportOption.DoNotIncludeJars.class})
class CustasArchitectureTest {

    @ArchTest
    static final ArchRule domain_nao_depende_de_spring_ou_jpa =
            noClasses().that().resideInAPackage("..modules.custas.domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("org.springframework..", "jakarta.persistence..", "javax.persistence..");

    @ArchTest
    static final ArchRule application_nao_depende_de_web =
            noClasses().that().resideInAPackage("..modules.custas.application..")
                    .should().dependOnClassesThat().resideInAnyPackage("..modules.custas.web..", "org.springframework.web..");

    @ArchTest
    static final ArchRule application_nao_acessa_repository =
            noClasses().that().resideInAPackage("..modules.custas.application..")
                    .should().dependOnClassesThat().haveSimpleNameEndingWith("Repository");

    @ArchTest
    static final ArchRule infrastructure_nao_depende_de_web =
            noClasses().that().resideInAPackage("..modules.custas.infrastructure..")
                    .should().dependOnClassesThat().resideInAnyPackage("..modules.custas.web..", "org.springframework.web..");

    @ArchTest
    static final ArchRule ports_nao_retornam_entity_legada =
            noMethods().that().areDeclaredInClassesThat().resideInAPackage("..modules.custas.api..")
                    .and().areDeclaredInClassesThat().haveSimpleNameEndingWith("Port")
                    .should().haveRawReturnType(com.tngtech.archunit.core.domain.JavaClass.Predicates.resideInAPackage("..model.entity.."));
}
```

Nota: a última regra (`ports_nao_retornam_entity_legada`) é vazia de efeito prático aqui porque `GruCodigoBarrasGenerator` não termina em `Port` (mantido o nome original pra não forçar rename sem necessidade) — incluída mesmo assim por fidelidade ao padrão de `PrazosArchitectureTest`, e porque protege qualquer `*Port` que vier a ser adicionado no futuro.

- [ ] **Step 2: Rodar o teste de arquitetura**

Run: `./mvnw test -pl pjb-api -Dtest=CustasArchitectureTest`
Expected: PASS (5/5 regras).

- [ ] **Step 3: Rodar o guard Python**

```bash
python scripts/modular_monolith_guard.py
```

Ler `docs/reports/modular_monolith_guard_report.md` gerado. Expected: `errors=0`, `baseline_issues=0` (nenhum orçamento de warning estourado). Se `errors > 0` ou `baseline_issues > 0`, ler o relatório completo, identificar exatamente qual regra disparou e em qual arquivo, e corrigir antes de prosseguir — não é uma dívida aceitável nesta fatia, o objetivo inteiro é zero erro novo.

- [ ] **Step 4: Atualizar `docs/architecture/monolith_to_modular_strategy.md`**

Adicionar uma nota na seção 1 ("Estado atual") registrando que `custas` é o segundo módulo (depois de `acordo`) com a estrutura completa de 5 camadas, com a data desta fatia. Não reescrever o documento inteiro — só adicionar essa linha de status.

- [ ] **Step 5: Rodar `test-compile` completo e a suíte unitária completa**

Run: `./mvnw test-compile -pl pjb-api` — BUILD SUCCESS.
Run: `./mvnw test -pl pjb-api` — comparar a contagem total de testes com a contagem de antes desta fatia começar (deve ser a mesma, só re-localizados); 0 falhas, 0 erros novos.

- [ ] **Step 6: Commit**

```bash
git add -A
git commit -m "test(custas): adiciona CustasArchitectureTest, confirma guard modular limpo, atualiza estrategia"
```

---

## Fechamento

Após a Task 8 verde: atualizar `docs/quality/DEBT_LOG.md` se aplicável (não há dívida específica de modularização de custas registrada hoje — conferir antes de assumir que não precisa de entrada nova), e seguir para `superpowers:finishing-a-development-branch`.
