# Plano de Melhoria v4 — Como ficou

Registro de execução do "Plano de Melhoria — PJB (v4)" da coorientação. O plano foi medido no commit `3834b16c`; este documento registra o estado real em `master` após a execução, com a evidência de cada fase.

Método: cada fase foi medida no `master` antes de agir (o número da capa do plano é o baseline `v1`, não o estado atual). Só se removeu ou reformulou o que a medição confirmou, com o compilador e a suíte como juiz e o portão de CI verde antes de cada merge.

## Sumário

O `master` já havia resolvido a maior parte do plano antes desta execução. O trabalho real e não-feito era **F2** (tabelas órfãs), executado aqui; as demais fases foram verificadas fase a fase.

| Fase | Alvo do plano | Como ficou |
|---|---|---|
| F1 · podar código morto | 439 classes | Ilusório no `master`: a maior parte do "sem referência" é feature não-conectada, vitrine protegida por guard, ou bean ligado por tipo. Removidas 2 classes de fato mortas (#217). |
| F2 · sepultar tabelas fantasmas | 50 tabelas | **Executado.** Restavam 16 órfãs de fato (não 50); dropadas via `V365` (#218) e criado guard preventivo (#219). |
| F3 · remover andaimes de teste | 53 classes | Já feito: os scaffolds de refactor já haviam sumido; resta apenas o custo de build dos arch-tests que varrem o filesystem. |
| F4 · achatar fachadas rasas | 25–52 fachadas | **Não executado por segurança:** as fachadas `*SurfaceFacadeService` são camada deliberada, exigida por testes de arquitetura (ex.: `PjbAjuizamentoHttpBoundaryArchitectureTest`). Achatá-las quebraria o build e a arquitetura. |
| F5 · corrigir testes de mock | 154 testes | Medido: a maioria dos `verify`-only é teste apropriado de interação (delegação, cache, agendador, evento). Corrigido um anti-padrão real de estado em `PrecatorioRadarServiceTest` (#221). |
| F6 · aprofundar god services | 104–178 beans | Aberto e incremental. Padrão comprovado numa fatia: `CitacaoIntimacaoEngine` 16→15 via extração de `CitacaoExpedicaoNotificacaoService` (#222). O guard de budget trava regressão a cada ganho. |
| F7 · RLS nas tabelas sensíveis | 39 tabelas | Já feito: as tabelas sensíveis nomeadas têm RLS; 44 no total, com guard de disciplina. |
| F8 · fechar MockGuard | 2 flags | Já feito: `MockGuardEnvironmentValidator` cobre `pje` e `pje-submission`, além de hsm/bnmp/dje/govbr/vector-search. |
| F9 · sanitizar superfície de IA | 43 classes | Já feito: `AiPromptEgressGuard` é o ponto único de egresso, com `AnthropicInputSanitizer`/`LegalContextSanitizer`. |
| F10 · travar ICP/HSM em produção | 4 flags | Já feito: `ProductionCriticalControlValidator` derruba o boot no perfil `prod` se ICP ou HSM estiver desligado. |

## Detalhe por fase

### F1 — Poda de código morto

Medição no `master`: das 329 classes sem referência de produção, 90 são mantidas de propósito por guards de CI (`universal_digital_core_guard`, `tribunal_readiness_guard`, `judicial_innovation_part_two/three_guard`), ~44 são ativadas por framework (`@Scheduled`, `@KafkaListener`, filtros, listeners) e a maioria do restante é ligada por tipo de interface (`List<IASkill>`, os `Legal*McpServer`, data-plane filters) ou é a superfície de features construídas-mas-não-conectadas (os "Aceleradores inteligentes" do README, GIGS, assinatura ICP, Justiça em Números). Os três maiores "engines mortos" que o plano cita já não existem no `master`.

Ação: removidas duas classes `@Service` sem nenhum consumidor — `OficialJusticaInstitutionalDispatchService` e `MotorCeleridadeConstitucionalService` (#217, 448 linhas).

### F2 — Sepultamento das tabelas fantasmas

Medição no `master`: 18 tabelas lógicas sem referência em Java, não as 50 do baseline — o `master` conectou a maior parte do bloco `V231–V252` desde a análise. Das 18, duas foram preservadas por dependência real (`pjb_outbox_event`, exigida por `FlywayPostgresMigrationIT`; `tb_database_retention_policy`, coberta por `DatabaseInfrastructureGovernanceTest`).

Ação: `V365__drop_tabelas_orfas.sql` removeu as 16 restantes com `DROP TABLE IF EXISTS ... CASCADE` (#218). As migrations de origem permanecem intactas — o histórico é imutável. Guard novo `orphan_table_guard.py` reprova o CI se uma migration criar tabela sem referência em Java, ignorando partições declarativas (#219).

Salvaguarda registrada: 96 tabelas `tb_outbox_event_yYYYYmMM` e `tb_authz_trail_yYYYYmMM` aparecem como "sem referência" mas são partições mensais vivas do outbox e da trilha de auditoria ABAC — não foram tocadas.

Verificação: `FlywayPostgresMigrationIT` aplicou as 327 migrations (incluindo `V365`) em base limpa, sem erro.

### F5 — Correção dos testes que verificam mock

Medição: os `verify`-only medidos no `master` são, em maioria, teste apropriado de interação — a chamada É o comportamento (delegação de fachada, cache/invalidação, agendador, publicação de evento). Os "15 métodos test-only em produção" citados pelo plano (`resetForRetry`, `presetCatalog`) são falsos positivos: métodos de domínio reais.

Ação: em `PrecatorioRadarServiceTest`, três testes de limite de RPV passaram a assertar o resultado observável (`valorLimiteRpv` = 60/40/30 salários mínimos), não só a chamada ao serviço de salário mínimo (#221).

### F6 — Aprofundamento dos god services

Estado: 178 beans acima do teto de 8 dependências, geridos por budget. É a única frente genuinamente aberta e é incremental: os piores god services concentram muitas dependências porque seus colaboradores são, em geral, genuinamente entrelaçados ou são colaboradores transversais de segurança (auditoria, HSM, ABAC) exigidos pela tese de "segurança por construção" — não por defeito. Seam limpo e seguro é a exceção.

Ação (padrão comprovado): `CitacaoIntimacaoEngine` repetia o trio portal + webhook em seis métodos de ciclo de vida; extraído para `CitacaoExpedicaoNotificacaoService` com um método por evento, que passou a deter o mapa evento→canais+payload. O engine caiu de 16 para 15 dependências, com auditoria, HSM e escalonamento ao juízo intactos (#222). O `constructor_injection_guard` trava a regressão.

## Estado do portão de integração

O portão de integração (`mvnw verify` contra Postgres real, a cada merge) tem instabilidade intermitente conhecida e documentada — contaminação de estado dependente de ordem entre classes de IT e o tipo `JSONB` no H2. O `test_isolation_guard` passa (isolamento estrutural garantido), nenhuma IT apaga `tb_usuario` diretamente e as entidades com `JSONB` têm mapeamento; a instabilidade é de interação de cache de contexto, não um defeito determinístico. Ela faz o portão piscar vermelho e voltar verde no re-run — foi o caso do commit `d33c2985`, que reprovou e passou no re-run com o mesmo código.
