# Plano de Melhoria v4 — Quadro de Progresso

Rastreador vivo da execução do Plano v4. Atualizado a cada fatia. Fonte de verdade para retomar sem repetir nem errar.

Legenda: ✅ feito · 🟡 em andamento · ⬜ pendente · ⛔ não fazer (colide com arquitetura/governança) · 🔁 contínuo

## Blocos (F1–F10)

| Bloco | Estado | Nota |
|---|---|---|
| F1 · podar código morto | ✅ | "439" ilusório no master; removido o morto real (#217); resto é feature não-conectada/vitrine-de-guard |
| F2 · sepultar tabelas fantasmas | ✅ | drop de 16 órfãs V365 (#218) + guard preventivo (#219) |
| F3 · remover andaimes de teste | 🟡 | scaffolds já removidos; **pendente**: migrar ~34 arch-tests que varrem FS p/ `@AnalyzeClasses` compartilhado (inclui o teste de 232s) |
| F4 · achatar fachadas rasas | ⛔/⬜ | as `*SurfaceFacadeService` são exigidas por arch-tests; achatar exige remover junto os arch-tests (decisão de arquitetura) — não iniciado |
| F5 · corrigir testes de mock | 🟡 | maioria é interação apropriada; corrigido RPV (#221); **pendente**: resíduo caso a caso |
| F6 · aprofundar god services | 🔁 | **177** beans acima do teto 8; ver tabela abaixo |
| F7 · RLS tabelas sensíveis | ✅ | já pronto no master (verificado) |
| F8 · fechar MockGuard | ✅ | pje + pje-submission cobertos (verificado) |
| F9 · sanitizar IA | ✅ | AiPromptEgressGuard ponto único (verificado) |
| F10 · travar ICP/HSM prod | ✅ | ProductionCriticalControlValidator (verificado) |

## F6 — god services (contador: 177 acima de 8)

### ✅ Resolvidos (cruzaram ≤8, saíram da lista)
| Bean | De→Para | PR |
|---|---|---|
| RecursalFormalizacaoService | 12→8 | #229 (pipeline PDF) |

### 🟡 Reduzidos (ainda acima de 8 — voltar depois)
| Bean | De→Para | PRs | Próximo corte possível |
|---|---|---|---|
| CitacaoIntimacaoEngine | 16→13 | #222,#226,#228 | resto é core essencial/acoplado — perto do piso |
| CidadaoDashboardSnapshotWriteService | 13→11 | #230 | cluster "widgets" (movRepo/docRepo) — avaliar |

### ⛔ Não reduzir (cada redução = dívida/risco; pular)
- **Audit/HSM/authz:** ProcessDigitalTwinService, JuizGabineteDecisionalService, CitacaoHoraCertaEngine, AcordoService, DiligenceOperationalClosureService (cluster de certificado)
- **Orquestrador/pipeline (1 método usa N deps):** NationalProcessRoutingService, ProcessoFechamentoTotalApplicationService, UnifiedProcessoIntentRouter, NegotiationMessagePreflightService, ScaleArchitectureService, TransitoJulgadoNarrativeSupport, ProcessoEncaixeFinalApplicationService, RecursalFluxoMinimoPersistenciaService
- **Fachada (*SurfaceFacadeService/*Painel):** deixar p/ F4
- **Não-conectado (bean sem chamador):** SecretariatDocumentBulkProcessor
- **Entrelaçado com helpers/records privados:** ConsultaPublicaWorkspaceService (personalSlice)

### ⬜ Fila de candidatos limpos (família de serviços-irmãos / cluster disjunto) — a confirmar por matriz antes
- (buscar próximos; critério: 2+ deps-irmãs usadas juntas, sem helper privado entrelaçado, sem audit/HSM)

## Método por fatia (não esquecer)
1. matriz método→dependência (scratchpad) → achar cluster disjunto limpo
2. ler os métodos → extrair serviço verbatim (deps ≤8) → delegar no original
3. **criar teste** do serviço novo + ajustar teste existente
4. atualizar budget (remover entrada se bean ≤8) + README (contagem de testes, mesmo commit) + **este quadro**
5. compile + testes verdes + guard verde → branch → PR → CI verde → merge → sync
