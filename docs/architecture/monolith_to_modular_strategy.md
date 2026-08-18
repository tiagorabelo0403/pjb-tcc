# Estrategia para evoluir o PJB para monolito modular

## 1. Estado atual

Classificacao: monolito modular parcial com nucleo ainda megamonolitico.

Evidencias:

- `pjb-api/src/main/java` possui 7832 arquivos Java.
- `pjb-core/src/main/java` possui 49 arquivos Java.
- Existem cerca de 395 controllers e 329 repositories.
- O pacote `com.tcc.pjb.backend.modules` existe, mas nem todos os modulos seguem a mesma fronteira.
- O modulo `acordo` ja nasceu com `domain`, `application`, `api` e `infrastructure`.
- O modulo `custas` foi migrado (2026-08-06) da estrutura legada (`core/financeiro/custas`, `model/entity/financeiro`, `controller/admin`) para a estrutura completa de 5 camadas, incluindo portas (`CustaJudicialStorePort`, `ProcessoCustaPort`) e adapters em `infrastructure`, seguindo o mesmo padrao hexagonal ja usado por `acordo`.
- Varios fluxos ainda usam `model`, `repository`, `service` e `controller` compartilhados.

Nao e correto chamar o PJB de monolito modular real ainda, porque as fronteiras entre dominios ainda nao sao uniformes nem protegidas o suficiente por testes e scripts.

## 2. Por que nao fazer refactor gigante

Um refactor grande agora seria tecnicamente perigoso:

- Pode quebrar testes por mudanca de wiring Spring.
- Pode quebrar migrations por alteracao indireta de entities.
- Pode quebrar integracoes externas e contratos HTTP.
- Pode misturar causa raiz de falhas funcionais com movimentacao de pacotes.
- Pode duplicar dominio entre legado e modulos novos.
- Pode atrasar a estabilizacao por transformar arquitetura em uma mudanca unica e dificil de revisar.

## 3. Estrategia correta

A estrategia recomendada e strangler fig interno.

Principios:

- O legado continua funcionando.
- Modulos novos nascem organizados.
- Novas violacoes claras sao proibidas.
- Violacoes antigas entram em baseline.
- A migracao ocorre por ondas pequenas.
- Cada onda tem teste e relatorio.
- Facades e ports isolam dependencias entre contextos.
- Queries pesadas migram para read models antes de mover dominio.

## 4. Bounded contexts candidatos

- processo
- documento
- seguranca
- sigilo
- acordo
- prazos
- comunicacao
- ciencia
- intimacao
- citacao
- secretaria
- magistrado
- recursal
- custas
- integracao-mni
- integracao-datajud
- ia
- auditoria
- observabilidade
- migracao-acervo

## 5. Estrutura obrigatoria de novos modulos

Novos modulos devem nascer sob:

```text
com.tcc.pjb.backend.modules.<modulo>.domain
com.tcc.pjb.backend.modules.<modulo>.application
com.tcc.pjb.backend.modules.<modulo>.infrastructure
com.tcc.pjb.backend.modules.<modulo>.web
com.tcc.pjb.backend.modules.<modulo>.api
```

Pacotes historicos como `controller`, `service`, `repository`, `entity` e `dto` dentro de `modules.*` sao tolerados como baseline legado, mas nao devem ser usados em novos modulos.

## 6. Funcao de cada camada

### domain

- Regras puras.
- Policies, state machines, value objects e enums de dominio.
- Sem Spring.
- Sem JPA.
- Sem web.
- Sem repository.

### application

- Casos de uso.
- Transacoes.
- Autorizacao.
- Auditoria.
- Ports de persistencia e integracao.
- Eventos de aplicacao.
- Sem HTTP.

### infrastructure

- JPA.
- Repositories.
- Adapters.
- Integracao com legado por portas.
- Mapeamento entre entities e snapshots.

### web

- Controller.
- DTOs de request/response.
- Bean Validation.
- Rate limit e entrada HTTP.
- Sem regra de negocio.
- Sem repository.

### api

- Ports.
- Facades.
- Eventos.
- Contracts usados por outros modulos.

## 7. Criterio de evolucao

Um contexto so deve ser considerado modular real quando:

- Tem fronteira documentada.
- Tem camada de dominio sem Spring/JPA.
- Expõe API/facade para outros modulos.
- Nao expõe repository interno para outro contexto.
- Possui testes de regra e arquitetura.
- Tem migration e persistencia isoladas quando aplicavel.
