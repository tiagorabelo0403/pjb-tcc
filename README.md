# PJB — Plataforma Judicial Brasileira

O PJB é uma plataforma judicial soberana construída em Java 21 e Spring Boot 3, desenvolvida para consolidar os fluxos críticos do processo judicial eletrônico em uma arquitetura única, verificável e modular. O objetivo central é oferecer uma base tecnológica capaz de substituir gradualmente os sistemas judiciais legados em operação no Brasil — PJe, e-SAJ, eProc, Creta e Projudi — com cobertura nacional e suporte a todos os ritos processuais.

O sistema foi desenhado a partir de um princípio simples: cada fluxo crítico deve ser rastreável, testável, resiliente, observável e seguro por padrão. Nenhuma decisão arquitetural deve introduzir opacidade onde existe a possibilidade de governança explícita.

---

## Objetivo institucional

A plataforma serve como núcleo tecnológico para operações judiciais de alta escala. A arquitetura evita os vícios consolidados nas plataformas legadas: rotas dispersas sem contrato, acoplamento forte entre interface e domínio, monólitos sem fronteira, jobs isolados, assinaturas frágeis, baixa observabilidade e inconsistência entre secretaria, gabinete, partes, órgãos externos e instâncias recursais.

Cada eixo funcional nasce de decisões arquiteturais documentadas (ADRs), com evidência executável — testes, guards Python e verificação de conformidade estrutural — garantindo que o crescimento da base não introduza degradação silenciosa.

---

## Stack principal

| Componente | Tecnologia |
|-----------|-----------|
| Linguagem | Java 21 (Virtual Threads, Records, Sealed Interfaces, Pattern Matching) |
| Framework | Spring Boot 3.5 |
| Build | Maven multi-module |
| Banco principal | PostgreSQL com RLS (Row Level Security) |
| Banco de testes | H2 em memória |
| Migrations | Flyway (252+ versões) |
| Persistência | JPA/Hibernate |
| Segurança | Spring Security, ABAC, Gov.br |
| Resiliência | Resilience4j (Circuit Breaker, Bulkhead, Retry) |
| Contratos | Pact (Consumer-Driven Contract Testing) |
| Qualidade | JaCoCo, Checkstyle, SpotBugs |
| IA Jurídica | Anthropic Managed Agents API (Memory Stores + Dreams) |
| Testes estruturais | ArchUnit + guards Python |

---

## Organização do repositório

```text
.
├── pjb-api       # aplicação principal, superfícies HTTP, serviços e testes
├── pjb-core      # núcleo modular, contratos de domínio e IA jurídica
├── docs          # ADRs, arquitetura, segurança, OpenAPI, relatórios
├── scripts       # guards arquiteturais e automação de validação
├── config        # políticas de qualidade (Checkstyle, SpotBugs)
├── infra         # Kubernetes, banco, gateway e infraestrutura
└── tooling       # ferramentas auxiliares
```

---

## Eixos arquiteturais

### Processo judicial

O ciclo de vida processual cobre todos os ritos brasileiros — cível comum, sumário, criminal, trabalhista, eleitoral, Juizados Especiais (cível, federal e da Fazenda Pública), Núcleo de Justiça Digital 4.0, falimentar, família, execução, precatório, controle concentrado de constitucionalidade e tutelas de urgência. O catálogo completo está em `LegalAiJudicialRiteCatalog`, com base legal e contexto de aprendizado para cada rito.

Cada processo tramita por:
- Bounded context `core.processo`: ciclo de vida, sigilo, execução e prazo
- Bounded context `core.procedural`: classificação, competência, rito, roteamento nacional
- Bounded context `core.kernel.recursal`: malha recursal, state machine e instâncias

### Secretaria judicial

A secretaria opera por filas, agenda, cobertura operacional e painel de gestão. Fluxos assíncronos de movimentação, despacho e expedição passam por descritores de execução governados, sem retenção de conexão de banco.

### Comunicação institucional

Entradas institucionais seguem rota canônica auditável. Certificação, onboarding, painel e governança integram o eixo `core.comunicacao.institucional`. Nenhuma rota diverge do registry canônico.

### Integração judicial

Conectores MNI e DataJud com replay, política, telemetria e segurança. Remessas, decisões e movimentações intercortes passam por validação de readiness e envelope de prova antes da submissão.

---

## Inteligência artificial jurídica

A IA do PJB opera como camada de suporte auditável — não como substituta da decisão humana. Cada capacidade respeita revisão humana, sigilo processual e rastreabilidade de evidência.

### Conversa jurídica com moldura pré-consciente

A conversa jurídica passa por uma moldura pré-consciente interna antes de qualquer resposta: ramo jurídico, tradição doutrinária, piso de autoridade, risco procedimental, proveniência de evidência, sigilo, trust zone e sinais de alucinação. A moldura não é decorativa — ela vira metadado operacional que guia o approval final.

### Memory Stores e aprendizado reflexivo

O sistema acumula aprendizados entre sessões por meio da Anthropic Managed Agents API. Memory Stores são coleções de documentos auditáveis montados na sessão do agente. Cada escrita gera uma `MemoryVersion` imutável com suporte a redact para conformidade LGPD.

O aprendizado é governado pela política de sigilo:

| Nível | Pode ir à Anthropic | Acesso |
|-------|--------------------|----|
| PUBLIC | Sim | read_write |
| INSTITUCIONAL | Sim | read_write |
| SIGILOSO | Não | read_only local |
| CRÍTICO | Não | negado |

Processos sigilosos e críticos jamais têm conteúdo enviado a serviços externos.

### Dreams — síntese noturna

Dreams são jobs assíncronos que recebem um memory store de entrada e até 100 transcrições de sessão. Consolidam duplicatas, removem contradições e extraem padrões, produzindo um novo store de saída sem modificar o original. O ciclo opera com outbox pattern e polling com backoff exponencial em Virtual Threads dedicadas.

### Síntese de conhecimento por rito processual

O `LegalAiKnowledgeSynthesisService` executa o ciclo de revisão e síntese interna da IA jurídica: varre os memory stores ativos, agrupa entradas por rito processual (usando o catálogo de 30+ ritos) e produz um relatório que alimenta o próximo ciclo de dreaming. Ritos sem cobertura são identificados explicitamente para priorização de aprendizado.

A plataforma trata todos os ritos processuais como primeiro cidadão — cível, criminal, trabalhista, eleitoral, especial, falimentar, família, execução e constitucional.

---

## Segurança

O modelo de segurança é orientado por identidade, papel, lotação, órgão, unidade, instância, sigilo e trilha auditável.

- **ABAC** para decisões sensíveis com trilha imutável
- **RLS (Row Level Security)** para leitura processual sigilosa por PostgreSQL
- **Step-up Gov.br** e certificado qualificado onde aplicável
- **Propagação governada de contexto** em execuções assíncronas com Scoped Values
- **Prevenção de prompt injection** com `AnthropicInputSanitizer`
- **Auditoria materializada** para toda operação sobre dado sigiloso — sem log de conteúdo

---

## Concorrência e execução

A base evita `CompletableFuture` solto, schedulers isolados e criação direta de executores fora da governança. Virtual Threads são centralizadas em `PjbVirtualThreadSpine`, com preservação de contexto por `PjbExecutionContextTaskDecorator` e semáforo de bounded concurrency via `PjbBoundedExecutorService`.

---

## Aprendizado estrutural governado

O endpoint `/codebase-learning` expõe uma leitura viva do estado estrutural do projeto: hotspots do core, trilhas internas de extração, blueprints de extração, fluxos críticos ponta a ponta e razão de cobertura de testes por fatia. O relatório de sanidade-aprendizado identifica pacotes com pressão de decomposição, sinalizando o que precisa ser endurecido antes de qualquer extração.

O snapshot em memória tem TTL curto para evitar rescanear a base a cada requisição. Use `refresh=true` nos endpoints administrativo e processual para forçar revarredura quando necessário — sem necessidade de reiniciar a aplicação.

```
GET /admin/governance/codebase-learning
GET /admin/governance/codebase-learning?refresh=true
```

---

## Qualidade executável

A suíte atual conta com **2.498 testes · 0 falhas novas · 0 erros**. Toda alteração só é aceita quando melhora o comportamento verificável sem reduzir maturidade arquitetural.

Guards de conformidade estrutural:

```bash
python scripts/architecture_hygiene_guard.py
python scripts/constructor_injection_guard.py
python scripts/runtime_concurrency_guard.py
python scripts/transactional_hotspot_guard.py --fail-on-missing-budgets
python scripts/config_taxonomy_guard.py
python scripts/repository_cleanliness_guard.py
python scripts/java_comment_discipline_guard.py
python scripts/canonical_institutional_route_guard.py
python scripts/replacement_matrix_guard.py
python scripts/tribunal_readiness_guard.py
python scripts/access_key_and_unavailability_guard.py
python scripts/judicial_innovation_guard.py
python scripts/judicial_innovation_part_two_guard.py
python scripts/judicial_innovation_part_three_guard.py
python scripts/legal_ai_policy_catalog_guard.py
```

No Windows:

```powershell
cd C:\pjb
$env:MAVEN_OPTS="-Xms512m -Xmx4096m -XX:+UseG1GC"
.\scripts\pjb-api-clean-test-errors.cmd
```

---

## Substituição nacional de sistemas judiciais

A evolução funcional é guiada por uma matriz de substituição nacional que compara capacidades diante de PJe, e-SAJ, eProc, Creta e Projudi. A matriz previne duplicação de bounded contexts e direciona entregas para os pacotes corretos.

```text
docs/product/NATIONAL_JUDICIAL_SYSTEM_REPLACEMENT_MATRIX.md
docs/product/NATIONAL_JUDICIAL_SYSTEM_REPLACEMENT_INDEX.json
```

---

## Inovação judicial responsável

A inovação é aplicada como suporte auditável — não como automação decisória opaca. As capacidades existentes cobrem:

- Triagem prévia de documentos e petições por IA
- Audiência digital governada com salvaguardas
- Acordos inteligentes com revisão humana obrigatória
- Precedentes vivos e score de acesso à Justiça
- Núcleo de Justiça 4.0 para Juizados Especiais Adjuntos (TJCE / Morada Nova)
- Observabilidade processual e painéis institucionais
- Continuidade offline controlada para comarcas de baixa conectividade

---

## Desenvolvimento local

Compilação:

```bash
./mvnw -pl pjb-api -DskipTests compile
```

Testes dirigidos:

```bash
./mvnw -pl pjb-api -Dtest=NomeDoTeste test -DtrimStackTrace=false
```

---

## Sincronização Git segura

```powershell
.\scripts\git-sync-safe.ps1 "descrição objetiva da mudança"
```

A barreira local bloqueia commits com chaves de API, senhas, tokens, certificados e arquivos `.env`. Detalhes em `docs/security/GIT_SAFE_SYNC.md`.

---

## Padrões de contribuição

- Código sem comentários redundantes — nomes expressivos substituem documentação
- Constructor injection em todas as classes de produção — zero `@Autowired` em fields
- Sem Lombok em camadas críticas — imutabilidade via records Java
- Sem classe com nome genérico (`Manager`, `Helper`, `Util`, `Processor`)
- Sem rotas institucionais fora do registry canônico
- Sem regressão em sigilo, auditoria, RLS, ABAC ou propagação de contexto

## Critério de aceite

Uma alteração só é aceita quando melhora o comportamento verificável sem reduzir maturidade arquitetural. O padrão mínimo é: compilar, preservar contratos públicos, manter os guards verdes e não aumentar falhas na suíte.
