# Plano de Referência: Sistemas Judiciais como Inspiração Conceitual

> Gerado em: 2026-05-17  
> IMPORTANTE: Este documento registra padrões conceituais de sistemas judiciais existentes **apenas como inspiração arquitetural**.  
> Nenhum código desses sistemas foi copiado ou está sendo proposto para cópia.  
> O PJB deve reimplementar esses conceitos com código 100% original, Java 21, Virtual Threads e arquitetura conforme ADRs.

---

## Legenda

- **Prioridade**: CRÍTICO | ALTO | MÉDIO | BAIXO | BACKLOG

---

| # | Sistema | Padrão conceitual | Já existe no PJB? | Como aplicar sem copiar | Prioridade |
|---|---------|-------------------|--------------------|--------------------------|------------|
| 1 | PJe | Fluxo de peticionamento eletrônico com validação de protocolo em etapas (número de protocolo, autuação, distribuição) | SIM — PeticionamentoSagaOrchestrator + LaianeProtocolService + V15 | Já implementado via Saga Pattern com orquestração própria. Manter evolução na direção de Laiane | BAIXO |
| 2 | PJe | Distribuição automática por critérios (competência, prevenção, dependência) | SIM — ProcessoDistribuicaoCompetencia + V232 | Expandir RitoComputavelMatrix com critérios de prevenção | MÉDIO |
| 3 | PJe | Controle de prazo processual com calendário judicial | SIM — PrazoService, PrazoRiskIntelligenceService, UserCalendarService | Já implementado. Adicionar radar de prazos críticos no dashboard do juiz | BAIXO |
| 4 | PJe | Acesso por perfis diferenciados (juiz, advogado, cidadão, MP, defensor) | SIM — SecurityConfig, PjbAuthorizationService, ABAC (ADR-0041) | Manter ABAC centralizado. Evitar replicar lógica de perfil em cada controller | BAIXO |
| 5 | PJe 2.x | Modelo baseado em microsserviços com barramento de eventos | PARCIAL — OutboxEvent + Kafka implícito | PJB optou por monólito modular (ADR correto). Não migrar para microsserviços | IGNORAR |
| 6 | PJe 2.x | BPM para workflows processuais | PARCIAL — RitoWorkflowService, WorkItem, MovimentacaoProcessual | Adotar workflow declarativo via sealed classes em vez de BPMS externo | MÉDIO |
| 7 | e-SAJ | Sessão de julgamento colegiado com votação eletrônica | SIM — JulgamentoColegiado, VotoColegiado, HomomorphicVoteService, NationalColegiadoEngine | Já implementado com criptografia homomórfica. Manter padrão | BAIXO |
| 8 | e-SAJ | Publicação de acórdãos no Diário de Justiça Eletrônico (DJE) | SIM — Acordao.java, SecretariaPublicacaoAcordaoRequest, V245 (publicacao_despachos) | Expandir pipeline de publicação com notificação push automática via OutboxEvent | MÉDIO |
| 9 | e-SAJ | Pauta de audiências com agendamento e redesignação | SIM — PautaAudienciaService, AudienciaDesignacaoService, V12 | Já implementado. Adicionar integração com calendário do advogado (UserCalendarService) | BAIXO |
| 10 | eproc | Consulta pública de processos com busca textual | SIM — ConsultaPublicaSearchService, PublicProcessoConsultaService, V219 | Expandir com full-text search (V257 já adiciona índices de texto) | BAIXO |
| 11 | eproc | Modelo de colegiado com relator, revisor e vogais | SIM — NationalColegiadoEngine, NationalColegiadoSessionSupport, DesembargadorColegialSurfaceFacadeService | Verificar cobertura de roles (relator/revisor) no modelo de votação | MÉDIO |
| 12 | eproc | Assinatura digital de documentos com certificado A3/ICP-Brasil | PARCIAL — DocumentTrustChainService, OfficeSignatureQueueService | Expandir integração com HSM (V146 já trata de comunicação com HSM) | ALTO |
| 13 | Projudi | Integração com tribunais via conector federado | SIM — ProjudiConnector.java, JudicialConnectorCommandCenterService, V108 (federalismo judicial) | Conector já implementado. Adicionar monitoramento de saúde em tempo real | MÉDIO |
| 14 | Projudi | Polo ativo/passivo com representação de partes | PARCIAL — ProcessoVinculoNacional, ProfessionalInstitutionalAccessGrant | Modelar explicitamente Polo (ativo/passivo) como value object no agregado Processo | ALTO |
| 15 | Projudi | Execução fiscal e autos complementares | PARCIAL — PostArchiveLifecycleService | Expandir modelo de pós-arquivo para execução fiscal | MÉDIO |
| 16 | CRETA 2.0 | Processo criminal com cadeia de custódia digital | SIM — CadeiaCustodiaDigitalLedgerEntry, CadeiaCustodiaDigitalSyncEvent, V138-V139 | Expandir com rastreabilidade forense de provas (ProcessoProvaApplicationService) | MÉDIO |
| 17 | CRETA 2.0 | Mandado eletrônico com geolocalização e rastreamento | PARCIAL — OficialJusticaEnderecoTriageService, OficialJusticaBalcaoVirtualService | Adicionar tracking de cumprimento de mandado com timestamptz | ALTO |
| 18 | CRETA 2.0 | Interrogatório por videoconferência | PARCIAL — AudienciaWebRtcService, V155 (webrtc tables) | Expandir AudienciaWebRtcSessao como entidade explícita com tabela própria | MÉDIO |
| 19 | CRETA 2.0 | Prisão preventiva e liberdade provisória | PARCIAL — InqueritoPolicialDigitalService, V195 (criminal_workflow) | Expandir workflow criminal com controle de habeas corpus e prazos de prisão | ALTO |
| 20 | PJe / eproc | DataJud — integração com CNJ para estatísticas | PARCIAL — DataJudFeedService (sem migration de tabela) | Criar migration para tb_datajud_checkpoint + service de sincronização incremental | ALTO |
| 21 | PJe | MNI — Modelo Nacional de Interoperabilidade | PARCIAL — MniConnector.java (stateless, sem tabela) | Manter stateless para envio; adicionar tabela de fila de transmissão MNI se necessário | MÉDIO |
| 22 | e-SAJ / eproc | Certidão eletrônica com QR Code de autenticação | SIM — V13__certidoes.sql | Expandir com geração de QR Code verificável publicamente | MÉDIO |
| 23 | PJe | Perícia judicial com sorteio imparcial de peritos | SIM — PeritoNomeacao, PeritoSorteioAudit, PeritoDisponibilidade | Expandir com critério de especialidade e distância geográfica | MÉDIO |
| 24 | eproc / STJ | Precedente vinculante com sobrestamento automático | SIM — SobrestamentoTema, TemaPrecedenteVinculanteService, V190, V153 | Expandir com API de notificação de sobrestamento em massa | MÉDIO |
| 25 | PJe / e-SAJ | Marketplace de integrações com APIs externas | SIM — MarketplaceClientApp, MarketplaceWebhookDelivery (completo) | NÃO_CRIAR — bem implementado | IGNORAR |
| 26 | CRETA 2.0 / eproc | Conciliação e mediação pré-processual (CEJUSC) | SIM — CejuscEngine, ConciliadorMediadorPainelService | Expandir proposta de acordo com BATNAs e homologação judicial | MÉDIO |

---

## Sistemas sem equivalência no PJB (gaps estratégicos)

| Sistema | Funcionalidade ausente | Impacto | Recomendação |
|---------|----------------------|---------|--------------|
| PJe 2.x | Painel de produtividade judicial com metas do CNJ | ALTO | Criar módulo de metas CNJ com cálculo automático por magistrado |
| eproc | Leilão eletrônico de bens penhorados | MÉDIO | BACKLOG — módulo separado |
| CRETA 2.0 | Central de flagrante eletrônico 24h | ALTO | BACKLOG — requer integração com Delegacia Digital |
| Projudi | Gratuidade de justiça com análise de renda | MÉDIO | BACKLOG — expandir CustaJudicial com módulo de isenção |
| PJe | Agravo regimental automatizado | MÉDIO | BACKLOG — expandir RecursalMesh |
