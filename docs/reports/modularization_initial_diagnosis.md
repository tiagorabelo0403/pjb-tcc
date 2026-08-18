# Diagnostico inicial de modularizacao do PJB

## 1. Estado inicial do git

- Diretorio: `C:\PJB`
- Branch: `master`
- HEAD inicial: `abac102 feat(acordo): integrar chat legado à sala de acordo processual`
- `origin/master`: alinhado com `HEAD` no inicio da rodada.
- `git status --short`: sem arquivos modificados ou nao rastreados.
- Observacao operacional: o Git emite aviso de permissao ao ler `C:\Users\tiago\.config\git\ignore`, sem indicar sujeira no workspace.

## 2. Tamanho aproximado do pjb-api

- Producao Java em `pjb-api/src/main/java`: 7832 arquivos.
- Testes Java em `pjb-api/src/test/java`: 1310 arquivos.
- A superficie e grande o suficiente para exigir governanca automatizada, baseline e migracao por ondas.

## 3. Tamanho aproximado do pjb-core

- Producao Java em `pjb-core/src/main/java`: 49 arquivos.
- O volume indica que o nucleo compartilhado ainda nao absorve a maior parte das regras puras do dominio.

## 4. Quantidade aproximada de controllers

- Arquivos com `@RestController` ou `@Controller`: 395.
- A quantidade mostra uma superficie HTTP grande e historicamente distribuida em varios pacotes.

## 5. Quantidade aproximada de repositories

- Arquivos declarando `@Repository`, `interface *Repository` ou `class *Repository`: 329.
- Arquivos com alguma referencia a `Repository`: 1025.

## 6. Indicios de megamonolito

- `pjb-api` concentra quase todo o codigo de producao, enquanto `pjb-core` ainda e pequeno.
- Existem muitos controllers e repositories expostos em pacotes compartilhados.
- A busca por imports suspeitos retornou 3867 ocorrencias de `repository`, `controller` ou `web` em imports.
- Ha 154 ocorrencias de `findAll(` em producao, varias em services/application flows.
- O pacote legado `com.tcc.pjb.backend.model` ainda funciona como area compartilhada ampla para entities, DTOs e repositories.
- Existem testes arquiteturais legados desabilitados para controller importando repository e ownership de entities.

## 7. Indicios de monolito modular parcial

- Ja existe `com.tcc.pjb.backend.modules`.
- O modulo `acordo` segue fronteira moderna com `domain`, `application`, `api` e `infrastructure`.
- Existem varios testes ArchUnit e testes de refinamento estrutural.
- Ha scripts de higiene arquitetural e injecao por construtor.
- Alguns subdominios em `core` ja possuem camadas mais explicitas, especialmente comunicacao institucional, processo, seguranca e auditoria.

## 8. Dependencias suspeitas

- Controllers e services legados ainda dependem diretamente de repositories compartilhados.
- Modulos parciais como `atendimento`, `advocacia`, `laiane` e `balcao` usam estrutura historica `controller/service/repository/entity`, diferente do padrao novo.
- Ha imports de web em configuracoes, filtros e controllers, que sao validos, mas dificultam varredura grosseira.
- Ha uso de repositories de outros contextos sem facade em diversos services legados.
- Ha consultas totais em services e application flows que devem virar consultas paginadas, read models ou facades.

## 9. Regras ArchUnit existentes

- Existem muitas regras ArchUnit por pacote e por convencao de nomes.
- `PjbMonolithModularConventionsArchTest` ja limita alguns usos de `util`, `service` e enums em controller.
- `PjbArchitectureTest` possui regras gerais importantes, mas duas estao desabilitadas por baseline legado.
- `AcordoArchitectureTest` ja valida parte da fronteira do modulo `acordo`.
- Nao foi encontrada dependencia de Spring Modulith configurada.

## 10. Riscos de refactor gigante

- Alto risco de quebrar contratos HTTP existentes.
- Alto risco de afetar migrations, entidades legadas e queries.
- Alto risco de misturar causa raiz de falhas funcionais com movimentacao de pacotes.
- Alto risco de duplicar dominio entre `model`, `core` e `modules`.
- Alto custo de revalidacao, pois a suite ja tem milhares de testes.

## 11. Recomendacao tecnica

Classificar o PJB como monolito modular parcial com nucleo ainda megamonolitico. A estrategia correta e strangler fig interno:

- Preservar legado funcionando.
- Exigir estrutura modular para novos modulos.
- Bloquear novas violacoes claras em `modules.*`.
- Catalogar divida antiga em baseline.
- Migrar por ondas pequenas, cada uma com teste, relatorio e evidencia.

## 12. Confirmacao de que nao houve push

Nao houve push durante o diagnostico inicial desta rodada.
