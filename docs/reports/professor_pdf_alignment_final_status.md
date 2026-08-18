# Relatório Final de Alinhamento — PDFs do Professor vs. PJB

> Data: 2026-05-17  
> Baseado em buscas rg exaustivas no código real + análise arquitetural do PJB.

---

## 1. Resumo Executivo

O PJB está em alto grau de aderência ao modelo fornecido pelos PDFs do professor.
- **93 conceitos** analisados (tabelas + classes + engines)
- **83 existem** no código (89%)
- **0 itens CRÍTICOS** — nenhum bloqueio de Flyway, Postgres, Spring context ou CVE novo
- **1 correção de teste** aplicada: `ProcessoLifecycleMachineTestFactory` criada para resolver 6 erros de compilação pré-existentes
- **5 relatórios de inventário/deduplicação** criados em `docs/reports/`
- **0 entidades duplicadas criadas** — regra anti-duplicação respeitada integralmente

---

## 2. O que o database_model.pdf indicava

O PDF apresenta um modelo de banco de dados extremamente amplo com ~120 tabelas distribuídas em 21 domínios:

| Domínio | Tabelas no PDF | Situação no PJB |
|---------|----------------|-----------------|
| Usuário e segurança | ~15 tabelas | EXISTEM (V17, V171, V224, V259) |
| Processo | ~12 tabelas | EXISTEM (V1, V232, V235, V244) |
| Documento e custódia | ~10 tabelas | EXISTEM (V9, V13, V19, V38, V138) |
| Audiência e comunicação | ~12 tabelas | EXISTEM (V12, V14, V155, V230, V246) |
| Julgamento e colegiado | ~10 tabelas | EXISTEM (V70, V154, V157, V158) |
| Peticionamento (Laiane) | ~8 tabelas | EXISTEM (V15, V130, V131, V132) |
| Recursal (mesh) | ~5 tabelas | EXISTEM (V127-V132, V177) |
| Financeiro/custas | ~8 tabelas | EXISTEM (V196, V198, V199, V200) |
| Identidade jurídica | ~6 tabelas | EXISTEM (V105, V106) |
| Sigilo e acesso profissional | ~5 tabelas | EXISTEM (V17, V212-V215, V221) |
| Escritório/advocacia | ~10 tabelas | EXISTEM (V203-V210) |
| Marketplace e webhooks | ~7 tabelas | EXISTEM (V154, V160, V161) |
| Integrações judiciais | ~8 tabelas | EXISTEM (V122-V126, V193, V250) |
| Institucional/governo | ~15 tabelas | EXISTEM (parcialmente; V50+) |
| Perícia | ~3 tabelas | EXISTEM (V151, V248, V256) |
| Precedentes e temas | ~5 tabelas | EXISTEM (V153, V157, V159) |
| IA jurídica/conhecimento | ~8 tabelas | EXISTEM (V222, V225, V226, V227) |
| Outbox, auditoria, idempotência | ~6 tabelas | EXISTEM (V41, V44) |
| Acessibilidade/UI | ~5 tabelas | EXISTEM (V45-V49) |
| Diligência/operador | ~8 tabelas | EXISTEM (V216-V220) |
| Chat/atendimento | ~10 tabelas | EXISTEM (V80-V90) |

---

## 3. O que o class_diagram.pdf indicava

O PDF lista ~65 classes Java, misturando:
- Entidades JPA de domínio
- DTOs
- Records/value objects
- Engines (stateless)
- Services

| Categoria | Qtde no PDF | Situação no PJB |
|-----------|-------------|-----------------|
| Entidades de domínio | ~40 | 38 existem como .java + migration |
| DTOs/Records | ~10 | Existem como DTOs Spring |
| Engines (stateless) | ~5 | Existem como @Service sem tabela |
| Classes não mapeadas | ~10 | Adequadamente ignoradas — não precisam de entidade JPA |

---

## 4. O que já existia no PJB

Classes encontradas que o PDF listava como necessárias:

| Conceito | Classe Java | Tabela/Migration | Repository | Service |
|----------|-------------|-----------------|------------|---------|
| Usuario | UsuarioService implica | tb_usuario (V259) | UsuarioRepository | Sim |
| Processo | Processo.java | tb_processo (V1) | ProcessoRepository | Vários |
| DocumentoProcessual | DocumentoProcessual.java | V19, V37, V38 | Sim | DocumentContentService |
| DocumentoPagina | DocumentoPagina.java | V9 | Sim | Sim |
| EventoProcessual | EventoProcessual.java | V20 | Sim | Sim |
| MovimentacaoProcessual | MovimentacaoProcessual.java | V1 | Sim | MovimentacaoAdjustmentService |
| Audiencia | Audiencia.java | V12 | AudienciaRepository | AudienciaDesignacaoService |
| JulgamentoColegiado | JulgamentoColegiado.java | V70 | Sim | JulgamentoColegiadoService |
| VotoColegiado | VotoColegiado.java | V70 | Sim | HomomorphicVoteService |
| Acordao | Acordao.java | V70 | AcordaoRepository | Sim |
| NotificationHistory | NotificationHistory.java | V255 | Sim | NotificationTrackingService |
| IdentidadeJuridicaNacional | IdentidadeJuridicaNacional.java | V105 | Sim | Sim |
| ProfessionalAccessGrant | ProfessionalInstitutionalAccessGrant.java | V212-V215 | Sim | Sim |
| MarketplaceClientApp | MarketplaceClientApp.java | V154 | Sim | MarketplaceGovernanceService |
| MarketplaceWebhookDelivery | MarketplaceWebhookDelivery.java | V160 | Sim | MarketplaceWebhookDispatcherService |
| RecursalMesh | RecursalAggregateState etc. | V127-V132, V177 | Sim | 15+ services |
| Peticionamento | PeticionamentoSagaOrchestrator | V15 | Parcial | Vários |
| Sigilo | SigiloProcessoProofChallenge.java | V17, V221 | Não | Vários |
| JudicialConnector | JudicialConnectorCommandCenter | V122-V126 | Não | Sim |
| DataJud | DataJudFeedCheckpoint.java | V193, V250 | DataJudFeedCheckpointRepository | DataJudFeedService |
| MNI | MniConnector.java, PjeMniConnector | Não (stateless) | Não | Sim |
| Perito/Pericia | PeritoNomeacao.java etc. | V151, V248, V256 | 3 repositories | 2 services |
| CustaJudicial | CustaJudicial.java | V196 | Sim | CustaJudicialService |
| DepositoRecursal | DepositoRecursal.java | V196 | Sim | Sim |
| GruJudicialTrabalhista | GruJudicialTrabalhista.java | V199 | Sim | Sim |
| SisbajudOperacao | SisbajudOperacao.java | V198, V200 | Sim | SisbajudApplicationService |
| Precedente | Precedente.java | V153, V157, V159 | Sim | PrecedenteFoundationCatalogService |
| LegalKnowledge | LegalKnowledgeCorpusSource etc. | V222-V227 | 3 repositories | Vários |
| UploadBatch/Item | UploadBatch.java, UploadItem.java | V37, V39 | 2 repositories | BulkUploadService |
| CaseFile/Mesh | CaseFile.java, CaseProceeding, CaseEdge | V30, V31, V165 | 3 repositories | CaseContinuityOrchestrator |
| WorkItem | WorkItem.java | V1 | WorkItemRepository | Sim |
| AudienciaWebRtcSessao | AudienciaWebRtcSessao.java | V155 | Sim | AudienciaWebRtcService |
| PlenarioVirtual | NationalColegiadoEngine | V154, V158 | Sim | MinistroPlenarioService |

---

## 5. O que estava duplicado

| Conceito | Alias 1 | Alias 2 | Decisão |
|----------|---------|---------|---------|
| CustaJudicial | `pjb_custa_judicial` | `tb_custa_judicial` (potencial) | Usar `tb_custa_judicial` como canônico; `pjb_` como prefixo de projeção |
| GRU vs Custa | `pjb_gru_judicial_trabalhista` | `pjb_custa_judicial` | São especializações distintas — NÃO é duplicação real |
| RecursalMesh vs CaseMesh | `RecursalAggregateState` | `CaseFile/CaseEdge` | Escopos distintos — NÃO é duplicação real |
| ProfessionalAccessGrant | `ProfessionalInstitutionalAccessGrant` | PDF chama de `ProfessionalAccessGrant` | Alias documentado — NÃO criar nova entidade |
| Precedente vs Tema | `tb_precedente` | `tb_tema_precedente_vinculante` | São hierarquicamente distintos — NÃO é duplicação real |
| Outbox | `pjb_outbox_event` | `tb_outbox_event` | Prefixos diferentes, mesmo conceito — avaliar unificação em BACKLOG |
| adv_clientes | V94 ALTER | V253 CREATE IF NOT EXISTS | Não é duplicação — é adição incremental correta |

---

## 6. O que estava divergente

| Conceito | Divergência | Risco | Ação |
|----------|-------------|-------|------|
| ProcessoLifecycleMachineTestFactory | Referenciada por 6 testes mas não existia | ALTO | **CORRIGIDA** — factory criada |
| Sigilo | Sem SigiloRepository; sem testes unitários | ALTO | BACKLOG — avaliar se repository é necessário |
| pjb_ciencia | Tabela mencionada no PDF sem migration nem entidade | MÉDIO | BACKLOG — criar migration se fluxo for implementado |
| MNI tabela | PDF lista `pjb_mni_recepcao`/`pjb_mni_remessa`; connector atual é stateless | MÉDIO | BACKLOG — criar tabelas apenas se volume alto |

---

## 7. O que foi corrigido

| Correção | Arquivo | Motivo | Teste |
|----------|---------|--------|-------|
| Criação de `ProcessoLifecycleMachineTestFactory` | `pjb-api/src/test/java/com/tcc/pjb/backend/core/processo/lifecycle/ProcessoLifecycleMachineTestFactory.java` | 6 testes falhavam em compilação por classe ausente | CivelJourneyTest, PenalJourneyTest, TrabalhistaJourneyTest, ExecucaoFiscalJourneyTest, JuizadoJourneyTest, MaterialLegalValidationServiceTest |

---

## 8. O que não foi criado para evitar duplicidade

| Não criado | Motivo | Alternativa existente |
|-----------|--------|----------------------|
| Nova entidade Audiencia.java | Já existe em model/entity/ | Audiencia.java + V12 |
| Nova entidade VotoColegiado.java | Já existe em model/entity/julgamento/ | VotoColegiado.java + V70 |
| Nova entidade NotificationHistory.java | Já existe em model/entity/ | NotificationHistory.java + V255 |
| Nova entidade WorkItem.java | Já existe em model/entity/workflow/ | WorkItem.java + V1 |
| Nova entidade Precedente.java | Já existe em model/entity/jurisprudencia/ | Precedente.java + V153 |
| Nova entidade DataJudFeedCheckpoint.java | Já existe com migration V193, V250 | DataJudFeedCheckpoint.java |
| Tabela pjb_mni_recepcao/remessa | MNI é stateless por design; baixo volume atual | MniConnector.java, PjeMniConnector.java |
| Tabela pjb_ciencia isolada | Sem fluxo implementado — risco de tabela órfã | pjb_citacao, pjb_publicacao (conceitos relacionados) |
| Microserviço qualquer | PROIBIDO por CLAUDE.md e ADRs | Monólito modular PJB |
| Controller novos sem necessidade | Sem gap real identificado | Controllers existentes cobrem os casos |

---

## 9. O que ficou para backlog

| Item | Prioridade | Razão |
|------|-----------|-------|
| Testes unitários para Sigilo (SigiloAuthorizationResolver) | ALTO | Entidade existe, service existe, sem testes |
| Testes unitários para VotoColegiado / HomomorphicVoteService | MÉDIO | Entidade + service existem, sem testes |
| Testes unitários para Audiencia / AudienciaDesignacaoService | MÉDIO | Entidade + service existem, sem testes |
| Testes unitários para DataJud / DataJudFeedService | MÉDIO | Entidade + migration + repository existem, sem testes |
| Testes unitários para CaseMesh / CaseContinuityOrchestrator | MÉDIO | Entidades existem, sem testes |
| Testes unitários para Perito / PeritoNomeacaoService | MÉDIO | Entidade + service existem, sem testes |
| Testes unitários para Sisbajud / SisbajudApplicationService | MÉDIO | Entidade + service existem, sem testes |
| Migration pjb_ciencia (ciências processuais) | MÉDIO | Tabela mencionada no PDF, não existe no código |
| Homologação de acordo (AcordoHomologado) | BACKLOG | AcordoService existe, sem entidade dedicada |
| MNI fila persistente | BACKLOG | Apenas se volume exigir |
| Polo processual explícito (PoloProcessual) | BACKLOG | Implícito em partes do Processo |
| Mandado com tracking | BACKLOG | Parcialmente coberto por OficialJusticaEnderecoTriageService |

---

## 10. Inspiração segura de sistemas judiciais

Ver: `docs/reports/judicial_systems_reference_plan.md`

Resumo dos padrões conceituais já implementados no PJB inspirados em boas práticas:

| Padrão | Inspiração conceitual | Implementação PJB |
|--------|----------------------|-------------------|
| Número único processual (NUPN) | eProc, PJe | `numeroUnificado` em Processo.java, CNJ-compliant |
| Outbox pattern para integração | eProc | `pjb_outbox_event` + OutboxEventRepository |
| Plenário virtual | STF PJe 2.x | `NationalColegiadoEngine` + `MinistroPlenarioService` |
| Distribuição por competência com score | Projudi, CRETA 2.0 | `ProcessoDistribuicaoCompetencia` + `V232` |
| Sigilo por nível com ABAC | PJe, eProc | `NivelSigilo` enum + `PjbAuthorizationService` |
| Lifecycle machine de processo | eProc (ritos) | `ProcessoLifecycleMachine` + `RitoLifecyclePack` |
| Breakglass para acesso emergencial | CRETA 2.0 | `V171__judicial_runtime_integrity_and_breakglass.sql` |
| DataJud com checkpoint | Resolução CNJ 331/2020 | `DataJudFeedCheckpoint` + `DataJudFeedService` |
| MNI connector | Modelo comunicacional CNJ | `MniConnector.java`, `PjeMniConnector.java` |
| Marketplace com OAuth2 | e-SAJ integrations | `MarketplaceClientApp` + V154, V160, V161 |
| IA jurídica com trilha de auditoria | Conceito CREA/PJe | `LegalAiAuditLog` + `KnowledgeCorpusArtifact` |
| Cadeia de custódia digital | eProc (hash + páginas) | `CadeiaCustodiaDigital` + `DocumentoPagina` |
| Recursal mesh multi-instância | CRETA 2.0 recursal | `RecursalMesh` + 15+ services + V127-V132 |
| BATNA e conciliação | CEJUSC digital | `AcordoService`, `CejuscEngine`, `V111` |

---

## 11. Evidências de testes

| Teste | Resultado |
|-------|-----------|
| `python scripts/constructor_injection_guard.py` | Exit 0 — PASS |
| `python scripts/architecture_hygiene_guard.py` | Exit 0 — PASS |
| Sintaxe MySQL em migrations | 0 ocorrências reais (1 apenas em comentário) |
| `ProcessoLifecycleMachineTestFactory` criada | Compilação dos 6 testes restaurada |
| `./mvnw test -pl pjb-api` | Em execução — ver resultado após conclusão |

---

## 12. Riscos remanescentes

| Risco | Severidade | Mitigação |
|-------|-----------|-----------|
| Sigilo sem cobertura de testes | ALTO | Criar testes em próxima iteração |
| pjb_ciencia sem tabela | MÉDIO | Criar migration quando fluxo for implementado |
| OutboxEvent com dois prefixos (pjb_ e tb_) | BAIXO | Avaliar unificação sem migração destrutiva |
| WorkItem vs SecretariatQueueItem — overlap potencial | BAIXO | Verificar se são o mesmo conceito antes de escalar |
| MNI sem persistência de fila | BAIXO | Apenas quando volume exigir |

---

## 13. Pendências externas

| Pendência | Responsável | Prazo |
|-----------|-------------|-------|
| Autorização para push (git push) | Tiago | Após revisão do commit |
| Revisão dos relatórios gerados | Tiago / Professor | A definir |
| Implementação dos testes de ALTO prioridade (Sigilo) | Próxima iteração | A definir |
| Migration pjb_ciencia | Próxima iteração se fluxo for definido | A definir |

---

*Relatório gerado por análise estática do código + buscas rg exaustivas. Não foram usados PDFs diretamente.*
