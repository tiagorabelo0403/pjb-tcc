# PJB — Plataforma Judicial Brasileira

Sistema judicial eletrônico construído em Java 21 e Spring Boot 3 para substituir os sistemas legados em operação no Brasil — PJe, e-SAJ, eProc, Creta e Projudi. O objetivo é oferecer uma base tecnológica unificada, verificável e modular, capaz de atender qualquer tribunal federal, estadual, trabalhista, eleitoral ou militar do país.

O PJB foi projetado com um princípio central: cada fluxo crítico deve ser rastreável, testável, resiliente e seguro por padrão. Nenhuma decisão arquitetural introduz opacidade onde existe a possibilidade de governança explícita.

---

## Por que um novo sistema

Os sistemas atuais (PJe, e-SAJ, eProc) acumulam décadas de débito técnico: rotas dispersas sem contrato, acoplamento forte entre interface e domínio, jobs isolados, assinaturas frágeis e baixa observabilidade. O PJB foi desenhado do zero para evitar esses problemas, com fronteiras de domínio rígidas, testes executáveis como critério de aceite e governança estrutural contínua via guards Python e ArchUnit.

---

## Stack

| Componente | Tecnologia |
|------------|-----------|
| Linguagem | Java 21 — Virtual Threads, Records, Sealed Interfaces, Pattern Matching |
| Framework | Spring Boot 3.5, Spring Framework 6 |
| Build | Maven multi-module (`pjb-core` + `pjb-api`) |
| Banco | PostgreSQL com Row Level Security por operação |
| Banco de testes | H2 em memória |
| Migrations | Flyway — 258 versões (V0–V258) |
| Persistência | JPA/Hibernate |
| Segurança | Spring Security, ABAC, Gov.br, ICP-Brasil |
| Resiliência | Resilience4j — Circuit Breaker, Bulkhead, Retry, Timeout |
| Contratos | Pact — Consumer-Driven Contract Testing |
| Qualidade | JaCoCo, Checkstyle, SpotBugs, ArchUnit |
| IA Jurídica | Anthropic Managed Agents API — Memory Stores, Dreams, síntese reflexiva |
| Guards estruturais | 20+ scripts Python + ArchUnit |

---

## Estrutura do repositório

```
pjb/
├── pjb-core/          domínio, aggregates, contratos de porta e IA jurídica
├── pjb-api/           aplicação Spring Boot — controllers, serviços, testes
├── docs/
│   ├── adr/           55 Architecture Decision Records
│   ├── database/      esquemas e políticas de banco
│   ├── openapi/       contratos de API pública
│   ├── security/      políticas de segurança e LGPD
│   ├── product/       matriz de substituição nacional
│   └── quality/       regras de higiene e taxonomia
├── scripts/           guards Python e automação de validação
├── config/            Checkstyle e SpotBugs
└── infra/             Kubernetes, gateway e infraestrutura
```

---

## Módulos funcionais

### 1 — Governança institucional

Gerencia papel, lotação, localização, competência e visibilidade de cada ator no processo. A matriz de visibilidade produz uma explicação auditável para cada decisão de acesso — quem pode ver o quê, por qual motivo, com registro imutável.

### 2 — Motor de rito e distribuição inteligente

Distribui processos por natureza, competência, rito e comarca. Suporta vara única, comarca do interior, JEC itinerante e qualquer configuração de tribunal. O engine explainável documenta cada decisão de distribuição.

### 3 — Motor de celeridade constitucional

Monitora prazos constitucionais por rito, calcula gargalos sistêmicos e sugere aceleradores por área do direito. Não pressiona magistrados — identifica onde o sistema está lento e por quê.

### 4 — Painel interno PJe++ e secretaria cartorária

Filas inteligentes, agrupadores semânticos, lote de assinatura com conferência obrigatória e hash SHA-256 por documento. Cada ato de secretaria tem rastreabilidade de quem fez, quando e com qual resultado.

### 5 — Aceleradores por área do direito

Fluxos especializados para cível, criminal, trabalhista, eleitoral, família, execução, Juizados Especiais (cível, federal e da Fazenda Pública), precatório, falimentar e controle concentrado de constitucionalidade.

### 6 — Chips inteligentes e conciliação

Marcadores semânticos de processo para priorização automática. Módulo de conciliação com sugestão de acordo baseada em precedentes, score de probabilidade e checklist de BATNA.

### 7 — Documentos, dossiê e cadeia de custódia

Cada documento tem origem, estado operacional, hash de integridade e cadeia de confiança. O dossiê documental consolida todos os artefatos de um processo com rastreabilidade completa.

### 8 — Autuação, retificação e qualidade de metadados

Retificação governada com diff jurídico — cada alteração passa por política, avaliação de impacto e aprovação explícita. Score de qualidade de metadados detecta classes ausentes, partes sem documento e rito incompatível antes que o processo avance.

### 9 — Importação e normalização de processos externos

Ingesta processos de PJe, e-SAJ, eProc, Projudi, Creta, MNI e PDPJ. Cada sistema externo tem normalizador específico que padroniza NPU, classe processual e rito antes de persistir.

### 10 — Mandados, certidões e comunicação resiliente

Gestão completa de mandados com diagnóstico de devolução e priorização de urgentes. Certidões automáticas com checklist de pendências e emissão em lote. Domicílio eletrônico judicial com retry exponencial e painel de falhas.

### 11 — GIGS, notas, lembretes e pendências

Atividades processuais (GIGS) com execução governada, visibilidade por sigilo, controle de atos jurisdicionais e lembrete automático de minuta pendente. Notas e lembretes com política de visibilidade por papel e localização.

### 12 — IA jurídica auditável

A IA opera como camada de suporte — nunca substitui decisão humana. A conversa jurídica passa por uma moldura pré-consciente que avalia ramo, tradição doutrinária, risco procedimental, proveniência de evidência e sigilo antes de responder.

**Memory Stores:** coleções de documentos auditáveis que acumulam aprendizado entre sessões. Cada escrita gera versão imutável com suporte a redact para LGPD. Processos sigilosos e críticos jamais têm conteúdo enviado a serviços externos.

**Dreams:** jobs assíncronos que consolidam transcrições de sessão, removem contradições e extraem padrões por rito processual. Operam com outbox pattern e Virtual Threads dedicadas.

### 13 — Relatórios e analytics sem ranking punitivo

Relatórios de gargalo, tempo por rito e retrabalho. Justiça em Números para exportação CNJ. Nenhum relatório identifica magistrado por desempenho — os dados servem à melhoria sistêmica, não à pressão individual.

### 14 — Envelope de integração PDPJ/MNI/API

Envelope canônico `PjbIntegrationEventEnvelope` com UUID, hash de payload, routing key e versão. Mapeamento de eventos judiciais para rota canônica `judicial.{sistema}.{tipo}.{rito}`.

---

## Dez aceleradores implementados

Capacidades que cobrem lacunas que nenhum sistema judicial brasileiro resolve bem:

| # | Serviço | O que faz |
|---|---------|-----------|
| 1 | `NulidadeProcessualRiskPolicy` | Diagnóstico preventivo de nulidade antes de qualquer movimentação — verifica intimação, representação, sigilo, prazo e competência |
| 2 | `ProcessoParalisacaoDiagnosisService` | Identifica por que um processo está parado: expediente sem ciência, documento sem assinatura, tarefa sem responsável, pendência vencida |
| 3 | `CivilSaneamentoChecklistService` | Checklist computável: preliminares, pontos controvertidos, provas, ônus, julgamento antecipado, acordo provável |
| 4 | `SobrestamentoInteligenteService` | Detecta quando o motivo de sobrestamento cessou e avisa automaticamente para dessobrestamento |
| 5 | `ProcessoClusterSimilarityService` | Agrupa processos com mesma parte, pedido e rito — base para lote inteligente e acordo provável |
| 6 | `PrecedenteAplicavelRadarService` | Sinaliza precedente repetitivo, tema suspenso ou divergência jurisprudencial — nunca decide, apenas informa |
| 7 | `ResponsavelWorkloadBalancer` | Sugere responsável por carga e especialidade com justificativa auditável — não impõe |
| 8 | `DomicilioJudicialResilienceService` | Retry com backoff exponencial, painel de falhas e fallback gracioso para domicílio eletrônico |
| 9 | `ArquivamentoPendenciaChecker` | Checklist seguro: custas, expedientes, prazos e documentos — nunca arquiva automaticamente |
| 10 | `ProcessMiningMaterializedViewService` | Tabelas materializadas com atualização assíncrona em Virtual Threads — gargalo por ato, fase e integração |

---

## Ritos processuais cobertos

O PJB trata todos os ritos como primeiro cidadão. O catálogo `RitoProcessual` inclui:

- **Cível:** procedimento comum ordinário, sumário, monitória, possessória, usucapião, consignação, ação civil pública, tutela de urgência e cautelar antecedente
- **Família:** alimentos, divórcio, inventário, arrolamento, adoção, tutela e curatela, investigação de paternidade
- **Criminal:** procedimento penal comum, sumário, sumaríssimo, júri, habeas corpus, execução penal
- **Trabalhista:** rito ordinário, sumaríssimo, mandado de segurança trabalhista, dissídio coletivo
- **Eleitoral:** ação de impugnação, recurso eleitoral
- **Especial:** mandado de segurança individual e coletivo, habeas data, ação popular, ADPF, ADI, ADC, ADIN
- **Execução:** título extrajudicial, título judicial, fiscal, cumprimento de sentença provisório
- **Recursal:** apelação, agravo de instrumento, agravo regimental, embargos de declaração, recurso ordinário
- **Juizados Especiais:** cível, federal e da Fazenda Pública
- **Falimentar, precatório, militar e extrajudicial**

---

## Segurança e conformidade

O modelo de segurança é orientado por identidade, papel, lotação, órgão, unidade, instância, sigilo e trilha auditável.

- **ABAC** com trilha imutável para toda decisão sensível
- **RLS (Row Level Security)** no PostgreSQL para leitura processual sigilosa
- **Step-up Gov.br** e certificado ICP-Brasil qualificado onde exigido
- **Propagação governada de contexto** em execuções assíncronas com Scoped Values (Java 21)
- **Prevenção de prompt injection** com `AnthropicInputSanitizer`
- **Auditoria materializada** para toda operação sobre dado sigiloso — sem log de conteúdo
- **LGPD:** dados sigilosos jamais enviados a serviços externos; redact auditável por versão

---

## Concorrência e execução

Toda execução assíncrona passa pelo `PjbExecutionOrchestrator`. Virtual Threads são centralizadas em `PjbVirtualThreadSpine` — nunca há criação direta de executores fora da governança. Contexto sigiloso é propagado via Scoped Values com bind/restore em toda operação assíncrona.

Bounded concurrency via `PjbBoundedExecutorService`. Structured Concurrency para operações com múltiplos ritos em paralelo. Zero `CompletableFuture` solto — o ADR-0051 define o modelo unificado de execução.

---

## Banco de dados

258 migrations Flyway em sequência numerada (V0–V258). Row Level Security ativo por operação para dados sigilosos. Tabelas materializadas com refresh assíncrono para analytics (ADR-0053). Outbox pattern para efeitos pós-commit sem risco de perda de evento.

```sql
-- Exemplo de política RLS para processo sigiloso
CREATE POLICY processo_sigilo ON processo
    USING (sigilo = false OR current_setting('app.papel') IN ('JUIZ', 'PROMOTOR'));
```

---

## Qualidade executável

A suíte conta com **2.764 testes · 0 falhas · 0 erros**. Toda alteração só é aceita quando melhora comportamento verificável sem reduzir maturidade arquitetural.

Guards de conformidade estrutural executáveis:

```powershell
# Windows
cd C:\pjb
python scripts\architecture_hygiene_guard.py
python scripts\constructor_injection_guard.py
python scripts\runtime_concurrency_guard.py
python scripts\transactional_hotspot_guard.py --fail-on-missing-budgets
python scripts\config_taxonomy_guard.py
```

```bash
# Linux/Mac
python scripts/architecture_hygiene_guard.py
python scripts/constructor_injection_guard.py
python scripts/runtime_concurrency_guard.py
```

55 ADRs documentam cada decisão arquitetural com motivação, consequências e alternativas consideradas. Ler antes de alterar qualquer estrutura de pacote, padrão de concorrência ou política de segurança.

---

## Quickstart — 1 comando

```bash
# Linux/Mac
bash demo.sh

# Windows
demo.cmd
```

O script:
1. Copia `.env.example` para `.env` com valores demo prontos (se não existir)
2. Compila os módulos `pjb-core` e `pjb-api`
3. Sobe PostgreSQL, Kafka, Elasticsearch, Redis e o backend via Docker Compose
4. Aguarda o backend ficar saudável e exibe os endpoints

Após subir, acesse:
- **`http://localhost:8080/livez`** — liveness check
- **`http://localhost:8080/demo/status`** — estatísticas em tempo real (usuários, processos, documentos)
- **`http://localhost:8080/swagger-ui.html`** — documentação de API

Ao iniciar com o profile `docker`, o sistema semeará automaticamente 4 usuários e 3 processos de demonstração para que os dados apareçam imediatamente.

Para parar: `docker compose down`

---

## Como rodar

**Compilar:**
```bash
./mvnw install -pl pjb-core -DskipTests
./mvnw test-compile -pl pjb-api
```

**Testes:**
```bash
# Suite completa
./mvnw test -pl pjb-api

# Teste específico
./mvnw test -pl pjb-api -Dtest=NomeDoTeste -DtrimStackTrace=false
```

**Infraestrutura local:**
```bash
docker compose up -d
```

---

## Sincronização Git segura

```powershell
.\scripts\git-sync-safe.ps1 "descrição da mudança"
```

A barreira local bloqueia commits com chaves de API, senhas, tokens e certificados. Detalhes em `docs/security/GIT_SAFE_SYNC.md`.

---

## Critérios de contribuição

- Constructor injection em todas as classes de produção — zero `@Autowired` em fields
- Sem Lombok em camadas críticas — imutabilidade via records Java 21
- Sem classe com nome genérico (`Manager`, `Helper`, `Util`, `Processor`)
- Sem rotas institucionais fora do registry canônico
- Zero comentários redundantes — nomes expressivos documentam o código
- `@Transactional` apenas em ApplicationService, sem I/O externo dentro da transação
- Sem regressão em sigilo, auditoria, RLS, ABAC ou propagação de contexto

**Critério mínimo:** compilar, preservar contratos públicos, manter os guards verdes e não aumentar falhas na suíte.

---

## Observabilidade

```
GET /admin/governance/codebase-learning
GET /admin/governance/codebase-learning?refresh=true
```

O endpoint expõe uma leitura viva do estado estrutural do projeto: hotspots do core, trilhas internas de extração, blueprints de extração, fluxos críticos ponta a ponta e razão de cobertura de testes por fatia. O relatório de sanidade-aprendizado identifica pacotes com pressão de decomposição, sinalizando o que precisa ser endurecido antes de qualquer extração.

O snapshot em memória tem TTL curto para evitar reescanear a base a cada requisição. Use `refresh=true` nos endpoints administrativo e processual para forçar revarredura quando necessário — sem necessidade de reiniciar a aplicação.

---

## Substituição nacional

A matriz de substituição compara capacidades do PJB frente a PJe, e-SAJ, eProc, Creta e Projudi por funcionalidade. Previne duplicação de bounded contexts e direciona entregas para os pacotes corretos.

```
docs/product/NATIONAL_JUDICIAL_SYSTEM_REPLACEMENT_MATRIX.md
docs/product/NATIONAL_JUDICIAL_SYSTEM_REPLACEMENT_INDEX.json
```
