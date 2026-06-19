![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6DB33F?logo=springboot&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)
![Maven](https://img.shields.io/badge/Build-Maven-C71A36?logo=apachemaven&logoColor=white)
![Testes](https://img.shields.io/badge/Testes-4.112%20%7C%200%20falhas-brightgreen)
![ADRs](https://img.shields.io/badge/ADRs-57-informational)
![Licença](https://img.shields.io/badge/Licença-MIT-blue)

# PJB — Plataforma Judicial Brasileira

> Sistema judicial eletrônico de nova geração, construído em Java 21 e Spring Boot 3.5, projetado para substituir integralmente PJe, e-SAJ, eProc, Creta e Projudi em todos os segmentos da Justiça brasileira.

**Idiomas / Languages:** [🇧🇷 Português (este arquivo)](./README.md) · [🇬🇧 English](./README.en.md)

---

## Navegação rápida

**Início rápido**
- [Sobre o projeto](#sobre-o-projeto)
- [O problema](#o-problema)
- [A proposta](#a-proposta)
- [Pré-requisitos](#pré-requisitos)
- [Instalação e configuração](#instalação-e-configuração)
- [Como executar](#como-executar)
- [Testes](#testes)
- [Documentação da API](#documentação-da-api)

**Arquitetura e domínio**
- [Domínio](#domínio)
- [Arquitetura](#arquitetura)
- [Stack técnica](#stack-técnica)
- [Módulos funcionais](#módulos-funcionais)
- [Aceleradores inteligentes](#aceleradores-inteligentes)
- [Ritos processuais cobertos](#ritos-processuais-cobertos)

**Infraestrutura e qualidade**
- [Segurança e conformidade](#segurança-e-conformidade)
- [Concorrência e execução assíncrona](#concorrência-e-execução-assíncrona)
- [Escalabilidade e resiliência operacional](#escalabilidade-e-resiliência-operacional)
- [Banco de dados](#banco-de-dados)
- [Qualidade executável](#qualidade-executável)
- [Observabilidade](#observabilidade)
- [Substituição nacional](#substituição-nacional)

**Contribuição e projeto**
- [Contribuindo](#contribuindo)
- [Sincronização Git segura](#sincronização-git-segura)
- [Próximos passos](#próximos-passos)
- [Autor](#autor)
- [Licença](#licença)
- [Glossário](#glossário)

---

## Sobre o projeto

O PJB é uma plataforma de substituição total — não incremental — dos sistemas judiciais eletrônicos em uso no Brasil. Cinco sistemas foram construídos ao longo de décadas por entidades diferentes, sem nenhuma coordenação de protocolo, modelo de dados ou interface. O resultado é uma infraestrutura que hoje suporta mais de **80 milhões de processos ativos**, **91 tribunais** e **cerca de 30 mil magistrados**, mas que não foi projetada para escalar, auditar ou integrar com o rigor que a legislação e a sociedade passaram a exigir.

O PJB foi construído do zero com três compromissos inegociáveis: rastreabilidade total em cada ação do sistema, testabilidade como critério de aceite de qualquer funcionalidade e segurança por construção — ABAC, RLS e propagação governada de sigilo não são camadas adicionadas depois, são restrições que guiam cada decisão arquitetural.

---

## O problema

| Sistema | Tribunal principal | Problema central |
|---------|-------------------|-----------------|
| PJe | CNJ / maioria dos tribunais | Acoplamento forte entre UI e domínio, rotas sem contrato |
| e-SAJ | TJSP, TJBA e outros estaduais | Modelo de dados proprietário, sem API pública |
| eProc | TRF1, TRF4 e estaduais | Jobs isolados, assinaturas frágeis |
| Creta | Justiça do Trabalho | Baixa observabilidade, sem suporte a novos ritos |
| Projudi | Tribunais estaduais menores | Débito técnico crítico, sem path de migração |

Nenhum dos cinco foi projetado com escalabilidade horizontal, auditoria de acesso granular ou suporte completo às classes processuais do CPC/2015 e das reformas trabalhistas. O PJB não é uma reescrita deles. É uma ruptura deliberada com esse modelo.

---

## A proposta

O PJB foi projetado do zero com três compromissos inegociáveis:

**1. Rastreabilidade total.** Toda decisão de acesso, distribuição, movimentação e comunicação produz uma trilha auditável, imutável e explicável. Não existe ação no sistema que não possa ser reconstituída — quem fez, quando fez, com qual autoridade e qual foi o efeito.

**2. Testabilidade como critério de aceite.** Nenhuma funcionalidade existe sem comportamento verificável. A suíte de testes é o contrato executável do sistema — se o teste passa, o comportamento está garantido. Funcionalidade sem teste não é funcionalidade: é intenção.

**3. Segurança por construção.** ABAC, RLS por operação, propagação governada de contexto sigiloso e Step-up Gov.br não são camadas adicionadas depois. São restrições que guiam cada decisão arquitetural desde o início — antes do primeiro endpoint, antes da primeira migration, antes da primeira linha de código de domínio.

---

## Pré-requisitos

Antes de clonar e rodar o projeto, certifique-se de ter instalado:

| Ferramenta | Versão mínima | Finalidade |
|------------|--------------|-----------|
| **JDK** | 21 | Compilação e execução (Virtual Threads obrigatórias) |
| **Maven** | 3.9+ | Build multi-module (`pjb-core` + `pjb-api`) |
| **Docker** | 24+ | PostgreSQL, Kafka, Redis, Elasticsearch via Compose |
| **Docker Compose** | v2 (plugin) | Orquestração da infraestrutura local |
| **Python** | 3.10+ | Guards estruturais em `scripts/` |

> **IDE recomendada:** IntelliJ IDEA 2024+ com os plugins Checkstyle e SonarLint ativos. O projeto usa records, sealed classes e pattern matching do Java 21 — versões anteriores da IDE não reconhecem toda a sintaxe.

---

## Instalação e configuração

### 1. Clonar o repositório

```bash
git clone https://github.com/tiagorabelo0403/pjb-tcc.git
cd pjb-tcc
```

### 2. Configurar variáveis de ambiente

```bash
cp .env.example .env
```

Abra o `.env` e preencha as variáveis obrigatórias:

| Variável | Descrição | Exemplo |
|----------|-----------|---------|
| `PJB_PG_HOST` | Host do PostgreSQL | `localhost` |
| `PJB_PG_PORT` | Porta do PostgreSQL | `5432` |
| `PJB_PG_PASSWORD` | Senha do banco | `pgpassword` |
| `PJB_MASTER_KEY_BASE64` | Chave mestra de criptografia (Base64, 32 bytes) | gerada pelo script |
| `PJB_ANTHROPIC_API_KEY` | Chave da API Anthropic para módulos de IA | `sk-ant-...` |
| `PJB_KAFKA_BOOTSTRAP` | Endereço do broker Kafka | `localhost:9092` |

> Para ambientes de demonstração, o `.env.example` já contém valores funcionais que o `demo.sh` / `demo.cmd` usa automaticamente.

### 3. Subir a infraestrutura

```bash
docker compose up -d
```

Isso sobe PostgreSQL 17, Apache Kafka 3.8, Redis 7.4 e Elasticsearch 8.15. As migrations Flyway (V0–V296) são aplicadas automaticamente na primeira conexão do backend.

### 4. Verificar os profiles Spring

O projeto usa profiles Spring Boot separados por ambiente. O arquivo base é `application.yml`; cada profile sobrescreve apenas o que muda:

| Profile | Arquivo | Quando usar |
|---------|---------|------------|
| `dev` | `application-dev.yml` | Desenvolvimento local com infraestrutura no Docker |
| `local` | `application-local.yml` | Banco e serviços rodando diretamente no host |
| `docker` | `application-docker.yml` | Backend dentro de container Docker |
| `prod` | `application-prod.yml` | Produção — exige todas as variáveis de ambiente |
| `k8s` | `application-k8s.yml` | Kubernetes |

Para rodar localmente, o profile `dev` é o recomendado. Ele é ativado automaticamente pelo `demo.sh` / `demo.cmd`. Para ativar manualmente:

```bash
# Via Maven
./mvnw spring-boot:run -pl pjb-api -Dspring-boot.run.profiles=dev

# Via variável de ambiente
export SPRING_PROFILES_ACTIVE=dev
java -jar pjb-api/target/pjb-api.jar
```

### 5. Compilar

```bash
# Compilar o módulo de domínio
./mvnw install -pl pjb-core -DskipTests

# Compilar o módulo de API (inclui geração de classes de teste)
./mvnw test-compile -pl pjb-api
```

---

## Como executar

### Quickstart completo (recomendado)

O script de demonstração faz tudo em sequência: copia o `.env`, compila, sobe a infraestrutura, aplica as migrations e aguarda o backend ficar saudável.

```bash
# Linux / macOS
bash demo.sh

# Windows
demo.cmd
```

### Executar apenas o backend (infraestrutura já no ar)

```bash
# Via Maven Wrapper (recomendado em desenvolvimento)
./mvnw spring-boot:run -pl pjb-api

# Via JAR empacotado
./mvnw package -pl pjb-api -DskipTests
java -jar pjb-api/target/pjb-api.jar
```

### Backend completo via Docker (build + infra juntos)

```bash
docker compose --profile app up -d --build
```

O serviço `backend` está no profile `app`. Sem ele, o Compose sobe apenas a infraestrutura de suporte. Se a porta `5432` já estiver em uso localmente, defina `PJB_PG_PORT=5433` no `.env` — o backend em Docker continua acessando `postgres:5432` pela rede interna do Compose.

### Endpoints após subir

| Endpoint | Descrição |
|----------|-----------|
| `http://localhost:8080/livez` | Liveness check |
| `http://localhost:8080/demo/status` | Estatísticas em tempo real |
| `http://localhost:8080/swagger-ui/index.html` | Documentação interativa da API |
| `http://localhost:8080/v3/api-docs` | Especificação OpenAPI 3.1 (JSON) |
| `http://localhost:8080/actuator/health` | Health check completo |
| `http://localhost:8080/actuator/metrics` | Métricas Micrometer |

Com o profile `docker`, o sistema semeia automaticamente usuários e processos de demonstração.

**Para encerrar:**
```bash
docker compose down
```

---

## Testes

### Rodar a suíte completa

```bash
./mvnw test -pl pjb-api
```

### Rodar um teste específico com stack trace completo

```bash
./mvnw test -pl pjb-api -Dtest=NomeDoTeste -DtrimStackTrace=false
```

### Rodar apenas testes de integração

```bash
./mvnw test -pl pjb-api -Dgroups=integration
```

### Métricas atuais

| Métrica | Valor |
|---------|-------|
| Total de testes | **4.112** |
| Falhas | **0** |
| Erros | **0** |
| Skipped | 5 |
| Tempo de execução (suite completa) | **~15 min** (914 s em hardware local) |

A suíte cobre unitários com Mockito, testes de integração com H2 em memória e integration tests contra schema PostgreSQL via Testcontainers. Toda alteração só é aceita quando melhora comportamento verificável sem reduzir maturidade arquitetural — sem regressão é critério de merge, não meta.

### Relatório de cobertura (JaCoCo)

```bash
./mvnw test -pl pjb-api
# Relatório gerado em:
# pjb-api/target/site/jacoco/index.html
```

---

## Documentação da API

O PJB expõe documentação interativa completa via **Swagger UI**, disponível após subir o backend:

```
http://localhost:8080/swagger-ui/index.html
```

A especificação OpenAPI 3.1 está disponível em:

```
http://localhost:8080/v3/api-docs
```

Os contratos versionados também estão documentados estaticamente em:

```
docs/openapi/
```

Toda rota REST é registrada no registry canônico de bounded contexts. O `PjbOpenApiContractWeaknessDetectorTest` valida automaticamente que nenhuma rota existe sem contrato OpenAPI registrado, que nenhum campo usa `Map<String,Object>` sem schema tipado e que datas seguem `format: date-time`.

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

**Distribuição** é o ato de atribuir um processo a uma vara. O motor de distribuição avalia natureza, competência, rito, comarca, carga da unidade e regras do tribunal. Cada decisão produz uma explicação auditável com todos os critérios avaliados.

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
| `criminal` | Boletins de ocorrência, inquéritos policiais, delegacias institucionais, escopo policial hierárquico por lotação |
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
│   ├── adr/                          57 Architecture Decision Records
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
| Banco de testes | H2 em memória + Testcontainers |
| Migrations | Flyway — V0–V296, com particionamento mensal em tabelas de evento |
| Persistência | JPA / Hibernate com `ddl-auto: validate` em produção |
| Mensageria | Apache Kafka 3.8 — eventos judiciais e outbox |
| Cache | Redis 7.4 |
| Busca | Elasticsearch 8.15 |
| Segurança | Spring Security, ABAC, Gov.br, ICP-Brasil, Passkey/WebAuthn |
| Resiliência | Resilience4j — Circuit Breaker auditável, Bulkhead, Retry, Timeout |
| Contratos | Pact — Consumer-Driven Contract Testing |
| IA Jurídica | Anthropic Claude API — Memory Stores, Dreams, síntese reflexiva |
| Observabilidade | Micrometer, Spring Actuator, Process Mining materializado |
| Análise estática | Qodana (JetBrains), JaCoCo, Checkstyle, SpotBugs, ArchUnit |
| Guards estruturais | 7 scripts Python + ArchUnit integrados ao CI |
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

**Gate de completude processual:** verifica se o pacote documental está completo antes de permitir que o processo avance de fase. A validação tem duas camadas: estrutural (checklists configuráveis por rito, com pendências tipificadas e prazo de resolução) e semântica (OCR + VectorSearch detecta a presença efetiva de conteúdo exigido em documentos já anexados, não apenas a existência do arquivo). Pendências são notificadas via outbox com ciclo de resolução rastreável. O processo não avança enquanto houver lacuna de completude — e a secretaria pode fazer override com justificativa mínima auditável.

### 13 — Relatórios e analytics sem ranking punitivo

Relatórios de gargalo, tempo médio por rito, taxa de retrabalho e taxa de conciliação. Exportação Justiça em Números para o CNJ. Nenhum relatório identifica magistrado por desempenho individual — os dados servem à melhoria sistêmica, não à pressão sobre pessoas.

### 14 — Envelope de integração PDPJ/MNI/API

Envelope canônico `PjbIntegrationEventEnvelope` com UUID, hash de payload, routing key e versão semântica. Mapeamento de eventos judiciais para rota canônica `judicial.{sistema}.{tipo}.{rito}`. Suporta emissão e consumo de eventos com garantia de at-least-once via outbox.

### 15 — Módulo criminal e investigação policial

A delegacia é modelada como unidade institucional de primeira linha, com lotação, competência territorial e grade de plantão — não como um papel genérico, mas como uma entidade com identidade e hierarquia própria dentro do bounded context criminal.

Boletins de ocorrência produzem inquéritos rastreáveis. Cada BO tem tipificação, envolvidos, cadeia de custódia de documentos e vínculo automático ao processo penal quando há autuação. O inquérito acompanha o processo desde a fase policial até a fase judicial, sem quebra de rastreabilidade.

O escopo policial é resolvido por lotação, não por papel. O que um delegado enxerga e movimenta é determinado pela delegacia onde está lotado. O DelegadoPainel materializa exatamente essa visão restrita — sem exposição de dados de outra unidade. O `WorkItemScopeGuard` aplica essa restrição como P0: qualquer acesso a item de trabalho fora do escopo de lotação é bloqueado no guard central, e o ArchUnit garante em tempo de build que não existe caminho de código que consiga contorná-lo.

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
| **Login por certificado ICP-Brasil** | Fluxo desafio-resposta completo: nonce criptográfico emitido pelo servidor, assinatura pelo certificado do usuário, verificação da cadeia ICP-Brasil, extração de identidade do subject DN e resolução de contexto institucional por lotação. A sessão de certificado é emitida como tipo distinto da sessão de senha — sem mistura de níveis de garantia |
| **Scoped Values (Java 21)** | Propagação de contexto sigiloso em Virtual Threads — sem vazamento |
| **AnthropicInputSanitizer** | Prevenção de prompt injection nas interações com IA |
| **Auditoria materializada** | Toda operação sobre dado sigiloso — sem log de conteúdo, só metadado |
| **AuthzTrail materializado** | Toda decisão de autorização produz registro imutável em `tb_authz_trail`, deduplicado por chave semântica — hash compacto de ator, recurso e efeito. Entradas idênticas colapsam; o ledger é consultável por padrão de acesso, não apenas por janela de tempo |
| **Sanitização ICP-Brasil** | CPF e CNPJ removidos de respostas de API, cache de certificados, eventos de assinatura e entradas do audit ledger ICP. Onde a correlação é necessária, o identificador é hasheado — jamais em claro |
| **BOLA guard (WorkItemScopeGuard)** | Impede que qualquer ator acesse item de trabalho de unidade ou lotação diferente da sua. Aplicado como controle P0 — ArchUnit garante em tempo de build que não existe caminho de código capaz de bypassar o guard |
| **Rate limiting** | Rotas críticas protegidas contra abuso com limite de requisições por período. Resposta padronizada RFC 7807. `createOficio` e endpoints de comunicação têm orçamento próprio, separado do tráfego geral |
| **Security event logger** | Todo evento de segurança relevante — autenticação, autorização negada, step-up, bypass tentado — produz entrada em log estruturado separado do log de aplicação, auditável de forma independente e sem mistura com ruído operacional |
| **Circuit breaker auditável** | Estado de abertura/fechamento de cada circuit breaker é registrado com timestamp, causa e contagem de falhas — a história de degradação de uma integração é rastreável, não apenas o estado atual |
| **LGPD** | Dados sigilosos nunca enviados a serviços externos; redact auditável por versão |
| **Dual approval** | Operações críticas exigem confirmação de segundo ator autorizado |

---

## Concorrência e execução assíncrona

Toda execução assíncrona passa obrigatoriamente pelo `PjbExecutionOrchestrator`. Virtual Threads são centralizadas em `PjbVirtualThreadSpine` — nenhum executor é criado diretamente fora da governança central.

Contexto sigiloso é propagado via Scoped Values com bind/restore em toda fronteira de execução assíncrona, impedindo que sigilo de um processo contamine outro em Virtual Thread diferente.

Bounded concurrency via `PjbBoundedExecutorService` previne explosão de conexões de banco em cargas de pico. Structured Concurrency gerencia operações que dependem de múltiplos ritos em paralelo — a falha de um filho cancela os demais, sem leak de recursos.

Zero `CompletableFuture` solto no código de produção. O ADR-0051 define o modelo unificado de execução e é aplicado por guard Python e ArchUnit a cada build.

---

## Escalabilidade e resiliência operacional

Não carregar dados desnecessariamente no JVM é tratado como restrição de projeto, não sugestão. O motor de redistribuição federativa calcula carga por jurisdição inteiramente no banco: uma única query com `GROUP BY jurisdicao_id` e dois `SUM(CASE WHEN...)` retorna os valores agregados diretamente. Nenhuma instância de `Processo` é construída, nenhuma lista é materializada, nenhum acumulador Java acumula o que o executor do PostgreSQL já sabe calcular.

A tabela `tb_outbox_event` é particionada mensalmente por `created_month`. Eventos processados não são deletados em linha — a partição inteira é descartada via `DROP TABLE` quando o mês vira. O custo de expurgo é O(1) independente do volume. Um tribunal com um milhão de eventos por mês tem exatamente o mesmo custo de limpeza que um com cem.

A trilha de autorização (`tb_authz_trail`) materializa toda decisão de acesso com uma chave semântica: hash compacto de ator, recurso e decisão, não UUID. Decisões idênticas repetidas colapsam na mesma entrada — sem duplicação silenciosa de registros para o mesmo par (sujeito, objeto, efeito). O ledger permanece consultável por padrão de acesso, não apenas por janela temporal.

Os 8 tópicos Kafka são declarados explicitamente via beans `NewTopic` em `PjbKafkaTopicConfig`, criados pelo `KafkaAdmin` do Spring no startup. O número de partições é derivado diretamente de `PjbKafkaScaleProperties.listenerConcurrency` (padrão 3) — os dois são matematicamente impossíveis de ficar fora de sincronia porque um lê do outro. Com 1 partição, o Kafka limita a 1 consumer ativo por grupo independente da concorrência configurada; com 3 partições, cada thread recebe uma partição e opera em paralelo real. Qualquer ambiente novo — dev, staging, produção — nasce configurado corretamente sem intervenção manual. Retenção explícita em 7 dias com segmentos de 512 MB.

Dados pessoais sensíveis — CPF e CNPJ — foram removidos de todas as camadas onde não precisam estar: resposta de API de metadados ICP-Brasil, cache de certificados, eventos de assinatura e entradas do audit ledger de cadeia ICP. Onde o identificador é necessário para correlação, é armazenado como referência hasheada, nunca em claro.

---

## Banco de dados

296 migrations Flyway (V0–V296), aplicadas em sequência, com `validateOnMigrate=true` e `outOfOrder=false`. O schema é sempre validado pelo Hibernate no startup — qualquer drift entre entidade e banco é detectado antes da primeira requisição.

Row Level Security ativo por operação para dados sigilosos. Tabelas materializadas com refresh assíncrono para analytics (ADR-0053). Outbox pattern para efeitos pós-commit sem risco de perda de evento em falha de transação. A tabela de outbox é particionada mensalmente — expurgo de partições inteiras via `DROP TABLE`, sem varredura de linha.

```sql
-- Exemplo de política RLS para processos sigilosos
CREATE POLICY processo_sigilo ON processo
    USING (sigilo = false OR current_setting('app.papel') IN ('JUIZ', 'PROMOTOR'));
```

---

## Qualidade executável

| Métrica | Estado |
|---------|--------|
| Testes | **4.112 · 0 falhas · 0 erros** |
| ADRs | 57 decisões arquiteturais documentadas |
| Guards Python | 7 scripts ativos em CI |
| SBOM | CycloneDX gerado a cada build |
| Correlation ID | Obrigatório em toda requisição |

57 ADRs documentam cada decisão arquitetural com motivação, consequências e alternativas consideradas. Devem ser lidos antes de alterar qualquer estrutura de pacote, padrão de concorrência ou política de segurança.

O pipeline gera automaticamente um SBOM CycloneDX a cada build, mantendo inventário auditável de todas as dependências com versão e licença. O evidence gate de CI rejeita merges sem cobertura de guarda estrutural completa. Correlation ID obrigatório em toda requisição — propagado via contexto e registrado em cada entrada de log, permitindo rastreamento ponta a ponta sem agregador externo.

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
| `anti_mock_prod_guard` | Bloqueia se mocks de integração crítica estiverem ativos em produção: Gov.br, ICP-Brasil, Kafka, Elasticsearch, IA |
| `openapi_weakness_detector` | Detecta `Map<String,Object>` sem schema tipado, campos sem `format: date-time` e rotas sem contrato OpenAPI registrado |

---

## Observabilidade

```
GET /admin/governance/codebase-learning
GET /admin/governance/codebase-learning?refresh=true
GET /admin/governance/sanidade-aprendizado
GET /admin/governance/health-matrix
GET /actuator/health
GET /actuator/metrics
```

Expõe leitura viva do estado estrutural: hotspots do core, trilhas internas de extração do core, blueprints de extração, fluxos críticos ponta a ponta e razão de cobertura por bounded context. O snapshot em memória tem TTL curto; use `refresh=true` para forçar revarredura sem reiniciar a aplicação.

---

## Contribuindo

### Estratégia de branches

| Branch | Finalidade |
|--------|-----------|
| `master` | Branch principal — sempre estável, reflete produção |
| `feature/nome-da-feature` | Novas funcionalidades |
| `fix/descricao-do-bug` | Correções de bug |
| `refactor/escopo` | Refatorações sem mudança de comportamento |
| `docs/escopo` | Atualizações de documentação |

### Padrão de commits (Conventional Commits)

Este projeto adota [Conventional Commits](https://www.conventionalcommits.org/pt-br/v1.0.0/):

```
<tipo>(escopo opcional): descrição em minúsculas

Corpo opcional explicando o "por quê", não o "o quê".
```

| Tipo | Quando usar |
|------|-------------|
| `feat` | Nova funcionalidade |
| `fix` | Correção de bug |
| `refactor` | Refatoração sem mudança de comportamento externo |
| `test` | Adição ou correção de testes |
| `docs` | Documentação |
| `chore` | Manutenção de build, CI, dependências |
| `perf` | Melhoria de performance |

### Abrindo um Pull Request

1. Crie uma branch a partir de `master` com o padrão acima
2. Rode os guards Python e confirme que passam localmente
3. Rode a suíte de testes e confirme 0 regressões: `./mvnw test -pl pjb-api`
4. Abra o PR com título seguindo Conventional Commits
5. Descreva o que mudou, por que mudou e quais testes cobrem a mudança

### Regras invioláveis

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

## Sincronização Git segura

```powershell
.\scripts\git-sync-safe.ps1 "descrição da mudança"
```

A barreira local inspeciona o diff antes de qualquer commit e bloqueia chaves de API, senhas, tokens JWT, certificados e qualquer padrão de segredo conhecido. Detalhes em `docs/security/GIT_SAFE_SYNC.md`.

---

## Substituição nacional

A matriz de substituição compara capacidades do PJB frente a PJe, e-SAJ, eProc, Creta e Projudi por funcionalidade, bounded context e segmento de Justiça. Previne duplicação de contextos e direciona entregas para os pacotes corretos.

```
docs/product/NATIONAL_JUDICIAL_SYSTEM_REPLACEMENT_MATRIX.md
docs/product/NATIONAL_JUDICIAL_SYSTEM_REPLACEMENT_INDEX.json
```

---

## Autor

**Tiago Rabelo**
Engenharia de Software — Universidade Católica de Quixadá (Unicatólica)
Trabalho de Conclusão de Curso — 2024/2025

🔗 [github.com/tiagorabelo0403](https://github.com/tiagorabelo0403)

---

## Licença

Este projeto está licenciado sob a [MIT License](./LICENSE).

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
| **BO** | Boletim de Ocorrência |
| **SBOM** | Software Bill of Materials — inventário auditável de dependências |

---

## Próximos passos

### Backend

O backend cobre integralmente os bounded contexts descritos neste documento — 15 módulos funcionais, 57 ADRs, 4.112 testes e 296 migrations aplicadas. A API REST está completamente documentada via OpenAPI 3.1 e Swagger UI, pronta para consumo por qualquer cliente.

### Frontend — em análise e planejamento

A camada de apresentação está em fase de análise e decisão arquitetural. O backend foi construído desde o início com a separação de frontend e backend como premissa — toda a comunicação acontece via REST com contratos OpenAPI versionados, o que dá liberdade total de escolha de tecnologia no lado do cliente.

As questões que estão sendo avaliadas antes de iniciar o desenvolvimento:

**Modelo de renderização:** SPA puro (React, Vue, Angular) ou SSR/SSG (Next.js, Nuxt) — a escolha impacta diretamente o SEO, o tempo de carregamento em conexões lentas (frequentes nos tribunais do interior) e a estratégia de cache de sessão.

**Perfis de interface:** o sistema tem atores com fluxos radicalmente diferentes — magistrado, servidor de secretaria, advogado, parte, delegado, administrador institucional. A decisão é entre uma SPA única com rotas protegidas por papel ou interfaces separadas por perfil, cada uma otimizada para o fluxo daquele ator específico.

**Autenticação no cliente:** o backend já implementa Gov.br (bronze/prata/ouro), ICP-Brasil com desafio-resposta por certificado, Passkey/WebAuthn e step-up contextual. O frontend precisará lidar com essa diversidade de flows de autenticação de forma coesa — a escolha de framework impacta como isso será gerenciado no estado da aplicação.

**Integração com o contrato OpenAPI:** o contrato `/v3/api-docs` já está disponível e estável. A geração automática de cliente tipado (via OpenAPI Generator ou similar) está sendo avaliada para eliminar a necessidade de manter DTOs duplicados entre backend e frontend.

A decisão final será registrada em um ADR dedicado antes de qualquer linha de código de frontend ser escrita.
