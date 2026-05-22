# PJB — Plataforma Judicial Brasileira

> Sistema judicial eletrônico de nova geração, construído em Java 21 e Spring Boot 3.5, para substituir integralmente PJe, e-SAJ, eProc, Creta e Projudi em todos os segmentos da Justiça brasileira.

---

## O problema

O Judiciário brasileiro opera sobre cinco sistemas legados desenvolvidos por diferentes entidades ao longo de décadas, sem coordenação de interface, protocolo ou modelo de dados. O resultado é uma infraestrutura fraturada:

| Sistema | Tribunal principal | Problema central |
|---------|-------------------|-----------------|
| PJe | CNJ / maioria dos tribunais | Acoplamento forte entre UI e domínio, rotas sem contrato |
| e-SAJ | TJSP, TJBA e outros estaduais | Modelo de dados proprietário, sem API pública |
| eProc | TRF1, TRF4 e estaduais | Jobs isolados, assinaturas frágeis |
| Creta | Justiça do Trabalho | Baixa observabilidade, sem suporte a novos ritos |
| Projudi | Tribunais estaduais menores | Débito técnico crítico, sem path de migração |

O Brasil tem hoje mais de **80 milhões de processos ativos**, distribuídos por **91 tribunais**, atendendo **cerca de 30 mil magistrados** e milhões de advogados, partes e servidores. Nenhum dos cinco sistemas foi projetado com escalabilidade horizontal, auditoria de acesso granular ou suporte a todas as classes processuais do CPC/2015 e das reformas trabalhistas.

O PJB não é uma reescrita incremental. É uma ruptura deliberada com esse modelo.

---

## A proposta

O PJB foi projetado do zero com três compromissos inegociáveis:

**1. Rastreabilidade total.** Toda decisão de acesso, distribuição, movimentação e comunicação produz uma trilha auditável, imutável e explicável. Não existe ação no sistema que não possa ser reconstituída.

**2. Testabilidade como critério de aceite.** Nenhuma funcionalidade existe sem comportamento verificável. A suíte de testes é o contrato executável do sistema — se o teste passa, o comportamento está garantido.

**3. Segurança por construção.** ABAC, RLS por operação, propagação governada de contexto sigiloso e Step-up Gov.br não são camadas adicionadas depois. São restrições que guiam cada decisão arquitetural desde o início.

---

## Domínio

### Atores

| Ator | Papel no sistema |
|------|-----------------|
| **Magistrado** | Profere decisões, assina documentos, gerencia sua pauta |
| **Servidor / Escrevente** | Realiza atos de secretaria, emite certidões, movimenta processos |
| **Advogado / Defensor** | Peticiona, acompanha prazos, acessa autos conforme sigilo |
| **Promotor / Procurador** | Atua nos processos de sua lotação e instância |
| **Parte / Jurisdicionado** | Acessa o que a lei lhe permite, sem identificação de magistrado |
| **Administrador institucional** | Configura varas, competências, calendários e acessos |
| **Sistema externo** | PJe, e-SAJ, eProc, MNI, PDPJ — integrados via envelope canônico |

### Conceitos centrais do domínio

**Processo judicial** é o aggregate raiz. Tem NPU (Número Processo Único), classe processual CNJ, assunto, valor da causa, rito, partes, representantes e movimentações. Cada processo existe dentro de uma jurisdição com competência material e territorial definida.

**Rito processual** define o fluxo obrigatório: quais fases existem, quais prazos se aplicam, quais atos são possíveis em cada fase. O catálogo é selado — nenhum rito pode ser inventado em runtime. Isso impede que o sistema aceite configurações inválidas.

**Distribuição** é o ato de atribuir um processo a uma vara. O motor de distribuição avalia natureza, competência, rito, comarca, carga da unidade e regras do tribunal. Cada decisão de distribuição produz uma explicação auditável com todos os critérios avaliados.

**Movimentação** é qualquer ato sobre o processo: despacho, decisão interlocutória, sentença, acórdão, certidão, mandado. Cada movimentação tem autor, timestamp, hash de integridade e vínculo com o ato processual correspondente.

**Sigilo** é uma dimensão transversal. Um processo sigiloso restringe visibilidade até o nível de registro no banco de dados, via Row Level Security. A propagação de sigilo em operações assíncronas é governada — nunca vazada.

**Jurisdição** é a unidade estrutural de competência: uma vara, uma câmara, uma seção judiciária. Tem grau, esfera, natureza, competência material e territorial. A hierarquia de jurisdições modela todos os segmentos: federal, estadual, trabalhista, eleitoral, militar.

### Bounded contexts

| Context | Responsabilidade |
|---------|-----------------|
| `institucional` | Órgãos, varas, lotações, competências, afiliações, credenciais |
| `processo` | Processo, movimentações, partes, prazos, distribuição |
| `documentos` | Documentos, dossiê, cadeia de custódia, assinaturas |
| `comunicacao` | Mandados, certidões, domicílio eletrônico, intimações |
| `seguranca` | ABAC, autenticação, auditoria, sigilo, Gov.br, ICP-Brasil |
| `analytics` | Process mining, gargalos, Justiça em Números, relatórios |
| `ia` | IA jurídica auditável, Memory Stores, Dreams, síntese reflexiva |
| `integracao` | Envelope canônico PDPJ/MNI, normalizadores PJe/e-SAJ/eProc |
| `advocacia` | Escritório, delegações, filas de assinatura, workspace |
| `laiane` | Módulo especializado de assistência jurídica via IA |

---

## Arquitetura

### Estrutura de módulos

O projeto segue arquitetura hexagonal com separação estrita entre domínio e infraestrutura:

```
pjb/
├── pjb-core/                         domínio puro — zero dependência de Spring
│   └── src/main/java/
│       └── com/tcc/pjb/core/
│           ├── domain/               aggregates, entities, value objects
│           ├── service/              application services e domain services
│           ├── port/                 interfaces de saída (repository, messaging)
│           └── ia/                   ports de IA jurídica
│
├── pjb-api/                          adaptadores — Spring Boot, JPA, HTTP
│   └── src/main/java/
│       └── com/tcc/pjb/backend/
│           ├── controller/           REST endpoints por bounded context
│           ├── model/entity/         entidades JPA
│           ├── model/repository/     Spring Data repositories
│           ├── core/                 serviços de aplicação e domínio
│           ├── configs/              Spring, Security, OpenAPI, DataSource
│           └── modules/              módulos especializados (laiane, advocacia)
│
├── docs/
│   ├── adr/                          55 Architecture Decision Records
│   ├── database/                     esquemas e políticas RLS
│   ├── openapi/                      contratos de API pública
│   ├── security/                     políticas LGPD e Gov.br
│   └── product/                      matriz de substituição nacional
│
├── scripts/                          guards Python — higiene estrutural contínua
├── config/                           Checkstyle e SpotBugs
└── infra/                            Kubernetes, gateway, infraestrutura
```

### Camadas e dependências

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
         ↑ depende de, nunca o inverso
```

`pjb-core` não conhece Spring, JPA nem HTTP. Toda injeção é por construtor com `@Inject` (Jakarta). Repositories são interfaces de porta em `pjb-core`; as implementações JPA ficam em `pjb-api`.

### Padrões arquiteturais aplicados

| Padrão | Onde | Por quê |
|--------|------|---------|
| Hexagonal (Ports & Adapters) | Estrutura global | Isolar domínio de infraestrutura |
| Aggregate Pattern (DDD) | `Processo`, `Jurisdicao`, `Usuario` | Invariantes de domínio garantidas |
| Outbox Pattern | Efeitos pós-commit | Zero perda de evento em falha de commit |
| CQRS leve | Analytics e projeções | Leituras materializadas sem pressão no write path |
| Sealed classes | `RitoProcessual`, `TipoJurisdicao` | Catálogo fechado, exaustividade em compile-time |
| Virtual Threads (Java 21) | Toda execução assíncrona | Alta concorrência sem pool sizing manual |
| Scoped Values (Java 21) | Propagação de sigilo | Contexto sigiloso não vaza entre Virtual Threads |
| Structured Concurrency | Operações multi-rito | Falha de um filho cancela os demais, sem leak |

---

## Stack técnica

| Componente | Tecnologia |
|------------|-----------|
| Linguagem | Java 21 — Virtual Threads, Records, Sealed Interfaces, Pattern Matching |
| Framework | Spring Boot 3.5, Spring Framework 6 |
| Build | Maven multi-module (`pjb-core` + `pjb-api`) |
| Banco | PostgreSQL 17 com Row Level Security por operação |
| Banco de testes | H2 em memória |
| Migrations | Flyway — 243 migrations (V0–V280) |
| Persistência | JPA / Hibernate com `ddl-auto: validate` em produção |
| Mensageria | Apache Kafka 3.8 — eventos judiciais e outbox |
| Cache | Redis 7.4 |
| Busca | Elasticsearch 8.15 |
| Segurança | Spring Security, ABAC, Gov.br, ICP-Brasil, Passkey/WebAuthn |
| Resiliência | Resilience4j — Circuit Breaker, Bulkhead, Retry, Timeout |
| Contratos | Pact — Consumer-Driven Contract Testing |
| Qualidade | JaCoCo, Checkstyle, SpotBugs, ArchUnit |
| IA Jurídica | Anthropic Claude API — Memory Stores, Dreams, síntese reflexiva |
| Observabilidade | Micrometer, Spring Actuator, Process Mining materializado |
| Guards estruturais | 20+ scripts Python + ArchUnit em CI |
| Containerização | Docker Compose (dev/test), Kubernetes (produção) |

---

## Módulos funcionais

### 1 — Governança institucional

Gerencia papel, lotação, localização, competência e visibilidade de cada ator no processo. A matriz de visibilidade produz uma explicação auditável para cada decisão de acesso — quem pode ver o quê, por qual motivo, com registro imutável.

Inclui gestão de afiliações, credenciais institucionais, atestação de fonte oficial e delegações formais entre unidades.

### 2 — Motor de rito e distribuição inteligente

Distribui processos por natureza, competência, rito e comarca. Suporta vara única, comarca do interior, JEC itinerante e qualquer configuração de tribunal. O engine explainável documenta cada critério avaliado na decisão de distribuição — nenhuma distribuição é uma caixa-preta.

### 3 — Motor de celeridade constitucional

Monitora prazos constitucionais por rito, calcula gargalos sistêmicos e sugere aceleradores por área do direito. Não pressiona magistrados individualmente — identifica onde o sistema está lento e por quê, com dados agregados e anônimos.

### 4 — Painel interno e secretaria cartorária

Filas inteligentes com priorização semântica, agrupadores por similaridade, lote de assinatura com conferência obrigatória e hash SHA-256 por documento. Cada ato de secretaria tem rastreabilidade de quem fez, quando, com qual resultado e em qual estado o processo se encontrava.

### 5 — Aceleradores por área do direito

Fluxos especializados para cível, criminal, trabalhista, eleitoral, família, execução, Juizados Especiais (cível, federal e da Fazenda Pública), precatório, falimentar e controle concentrado de constitucionalidade. Cada área tem checklist computável, diagnóstico de risco e sugestão de próximo ato.

### 6 — Chips inteligentes e conciliação

Marcadores semânticos de processo para priorização automática por urgência, complexidade e probabilidade de acordo. O módulo de conciliação sugere acordos baseados em precedentes semelhantes, com score de probabilidade, BATNA calculado e histórico de propostas.

### 7 — Documentos, dossiê e cadeia de custódia

Cada documento tem origem, estado operacional, hash de integridade e cadeia de confiança verificável. O dossiê documental consolida todos os artefatos de um processo com rastreabilidade completa desde a criação até o arquivamento.

### 8 — Autuação, retificação e qualidade de metadados

Retificação governada com diff jurídico — cada alteração passa por política, avaliação de impacto e aprovação explícita. Score de qualidade de metadados detecta classes ausentes, partes sem documento e rito incompatível antes que o processo avance para a fase seguinte.

### 9 — Importação e normalização de processos externos

Ingesta processos de PJe, e-SAJ, eProc, Projudi, Creta, MNI e PDPJ. Cada sistema externo tem normalizador específico que padroniza NPU, classe processual CNJ e rito antes de persistir. Conflitos de importação são registrados com diff auditável.

### 10 — Mandados, certidões e comunicação resiliente

Gestão completa de mandados com diagnóstico de devolução e priorização de urgentes. Certidões automáticas com checklist de pendências e emissão em lote. Domicílio eletrônico judicial com retry exponencial, painel de falhas e fallback auditável.

### 11 — GIGS, notas, lembretes e pendências

Atividades processuais (GIGS) com execução governada, visibilidade controlada por sigilo e papel, controle de atos jurisdicionais e lembrete automático de minuta pendente. Notas e lembretes com política de visibilidade por papel, lotação e prazo de expiração.

### 12 — IA jurídica auditável

A IA opera como camada de suporte — nunca substitui decisão humana. Toda interação passa por uma moldura pré-consciente que avalia ramo do direito, tradição doutrinária, risco procedimental, proveniência de evidência e classificação de sigilo antes de formular qualquer resposta.

**Memory Stores:** repositórios de documentos auditáveis que acumulam aprendizado entre sessões. Cada escrita gera versão imutável com suporte a redact para conformidade LGPD. Processos sigilosos jamais têm conteúdo enviado a serviços externos.

**Dreams:** jobs assíncronos que consolidam transcrições de sessão, eliminam contradições e extraem padrões por rito processual. Operam via outbox pattern com Virtual Threads dedicadas e janela de silêncio configurável.

### 13 — Relatórios e analytics sem ranking punitivo

Relatórios de gargalo, tempo médio por rito, taxa de retrabalho e taxa de conciliação. Exportação Justiça em Números para o CNJ. Nenhum relatório identifica magistrado por desempenho individual — os dados servem à melhoria sistêmica, não à pressão sobre pessoas.

### 14 — Envelope de integração PDPJ/MNI/API

Envelope canônico `PjbIntegrationEventEnvelope` com UUID, hash de payload, routing key e versão semântica. Mapeamento de eventos judiciais para rota canônica `judicial.{sistema}.{tipo}.{rito}`. Suporta emissão e consumo de eventos com garantia de at-least-once via outbox.

---

## Aceleradores inteligentes

Dez serviços que cobrem lacunas que nenhum sistema judicial brasileiro resolve de forma sistemática:

| # | Serviço | Capacidade |
|---|---------|-----------|
| 1 | `NulidadeProcessualRiskPolicy` | Diagnóstico preventivo de nulidade antes de qualquer movimentação — verifica intimação, representação, sigilo, prazo e competência |
| 2 | `ProcessoParalisacaoDiagnosisService` | Identifica por que um processo está parado: expediente sem ciência, documento sem assinatura, tarefa sem responsável, pendência vencida |
| 3 | `CivilSaneamentoChecklistService` | Checklist computável de saneamento: preliminares, pontos controvertidos, provas, ônus, julgamento antecipado e probabilidade de acordo |
| 4 | `SobrestamentoInteligenteService` | Detecta automaticamente quando o motivo de sobrestamento cessou e notifica para dessobrestamento, sem intervenção manual |
| 5 | `ProcessoClusterSimilarityService` | Agrupa processos com mesma parte, pedido e rito — base para julgamento em lote inteligente e acordo coletivo |
| 6 | `PrecedenteAplicavelRadarService` | Sinaliza precedente repetitivo, tema suspenso ou divergência jurisprudencial antes da decisão — nunca decide, apenas informa |
| 7 | `ResponsavelWorkloadBalancer` | Sugere responsável por carga atual e especialidade com justificativa auditável — nunca impõe, sempre explica |
| 8 | `DomicilioJudicialResilienceService` | Retry com backoff exponencial, painel de falhas persistente e fallback gracioso para comunicação eletrônica |
| 9 | `ArquivamentoPendenciaChecker` | Checklist de segurança para arquivamento: custas, expedientes, prazos e documentos — nunca arquiva automaticamente |
| 10 | `ProcessMiningMaterializedViewService` | Tabelas materializadas atualizadas em Virtual Threads — gargalo por ato, fase, rito e integração com refresh assíncrono |

---

## Ritos processuais cobertos

O catálogo `RitoProcessual` é selado (sealed). Todos os ritos abaixo são tratados como primeiro cidadão — com validações, prazos e checklists próprios:

**Cível:** procedimento comum ordinário, sumário, monitória, possessória, usucapião, consignação em pagamento, ação civil pública, tutela de urgência antecedente, cautelar antecedente

**Família:** alimentos, divórcio consensual e litigioso, inventário judicial, arrolamento, adoção, tutela, curatela, investigação de paternidade, guarda e regime de visitas

**Criminal:** procedimento penal comum, sumário, sumaríssimo, júri popular, habeas corpus, execução penal, medida de segurança

**Trabalhista:** rito ordinário, sumaríssimo, mandado de segurança trabalhista, dissídio coletivo, execução trabalhista, reclamação de jurisdição voluntária

**Eleitoral:** ação de impugnação de mandato, recurso eleitoral, ação penal eleitoral

**Constitucional:** mandado de segurança individual e coletivo, habeas data, ação popular, ADPF, ADI, ADC, ADIN, controle concreto de constitucionalidade

**Execução:** título extrajudicial, título judicial, execução fiscal, cumprimento de sentença provisório e definitivo, execução contra a Fazenda Pública

**Recursal:** apelação, agravo de instrumento, agravo regimental, embargos de declaração, recurso ordinário, recurso especial, recurso extraordinário

**Juizados Especiais:** cível (JEC), federal (JEF), da Fazenda Pública (JEFP) — com rito próprio e limites de valor

**Especializados:** falimentar, recuperação judicial, precatório, militar, extrajudicial, arbitragem com homologação

---

## Segurança e conformidade

O modelo de segurança é orientado por identidade, papel, lotação, órgão, unidade, instância, sigilo e trilha auditável imutável.

| Mecanismo | O que protege |
|-----------|--------------|
| **ABAC** (Attribute-Based Access Control) | Toda decisão sensível — com trilha de quem autorizou, quando e por quê |
| **RLS** (Row Level Security no PostgreSQL) | Leitura de processos sigilosos — o banco recusa o dado antes do ORM |
| **Step-up Gov.br** | Atos que exigem nível de autenticação elevado (prata/ouro) |
| **ICP-Brasil** | Assinatura digital qualificada de documentos e atos jurisdicionais |
| **Passkey / WebAuthn** | Autenticação sem senha para servidores e advogados |
| **Scoped Values (Java 21)** | Propagação de contexto sigiloso em Virtual Threads — sem vazamento |
| **AnthropicInputSanitizer** | Prevenção de prompt injection nas interações com IA |
| **Auditoria materializada** | Toda operação sobre dado sigiloso — sem log de conteúdo, só metadado |
| **LGPD** | Dados sigilosos nunca enviados a serviços externos; redact auditável por versão |
| **Dual approval** | Operações críticas exigem confirmação de segundo ator autorizado |

---

## Concorrência e execução assíncrona

Toda execução assíncrona passa obrigatoriamente pelo `PjbExecutionOrchestrator`. Virtual Threads são centralizadas em `PjbVirtualThreadSpine` — nenhum executor é criado diretamente fora da governança central.

Contexto sigiloso é propagado via Scoped Values com bind/restore em toda fronteira de execução assíncrona, impedindo que sigilo de um processo contamine outro em Virtual Thread diferente.

Bounded concurrency via `PjbBoundedExecutorService` previne explosão de conexões de banco em cargas de pico. Structured Concurrency gerencia operações que dependem de múltiplos ritos em paralelo — a falha de um filho cancela os demais, sem leak de recursos.

Zero `CompletableFuture` solto no código de produção. O ADR-0051 define o modelo unificado de execução e é aplicado por guard Python e ArchUnit a cada build.

---

## Banco de dados

243 migrations Flyway (V0–V280), aplicadas em sequência, com `validateOnMigrate=true` e `outOfOrder=false`. O schema é sempre validado pelo Hibernate no startup — qualquer drift entre entidade e banco é detectado antes da primeira requisição.

Row Level Security ativo por operação para dados sigilosos. Tabelas materializadas com refresh assíncrono para analytics (ADR-0053). Outbox pattern para efeitos pós-commit sem risco de perda de evento em falha de transação.

```sql
CREATE POLICY processo_sigilo ON processo
    USING (sigilo = false OR current_setting('app.papel') IN ('JUIZ', 'PROMOTOR'));
```

---

## Qualidade executável

A suíte conta com **3.699 testes · 0 falhas · 0 erros**. Toda alteração só é aceita quando melhora comportamento verificável sem reduzir maturidade arquitetural.

55 ADRs documentam cada decisão arquitetural com motivação, consequências e alternativas consideradas. Devem ser lidos antes de alterar qualquer estrutura de pacote, padrão de concorrência ou política de segurança.

### Guards estruturais

Executáveis localmente antes de qualquer commit:

```powershell
# Windows
python scripts\architecture_hygiene_guard.py
python scripts\constructor_injection_guard.py
python scripts\runtime_concurrency_guard.py
python scripts\transactional_hotspot_guard.py --fail-on-missing-budgets
python scripts\config_taxonomy_guard.py
```

```bash
# Linux / macOS
python scripts/architecture_hygiene_guard.py
python scripts/constructor_injection_guard.py
python scripts/runtime_concurrency_guard.py
```

| Guard | O que verifica |
|-------|---------------|
| `architecture_hygiene_guard` | Nomes de classe, pacotes, dependências cruzadas proibidas |
| `constructor_injection_guard` | Zero `@Autowired` em fields — apenas injeção por construtor |
| `runtime_concurrency_guard` | Zero executor criado fora da governança `PjbVirtualThreadSpine` |
| `transactional_hotspot_guard` | `@Transactional` apenas em ApplicationService, sem I/O externo |
| `config_taxonomy_guard` | Propriedades de configuração dentro da taxonomia definida |

---

## Quickstart

```bash
# Linux / macOS
bash demo.sh

# Windows
demo.cmd
```

O script realiza automaticamente:
1. Copia `.env.example` → `.env` com valores de demonstração (se não existir)
2. Compila `pjb-core` e `pjb-api` via Maven
3. Sobe PostgreSQL, Kafka, Elasticsearch e Redis via Docker Compose
4. Aplica todas as 243 migrations Flyway
5. Aguarda o backend ficar saudável
6. Exibe os endpoints disponíveis

**Após subir:**

| Endpoint | Descrição |
|----------|-----------|
| `http://localhost:8080/livez` | Liveness check |
| `http://localhost:8080/demo/status` | Estatísticas em tempo real |
| `http://localhost:8080/swagger-ui/index.html` | Documentação de API completa |

Com o profile `docker`, o sistema semeia automaticamente usuários e processos de demonstração para que os dados apareçam imediatamente.

**Para parar:**
```bash
docker compose down
```

---

## Como rodar

### Compilar

```bash
./mvnw install -pl pjb-core -DskipTests
./mvnw test-compile -pl pjb-api
```

### Testes

```bash
# Suite completa
./mvnw test -pl pjb-api

# Teste específico com stack trace completo
./mvnw test -pl pjb-api -Dtest=NomeDoTeste -DtrimStackTrace=false
```

### Infraestrutura local (sem backend)

```bash
docker compose up -d
```

### Backend completo via Docker

```bash
docker compose --profile app up -d --build
```

O serviço `backend` está no profile `app`. Sem ele, o Compose sobe apenas a infraestrutura de suporte. Se a porta `5432` já estiver em uso localmente, defina `PJB_PG_PORT=5433` no `.env` — o backend em Docker continua acessando `postgres:5432` pela rede interna do Compose.

### Variáveis de ambiente necessárias

| Variável | Descrição |
|----------|-----------|
| `PJB_PG_HOST` | Host do PostgreSQL (default: `localhost`) |
| `PJB_PG_PORT` | Porta do PostgreSQL (default: `5432`) |
| `PJB_PG_PASSWORD` | Senha do banco |
| `PJB_MASTER_KEY_BASE64` | Chave mestra de criptografia (Base64, 32 bytes) |
| `PJB_ANTHROPIC_API_KEY` | Chave da API Anthropic para módulos de IA |
| `PJB_KAFKA_BOOTSTRAP` | Endereço do broker Kafka |

---

## Sincronização Git segura

```powershell
.\scripts\git-sync-safe.ps1 "descrição da mudança"
```

A barreira local inspeciona o diff antes de qualquer commit e bloqueia chaves de API, senhas, tokens JWT, certificados e qualquer padrão de segredo conhecido. Detalhes em `docs/security/GIT_SAFE_SYNC.md`.

---

## Critérios de contribuição

### Obrigatórios

- Constructor injection em todas as classes de produção — zero `@Autowired` em fields
- `@Inject` (Jakarta) nos construtores — nunca `@Autowired` Spring em campos
- Sem Lombok em camadas críticas — imutabilidade via Records Java 21
- Sem classe com nome genérico (`Manager`, `Helper`, `Util`, `Processor`, `Handler`)
- Sem rotas REST fora do registry canônico de bounded contexts
- Zero comentários redundantes — nomes expressivos documentam o código
- `@Transactional` apenas em ApplicationService, sem I/O externo dentro da transação
- Sem `CompletableFuture` solto — seguir ADR-0051

### Proibições de regressão

- Sem regressão em sigilo, auditoria, RLS ou ABAC
- Sem regressão em propagação de contexto assíncrono
- Sem aumento no número de falhas na suíte de testes
- Sem alteração de migration já aplicada (checksum Flyway)

### Critério mínimo de aceite

Compilar + guards Python verdes + suíte sem regressão + contratos públicos preservados.

---

## Observabilidade

```
GET /admin/governance/codebase-learning
GET /admin/governance/codebase-learning?refresh=true
```

Expõe leitura viva do estado estrutural: hotspots do core, trilhas de extração, blueprints arquiteturais, fluxos críticos ponta a ponta e razão de cobertura por bounded context. O snapshot tem TTL curto; use `refresh=true` para forçar revarredura sem reiniciar a aplicação.

```
GET /admin/governance/health-matrix
GET /actuator/health
GET /actuator/metrics
```

---

## Substituição nacional

A matriz de substituição compara capacidades do PJB frente a PJe, e-SAJ, eProc, Creta e Projudi por funcionalidade, bounded context e segmento de Justiça. Previne duplicação de contextos e direciona entregas para os pacotes corretos.

```
docs/product/NATIONAL_JUDICIAL_SYSTEM_REPLACEMENT_MATRIX.md
docs/product/NATIONAL_JUDICIAL_SYSTEM_REPLACEMENT_INDEX.json
```

---

## Glossário

| Termo | Significado |
|-------|-------------|
| **NPU** | Número Processo Único — identificador padronizado CNJ (ex: 0000001-00.2024.8.26.0001) |
| **Rito** | Fluxo processual obrigatório definido pela lei (ordinário, sumaríssimo, JEC, etc.) |
| **Autuação** | Ato de registrar formalmente o processo no sistema, com classe, assunto e partes |
| **Distribuição** | Atribuição do processo a uma vara ou juízo competente |
| **Movimentação** | Qualquer ato praticado sobre o processo (despacho, decisão, sentença, certidão) |
| **GIGS** | Grupo de Atividades — conjunto de tarefas processuais com prazo e responsável |
| **Sobrestamento** | Suspensão temporária do processo aguardando julgamento de paradigma |
| **BATNA** | Best Alternative to a Negotiated Agreement — análise de alternativa ao acordo |
| **ABAC** | Attribute-Based Access Control — controle de acesso por atributos do contexto |
| **RLS** | Row Level Security — política de segurança aplicada no nível do banco de dados |
| **ADR** | Architecture Decision Record — registro formal de decisão arquitetural |
| **ICP-Brasil** | Infraestrutura de Chaves Públicas Brasileira — padrão de assinatura digital |
| **Gov.br** | Sistema de autenticação federal com níveis de confiança bronze, prata e ouro |
| **PDPJ** | Plataforma Digital do Poder Judiciário — barramento de integração nacional |
| **MNI** | Modelo Nacional de Interoperabilidade — protocolo de troca entre sistemas judiciais |
| **CNJ** | Conselho Nacional de Justiça — órgão regulador que define classes, assuntos e tabelas |
| **JEC** | Juizado Especial Cível |
| **JEF** | Juizado Especial Federal |
| **JEFP** | Juizado Especial da Fazenda Pública |
