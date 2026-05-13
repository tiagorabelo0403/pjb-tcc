# AKASHIC-PJB OMEGA X

AKASHIC-PJB OMEGA X é uma plataforma judicial soberana em Java 21 e Spring Boot 3, desenhada para consolidar comunicação processual, tramitação eletrônica, secretaria judicial, gabinete, malha recursal, integrações oficiais, auditoria, segurança institucional e experiência operacional em uma arquitetura única, verificável e modular.

O produto é orientado por um princípio simples: todo fluxo crítico do processo judicial deve ser rastreável, testável, resiliente, observável e seguro por padrão.

## Objetivo institucional

O sistema foi estruturado para servir como núcleo tecnológico de uma operação judicial nacional de alta escala. A arquitetura evita os vícios tradicionais de plataformas judiciais legadas: rotas dispersas, acoplamento forte entre telas e domínio, monólitos sem fronteira, jobs isolados, assinaturas frágeis, baixa observabilidade e inconsistência entre secretaria, gabinete, partes, órgãos externos e instâncias recursais.

## Stack principal

- Java 21
- Spring Boot 3.5
- Maven multi-module
- PostgreSQL e H2 para cenários de teste
- Flyway
- JPA/Hibernate
- Spring Security
- Resilience4j
- Pact
- JaCoCo
- Checkstyle
- Python guards para governança estrutural

## Organização

```text
.
├── pjb-api       # aplicação, superfícies HTTP, domínio operacional, serviços e testes
├── pjb-core      # núcleo modular extraído e contratos de modularidade
├── docs          # ADRs, arquitetura, segurança, infraestrutura, OpenAPI e relatórios técnicos
├── scripts       # automação de validação, coleta de falhas e guardas arquiteturais
├── config        # Checkstyle, SpotBugs e políticas de qualidade
├── infra         # Kubernetes, banco, gateway e infraestrutura operacional
└── tooling       # ferramentas auxiliares do repositório
```

## Arquitetura

A base segue monólito modular com fronteiras explícitas e extração progressiva para módulos de núcleo. A aplicação privilegia estabilidade transacional, baixo acoplamento, contratos claros e evolução guiada por evidência.

### Eixos centrais

- `core.comunicacao.institucional`: comunicação institucional, entrada segura, rotas canônicas, painéis, onboarding, governança, certificação e fonte oficial.
- `core.processo`: ciclo de vida processual, sigilo, execução, painel, migração, prazo e integração.
- `core.procedural`: classificação, competência, rito, forum allocation, placement review, mensagens centralizadas e roteamento nacional.
- `core.kernel.recursal`: malha recursal, state machine, catálogo recursal, subida multigrau e controles de instância.
- `service.secretariat`: secretaria judicial, filas, agenda, retorno ao processo, cobertura operacional e painéis de gestão.
- `integration.judicial`: MNI, DataJud, conectores judiciais, replay, política, segurança e telemetria.
- `core.security`: ABAC, Gov.br, autorização, trilhas de decisão, perfil profissional e propagação segura de contexto.
- `core.lgpd`: classificação de dados, RLS, envelopes de sigilo e pontos de entrada não HTTP.

## Rotas e superfície pública

Rotas institucionais devem nascer de registries canônicos. Controllers não devem expor literais divergentes nem aliases legados. A superfície HTTP é uma camada fina; regras de negócio ficam em serviços de aplicação e domínio.

Contratos críticos são protegidos por testes de arquitetura, Pact provider verification e validações de superfície. A disciplina de API existe para impedir proliferação de endpoints satélite e regressões em integrações judiciais.


## Substituição nacional de sistemas judiciais

A evolução funcional do PJB é guiada por uma matriz de substituição nacional que compara capacidades existentes e lacunas reais diante de PJe, PJe 2.x, e-SAJ, eproc, Creta e Projudi. Essa matriz impede duplicação de bounded contexts e direciona novas entregas para os pacotes corretos.

Artefatos de produto:

```text
docs/product/NATIONAL_JUDICIAL_SYSTEM_REPLACEMENT_MATRIX.md
docs/product/NATIONAL_JUDICIAL_SYSTEM_REPLACEMENT_INDEX.json
```

O catálogo executável de capacidades fica no bounded context de substituição nacional:

```text
pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/substituicao/domain/PjbSubstituicaoNacionalCapabilityCatalog.java
```

Novas funcionalidades de substituição devem primeiro consultar esse catálogo. Se a capacidade já existir parcialmente, a implementação deve evoluir o eixo existente em vez de criar pacote paralelo.


## Readiness nacional e evolução de produto

A substituição nacional não é tratada como coleção de telas. O PJB consolida readiness por tribunal, matriz de interoperabilidade, chave de acesso processual, portal público, migração de acervo e impacto de indisponibilidade como capacidades governadas por catálogo.

Artefatos de produto:

```text
docs/product/NATIONAL_JUDICIAL_SYSTEM_REPLACEMENT_MATRIX.md
docs/product/NATIONAL_JUDICIAL_SYSTEM_REPLACEMENT_INDEX.json
docs/product/TRIBUNAL_PRODUCTION_READINESS.md
docs/product/PUBLIC_PORTAL_CAPABILITY_SPEC.md
docs/product/MIGRATION_AND_INTEROPERABILITY_STRATEGY.md
```

Eixos executáveis adicionados sem duplicar bounded contexts:

```text
pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/substituicao/readiness
pjb-api/src/main/java/com/tcc/pjb/backend/core/security/accesskey
pjb-api/src/main/java/com/tcc/pjb/backend/core/observability/unavailability
pjb-api/src/main/java/com/tcc/pjb/backend/integration/mni/compatibility
pjb-api/src/main/java/com/tcc/pjb/backend/core/frontend/publicaccess
```

Esses eixos complementam substituição nacional, segurança, observabilidade, MNI e frontend público já existentes. Não há criação de novo módulo concorrente de processo, secretaria, integração judicial, recursal ou substituição nacional.



## Inovação judicial segura

A inovação do PJB é aplicada como camada de suporte auditável, não como automação decisória opaca. Novas capacidades devem reduzir nulidade, fila, indisponibilidade, ruído de atendimento e risco de migração, preservando revisão humana nos pontos sensíveis.

Artefato de produto:

```text
docs/product/JUDICIAL_INNOVATION_BLUEPRINT.md
```

Eixos executáveis de inovação:

```text
pjb-api/src/main/java/com/tcc/pjb/backend/core/governance/changeimpact
pjb-api/src/main/java/com/tcc/pjb/backend/core/processo/health
pjb-api/src/main/java/com/tcc/pjb/backend/core/peticionamento/blackbox
pjb-api/src/main/java/com/tcc/pjb/backend/core/frontend/publicaccess
pjb-api/src/main/java/com/tcc/pjb/backend/core/observability/unavailability
pjb-api/src/main/java/com/tcc/pjb/backend/core/distribuicao/explainable
pjb-api/src/main/java/com/tcc/pjb/backend/service/secretariat/autopilot
pjb-api/src/main/java/com/tcc/pjb/backend/core/processo/migracao/intelligence
pjb-api/src/main/java/com/tcc/pjb/backend/core/kernel/twin
pjb-api/src/main/java/com/tcc/pjb/backend/core/frontend/virtualcounter
```

Esses eixos reaproveitam governança, processo, peticionamento, frontend público, observabilidade, distribuição, secretaria, migração e twin já existentes. A finalidade é antecipar risco, explicar o processo, preservar prova técnica, melhorar atendimento e simular impacto antes de mudanças críticas.

## Segurança

O modelo de segurança é orientado por identidade, papel, lotação, órgão, unidade, instância, sigilo e trilha auditável.

Controles aplicados:

- ABAC para decisões sensíveis.
- Step-up Gov.br e certificado qualificado onde aplicável.
- RLS para leitura processual sigilosa.
- Propagação governada de contexto em execuções assíncronas.
- Trilhas imutáveis de decisão e autorização.
- Separação entre dado público, institucional, sigiloso e crítico.

## Concorrência e execução

A base evita `CompletableFuture` cru, schedulers isolados e criação direta de executores fora da espinha oficial. Execuções assíncronas devem passar por descritores e lanes governadas. Virtual Threads são usadas de forma centralizada, com preservação de contexto e sem retenção indevida de dados sensíveis.

## Transações

Transações devem ser curtas, explícitas e compatíveis com o custo real do fluxo. I/O externo, assinatura, chamada de conector, replay e fan-out operacional não devem ficar presos a transações longas. Hotpaths são acompanhados por budgets transacionais e guardas de qualidade.

## Documentos, assinatura e evidência

O eixo documental trabalha com evidência criptográfica, assinatura controlada, validação ICP-Brasil, trilha de prova e suporte a perfis de preservação de longo prazo. Artefatos recursais e protocoláveis carregam metadados verificáveis, hash, envelope de prova, carimbo temporal e validação de readiness antes de submissão ao conector judicial.

## Observabilidade

A operação deve produzir sinais úteis para engenharia, secretaria e governança:

- SLOs por superfície crítica.
- Correlação de requisição.
- Telemetria de conectores judiciais.
- Diagnóstico de pressão runtime.
- Painéis institucionais com readiness, fila, agenda e cobertura.
- Auditoria materializada para decisões e eventos sensíveis.


## Inovação judicial assistida II

A segunda camada de inovação amplia o PJB com audiência digital governada, núcleo de Justiça Digital, continuidade offline controlada, atermação assistida, precedentes vivos, acordos inteligentes com salvaguardas, score de acesso à Justiça, observabilidade processual, marketplace judicial governado e kit de paridade com sistemas legados.

Essas capacidades foram adicionadas em pacotes já existentes ou subpacotes naturais, sem criar motor processual paralelo, secretaria paralela, marketplace paralelo ou camada duplicada de substituição nacional. A especificação está em `docs/product/JUDICIAL_INNOVATION_PART_TWO.md`.


## Inovação judicial assistida III

A terceira camada de inovação incorpora o funcionamento de Núcleos de Justiça 4.0 para Juizados Especiais Adjuntos, com base operacional no cenário TJCE e na comarca de Morada Nova. O PJB passa a tratar a opção facultativa pelo Núcleo 4.0 como decisão de protocolo governada, vinculada ao cadastro da ação no PJe, sem aceitar menção isolada na petição inicial como escolha válida e sem redistribuição automática quando a parte autora não opta.

A implementação foi conectada ao eixo existente de Justiça Digital:

```text
pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/sustentacao/digitaljustice
```

O detalhamento está em `docs/product/JUDICIAL_INNOVATION_PART_THREE.md`.

## Qualidade executável

A arquitetura é protegida por testes unitários, testes de integração, testes de contrato, guards Python e testes estruturais de código-fonte.

Guards principais:

```bash
python scripts/architecture_hygiene_guard.py
python scripts/constructor_injection_guard.py
python scripts/runtime_concurrency_guard.py
python scripts/transactional_hotspot_guard.py --fail-on-missing-budgets
python scripts/config_taxonomy_guard.py
python scripts/repository_cleanliness_guard.py
python scripts/readme_truthfulness_guard.py
python scripts/java_comment_discipline_guard.py
python scripts/canonical_institutional_route_guard.py
python scripts/replacement_matrix_guard.py
python scripts/tribunal_readiness_guard.py
python scripts/access_key_and_unavailability_guard.py
python scripts/judicial_innovation_guard.py
python scripts/judicial_innovation_part_two_guard.py
python scripts/judicial_innovation_part_three_guard.py
```

No Windows, use o launcher assinado pela própria estrutura do repositório:

```powershell
cd C:\PJB
$env:MAVEN_OPTS="-Xms512m -Xmx4096m -XX:+UseG1GC"
.\scripts\pjb-api-clean-test-errors.cmd
```


## Pré-consciência jurídica soberana

A conversa jurídica agora possui uma moldura pré-consciente interna, executada dentro do `LegalAiConversationOrchestrator`, sem controller novo e sem rota nova. A finalidade é simular uma leitura preliminar humana de alto nível antes da resposta: ramo jurídico, tradição doutrinária, piso de autoridade, risco procedimental, proveniência de evidência, sigilo, trust zone, sinais de alucinação, contradições e candidato de aprendizagem.

A implementação foi decomposta em classes pequenas e auditáveis:

```text
pjb-api/src/main/java/com/tcc/pjb/backend/ai/juridica/conversation/LegalAiJuridicalLineageRegistry.java
pjb-api/src/main/java/com/tcc/pjb/backend/ai/juridica/conversation/LegalAiPreConsciousSignalExtractor.java
pjb-api/src/main/java/com/tcc/pjb/backend/ai/juridica/conversation/LegalAiPreConsciousFrameService.java
pjb-api/src/main/java/com/tcc/pjb/backend/ai/juridica/conversation/LegalAiPreConsciousToolScopeEnricher.java
pjb-api/src/main/java/com/tcc/pjb/backend/model/dto/ai/legal/conversation/LegalAiJuridicalLineageDescriptor.java
pjb-api/src/main/java/com/tcc/pjb/backend/model/dto/ai/legal/conversation/LegalAiPreConsciousSignal.java
pjb-api/src/main/java/com/tcc/pjb/backend/model/dto/ai/legal/conversation/LegalAiPreConsciousFrameSnapshot.java
```

A linhagem jurídica não funciona como enfeite textual. Ela vira metadado operacional: cada ramo mapeia tradição, autores de referência, doutrina brasileira, lentes hermenêuticas e checks obrigatórios. O frame resultante é anexado ao `conversationContext`, ao `juridicaPreConsciousFrame`, aos safeguards e ao tool scope final antes do approval.

Controles de regressão:

```text
pjb-api/src/test/java/com/tcc/pjb/backend/ai/juridica/conversation/LegalAiPreConsciousFrameServiceTest.java
pjb-api/src/test/java/com/tcc/pjb/backend/ai/juridica/conversation/LegalAiPreConsciousToolScopeEnricherTest.java
pjb-api/src/test/java/com/tcc/pjb/backend/ai/juridica/conversation/JuridicaLegalAiPreConsciousArchitectureTest.java
```

Esses testes protegem três pontos: ausência de rota duplicada, limite de 200 linhas nas novas classes da pré-consciência e execução da moldura antes do approval final.

## Desenvolvimento local

Compilação dirigida:

```bash
./mvnw -pl pjb-api -DskipTests compile
```

Testes dirigidos:

```bash
./mvnw -pl pjb-api -Dtest=NomeDoTeste test -DtrimStackTrace=false
```

Coleta de falhas:

```bash
./scripts/pjb-api-clean-test-errors.cmd
```

## Governança de aprendizado da base

A plataforma inclui capacidades de auditoria de código e aprendizado guiado por hotspots. As superfícies de `codebase-learning` e `sanidade-aprendizado` existem para transformar sinais do repositório em priorização de arquitetura.

O processo usa trilhas internas de extração, blueprints de extração, fluxos críticos, `refresh=true` e snapshot em memória para reduzir recomputação, evitar ruído e priorizar pontos com impacto real.

## Padrões de contribuição

- Código sem comentários redundantes.
- Nomes expressivos em classes, métodos e objetos de valor.
- Constructor injection.
- Sem field injection.
- Sem arquivos temporários no repositório.
- Sem markdown solto na raiz fora do `README.md`.
- Sem duplicação de FQN.
- Sem imports internos quebrados.
- Sem rotas institucionais fora do registry canônico.
- Sem regressão em sigilo, auditoria, RLS, ABAC ou propagação de contexto.

## Critério de aceite

Uma alteração só é aceitável quando melhora o comportamento verificável da base sem reduzir maturidade arquitetural. O padrão mínimo é compilar, preservar contratos públicos, manter os guards verdes e não aumentar falhas na suíte.

## Universal Digital Core & Zero-Error Triage

O PJB trata Núcleos Digitais, Juizados e ritos especializados como contexto de distribuição, painel e preflight. A plataforma não cria módulo paralelo de Juizado: ela identifica o rito pelos metadados da inicial, resolve a estratégia de distribuição, aplica `RitoContext`, adapta o painel e executa triagem prévia contra incompatibilidades documentais, territoriais, custas indevidas, urgência e complexidade probatória.

A evolução permanece em Java 21 e Spring Boot. IA, OCR e análise semântica entram por ports/adapters e eventos assíncronos do ecossistema Java, sem dependência obrigatória de outra linguagem.

Validações associadas:

```bash
python scripts/universal_digital_core_guard.py
python scripts/java_comment_discipline_guard.py
python scripts/repository_cleanliness_guard.py
```

