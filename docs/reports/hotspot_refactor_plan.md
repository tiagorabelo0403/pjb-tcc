# Hotspot Refactor Plan

**Data de geração:** 2026-05-15  
**Fonte:** `scripts/architecture_hygiene_guard.py` (7718 arquivos Java analisados)  
**Critério:** services/engines acima de 900 linhas (31 detectados); classes acima de 1000 linhas (19 detectados)

---

## Princípio geral

Classes longas em domain services concentram múltiplas responsabilidades, dificultam testes unitários e violam o SRP. O padrão de extração para este projeto é:

- **Assemblers/Projections**: montagem de DTOs e snapshots fora do service.
- **Sub-services delegados**: lógica de decisão separada da lógica de orquestração.
- **Read/Write split**: queries somente-leitura em query-services separados.
- **Strategy pattern**: variantes comportamentais (por tipo processual, por rito, por modalidade) extraídas como implementações de interface.

---

## Top 10 hotspots — plano por classe

### 1. `CitacaoIntimacaoEngine.java` — 1186 linhas
**Pacote:** `core.comunicacao.judicial`  
**Problema:** Engine central de citação/intimação acumula routing, validação de endereço, construção de ato e controle de prazo.

| Extração sugerida | Responsabilidade |
|---|---|
| `CitacaoEnderecoResolverService` | Resolução e validação de endereço (físico, eletrônico, edital) |
| `IntimacaoAtoAssembler` | Montagem do ato de intimação (subject + corpo + metadados) |
| `PrazoCitacaoCalculatorService` | Cálculo de prazo após citação por modalidade |

**Impacto:** reduz engine para ~400 linhas; cada sub-service é testável isoladamente.

---

### 2. `NationalCommunicationFlowFacade.java` — 1170 linhas
**Pacote:** `service.processual.comunicacao.flow`  
**Problema:** Fachada nacional de comunicação orquestra múltiplos canais (MNI, e-mail, SMS, app) com lógica de fallback inline.

| Extração sugerida | Responsabilidade |
|---|---|
| `CommunicationChannelRouter` | Seleção de canal por preferência e disponibilidade |
| `CommunicationFallbackChain` | Chain of responsibility para fallback entre canais |
| `NationalCommunicationAuditService` | Registro e rastreio de tentativas de comunicação |

---

### 3. `NotificacaoInteligentePJB.java` — 1161 linhas
**Pacote:** `platform.jusos.v2.notificacao`  
**Problema:** Notificação inteligente mistura lógica de personalização, agendamento e push em uma classe.

| Extração sugerida | Responsabilidade |
|---|---|
| `NotificacaoPersonalizacaoStrategy` | Interface + implementações por perfil de usuário |
| `NotificacaoAgendadorService` | Lógica de timing e janela de envio |
| `PushNotificacaoDispatchService` | Dispatch por canal (FCM, APNs, web-push) |

---

### 4. `ProfessionalInstitutionalAccessGrantAdminService.java` — 1160 linhas
**Pacote:** `service.professional`  
**Problema:** Service de concessão de acesso profissional acumula validação de habilitação OAB/MP, grant de perfil e auditoria.

| Extração sugerida | Responsabilidade |
|---|---|
| `HabilitacaoProfissionalValidator` | Validação de OAB, MP, DPU por tipo de profissional |
| `PerfilAccessGrantService` | Concessão e revogação de perfis ABAC |
| `AccessGrantAuditService` | Trilha de auditoria de acessos concedidos |

---

### 5. `LaianePeticaoAssistService.java` — 1145 linhas
**Pacote:** `modules.laiane.service`  
**Problema:** Assistente de peticionamento IA acumula parse de intenção, geração de minuta, validação processual e submissão.

| Extração sugerida | Responsabilidade |
|---|---|
| `LaianePeticaoIntencaoParser` | Extração de intenção e contexto processual do input |
| `LaianeMindutaGeneratorService` | Geração de minuta por template + LLM |
| `LaianePeticaoValidacaoService` | Validação processual antes da submissão |

---

### 6. `RecursalIaPlannerService.java` — 1119 linhas
**Pacote:** `service.processual.recursal.ia`  
**Problema:** Planner de IA recursal mistura análise de viabilidade, seleção de tese e geração de roteiro.

| Extração sugerida | Responsabilidade |
|---|---|
| `RecursalViabilidadeAnalyzer` | Score de viabilidade por tipo recursal |
| `RecursalTeseSelectionStrategy` | Seleção de tese por jurisprudência e padrões |
| `RecursalRoteiroAssembler` | Montagem do roteiro final (DTO) |

---

### 7. `SecretariaOficialCumprimentoRoutingService.java` — 1107 linhas
**Pacote:** `service.secretariat.oficial`  
**Problema:** Routing de cumprimento de mandados acumula distribuição geográfica, priorização e rastreio de status.

| Extração sugerida | Responsabilidade |
|---|---|
| `CumprimentoGeoRouter` | Routing por comarca e competência territorial |
| `CumprimentoPrioridadeRanker` | Priorização por urgência e tipo de mandado |
| `CumprimentoStatusTracker` | Rastreio de status e notificação de devolução |

---

### 8. `CooperacaoJuridicaEngine.java` — 1106 linhas
**Pacote:** `platform.jusos.v2.cooperacao`  
**Problema:** Engine de cooperação jurídica (cartas precatórias, rogadas) mistura emissão, rastreio e cumprimento.

| Extração sugerida | Responsabilidade |
|---|---|
| `CartaPrecatoriaEmissaoService` | Emissão e numeração de cartas precatórias |
| `CooperacaoRastreioService` | Rastreio de tramitação e prazos |
| `CooperacaoRogadaGateway` | Integração com tribunais estrangeiros (rogadas) |

---

### 9. `PeticionamentoEditorBlueprintCatalogService.java` — 1098 linhas
**Pacote:** `service.processual.peticionamento`  
**Problema:** Catálogo de blueprints de editor acumula CRUD de templates, validação de estrutura e versionamento.

| Extração sugerida | Responsabilidade |
|---|---|
| `BlueprintTemplateValidator` | Validação de estrutura e campos obrigatórios |
| `BlueprintVersioningService` | Controle de versão e histórico de blueprints |
| `BlueprintCatalogQueryService` | Queries somente-leitura (read split) |

---

### 10. `IdentidadeJuridicaGraphApplicationService.java` — 1084 linhas
**Pacote:** `core.identidade.grafo.application`  
**Problema:** Application service do grafo de identidade jurídica acumula construção do grafo, resolução de vínculos e projeções.

| Extração sugerida | Responsabilidade |
|---|---|
| `IdentidadeGrafoBuilder` | Construção e atualização do grafo de vínculos |
| `VinculoJuridicoResolverService` | Resolução de vínculos (representação, curatela, sucessão) |
| `IdentidadeGrafoProjectionService` | Projeções read-only por ponto de vista (advogado, juiz, cidadão) |

---

## Sequência de refatoração recomendada

1. **Preparar**: adicionar testes unitários cobrindo os fluxos principais do service antes de extrair.
2. **Extrair por responsabilidade**: uma classe de cada vez, sem alterar comportamento externo.
3. **Mover testes**: cada sub-service deve ter seu próprio arquivo de teste.
4. **Validar com guards**: rodar `architecture_hygiene_guard.py` após cada extração para confirmar redução de linhas.
5. **PR por hotspot**: um PR por classe refatorada, para isolar risco.

## Guard de regressão

Após cada refatoração rodar:
```
python scripts/architecture_hygiene_guard.py
python scripts/constructor_injection_guard.py
./mvnw test -pl pjb-api
```

## Critério de conclusão

Nenhuma classe de service/engine acima de 600 linhas. Threshold atual do guard: 900 linhas (19 violações ativas).
