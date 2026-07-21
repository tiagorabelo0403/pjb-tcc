![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven&logoColor=white)
![Unit Tests](https://img.shields.io/badge/Unit%20Tests-4%2C138%20%7C%200%20failures-brightgreen)
![Integration Tests](https://img.shields.io/badge/IT%20Tests-230%20%7C%200%20known%20failures-brightgreen)
![ADRs](https://img.shields.io/badge/ADRs-57-informational)
![License](https://img.shields.io/badge/License-MIT-blue)

# PJB — Brazilian Judicial Platform

> A next-generation electronic judicial system built on Java 21 and Spring Boot 3.5, designed to fully replace PJe, e-SAJ, eProc, Creta, and Projudi across all segments of the Brazilian justice system.

**Languages / Idiomas:** [🇬🇧 English (this file)](./README.en.md) · [🇧🇷 Português](./README.md)

---

## Quick Navigation

**Quick Start**
- [About the Project](#about-the-project)
- [The Problem](#the-problem)
- [The Proposal](#the-proposal)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Running the Application](#running-the-application)
- [Tests](#tests)
- [API Documentation](#api-documentation)

**Architecture & Domain**
- [Domain](#domain)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Functional Modules](#functional-modules)
- [Smart Accelerators](#smart-accelerators)
- [Procedural Types Covered](#procedural-types-covered)

**Infrastructure & Quality**
- [Security & Compliance](#security--compliance)
- [Concurrency and Async Execution](#concurrency-and-async-execution)
- [Scalability and Operational Resilience](#scalability-and-operational-resilience)
- [Database](#database)
- [Code Quality](#code-quality)
- [Observability](#observability)
- [National Replacement](#national-replacement)

**Contribution & Project**
- [Contributing](#contributing)
- [Safe Git Sync](#safe-git-sync)
- [Next Steps](#next-steps)
- [Author](#author)
- [License](#license)
- [Glossary](#glossary)

---

## About the Project

PJB is a total-replacement platform — not an incremental patch — for the electronic judicial systems currently running in Brazil. Five systems were built over decades by different entities, with no coordination of protocol, data model, or interface. Today, this fractured infrastructure supports more than **80 million active cases**, **91 courts**, and **approximately 30,000 judges**, alongside tens of millions of lawyers, litigants, and court staff — and none of those systems were designed to talk to each other.

PJB was built from scratch to solve this problem properly. It is not a wrapper around legacy systems. It is a deliberate break from that model: domain modeled from the Brazilian Civil Procedure Code (CPC/2015), labor reforms, and current criminal legislation; attribute-based access control with row-level security at the database; an immutable audit trail on every action; and Java 21 Virtual Threads to scale without the cost of managing manual thread pools.

---

## The Problem

| System | Primary Court | Core Issue |
|--------|--------------|-----------|
| PJe | CNJ / most courts | Tight coupling between UI and domain; routes without contracts |
| e-SAJ | TJSP, TJBA, other state courts | Proprietary data model; no public API |
| eProc | TRF1, TRF4, state courts | Isolated jobs; fragile digital signatures |
| Creta | Labor Justice | Low observability; no support for new procedural types |
| Projudi | Smaller state courts | Critical technical debt; no migration path |

None of the five were designed for horizontal scalability, granular access auditing, or complete support for all procedural classes under the CPC/2015 and labor reforms. PJB does not attempt to rewrite them. It replaces the entire model.

---

## The Proposal

PJB was designed from scratch around three non-negotiable commitments:

**1. Total traceability.** Every decision involving access, distribution, movement, or communication produces an auditable, immutable, and explainable trail. There is no action in the system that cannot be reconstructed — who did it, when, under what authority, and what the effect was.

**2. Testability as acceptance criteria.** No feature exists without verifiable behavior. The test suite is the system's executable contract — if the test passes, the behavior is guaranteed. A feature without a test is not a feature: it is intent.

**3. Security by construction.** ABAC, per-operation RLS, governed propagation of confidential context, and Step-up Gov.br are not layers added afterward. They are constraints that guide every architectural decision from the start — before the first endpoint, before the first migration, before the first line of domain code.

---

## Prerequisites

Before cloning and running the project, make sure you have the following installed:

| Tool | Minimum Version | Purpose |
|------|----------------|---------|
| **JDK** | 21 | Compilation and execution (Virtual Threads required) |
| **Maven** | 3.9+ | Multi-module build (`pjb-core` + `pjb-api`) |
| **Docker** | 24+ | PostgreSQL, Kafka, Redis, Elasticsearch via Compose |
| **Docker Compose** | v2 (plugin) | Local infrastructure orchestration |
| **Python** | 3.10+ | Structural guards in `scripts/` |

> **Recommended IDE:** IntelliJ IDEA 2024+ with the Checkstyle and SonarLint plugins active. The project uses Java 21 records, sealed classes, and pattern matching — older IDE versions may not recognize the full syntax.

---

## Installation & Setup

### 1. Clone the Repository

```bash
git clone https://github.com/tiagorabelo0403/pjb-tcc.git
cd pjb-tcc
```

### 2. Configure Environment Variables

```bash
cp .env.example .env
```

Open `.env` and fill in the required variables:

| Variable | Description | Example |
|----------|-------------|---------|
| `PJB_PG_HOST` | PostgreSQL host | `localhost` |
| `PJB_PG_PORT` | PostgreSQL port | `5432` |
| `PJB_PG_PASSWORD` | Database password | `pgpassword` |
| `PJB_MASTER_KEY_BASE64` | Master encryption key (Base64, 32 bytes) | generated by script |
| `PJB_ANTHROPIC_API_KEY` | Anthropic API key for AI modules | `sk-ant-...` |
| `PJB_KAFKA_BOOTSTRAP` | Kafka broker address | `localhost:9092` |

> For demo environments, `.env.example` already contains working values that `demo.sh` / `demo.cmd` uses automatically.

### 3. Start Infrastructure

```bash
docker compose up -d
```

This starts PostgreSQL 17, Apache Kafka 3.8, Redis 7.4, and Elasticsearch 8.15. Flyway migrations (numbered up to V306) are applied automatically on the first backend connection.

### 4. Check Spring Profiles

The project uses separate Spring Boot profiles per environment. The base file is `application.yml`; each profile overrides only what changes:

| Profile | File | When to Use |
|---------|------|------------|
| `dev` | `application-dev.yml` | Local development with infrastructure in Docker |
| `local` | `application-local.yml` | Database and services running directly on the host |
| `docker` | `application-docker.yml` | Backend inside a Docker container |
| `prod` | `application-prod.yml` | Production — requires all environment variables |
| `k8s` | `application-k8s.yml` | Kubernetes |

For local development, the `dev` profile is recommended. It is activated automatically by `demo.sh` / `demo.cmd`. To activate manually:

```bash
# Via Maven
./mvnw spring-boot:run -pl pjb-api -Dspring-boot.run.profiles=dev

# Via environment variable
export SPRING_PROFILES_ACTIVE=dev
java -jar pjb-api/target/pjb-api.jar
```

### 5. Build

```bash
# Build the domain module
./mvnw install -pl pjb-core -DskipTests

# Build the API module (includes test class generation)
./mvnw test-compile -pl pjb-api
```

---

## Running the Application

### Full Quickstart (Recommended)

The demo script does everything in sequence: copies `.env`, compiles, starts infrastructure, applies migrations, and waits for the backend to become healthy.

```bash
# Linux / macOS
bash demo.sh

# Windows
demo.cmd
```

### Run Backend Only (Infrastructure Already Running)

```bash
# Via Maven Wrapper (recommended for development)
./mvnw spring-boot:run -pl pjb-api

# Via packaged JAR
./mvnw package -pl pjb-api -DskipTests
java -jar pjb-api/target/pjb-api.jar
```

### Full Stack via Docker (Build + Infrastructure Together)

```bash
docker compose --profile app up -d --build
```

The `backend` service is in the `app` profile. Without it, Compose only starts the supporting infrastructure. If port `5432` is already in use locally, set `PJB_PG_PORT=5433` in `.env` — the backend in Docker continues accessing `postgres:5432` via the internal Compose network.

### Available Endpoints

| Endpoint | Description |
|----------|-------------|
| `http://localhost:8080/livez` | Liveness check |
| `http://localhost:8080/demo/status` | Real-time statistics |
| `http://localhost:8080/swagger-ui/index.html` | Interactive API documentation |
| `http://localhost:8080/v3/api-docs` | OpenAPI 3.1 specification (JSON) |
| `http://localhost:8080/actuator/health` | Full health check |
| `http://localhost:8080/actuator/metrics` | Micrometer metrics |

With the `docker` profile, the system automatically seeds demo users and cases so data appears immediately.

**To stop:**
```bash
docker compose down
```

---

## Tests

The project has two test levels with very different characteristics:

- **Unit tests (Surefire):** 4,138 tests with Mockito and in-memory H2. Fast, no Docker required.
- **Integration tests (Failsafe):** 230 tests against real PostgreSQL and Kafka via Testcontainers. Requires Docker. Slower.

### Run Unit Tests Only (fast)

```bash
./mvnw test -pl pjb-api
```

Expected time: **~15 min** on local hardware. Does not require Docker.

### Run the Full Suite Including Integration Tests (official gate)

```bash
./mvnw verify -pl pjb-api
```

This is the official project gate. It runs the 4,138 unit tests (Surefire) and then the 230 integration tests (Failsafe) against real PostgreSQL 17 and Kafka containers. Testcontainers handles container lifecycle automatically — no manual setup needed.

Expected time: **~50 min** on local hardware. Most of this time is the Spring context boot with Testcontainers and the IT tests that perform real HTTP requests against the running server. A full verify produces a complete diagnostic of every failure cluster in the suite — if you are investigating a problem, this is the number that matters, not the `test` output alone.

> **Why so slow?** Each IT class boots a full Spring context with a real PostgreSQL, applies the Flyway migrations, and executes requests the way an external client would. That gives full confidence that what passed in test will pass in production — but it costs time.

The Surefire/Failsafe `argLine` sets `-Dpjb.runtime.lifecycle.drain-quiet-period=10ms`. The graceful drain coordinator (`PjbRuntimeDrainCoordinator`) sleeps 20s by default on every Spring context close — correct in production, where there is real traffic to drain before shutdown, but pure waste in a test JVM. Without this override, a full `verify` run can exceed Surefire's own 30s fork-exit watchdog (`forkedProcessExitTimeoutInSeconds`) and force-kill the forked JVM at teardown, even with every test already green — a symptom that only shows up on long full-suite runs, never in an isolated class.

### Run a Specific Test with Full Stack Trace

```bash
./mvnw test -pl pjb-api -Dtest=TestClassName -DtrimStackTrace=false
```

### Current Metrics

| Metric | Phase | Value |
|--------|-------|-------|
| Total unit tests | Surefire | **4,138** |
| Unit test failures | Surefire | **0** |
| Skipped | Surefire | 5 |
| Unit test execution time | Surefire | **~15 min** |
| Total integration tests | Failsafe | **230** ¹ |
| Polo-composition-engine tests | Failsafe | **+10 green** (role by procedural type: ACUSACAO, RECLAMANTE, IMPETRANTE, SEGURADO…) |
| IT failures | Failsafe | **0** (0E + 0F) |
| Full verify execution time | Surefire + Failsafe | **~50 min** |

The integration suite went through a full stabilization process: 49 failures at the start, 14 after eliminating clusters CG-1 (22E — wrong environment variable), CG-2-Postgres (5E — cross-test data contamination from unclean fixtures), CG-3 (3E — hardcoded IDs without seeding), and CG-7 (1E); 10 after closing `ConsultaPublicaSearchFlowIT` and the 3 `ProcessoCommandControllerIT` (debt D-d25-testes-anexo); and **0** after closing `D-routing-preprotocolo` and the remaining 9 pre-existing failures. Two of those fixes touched production bugs, not just test setup: `AuditLedgerService` recorded audit events only in memory, without persisting to the repository the audit endpoints actually query; and root-proceeding resolution in `CaseContinuityOrchestratorService` used a field that changes state during the case lifecycle, causing ambiguity between the root proceeding and its branches (e.g., judgment enforcement) after archiving. The 10 polo-composition-by-procedural-type tests are all green and are not part of the failure history. The territorial competence slice added 24 more tests after reaching zero, green since creation — 9 from `Trt7CearaJurisdicaoCargaIT` (TRT7/CE), 7 from `Trt3MgJurisdicaoCargaIT` (TRT3/MG), and 8 from `Trt21RnJurisdicaoCargaIT` (TRT21/RN) — also outside the failure history.

The 206 confirmed across 5 batches via explicit `-Dtest=` (goal `test`/Surefire, not `verify`/Failsafe) — same `argLine` and same default 10-minute timeout between the two plugins, but a different goal identity than the one CI uses in the official gate.

¹ The default `verify` (Failsafe) does not reach 10 test methods spread across 5 classes (`OabLegitimidadePeticionamentoTest`, `PjbFluxoJudicialCompletoE2ETest`, `DistribuicaoProcessoProtocoladoTest`, `ConsultaPublicaProcessoProtocoladoTest`, `ApiMarketplaceServicePoloMaterializacaoTest`) — the `*Test.java` name combined with `@Tag("integration")` makes Surefire exclude by tag while Failsafe does not recognize the file pattern. The 10 have already been confirmed green individually (`-Dit.test=`), but do not enter this count since they run outside the routine `verify`.

### Coverage Report (JaCoCo)

```bash
./mvnw test -pl pjb-api
# Report generated at:
# pjb-api/target/site/jacoco/index.html
```

---

## API Documentation

PJB exposes complete interactive documentation via **Swagger UI**, available after starting the backend:

```
http://localhost:8080/swagger-ui/index.html
```

The OpenAPI 3.1 specification is available at:

```
http://localhost:8080/v3/api-docs
```

Versioned contracts are also documented statically in:

```
docs/openapi/
```

Every REST route is registered in the canonical bounded context registry. The `PjbOpenApiContractWeaknessDetectorTest` automatically validates that no route exists without a registered OpenAPI contract, no field uses `Map<String,Object>` without a typed schema, and all dates follow `format: date-time`.

---

## Domain

### Actors

| Actor | Role in the System |
|-------|-------------------|
| **Judge** | Issues decisions, signs documents, manages their docket |
| **Court Clerk** | Performs clerical acts, issues certificates, moves cases |
| **Lawyer / Public Defender** | Files petitions, tracks deadlines, accesses records per confidentiality rules |
| **Prosecutor / Attorney General** | Acts on cases within their jurisdiction and instance |
| **Party / Litigant** | Accesses what the law allows, without identifying the judge |
| **Institutional Administrator** | Configures courts, jurisdictions, calendars, and access |
| **External System** | PJe, e-SAJ, eProc, MNI, PDPJ — integrated via canonical envelope |

### Core Domain Concepts

**Judicial case** is the root aggregate. It has an NPU (Unique Process Number), CNJ procedural class, subject, case value, procedural type, parties, representatives, and movements. Each case exists within a jurisdiction with defined subject-matter and territorial competence.

**Procedural type** defines the mandatory flow: which phases exist, which deadlines apply, which acts are possible in each phase. The catalog is sealed — no procedural type can be invented at runtime. This prevents the system from accepting invalid configurations.

**Distribution** is the act of assigning a case to a court. The distribution engine evaluates nature, competence, procedural type, district, unit workload, and court rules. Each distribution decision produces an auditable explanation with all evaluated criteria.

**Movement** is any act on the case: ruling, interlocutory decision, judgment, decree, certificate, order. Each movement has an author, timestamp, integrity hash, and link to the corresponding procedural act.

**Confidentiality** is a cross-cutting dimension. A confidential case restricts visibility down to the database record level via Row Level Security. Confidentiality propagation in asynchronous operations is governed — never leaked.

**Jurisdiction** is the structural unit of competence: a court, a chamber, a judicial section. It has degree, sphere, nature, subject-matter competence, and territorial scope. The jurisdiction hierarchy models all segments: federal, state, labor, electoral, military.

### Bounded Contexts

| Context | Responsibility |
|---------|---------------|
| `institucional` | Organizations, courts, assignments, competencies, affiliations, credentials |
| `processo` | Case, movements, parties, deadlines, distribution |
| `documentos` | Documents, dossier, chain of custody, signatures |
| `comunicacao` | Orders, certificates, electronic domicile, notifications |
| `seguranca` | ABAC, authentication, audit, confidentiality, Gov.br, ICP-Brasil |
| `criminal` | Occurrence reports, police investigations, institutional precincts, hierarchical police scope |
| `analytics` | Process mining, bottlenecks, Justice in Numbers reports |
| `ia` | Auditable legal AI, Memory Stores, Dreams, reflective synthesis |
| `integracao` | Canonical PDPJ/MNI envelope, PJe/e-SAJ/eProc normalizers |
| `advocacia` | Law firm, delegations, signature queues, workspace |
| `laiane` | Specialized legal assistance module via AI |

---

## Architecture

### Module Structure

The project follows hexagonal architecture with strict separation between domain and infrastructure:

```
pjb/
├── pjb-core/                         pure domain — zero Spring dependency
│   └── src/main/java/
│       └── com/tcc/pjb/core/
│           ├── domain/               aggregates, entities, value objects
│           ├── service/              application services and domain services
│           ├── port/                 output interfaces (repository, messaging)
│           └── ia/                   legal AI ports
│
├── pjb-api/                          adapters — Spring Boot, JPA, HTTP
│   └── src/main/java/
│       └── com/tcc/pjb/backend/
│           ├── controller/           REST endpoints per bounded context
│           ├── model/entity/         JPA entities
│           ├── model/repository/     Spring Data repositories
│           ├── core/                 application and domain services
│           ├── configs/              Spring, Security, OpenAPI, DataSource
│           └── modules/              specialized modules (laiane, advocacia)
│
├── docs/
│   ├── adr/                          57 Architecture Decision Records
│   ├── database/                     schemas and RLS policies
│   ├── openapi/                      public API contracts
│   ├── security/                     LGPD and Gov.br policies
│   └── product/                      national replacement matrix
│
├── scripts/                          Python guards — continuous structural hygiene
├── config/                           Checkstyle and SpotBugs
└── infra/                            Kubernetes, gateway, infrastructure
```

### Layers and Dependencies

```
┌─────────────────────────────────────────┐
│              pjb-api                    │
│  Controllers · JPA Entities · Config    │
│  Spring Boot · Security · OpenAPI       │
├─────────────────────────────────────────┤
│              pjb-core                   │
│  Domain Services · Application Services │
│  Aggregates · Value Objects · Ports     │
└─────────────────────────────────────────┘
         ↑ depends on, never the reverse
```

`pjb-core` has no knowledge of Spring, JPA, or HTTP. All injection is constructor-based with `@Inject` (Jakarta). Repositories are port interfaces in `pjb-core`; JPA implementations live in `pjb-api`.

### Applied Architectural Patterns

| Pattern | Where | Why |
|---------|-------|-----|
| Hexagonal (Ports & Adapters) | Global structure | Isolate domain from infrastructure |
| Aggregate Pattern (DDD) | `Processo`, `Jurisdicao`, `Usuario` | Domain invariants enforced at the aggregate boundary |
| Outbox Pattern | Post-commit effects | Zero event loss on transaction failure |
| Lightweight CQRS | Analytics and projections | Materialized reads without write-path pressure |
| Sealed Classes | `RitoProcessual`, `TipoJurisdicao` | Closed catalog; exhaustiveness checked at compile time |
| Virtual Threads (Java 21) | All async execution | High concurrency without manual pool sizing |
| Scoped Values (Java 21) | Confidentiality propagation | Confidential context never leaks across Virtual Threads |
| Structured Concurrency | Multi-type operations | Child failure cancels siblings; no resource leak |

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 — Virtual Threads, Records, Sealed Interfaces, Pattern Matching |
| Framework | Spring Boot 3.5, Spring Framework 6 |
| Build | Maven multi-module (`pjb-core` + `pjb-api`) |
| Database | PostgreSQL 17 with Row Level Security per operation |
| Test Database | In-memory H2 + Testcontainers |
| Migrations | Flyway — V0–V296, with monthly partitioning on event tables |
| Persistence | JPA / Hibernate with `ddl-auto: validate` in production |
| Messaging | Apache Kafka 3.8 — judicial events and outbox |
| Cache | Redis 7.4 |
| Search | Elasticsearch 8.15 |
| Security | Spring Security, ABAC, Gov.br, ICP-Brasil, Passkey/WebAuthn |
| Resilience | Resilience4j — Auditable Circuit Breaker, Bulkhead, Retry, Timeout |
| Contracts | Pact — Consumer-Driven Contract Testing |
| Legal AI | Anthropic Claude API — Memory Stores, Dreams, reflective synthesis |
| Observability | Micrometer, Spring Actuator, materialized Process Mining |
| Static Analysis | Qodana (JetBrains), JaCoCo, Checkstyle, SpotBugs, ArchUnit |
| Structural Guards | 7 Python scripts + ArchUnit integrated into CI |
| Containerization | Docker Compose (dev/test), Kubernetes (production) |

---

## Functional Modules

### 1 — Institutional Governance

Manages role, assignment, location, competence, and visibility of each actor in the case. The visibility matrix produces an auditable explanation for every access decision — who can see what, for what reason, with an immutable record.

Includes management of affiliations, institutional credentials, official source attestation, and formal delegations between units.

### 2 — Procedural Engine and Intelligent Distribution

Distributes cases by nature, competence, procedural type, and district. Supports single courts, small-town districts, itinerant Small Claims Courts, and any tribunal configuration. The explainable engine documents every criterion evaluated in the distribution decision — no distribution is a black box.

Territorial competence is a property of the procedural type (`CriterioTerritorial` maps CPC art. 47/48/53-II, CLT art. 651, and CPP art. 70) — a procedural type without a verified criterion returns an explicit absence, it never assumes the defendant's domicile by default. The `tb_jurisdicao_territorial` catalog resolves the municipality (by IBGE code) to the competent unit(s) via `CompetenciaTerritorialResolver`, with temporal-overlap exclusion guaranteed by the schema itself (a PostgreSQL `EXCLUDE` constraint, not application-level validation) and native support for a municipality with concurrent competence across courts — Belo Horizonte has 48 concurrent labor courts in a single catalog row, Fortaleza 18, Natal 13.

Three Labor Justice regions were loaded with real data, extracted from an official TST PDF and cross-checked against the IBGE locality API — not as national coverage, but as a demonstration that the engine works end to end without a schema redesign between regions:

| Region | Municipalities | Units (courts) | Municipality-court pairs | Source |
|--------|-----------|-------------------|----------------------|-------|
| TRT7 — Ceará | 184 | 37 | 288 | `End07.pdf` |
| TRT3 — Minas Gerais | 847 | 155 | 1,498 | `End03.pdf` |
| TRT21 — Rio Grande do Norte | 129 | 20 | 411 | `End21.pdf` |
| **Total** | **1,160** | **212** | **2,197** | — |

Each load matched the municipality name from the PDF against the official IBGE list by 7-digit code and state — never by name alone. Cross-state homonyms are real and were proven, not hypothesized: São Gonçalo do Amarante (RN and CE) and Ouro Branco (RN and MG) resolve to different courts from the same name in dedicated tests — it is the IBGE code that guarantees correct resolution, not the name text. Spelling divergences between the PDF and the official registry (accents, hyphens, swapped "de/do/dos", and one popular name IBGE never formalized — Boa Saúde, registered since 1953 as Januário Cicco) were resolved by a single confirmed match against each state's full list, never by approximation; a name that did not match was left out and is documented.

`vigencia_inicio` uses a presumed date (the 1988 Constitution's promulgation) for continuity across all three regions — a decision kept even where the source document carried a real per-court installation date (the TRT3/MG case, with Belo Horizonte courts installed between 1941 and 2013), because the current schema only supports one `vigencia_inicio` per municipality row, not per individual court (`D-vigencia-trt7-e-futuras-regioes-presumida-nao-documentada`, `docs/quality/DEBT_LOG.md`). Two recurring inconsistencies in the TST's primary source were recorded as debt instead of being silently worked around: duplicate court codes between physically distinct units (3 pairs in MG, 3 pairs in RN, for different reasons in each region — `D-trt3-codigo-unidade-duplicado-fonte`) and municipalities with no documented court (6 in MG, likely delegated to the district's judge; 38 in RN covered by an Advanced Post with no formally assigned code — `D-trt3-municipios-sem-vara-competencia-delegada`, `D-trt21-posto-avancado-sem-codigo`).

Each of the three loads is locked by a permanent regression test against the source document — the municipality-to-court distribution is re-parsed independently of the script that generated the migration before it becomes an `assert`, so a future migration change, or a migration from another region that accidentally corrupts data via a table-name mistake, gets caught instead of silently accepted.

### 3 — Constitutional Timeliness Engine

Monitors constitutional deadlines by procedural type, calculates systemic bottlenecks, and suggests accelerators by area of law. It does not put pressure on individual judges — it identifies where the system is slow and why, using aggregated and anonymous data.

### 4 — Internal Panel and Clerical Registry

Intelligent queues with semantic prioritization, similarity groupings, batch signing with mandatory verification, and SHA-256 hash per document. Every clerical act has full traceability: who did it, when, with what result, and what state the case was in.

### 5 — Area-Specific Legal Accelerators

Specialized flows for civil, criminal, labor, electoral, family, enforcement, Small Claims Courts (civil, federal, and public treasury), bankruptcy, and concentrated constitutionality review. Each area has a computable checklist, risk diagnosis, and suggested next act.

### 6 — Smart Tags and Conciliation

Semantic case markers for automatic prioritization by urgency, complexity, and settlement probability. The conciliation module suggests settlements based on similar precedents, with a probability score, calculated BATNA, and proposal history.

### 7 — Documents, Dossier, and Chain of Custody

Each document has origin, operational state, integrity hash, and a verifiable chain of trust. The documentary dossier consolidates all artifacts of a case with complete traceability from creation to archiving.

The qualified signature envelope (`QualifiedDocumentSignatureEnvelopeService`) computes `cadeiaCustodiaElegivel`, `assinaturaCompletaMaterializada`, and `rubricaDataHoraLocalPresentes` from the input certificate and the already-materialized envelope — all three were hardcoded `true`, with no real verification, until they were fixed. `classificacaoContextualCoerente` compares the signer's role against the actual institutional segment in 12 of 14 callers (`resolveSegmentoInstitucional` stopped using a tautological fallback and now recognizes police clerks via `isSegurancaPublica()`); the 2 remaining callers still fall back to the permissive `true` default for lack of institutional-capacity mapping — a registered debt (`D-classificacao-contextual-default-permissivo`), not a silent regression.

The document vocabulary is canonical and sealed: `TipoDocumento` (~105 values) carries a `CategoriaDocumento` (`PECA_INAUGURAL`, `PECA_RECURSAL`, `DOC_INSTRUCAO`, `DOC_QUALIFICACAO`). Built on top of that vocabulary is a document-completeness gate by procedural type/class, which will read category and type to decide protocol eligibility — replacing today's attachment-count check with typed validation. The design goal is that a missing type is an explicit rejection, never a silent pass-through.

**HTTP boundary (slice 1b′ — done):** the lawyer can declare a `TipoDocumento` per attachment via `AnexoDeclarado { nomeArquivo, tipo }` in the filing multipart request. `SmartFileSplitter` validates the correlation (name ↔ declaration, bidirectionally) with an explicit 400 in four cases: missing name, duplicate names, a file without a matching declaration, and a declaration without a matching file. When declared and the correlation matches, `Attachment.tipoDocumento` is populated; declaring is optional in this slice — mandating it by procedural type is a decision for the gate (1c). The completeness gate (slice 1c) will read this field to enforce the requirement by procedural type/class — the policy decision (undeclared attachment = rejection or tolerance) belongs to the gate, not to the boundary.

**Typed channel (slice 1d — done):** `Attachment.tipoDocumento` is propagated from `SmartFileSplitter` all the way to the routing payload via `NationalProceduralProcessoEntityPayloadAssembler` (key `documentosTipados`) and consumed by `NationalProceduralPreflightPayloadFactory.extractPresentDocuments`. Boundary 2 is protected: the key is only added to the payload when at least one non-null `tipoDocumento` is present (`!tipados.isEmpty()`), preventing an empty list from activating the typed channel for callers without a declaration. The 3 `ProcessoCommandControllerIT` classes that exercise civil filing without `AnexoDeclarado` were closed (D-d25-testes-anexo): isolated from the real routing/completeness engine, coverage that already exists in `ValidacaoDocumentoAjuizamentoIT` and `CompletudeDocumentalAjuizamentoIT`.

**Party composition by procedural type:** filing does not force the civil mold onto every segment. The system reads the catalog by procedural type and materializes the correct procedural role: `ACUSACAO`/`ACUSADO` in criminal cases, `RECLAMANTE`/`RECLAMADA` in labor cases, `IMPETRANTE`/`IMPETRADO` in writs of mandamus, `SEGURADO` in social-security cases (the INSS does not automatically become a party — it is an extension point for future integration), `INVESTIGADO` in military inquiries. Where the active/passive dichotomy does not legally exist — in habeas corpus, the patient is not an adversarial party — no party is created. Procedural types not covered by the catalog keep null composition until their party profiles are specified. The catalog by procedural type is the single source of truth: the same catalog that defines which documents are required also defines who the parties are. `PoloProcessual` also records the party's procedural domicile (`uf_domicilio`, `comarca_domicilio`, `municipio_domicilio`) and corporate name for legal entities, kept separate from the routing territory — which lives in `tb_processo` (`uf_autor`, `comarca_autor`, `uf_reu`, `comarca_reu`). Filing via REST and the initial-petition assistant (Laiane) already capture this information at input time: `EstruturarRequest` receives `ufAutor`/`comarcaAutor`/`ufReu`/`comarcaReu`, the draft session carries them through to `protocolar()`, and `Processo` persists them, with the `enderecoReuDesconhecido` flag following the same pattern as PJe — the defendant's address is often unknown at filing time — and overriding the informed values when set. The integrator marketplace captures the same 4 fields via `MarketplaceProtocoloRequest`, propagated by `MarketplaceSurfaceFacadeService` to the equivalent internal record in `ApiMarketplaceService`, applying the same precedence rule. The MNI channel captures the party's UF: `MniXmlToProcessoAdapter.resolvePartes` reads the `<estado>` element from the first `<endereco>` of each `<pessoa>` in the XML (the MNI 2.2.2 XSD defines `estado` as a free-text element, with no format restriction), normalizes it to a 2-letter uppercase code, and discards anything outside that format — never persisting raw garbage, never throwing on a missing address. County-equivalent (`comarca`) and municipality remain null on this channel: MNI has no comarca-equivalent element, and the only `codigoMunicipioIBGE` in the standard belongs to `tipoOrgaoJulgador` (the adjudicating court), not to a party's address — confirmed by an exhaustive search of the schema documentation, so as not to assume data the standard does not actually carry. Debt `D-domicilio-parte-dois-canais-nao-populam` documents these residual comarca/municipality gaps in MNI and Marketplace; none of the four channels leaves party domicile entirely null anymore. The engine (`PoloCompositionPolicy` + `PoloRoleMappingTable`) is the single funnel for party materialization — filing via REST, the initial-petition assistant (Laiane), MNI import, and the integrator marketplace all converge on the same mechanism (for Laiane and the marketplace, materialized inside `AjuizamentoService.ajuizar()`; MNI materializes via an equivalent dedicated method in `MniRecepcaoService`, so no caller needs to remember to invoke the engine), with no divergent path producing a generic label (`AUTOR`/`REU`) where the procedural type requires a specific role.

### 8 — Filing, Correction, and Metadata Quality

Governed correction with legal diff — every change goes through policy, impact assessment, and explicit approval. The metadata quality score detects missing classes, parties without documents, and incompatible procedural types before the case advances.

### 9 — Import and Normalization of External Cases

Ingests cases from PJe, e-SAJ, eProc, Projudi, Creta, MNI, and PDPJ. Each external system has a specific normalizer that standardizes NPU, CNJ procedural class, and type before persisting. Import conflicts are recorded with auditable diffs.

The MNI adapter (`intercomunicacao-2.2.2`, the `polo`/`parte`/`pessoa` attributes from the CNJ's official schema) materializes the plaintiff and defendant of the imported case, including the party record, through the same procedural-type composition engine used for direct filing — a case imported via MNI is no longer left without identified parties.

### 10 — Court Orders, Certificates, and Resilient Communication

Complete management of court orders with return diagnosis and urgent prioritization. Automatic certificates with pending checklists and batch issuance. Electronic judicial domicile with exponential retry, failure dashboard, and auditable fallback.

### 11 — GIGS, Notes, Reminders, and Pending Items

Procedural activities (GIGS) with governed execution, visibility controlled by confidentiality and role, jurisdictional act control, and automatic draft reminders. Notes and reminders with visibility policy by role, assignment, and expiration deadline.

### 12 — Auditable Legal AI

AI operates as a support layer — it never replaces human decision-making. Every interaction passes through a pre-conscious framework that evaluates the area of law, doctrinal tradition, procedural risk, evidence provenance, and confidentiality classification before formulating any response.

**Memory Stores:** auditable document repositories that accumulate learning between sessions. Each write generates an immutable version with redact support for LGPD compliance. Confidential cases never have content sent to external services.

**Dreams:** asynchronous jobs that consolidate session transcripts, eliminate contradictions, and extract patterns by procedural type. They operate via outbox pattern with dedicated Virtual Threads and a configurable silence window.

**Process Completeness Gate:** verifies that the document package is complete before allowing the case to advance to the next phase. Validation has two layers: structural (configurable checklists per procedural type, with typed pending items and resolution deadlines) and semantic (OCR + VectorSearch detects the actual presence of required content in already-attached documents, not just the existence of the file). Pending items are notified via outbox with a traceable resolution cycle. The case does not advance while there is a completeness gap — and the clerk can override with a minimum auditable justification.

**Judicial decision advisory:** `advisoryMode` always returns `ADVISORY_DRAFT_ONLY` — Laiane produces only an assisted draft, it never decides. `reviewRequired` and `publicationLocked` are always `true`: every consultation requires full human review before publication, with no exception per template or case. This is not conditional behavior, it is a deliberate security policy — the three advisory modes (`SUGESTIVO`, `RESTRITIVO`, `BLOQUEADOR`) documented in an earlier version of the API were never actually implemented, and differentiating advisory levels remains an open product decision (`D-advisory-modos-nao-implementados`), not a pending bug fix.

### 13 — Reports and Analytics Without Punitive Rankings

Bottleneck reports, average time per procedural type, rework rate, and conciliation rate. Justice in Numbers export for the CNJ. No report identifies a judge by individual performance — data serves systemic improvement, not pressure on people.

### 14 — PDPJ/MNI/API Integration Envelope

Canonical `PjbIntegrationEventEnvelope` with UUID, payload hash, routing key, and semantic versioning. Mapping of judicial events to canonical route `judicial.{system}.{type}.{procedural_type}`. Supports event emission and consumption with at-least-once guarantee via outbox.

### 15 — Criminal Module and Police Investigation

The precinct is modeled as a first-line institutional unit, with assignments, territorial competence, and shift schedule — not as a generic role, but as an entity with its own identity and hierarchy within the criminal bounded context.

Occurrence reports produce traceable investigations. Each report has classification, involved parties, document chain of custody, and automatic link to the criminal case upon formal filing. The investigation follows the case from the police phase through the judicial phase without any break in traceability.

Police scope is resolved by assignment, not by role. What a police chief sees and moves is determined by the precinct where they are assigned. The DelegadoPainel materializes exactly that restricted view — without exposing data from another unit. The `WorkItemScopeGuard` applies this restriction as a P0 control: any access to a work item outside the assignment scope is blocked at the central guard, and ArchUnit ensures at build time that no code path can bypass it.

---

## Smart Accelerators

Ten services that cover gaps no Brazilian judicial system currently addresses systematically:

| # | Service | Capability |
|---|---------|-----------|
| 1 | `NulidadeProcessualRiskPolicy` | Preventive nullity diagnosis before any movement — checks notification, representation, confidentiality, deadline, and competence |
| 2 | `ProcessoParalisacaoDiagnosisService` | Identifies why a case is stalled: unacknowledged order, unsigned document, unassigned task, overdue pending item |
| 3 | `CivilSaneamentoChecklistService` | Computable settlement checklist: preliminary objections, disputed facts, evidence, burden of proof, summary judgment, and settlement probability |
| 4 | `SobrestamentoInteligenteService` | Automatically detects when the reason for suspension has ceased and notifies for resumption, without manual intervention |
| 5 | `ProcessoClusterSimilarityService` | Groups cases with the same party, claim, and procedural type — foundation for intelligent batch judgment and collective settlement |
| 6 | `PrecedenteAplicavelRadarService` | Flags repetitive precedents, suspended themes, or jurisprudential divergences before the decision — never decides, only informs |
| 7 | `ResponsavelWorkloadBalancer` | Suggests the responsible party by current workload and specialty with auditable justification — never imposes, always explains |
| 8 | `DomicilioJudicialResilienceService` | Exponential retry with backoff, persistent failure dashboard, and graceful fallback for electronic communication |
| 9 | `ArquivamentoPendenciaChecker` | Safety checklist for archiving: court fees, orders, deadlines, and documents — never archives automatically |
| 10 | `ProcessMiningMaterializedViewService` | Materialized tables updated in Virtual Threads — bottleneck by act, phase, procedural type, and asynchronous refresh integration |

---

## Procedural Types Covered

The `RitoProcessual` catalog is sealed. All procedural types below are first-class citizens — with their own validations, deadlines, and checklists:

**Civil:** ordinary procedure, summary procedure, monitorial, possessory, adverse possession, payment in court, class action, emergency relief, precautionary relief

**Family:** alimony, consensual and contested divorce, probate, inventory, adoption, guardianship, curatorship, paternity investigation, custody and visitation

**Criminal:** ordinary criminal procedure, summary, abbreviated, jury trial, habeas corpus, criminal enforcement, security measure

**Labor:** ordinary procedure, abbreviated, labor mandamus, collective bargaining dispute, labor enforcement, voluntary jurisdiction

**Electoral:** mandate challenge action, electoral appeal, electoral criminal action

**Constitutional:** individual and collective mandamus, habeas data, popular action, ADPF, ADI, ADC, concrete constitutionality review

**Enforcement:** extrajudicial title, judicial title, tax enforcement, provisional and final judgment enforcement, enforcement against the government

**Appeals:** appeal, interlocutory appeal, internal appeal, clarification motion, ordinary appeal, special appeal, extraordinary appeal

**Small Claims:** civil (JEC), federal (JEF), public treasury (JEFP) — with their own procedures and value limits

**Specialized:** bankruptcy, judicial reorganization, precatory, military, extrajudicial, arbitration with court confirmation

---

## Security & Compliance

The security model is driven by identity, role, assignment, organization, unit, instance, confidentiality, and an immutable auditable trail.

| Mechanism | What It Protects |
|-----------|-----------------|
| **ABAC** (Attribute-Based Access Control) | Every sensitive decision — with a trail of who authorized it, when, and why |
| **RLS** (Row Level Security in PostgreSQL) | Reading confidential cases — the database refuses the data before the ORM sees it |
| **Step-up Gov.br** | Acts requiring elevated authentication level (silver/gold) |
| **ICP-Brasil** | Qualified digital signatures for documents and judicial acts |
| **Passkey / WebAuthn** | Passwordless authentication for court staff and lawyers |
| **ICP-Brasil Certificate Login** | Full challenge-response flow: cryptographic nonce issued by server, user signs with their certificate, ICP-Brasil chain verification, subject DN identity extraction, institutional context resolution by assignment. Certificate session issued as a distinct type from password session — no mixing of assurance levels |
| **Scoped Values (Java 21)** | Confidential context propagation in Virtual Threads — no leakage |
| **AnthropicInputSanitizer** | Prompt injection prevention in AI interactions |
| **Materialized Audit Trail** | Every operation on confidential data — no content logged, only metadata |
| **Materialized AuthzTrail** | Every authorization decision produces an immutable record in `tb_authz_trail`, deduplicated by semantic key — compact hash of actor, resource, and effect. Identical entries collapse; the ledger is queryable by access pattern, not just time window |
| **ICP-Brasil Sanitization** | CPF and CNPJ removed from API responses, certificate cache, signature events, and ICP chain audit ledger. Where the identifier is needed for correlation, it is stored as a hashed reference — never in clear text |
| **BOLA Guard (WorkItemScopeGuard)** | Prevents any actor from accessing a work item from a different unit or assignment. Applied as a P0 control — ArchUnit guarantees at build time that no code path can bypass the guard |
| **Rate Limiting** | Critical routes protected against abuse with per-period request limits. RFC 7807 standardized response. `createOficio` and communication endpoints have their own budget, separate from general traffic |
| **Security Event Logger** | Every relevant security event — authentication, denied authorization, step-up, attempted bypass — produces a structured log entry separate from the application log, independently auditable and free of operational noise |
| **Auditable Circuit Breaker** | Open/closed state of each circuit breaker is recorded with timestamp, cause, and failure count — the degradation history of an integration is traceable, not just the current state |
| **LGPD** | Confidential data never sent to external services; auditable redact by version |
| **Dual Approval** | Critical operations require confirmation from a second authorized actor |

---

## Concurrency and Async Execution

All asynchronous execution passes mandatorily through `PjbExecutionOrchestrator`. Virtual Threads are centralized in `PjbVirtualThreadSpine` — no executor is created directly outside the central governance.

Confidential context is propagated via Scoped Values with bind/restore at every asynchronous execution boundary, preventing confidentiality from one case contaminating another in a different Virtual Thread.

Bounded concurrency via `PjbBoundedExecutorService` prevents connection pool explosion under peak loads. Structured Concurrency manages operations that depend on multiple procedural types in parallel — a child failure cancels the rest, without resource leaks.

Zero loose `CompletableFuture` in production code. ADR-0051 defines the unified execution model and is enforced by a Python guard and ArchUnit on every build.

---

## Scalability and Operational Resilience

Not loading data unnecessarily into the JVM is treated as a project constraint, not a suggestion. The federal redistribution engine calculates load per jurisdiction entirely in the database: a single query with `GROUP BY jurisdicao_id` and two `SUM(CASE WHEN...)` expressions returns aggregated values directly. No `Processo` instance is constructed, no list is materialized, no Java accumulator accumulates what the PostgreSQL executor already knows how to calculate.

The `transactional_hotspot_guard` scans every `@Transactional` method in the module for heavy I/O inside the transaction. Of 51 findings individually reviewed, one was a real risk: `UsuarioService.listarTodosUsuarios` loaded the entire user table on every call, with no pagination, on the admin endpoint `GET /api/v1/usuarios` — fixed to return `Page<UsuarioResponse>` via `Pageable`, the same pattern already used by `JurisdicaoService.listarPaginado`. The other 50 were small reference tables, already-cached queries, already-paginated calls, or already the correct paginated-batch-plus-single-`saveAll` pattern — reviewed and annotated with `@PjbTransactionalBudget`, which documents the accepted budget instead of silencing the alert.

The `tb_outbox_event` table is partitioned monthly by `created_month`. Processed events are not deleted row-by-row — the entire partition is dropped via `DROP TABLE` when the month turns. The purge cost is O(1) regardless of volume. A court with one million events per month has exactly the same cleanup cost as one with a hundred.

The authorization trail (`tb_authz_trail`) materializes every access decision with a semantic key: compact hash of actor, resource, and decision — not a UUID. Identical repeated decisions collapse into the same entry — no silent duplication of records for the same (subject, object, effect) pair. The ledger remains queryable by access pattern, not just time window.

All 8 Kafka topics are declared explicitly via `NewTopic` beans in `PjbKafkaTopicConfig`, provisioned by Spring's `KafkaAdmin` at startup. The partition count is derived directly from `PjbKafkaScaleProperties.listenerConcurrency` (default 3) — the two values are mathematically impossible to drift apart because one reads from the other. With 1 partition, Kafka limits each group to 1 active consumer regardless of configured concurrency; with 3 partitions, each thread owns one partition and processes in true parallel. Any new environment — dev, staging, production — is correctly configured from first boot with no manual intervention. Log retention is explicitly set to 7 days with 512 MB segments.

Sensitive personal data — CPF and CNPJ — have been removed from every layer where they are not needed: ICP-Brasil API metadata responses, certificate cache, signature events, and ICP chain audit ledger entries. Where the identifier is needed for correlation, it is stored as a hashed reference, never in clear text.

---

## Database

269 Flyway migrations (non-contiguous numbering up to V306 — 38 sequence numbers have no corresponding file in the repository), applied in sequence, with `validateOnMigrate=true` and `outOfOrder=false`. The schema is always validated by Hibernate on startup — any drift between entity and database is detected before the first request.

Row Level Security active per operation for confidential data. Materialized tables with asynchronous refresh for analytics (ADR-0053). Outbox pattern for post-commit effects with no risk of event loss on transaction failure. The outbox table is partitioned monthly — entire partition purge via `DROP TABLE`, no row scanning.

```sql
-- Example RLS policy for confidential cases
CREATE POLICY processo_sigilo ON processo
    USING (sigilo = false OR current_setting('app.papel') IN ('JUIZ', 'PROMOTOR'));
```

---

## Code Quality

| Metric | Status |
|--------|--------|
| Unit tests (Surefire) | **4,138 · 0 failures · 0 errors** |
| Integration tests (Failsafe) | **230 · 0 known failures** (from 49 → 14 → 10 → 0; D-routing-preprotocolo and the 9 remaining pre-existing failures closed; +10 polo-composition-engine green; +24 from the territorial competence slice — CE, MG and RN — green since creation — see note¹ in the Tests section about 10 tests confirmed outside this count) |
| K8s manifests (Kustomize) | Schema-validated: `kubernetes-validate 1.36.0` (K8s 1.30, offline) |
| ADRs | 57 architectural decisions documented |
| Python Guards | 7 scripts active in CI |
| SBOM | CycloneDX generated on every build |
| Correlation ID | Mandatory on every request |

57 ADRs document each architectural decision with motivation, consequences, and alternatives considered. They must be read before altering any package structure, concurrency pattern, or security policy.

The pipeline automatically generates a CycloneDX SBOM on every build, maintaining an auditable inventory of all dependencies with version and license. The CI evidence gate rejects merges without full structural guard coverage. Correlation ID mandatory on every request — propagated via context and recorded in every log entry, enabling end-to-end tracing without an external aggregator.

### Kubernetes Manifest Validation

Manifests in `infra/k8s/` are schema-validated before every commit using `kubernetes-validate` with bundled schemas (no network dependency):

```bash
pip install kubernetes-validate pyyaml --break-system-packages
python infra/k8s_schema_validate.py
```

The script validates all core K8s resources across four main overlays (`base`, `prod`, `prod-sovereign-fapi-gateway`, `prod-sovereign-opa-ext-authz`). CRDs without bundled schemas (VPA, Gateway API, KEDA ScaledObject) are listed by name as skipped — equivalent to kubeconform's `-ignore-missing-schemas`.

> **Registered production debts:** CIDR-based egress is unworkable for AI destinations behind CDN (Anthropic/OpenAI/Google AI) — requires Cilium FQDN NetworkPolicy or an egress gateway. The `legalai/dreams` subsystem will not function in production without this layer. Real cluster secrets (Gateway TLS, database credentials) must be provisioned externally (cert-manager, ICP-Brasil, vault) — never versioned in the repository.

### Structural Guards

Run locally before any commit:

```bash
# Linux / macOS
python scripts/architecture_hygiene_guard.py
python scripts/constructor_injection_guard.py
python scripts/runtime_concurrency_guard.py

# Windows
python scripts\architecture_hygiene_guard.py
python scripts\constructor_injection_guard.py
python scripts\runtime_concurrency_guard.py
python scripts\transactional_hotspot_guard.py --fail-on-findings --fail-on-missing-budgets
python scripts\config_taxonomy_guard.py
```

| Guard | What It Verifies |
|-------|-----------------|
| `architecture_hygiene_guard` | Class names, packages, prohibited cross-dependencies |
| `constructor_injection_guard` | Zero `@Autowired` on fields — constructor injection only |
| `runtime_concurrency_guard` | Zero executor created outside `PjbVirtualThreadSpine` governance |
| `transactional_hotspot_guard` | Zero unreviewed heavy-I/O finding inside `@Transactional` — a reviewed hotspot requires `@PjbTransactionalBudget` |
| `config_taxonomy_guard` | Configuration properties within the defined taxonomy |
| `anti_mock_prod_guard` | Blocks if critical integration mocks are active in production: Gov.br, ICP-Brasil, Kafka, Elasticsearch, AI |
| `openapi_weakness_detector` | Detects `Map<String,Object>` without typed schema, fields without `format: date-time`, routes without registered OpenAPI contract |

---

## Observability

```
GET /admin/governance/codebase-learning
GET /admin/governance/codebase-learning?refresh=true
GET /admin/governance/sanidade-aprendizado
GET /admin/governance/health-matrix
GET /actuator/health
GET /actuator/metrics
```

Exposes a live read of the structural state: core hotspots, internal core extraction trails, extraction blueprints, end-to-end critical flows, and coverage ratio per bounded context. The in-memory snapshot has a short TTL; use `refresh=true` to force a rescan without restarting the application.

---

## Contributing

### Branch Strategy

| Branch | Purpose |
|--------|---------|
| `master` | Main branch — always stable, reflects production |
| `feature/feature-name` | New features |
| `fix/bug-description` | Bug fixes |
| `refactor/scope` | Refactoring without behavioral changes |
| `docs/scope` | Documentation updates |

### Commit Standards (Conventional Commits)

This project follows [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/):

```
<type>(optional scope): lowercase description

Optional body explaining the "why", not the "what".
```

| Type | When to Use |
|------|-------------|
| `feat` | New feature |
| `fix` | Bug fix |
| `refactor` | Refactoring without external behavioral changes |
| `test` | Adding or fixing tests |
| `docs` | Documentation |
| `chore` | Build maintenance, CI, dependencies |
| `perf` | Performance improvement |

### Opening a Pull Request

1. Create a branch from `master` following the naming convention above
2. Run the Python guards and confirm they pass locally
3. Run the test suite and confirm 0 regressions: `./mvnw test -pl pjb-api`
4. Open the PR with a title following Conventional Commits
5. Describe what changed, why it changed, and which tests cover the change

### Non-Negotiable Rules

- Constructor injection in all production classes — zero `@Autowired` on fields
- `@Inject` (Jakarta) on constructors — never `@Autowired` Spring on fields
- No Lombok on critical layers — immutability via Java 21 Records
- No class with generic names (`Manager`, `Helper`, `Util`, `Processor`, `Handler`)
- No REST routes outside the canonical bounded context registry
- Zero redundant comments — expressive names document the code
- `@Transactional` only on ApplicationService, no external I/O inside the transaction
- No loose `CompletableFuture` — follow ADR-0051

### Regression Prohibitions

- No regression in confidentiality, auditing, RLS, or ABAC
- No regression in asynchronous context propagation
- No increase in the number of test failures
- No alteration of already-applied migrations (Flyway checksum)

### Minimum Acceptance Criteria

Compile + Python guards green + suite without regression + public contracts preserved.

---

## Safe Git Sync

```powershell
.\scripts\git-sync-safe.ps1 "change description"
```

The local barrier inspects the diff before any commit and blocks API keys, passwords, JWT tokens, certificates, and any known secret pattern. Details in `docs/security/GIT_SAFE_SYNC.md`.

---

## National Replacement

The replacement matrix compares PJB capabilities against PJe, e-SAJ, eProc, Creta, and Projudi by feature, bounded context, and justice segment. It prevents context duplication and directs deliveries to the correct packages.

```
docs/product/NATIONAL_JUDICIAL_SYSTEM_REPLACEMENT_MATRIX.md
docs/product/NATIONAL_JUDICIAL_SYSTEM_REPLACEMENT_INDEX.json
```

---

## Author

**Tiago Rabelo**
Software Engineering — Universidade Católica de Quixadá (Unicatólica), Brazil
Final Undergraduate Thesis (TCC) — 2024/2025

🔗 [github.com/tiagorabelo0403](https://github.com/tiagorabelo0403)

---

## License

This project is licensed under the [MIT License](./LICENSE).

```
MIT License — Copyright (c) 2025 Tiago Rabelo

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

---

## Glossary

| Term | Meaning |
|------|---------|
| **NPU** | Unique Process Number — CNJ standardized identifier (e.g., 0000001-00.2024.8.26.0001) |
| **Rito** | Mandatory procedural flow defined by law (ordinary, abbreviated, small claims, etc.) |
| **Autuação** | Act of formally registering the case in the system, with class, subject, and parties |
| **Distribuição** | Assignment of the case to a competent court or judge |
| **Movimentação** | Any act performed on the case (ruling, decision, judgment, certificate) |
| **GIGS** | Activity Group — a set of procedural tasks with a deadline and responsible party |
| **Sobrestamento** | Temporary suspension of the case awaiting a paradigm ruling |
| **BATNA** | Best Alternative to a Negotiated Agreement |
| **ABAC** | Attribute-Based Access Control |
| **RLS** | Row Level Security — security policy applied at the database level |
| **ADR** | Architecture Decision Record — formal record of an architectural decision |
| **ICP-Brasil** | Brazilian Public Key Infrastructure — digital signature standard |
| **Gov.br** | Federal authentication system with bronze, silver, and gold trust levels |
| **PDPJ** | Digital Platform of the Judiciary — national integration bus |
| **MNI** | National Interoperability Model — exchange protocol between judicial systems |
| **CNJ** | National Council of Justice — regulatory body that defines classes, subjects, and tables |
| **JEC** | Civil Small Claims Court |
| **JEF** | Federal Small Claims Court |
| **JEFP** | Public Treasury Small Claims Court |
| **BO** | Boletim de Ocorrência — Police Occurrence Report |
| **SBOM** | Software Bill of Materials — auditable dependency inventory |
| **CPF** | Brazilian individual taxpayer identification number |
| **CNPJ** | Brazilian corporate taxpayer identification number |

---

## Next Steps

### Backend

The backend fully covers the bounded contexts described in this document — 15 functional modules, 57 ADRs, 4,138 unit tests plus 230 integration tests, and 269 applied migrations. The REST API is fully documented via OpenAPI 3.1 and Swagger UI, ready for consumption by any client.

### Frontend — Under Analysis and Planning

The presentation layer is in an architectural analysis and decision phase. The backend was built from the start with the assumption that frontend and backend would be fully separated — all communication happens via REST with versioned OpenAPI contracts, which gives complete freedom of choice on the client side.

The questions currently being evaluated before development begins:

**Rendering model:** Pure SPA (React, Vue, Angular) or SSR/SSG (Next.js, Nuxt) — the choice directly impacts SEO, load times on slow connections (common in smaller Brazilian courts), and session cache strategy.

**Interface profiles:** the system has actors with radically different workflows — judge, court clerk, lawyer, party, police chief, institutional administrator. The decision is between a single SPA with role-protected routes or separate interfaces per profile, each optimized for that specific actor's workflow.

**Client-side authentication:** the backend already implements Gov.br (bronze/silver/gold), ICP-Brasil with certificate challenge-response, Passkey/WebAuthn, and contextual step-up. The frontend will need to handle this diversity of authentication flows in a cohesive way — the choice of framework impacts how this is managed in application state.

**OpenAPI contract integration:** the `/v3/api-docs` contract is already available and stable. Automatic typed client generation (via OpenAPI Generator or similar) is being evaluated to eliminate the need to maintain duplicate DTOs between backend and frontend.

The final decision will be recorded in a dedicated ADR before any line of frontend code is written.
