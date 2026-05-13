# ADR-055 — Integração Anthropic Managed Agents: Memory Stores + Dreams

**Status:** Aceito  
**Data:** 2026-05-09  
**Autor:** Arquitetura PJB

---

## Contexto

Agentes jurídicos do PJB operam em sessões isoladas. Cada sessão termina sem preservar aprendizados operacionais: padrões de triagem, preferências de usuário, erros de roteamento e contexto institucional se perdem entre execuções. O resultado é que a IA começa cada atendimento do zero, repetindo erros e ignorando convenções já estabelecidas por cada tribunal, comarca ou usuário.

A Anthropic lançou em preview a API Managed Agents, composta por:
- **Memory Stores**: coleções de documentos de texto montadas em `/mnt/memory/` dentro da sessão do agente, com audit trail imutável via `memory_version`, suporte a acesso `read_write`/`read_only` e compliance de rollback por 30 dias.
- **Dreams**: job assíncrono que recebe um memory store de entrada e até 100 transcrições de sessão, consolida duplicatas, remove contradições e extrai padrões, produzindo um novo store de saída sem modificar o original.

---

## Decisão

Integrar Memory Stores e Dreams da Anthropic ao módulo `pjb-legal-ai`, camada `com.tcc.pjb.backend.ai.legalai`, com as seguintes restrições obrigatórias:

### Restrições de Sigilo (LGPD / sigilo judicial)

A camada `MemoryAccessPolicy` avalia `MemorySigiloNivel` antes de qualquer operação com a API Anthropic:

| Nível | Pode enviar à Anthropic | Access Type |
|-------|------------------------|-------------|
| PUBLIC | Sim | read_write |
| INSTITUCIONAL | Sim | read_write |
| SIGILOSO | Não | read_only local |
| CRITICO | Não | denied |

Processos sigilosos e críticos jamais têm conteúdo enviado à API externa. O sistema continua operando localmente para esses casos.

### Tipos de Memória Governada

Cinco categorias de memória com políticas distintas de promoção:

1. **PREFERENCIA_USUARIO** — promoção automática permitida
2. **INSTITUCIONAL** — requer aprovação do gestor da unidade
3. **PROCESSUAL** — isolada por processo, nunca promovida para memória global
4. **JURIDICA_VALIDADA** — exige fonte, versão e data de proveniência
5. **NEGATIVA** — erros registrados para prevenção de reincidência

### Outbox Pattern

A criação de um Dream grava em `dream_outbox` dentro da mesma transação que persiste o Dream. Um scheduler (`DreamingScheduler`) processa o outbox em Virtual Threads, garantindo:
- Transação curta no caminho crítico
- Sem bloqueio do pool de threads principal
- At-least-once delivery

### Circuit Breaker

Resilience4j protege ambos os clients HTTP:
- `anthropic-memory` — falha graciosamente, lança `AnthropicApiUnavailableException`
- `anthropic-dreaming` — falha graciosamente, fallback retorna `Optional.empty()`

Sistema continua operando sem dreaming quando Anthropic está indisponível.

### Polling com Virtual Threads

O polling de status de Dream usa backoff exponencial: `10s, 20s, 40s... máx 300s`. Executa em Virtual Thread dedicado para não bloquear o pool principal durante jobs que podem durar minutos.

### Feature Flag

`DreamingScheduler` só é instanciado com `pjb.legal-ai.dreaming.enabled=true`. O default é `false` em todos os perfis. Ambientes de produção ativam explicitamente via variável de ambiente.

---

## Headers Obrigatórios da API Anthropic

```
Memory Stores:
  anthropic-beta: managed-agents-2026-04-01
  x-api-key: ${ANTHROPIC_API_KEY}
  anthropic-version: 2023-06-01

Dreams:
  anthropic-beta: managed-agents-2026-04-01, dreaming-2026-04-21
  x-api-key: ${ANTHROPIC_API_KEY}
  anthropic-version: 2023-06-01
```

---

## Alternativas Consideradas

| Alternativa | Razão da Rejeição |
|-------------|-------------------|
| RAG próprio com vector store local | Requer infra adicional (pgvector/elasticsearch), manutenção de embeddings e pipeline de atualização; Anthropic Dreams já resolve isso para sessões. |
| Fine-tuning do modelo | Não adequado para PJB: muda o modelo permanentemente, viola separação entre dados de treinamento e operação, custo proibitivo e sem rollback por processo. |
| Memória in-process (cache Redis) | Sem persistência de longo prazo, sem consolidação semântica, sem audit trail imutável. |
| LangChain Memory abstraction | Acoplamento a framework externo; o PJB já tem sua própria camada de sessão e trust zone. |

---

## Consequências

### Positivas
- IA jurídica acumula aprendizados operacionais entre sessões sem sacrificar sigilo
- Memória reflexiva governada: toda promoção tem proveniência, TTL e possibilidade de revogação
- Sistema degrada graciosamente quando Anthropic está indisponível
- Virtual Threads absorvem o custo de polling assíncrono sem impacto no throughput

### Negativas
- Custo de tokens da Anthropic proporcional ao volume de sessões e complexidade dos Dreams
- Dependência de API em preview: contrato pode mudar; adapters isolados facilitam refactoring
- Necessidade de gestão de chave `ANTHROPIC_API_KEY` com rotação segura
- O scheduler noturno adiciona janela de manutenção a monitorar

### Riscos e Mitigações
- **Risco**: API preview descontinuada. **Mitigação**: toda dependência encapsulada em `AnthropicMemoryStoreClient` e `AnthropicDreamingClient`; trocar por RAG próprio requer apenas substituir o adapter.
- **Risco**: vazamento de dados sigilosos. **Mitigação**: `MemoryAccessPolicy` bloqueia qualquer store com nível SIGILOSO ou CRITICO antes do client HTTP.
- **Risco**: polling infinito. **Mitigação**: `pollingMaxAttempts` configurable (default 30) com backoff exponencial e fallback `marcarFalha`.

---

## Dependências de ADR

- ADR-0041: Autorização ABAC, sigilo e auditoria — define os níveis de sigilo usados por `MemoryAccessPolicy`
- ADR-0051: Governança unificada de execução assíncrona — `DreamingScheduler` usa Virtual Threads conforme prescrito
- ADR-0052: Runtime guardrails unificados — circuit breaker Resilience4j integrado

---

## Estrutura de Pacotes

```
pjb-core:
  com.tcc.pjb.backend.ai.legalai.config          ← Properties
  com.tcc.pjb.backend.ai.legalai.memory.domain   ← Aggregates, ports, policies
  com.tcc.pjb.backend.ai.legalai.memory.application ← Reflection service
  com.tcc.pjb.backend.ai.legalai.dreaming.domain ← Dream aggregate, policy
  com.tcc.pjb.backend.ai.legalai.dreaming.application ← Orchestrator, scheduler
  com.tcc.pjb.backend.ai.legalai.dreaming.infra  ← HTTP adapters, JPA entities

pjb-api:
  com.tcc.pjb.backend.ai.legalai                 ← Controllers, DTOs
```
