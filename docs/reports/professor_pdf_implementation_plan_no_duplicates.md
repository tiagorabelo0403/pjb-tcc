# Plano de Implementação Sem Duplicatas — Conceitos PDF do Professor

> Gerado em: 2026-05-17  
> Baseado exclusivamente nos resultados das buscas rg no código existente.  
> REGRAS: NÃO criar se já existe. NÃO duplicar. NÃO modificar o que está funcionando.

## Legenda

- **Prioridade**: CRÍTICO | ALTO | MÉDIO | BACKLOG | NÃO_CRIAR
- **Ação segura**: operação que pode ser executada sem risco de regressão

---

## CRÍTICO — Nenhum item nesta categoria

*Nenhum conceito dos PDFs está completamente ausente e seja bloqueante para o funcionamento atual.*

---

## ALTO — Implementar em curto prazo

> NOTA: Verificações adicionais confirmaram que Audiencia.java, VotoColegiado.java, WorkItem.java, Precedente.java, NotificationHistory.java **já existem**. DataJud também tem migration (V193, V250) e repository. O foco ALTO real é: testes e Sigilo.

| # | Prioridade | Item | Já existe? | Duplicado? | Ação segura | Arquivos afetados | Teste obrigatório |
|---|-----------|------|-----------|-----------|-------------|-------------------|--------------------|
| 1 | ALTO | Sigilo — cobertura de testes | SIM (classes existem) | NÃO | Criar testes unitários para `PjbAuthorizationSigiloResolver` e `ProcessoSigiloProbatorioApplicationService` | Nenhum arquivo novo — apenas testes | `SigiloAuthorizationResolverTest.java`, `ProcessoSigiloProbatorioTest.java` |
| 2 | ALTO | Assinatura digital HSM — expansão | PARCIAL (V146 HSM migration, OfficeSignatureQueueService existe) | NÃO | Completar serviço de validação de assinatura com integração HSM real | `OfficeSignatureQueueService`, `DocumentTrustChainService` | `HsmSignatureValidationTest.java` |

---

## MÉDIO — Implementar em médio prazo

| # | Prioridade | Item | Já existe? | Duplicado? | Ação segura | Arquivos afetados | Teste obrigatório |
|---|-----------|------|-----------|-----------|-------------|-------------------|--------------------|
| 7 | MÉDIO | VotoColegiado — testes | SIM (VotoColegiado.java existe em model/entity/julgamento/) | NÃO | Criar testes para VotoColegiado + HomomorphicVoteService | Nenhum arquivo novo | `VotoColegiadoTest.java` |
| 8 | MÉDIO | Precedente — testes adicionais | SIM (Precedente.java existe em model/entity/jurisprudencia/) | SIM (tb_precedente vs tema) | Criar testes de Precedente.java + documentar relação com TemaPrecedenteVinculante | Nenhum arquivo novo | `PrecedenteTest.java` |
| 9 | MÉDIO | AudienciaWebRtcSessao — testes | SIM (AudienciaWebRtcSessao.java existe em model/entity/audiencia/) | NÃO | Criar testes para AudienciaWebRtcSessao + AudienciaWebRtcService | Nenhum arquivo novo | `AudienciaWebRtcSessaoTest.java` |
| 10 | MÉDIO | pjb_ciencia — tabela ausente | NÃO | NÃO | Criar migration `V261__ciencia_judicial.sql` com tabela de ciências processuais (comunicação automática) | `V261__ciencia_judicial.sql` | IT de ciência judicial |
| 11 | MÉDIO | Polo processual (ativo/passivo) | NÃO explicitamente | NÃO | Adicionar value object `PoloProcessual` no agregado Processo | `model/entity/processo/PoloProcessual.java` | `PoloProcessualTest.java` |
| 12 | MÉDIO | Mandado eletrônico com tracking | PARCIAL (OficialJusticaEnderecoTriageService) | NÃO | Criar `MandadoTracking` service + migration de tracking de cumprimento | Nova migration + service | `MandadoTrackingTest.java` |
| 13 | MÉDIO | DocumentoPagina — controller explícito | PARCIAL (entity + repo existem, sem controller dedicado) | NÃO | Criar `DocumentoPaginaController` ou expor via `DocumentoController` existente | `DocumentoController.java` (expandir) | `DocumentoPaginaControllerTest.java` |
| 14 | MÉDIO | Certidão — QR Code de autenticação | PARCIAL (V13 tabela certidão) | NÃO | Adicionar campo `qr_code_token` na tabela tb_certidao + service de geração | Migration de ALTER + service novo | `CertidaoQrCodeTest.java` |
| 15 | MÉDIO | pjb_decision_trace — tabela explícita | PARCIAL (DecisionFocusSessionRepository existe, sem migration CREATE TABLE explícito) | NÃO | Verificar se tabela existe; se não, criar migration `V262__decision_trace_explicit.sql` | Migration nova | IT de trace |
| 16 | MÉDIO | EventoProcessual — testes unitários | SIM (EventoProcessual.java e repository existem) | NÃO | Criar testes unitários de EventoProcessual | Nenhum arquivo novo | `EventoProcessualTest.java` |
| 17 | MÉDIO | MovimentacaoProcessual — testes | SIM | NÃO | Criar testes para MovimentacaoAdjustmentService e MovimentacaoComplianceReviewService | Nenhum arquivo novo | `MovimentacaoProcessualTest.java` |
| 18 | MÉDIO | Sisbajud — testes | SIM | NÃO | Criar testes para SisbajudApplicationService + SisbajudBloqueioService | Nenhum arquivo novo | `SisbajudOperacaoServiceTest.java` |
| 18b | MÉDIO | DataJud — testes de integração | SIM (DataJudFeedCheckpoint.java + V193 + V250 + repository existem) | NÃO | Criar testes de integração para DataJudFeedService + DataJudFeedCheckpointRepository | Nenhum arquivo novo | `DataJudFeedServiceTest.java`, `DataJudFeedCheckpointIT.java` |
| 19 | MÉDIO | ProfessionalAccessGrant — alias documentado | SIM (nome é ProfessionalInstitutionalAccessGrant) | SIM (nome diverge do PDF) | Adicionar alias/comentário no CLAUDE.md e no Javadoc da classe | `ProfessionalInstitutionalAccessGrant.java` | Nenhum teste novo |
| 20 | MÉDIO | JudicialConnector — testes | SIM | NÃO | Criar testes para JudicialConnectorCommandCenterService e connectors | Nenhum arquivo novo | `JudicialConnectorCommandCenterServiceTest.java` |
| 21 | MÉDIO | CaseMesh — testes | SIM (entidades existem) | NÃO | Criar testes para CaseContinuityOrchestratorService e CaseFileResolverService | Nenhum arquivo novo | `CaseContinuityOrchestratorServiceTest.java` |
| 22 | MÉDIO | Perito/Pericia — testes | SIM | NÃO | Criar testes para PeritoNomeacaoService e PeritoDisponibilidadeService | Nenhum arquivo novo | `PeritoNomeacaoServiceTest.java` |

---

## BACKLOG — Implementar quando prioritário

| # | Prioridade | Item | Já existe? | Duplicado? | Ação segura | Arquivos afetados | Teste obrigatório |
|---|-----------|------|-----------|-----------|-------------|-------------------|--------------------|
| 23 | BACKLOG | Prisão preventiva / habeas corpus | PARCIAL (V195 criminal_workflow) | NÃO | Expandir workflow criminal com controle de prazo de prisão cautelar | Novo service + migration | IT criminal |
| 24 | BACKLOG | Leilão eletrônico de bens penhorados | NÃO | NÃO | Criar módulo leilão_judicial separado | Novo módulo | IT leilão |
| 25 | BACKLOG | Central de flagrante eletrônico | NÃO | NÃO | Integração com Delegacia Digital — requer parceria | Novo conector | IT flagrante |
| 26 | BACKLOG | Painel de metas CNJ por magistrado | NÃO | NÃO | Criar módulo de metas com cálculo automático baseado em MovimentacaoProcessual | Novo service + dashboard | IT metas |
| 27 | BACKLOG | Agravo regimental automatizado | PARCIAL (RecursalMesh) | NÃO | Expandir RecursalMesh com fluxo específico de agravo | RecursalMesh extensions | IT recursal |
| 28 | BACKLOG | Gratuidade de justiça — análise de renda | PARCIAL (CustaJudicial com isenção) | NÃO | Expandir módulo de isenção com análise de renda e CNIS | CustaJudicialService | IT isenção |
| 29 | BACKLOG | BATNAs e homologação de acordo | PARCIAL (V111 facilitador_batna, AcordoService) | NÃO | Completar AcordoHomologadoService com fluxo de homologação judicial | AcordoService, novo controller | IT conciliação |
| 30 | BACKLOG | MNI — fila de transmissão persistente | NÃO (MniConnector é stateless) | NÃO | Se volume alto de transmissões MNI, criar tabela de fila MNI | Nova migration | IT MNI |

---

## NÃO_CRIAR — Conceitos já adequadamente implementados

| # | Item | Justificativa |
|---|------|--------------|
| A | RecursalMesh | 19 testes, 15+ services, 6 migrations — bem estabelecido |
| B | Peticionamento | 20+ testes, saga orquestrado, Laiane completo |
| C | MarketplaceClientApp + WebhookDelivery | Entities + repositories + services + migrations completos |
| D | LegalKnowledgeCorpus | 4 migrations, múltiplos services, testes presentes |
| E | Upload (UploadBatch + UploadItem) | Entities + repositories + controller + migrations + testes |
| F | Segurança (SecurityConfig + ABAC) | Arquitetura ABAC madura (ADR-0041), múltiplos testes |
| G | OutboxEvent | V41, V44, entity + repository presentes |
| H | IdentidadeJuridicaNacional | Entidade + repository + service + migration V105 |
| I | SisbajudOperacao | Entity + snapshots + service + migration V198 — completo |
| J | PlenarioVirtual / Sessão Plenária | NationalColegiadoEngine + múltiplos services + V154, V158 |
| K | MarketplaceAuditEvent | Entity + repository completos |
| L | MNI connector | Adequado como connector stateless — não precisa de tabela |
| M | ProntuarioNacional | Entity + service + V106 |
| N | Cidadao dashboard | V71, entities, services completos |
| O | AtendimentoThread / Message | V80-V90, entities + repositories + services + controller |
| P | UserCalendar | V77, V78, V148, V202 — bem coberto |
| Q | AccessibilityUsageSnapshot + UiStateHistory | Entities + repositories + services + migrations V45-V49 |
| R | Acordo/Conciliação (base) | AcordoService, CejuscEngine presentes |
| S | PeritoSorteioAudit | Entity + repository + migration V256 |
| T | AdvOffice (workspace completo) | V203-V210, múltiplas entities + repositories + services |
| U | Audiencia | Audiencia.java EXISTE em model/entity/ — não criar |
| V | VotoColegiado | VotoColegiado.java EXISTE em model/entity/julgamento/ — não criar |
| W | WorkItem | WorkItem.java EXISTE em model/entity/workflow/ — não criar |
| X | NotificationHistory | NotificationHistory.java EXISTE em model/entity/ — não criar |
| Y | Precedente | Precedente.java EXISTE em model/entity/jurisprudencia/ — não criar |
| Z | AudienciaWebRtcSessao | AudienciaWebRtcSessao.java EXISTE em model/entity/audiencia/ — não criar |
| AA | DataJud | DataJudFeedCheckpoint.java EXISTE + V193 + V250 + repository — não criar |

---

## Verificações Obrigatórias Antes de Qualquer Implementação

1. Executar `python scripts/constructor_injection_guard.py` — garantir zero @Autowired em fields
2. Executar `python scripts/architecture_hygiene_guard.py` — verificar higiene geral
3. Executar `python scripts/test_drift_guard.py` — verificar drift antes de criar testes
4. Executar `./mvnw test -pl pjb-api` antes e depois — o número de falhas NÃO pode aumentar
5. Verificar tabela real de `SecretariatQueueItemRepository` antes de criar WorkItem.java (risco de duplicidade)
6. Verificar se `Audiencia.java` existe em algum lugar não buscado (pjb-core) antes de criar
