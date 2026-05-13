\# CLAUDE.md — PJB (Processo Judicial Brasileiro)



\## Missão

Substituir integralmente PJe, e-SAJ, eProc, Creta e Projudi no Brasil.

Sistema distribuído Java 21 — monólito modular com fronteiras de domínio rígidas.



\## Stack obrigatória

\- Java 21 — Virtual Threads, Records, Pattern Matching, Sealed Classes

\- Spring Boot 3.x, Spring Framework 6.x

\- Maven multi-module: `pjb-core` (domínio) + `pjb-api` (surface)

\- PostgreSQL com RLS por operação

\- Docker Compose para infra local



\## Estrutura do projeto

pjb-core/     → domínio, aggregates, services, ports

pjb-api/      → controllers, DTOs, filtros, configuração Spring

docs/adr/     → 53 ADRs — ler antes de qualquer decisão arquitetural

docs/quality/ → regras de higiene e taxonomia

scripts/      → guards Python — rodar para validar antes de commitar

config/       → checkstyle e spotbugs ativos

## ADRs críticos — ler sempre antes de alterar

\- ADR-0001: nomenclatura de pacotes

\- ADR-0007: política centralizada de Virtual Threads

\- ADR-0040: blindagem contínua de Virtual Threads

\- ADR-0041: autorização ABAC, sigilo e auditoria

\- ADR-0051: governança unificada de execução assíncrona

\- ADR-0052: runtime guardrails unificados

\- ADR-0053: faseamento transacional de serviços analíticos



\## Regras invioláveis de código

\- DI exclusivamente por construtor — zero `@Autowired` em fields

\- Zero comentários redundantes no código

\- Sem placeholders, sem métodos inventados, sem APIs alucinadas

\- Sem `CompletableFuture` solto — seguir ADR-0051

\- Transações curtas — sem I/O pesado dentro de `@Transactional`

\- Sem vazamento de thread pool ou conexão de banco

\- Sem injeção de repository entre módulos

\- Preservar README.md sempre



\## Regras invioláveis de teste

\- O número de falhas JAMAIS pode aumentar

\- Corrigir testes apenas se estiverem desatualizados em relação ao código

\- Nunca usar `null` para esconder erro em teste

\- Verificar efeito cascata antes de qualquer alteração



\## Protocolo de correção obrigatório

1\. Ler o log completo antes de propor qualquer coisa

2\. Identificar causa raiz — não sintoma

3\. Varrer classes irmãs para avaliar efeito cascata

4\. Pesquisar se necessário — zero alucinação

5\. Corrigir de forma arquitetural, não cosmética

6\. Validar com os guards Python em `scripts/`



\## Guards disponíveis em scripts/

\- `constructor\_injection\_guard.py` — valida DI por construtor

\- `runtime\_concurrency\_guard.py` — valida Virtual Threads

\- `transactional\_hotspot\_guard.py` — detecta transações longas

\- `architecture\_hygiene\_guard.py` — higiene geral

\- `import\_sanity\_probe.py` — imports inválidos

\- `internal\_reference\_drift\_guard.py` — drift entre módulos

\- `test\_drift\_guard.py` — drift de testes



\## Estado atual

\- \~8.363 arquivos Java, \~2.235 testes, 53 ADRs

\- Objetivo imediato: zerar falhas remanescentes sem adicionar nenhuma nova

\- Round 28AE em andamento



\## Como rodar

```bash

\# Compilar core

./mvnw install -pl pjb-core -DskipTests



\# Compilar api

./mvnw test-compile -pl pjb-api



\# Rodar testes

./mvnw test -pl pjb-api



\# Rodar guard específico

python scripts/constructor\_injection\_guard.py

```

