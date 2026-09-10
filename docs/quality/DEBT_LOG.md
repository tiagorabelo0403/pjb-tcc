# Débitos Técnicos — Registro Aberto

Registro de dívidas técnicas conhecidas e ainda não fechadas. Diferente da seção de Testes do
`README.md` (que narra dívidas já resolvidas), este arquivo documenta lacunas abertas — sem bloquear
nenhuma entrega em andamento — para que não fiquem só na memória de quem investigou.

Cada entrada sai daqui quando a dívida é fechada; o fechamento é então narrado no `README.md`, seguindo
o padrão já em uso (ex.: D-routing-preprotocolo, D-d25-testes-anexo).

## D-schedulers-processuais-desligados-por-decisao-nao-tomada

**Status:** aberta — flags agora visíveis; decisão de ativação pendente

Varredura por `@ConditionalOnProperty` sem `matchIfMissing` cuja propriedade não existe em nenhum
`application*.yml` encontrou **16 beans desligados em todo ambiente**, por flags que não apareciam em
config, env, compose nem k8s. Não era decisão de desligar: era ausência de declaração.

As flags foram declaradas em `application.yml` seguindo a convenção do projeto
(`${PJB_XXX:default}`), com o **default efetivo atual**, então nada mudou de comportamento —
mudou a possibilidade de decidir. Verificado antes: nenhuma delas é usada com `matchIfMissing = true`
em outro ponto, então declarar `false` é neutro.

**O que fica pendente de decisão de produto, agora que é visível:**

| Flag | Bean | Consequência de seguir desligado |
|---|---|---|
| `pjb.outbox.relay.enabled` | `OutboxRelayWorker` | `pjb.outbox.ingress.enabled` é `true` por padrão, então eventos entram no outbox e **nada os drena**. O README promete "zero perda de evento em falha de commit" |
| `pjb.jobs.ciencia-ficta.enabled` | `CienciaFictaScheduler` | ciência ficta é marco de prazo processual; sem o scheduler, não é aplicada automaticamente |
| `pjb.jobs.conclusao-expirada.enabled` | `ConclusaoExpiradaScheduler` | conclusão vencida não é detectada |
| `pjb.jobs.protocolo-completude.enabled` | 2 schedulers de expiração e notificação | prazo de completude documental não expira nem notifica |
| `pjb.sync.salario-minimo.enabled` | `SalarioMinimoNacionalSyncScheduler` | ver `D-scheduler-salario-minimo-nunca-ativado` |
| `pjb.secretariat.enabled` | `SecretariadoCommandCenter`, `SecretariatDossieController` | superfície de secretaria indisponível |

**Por que não liguei nada:** schedulers processuais **mutam estado de processo em background**. Ligar
por conta própria, sem ambiente para observar o efeito, é o oposto de "não quebrar o projeto". A
decisão de quais devem rodar em produção é de produto e de operação.

**Guarda criada:** `conditional_property_declared_guard.py`, no job *Guards (enforce)*, falha o build
quando um bean exige propriedade que não está declarada em nenhum config. Impede que a próxima flag
nasça invisível.

## D-teto-rpv-duplicado-como-literal-em-seis-pontos

**Status:** aberta — achado do revisor ao fechar `PrecatorioRadarService`

O teto de RPV é parâmetro legal e está duplicado como literal `new BigDecimal("60")` /
`new BigDecimal("40")` em seis pontos:

| Valor | Onde |
|---|---|
| 60 SM (RPV federal / competência JEF) | `CalculoJudicialAssistenciaService:259`, `CalculoJudicialIaFinanceiraService:554`, `FederalPrevidenciarioCjfCalculoAvancadoService:144`, `PrecatorioRadarService` |
| 40 SM (competência JEC / ente subnacional) | `NationalRulePackEngine:418`, `PrecatorioRadarService` |

**Por que importa:** teto de RPV muda por lei, e cada ente federado pode fixar o seu (ADCT art. 87
estabelece pisos até que estados e municípios legislem). Com o valor espalhado, uma mudança
normativa exige encontrar os seis pontos, e esquecer um produz classificação RPV/precatório
divergente entre telas do mesmo sistema.

**Correção sugerida:** fonte canônica de parâmetros monetários processuais, no mesmo espírito de
`SalarioMinimoNacionalService` — que já é a fonte única do salário mínimo e é consultada por data.
O teto em salários mínimos deveria ser resolvido por ente e por data de referência, não por
literal.

**Por que não foi feito na mesma fatia:** três dos quatro pontos de 60 SM estão em serviços de
cálculo com consumidores reais, e o valor entra neles como *default* de request opcional. Unificar
muda a superfície desses serviços. É fatia própria, com verificação própria.

## D-rpv-municipal-sem-limite-proprio

**Status:** aberta — sem evidência no projeto, não inventada

`PrecatorioRadarService.limiteRpv` trata `MUNICIPAL` com o mesmo teto de `ESTADUAL` (40 salários
mínimos), porque a busca no projeto não encontrou nenhuma fonte definindo limite municipal próprio.

O ADCT art. 87 estabelece pisos distintos por ente enquanto não houver lei local, e a constante foi
nomeada `SALARIOS_MINIMOS_RPV_SUBNACIONAL` para deixar explícito que hoje os dois entes compartilham
o mesmo valor por ausência de fonte, não por decisão.

**O que falta:** confirmar com o Tiago qual valor o PJB deve adotar para município, e se o teto deve
ser configurável por ente federado (ver `D-teto-rpv-duplicado-como-literal-em-seis-pontos`).

## D-laiane-substabelecimento-503-em-contexto-compartilhado

**Status:** aberta — contida, causa não identificada

**Sintoma:** `LaianeLawyerSubstabelecimentoIT` devolve **503 nos dois testes** quando roda em lote
compartilhando contexto Spring — tanto no que espera 200 quanto no que espera 403. Isolada, em
contexto novo, passa 2/2. O 503 acontece antes da lógica de negócio: os dois casos falham com o
mesmo status, apesar de exercitarem caminhos de autorização diferentes.

**Como apareceu:** a PR #108 removeu o `@MockitoBean CapabilityRateLimiter` de 11 ITs, trocando por
`pjb.security.capability-ratelimit.enabled: false`. O mock era o que dava a esta classe um contexto
Spring próprio; sem ele, ela passou a entrar no contexto compartilhado e a falhar no lote 4.

**O que foi descartado como causa:**

- Rate limit negando: negação devolve 429, não 503, e o desligamento por configuração retorna antes
  de `store.tryConsume(...)`.
- Exceção não tratada: o handler genérico de `ApiExceptionHandler` registra `Unhandled exception` em
  log, e não há registro no relatório do Failsafe.
- Dado ausente por `TRUNCATE`: o teste cria substabelecente, destinatário e procuração dentro do
  próprio método.

**Contenção aplicada:** o mock foi restaurado **apenas nesta classe**, o que devolve a ela o contexto
dedicado e o estado verde anterior. As outras 10 ITs seguem com a neutralização por configuração.

**Custo da contenção:** 1 contexto Spring, algo em torno de 1 minuto na suíte.

**Próximo passo sugerido:** capturar o corpo do `ProblemDetail` do 503 (o teste hoje assere só o
status) para identificar o `type` e, com ele, o handler de origem. Sem esse dado qualquer correção
seria chute.

## D-fragmentacao-de-contexto-spring-nos-its

**Status:** aberta — medida, com alavanca identificada; exige julgamento por teste

**Medição de 2026-09-09**, primeira execução completa da suíte de integração (116 classes, 292
testes, ~1h38 contra Postgres e Kafka reais):

| Métrica | Valor |
|---|---|
| Configurações distintas de contexto Spring | 44 (eram 47) |
| Configurações usadas por uma única classe | 33 (eram 38) |
| Configurações se nenhum IT mockasse bean | 21 |
| ITs que mockam bean | 35 de 116 (eram 38) |
| Declarações de mock em ITs | 73, sobre 49 tipos distintos |

O custo da suíte é governado por **quantos contextos distintos existem**, não por quantos testes há.
Classe que sobe contexto próprio custa 70–220 s; classe que reaproveita contexto roda em frações de
segundo (medido: 0,055 s a 0,66 s). Cada `@MockitoBean` distinto entra na chave de cache do contexto,
então cada combinação de mocks cria um contexto novo.

**Dois padrões concretos por trás da fragmentação:**

1. ~~**Mock como neutralizador**~~ — **RESOLVIDO**: os 11 mocks de `CapabilityRateLimiter` deram
   lugar a `pjb.security.capability-ratelimit.enabled: false` no perfil `integration-test`.
   Confirmado antes de trocar que nenhum dos 11 simulava bloqueio (9 sem stub, 2 stubando
   `CapabilityRateLimitDecision(true, ...)`) e que nenhum teste do projeto exercita negação real.
   Ganho de quebra: o limiter usa Redis downstream, e o desligamento retorna antes de
   `store.tryConsume(...)`, removendo dependência latente de infra em ambiente sem Redis.

2. **Mock do próprio serviço sob teste.** Ex.: `DefensorPublicoPainelControllerIT` mocka
   `DefensorPublicoPainelService`. Isso não é teste de integração — é teste de fatia de controller
   pagando custo de contexto Spring completo mais container Postgres. É o espelho do achado da PR #98
   (unitários com nome de integração); aqui são fatias com custo de integração.

**Por que não foi resolvido em lote:** remover mock muda o que cada teste prova. O próprio plano de
melhoria proíbe refatoração comportamental em lote, e converter 38 classes de uma vez reproduziria o
processo que gerou o problema.

**Pré-requisito que isto bloqueia:** qualquer forma de CI de integração. Com 1h38 não existe versão
que caiba no caminho do PR; reduzir contexto é o que torna a discussão possível.

## D-f1-remocao-govregistryclient-ajuizamentoworkflowadapter

**Status:** aberta — dormente, não bloqueia boot

**Contexto:** o lote 2/3 de remoção de código morto (F1, commit `cac80840`, 409 arquivos) removeu
`NoopGovRegistryClient`/`ResilientGovRegistryClient` (únicas implementações de `GovRegistryClient`)
e `DefaultAjuizamentoWorkflowAdapter` (única implementação de `AjuizamentoWorkflowAdapter`) por
"zero referência direta" — mesmo padrão de falso positivo já corrigido para
`PjbInetAddressResolverProvider` (ServiceLoader) e para `VectorSearchServiceDisabled`/
`HeuristicEvidenceContradictionResolver`/`DeterministicHashEmbeddingService`/
`JudicialConnectorSecureTransport` (interface injetada só por tipo): o scanner de classes mortas
não cruza contra injeção Spring por tipo de interface.

Essas duas não quebram o boot hoje porque os únicos consumidores estão desligados por padrão:
`CrcIntegrationService` exige `pjb.gov.vital-monitor.enabled=true` (não setado em nenhum profile);
`ComandoAjuizamentoConsumer` exige `@ConditionalOnBean(ZeebeClient.class)`, e nenhum `@Bean` de
`ZeebeClient` existe no projeto. Confirmado por leitura de código + suíte de arquitetura completa
verde após restaurar as 4 classes que quebravam o boot de verdade.

**Quando revisitar:** se `pjb.gov.vital-monitor.enabled` ou a integração Zeebe forem ativados algum
dia, essas duas features vão falhar no boot com `NoSuchBeanDefinitionException` até as
implementações serem restauradas (`git show b0ac4bd9:<caminho>`) ou reescritas.

## D-territorio-string-solta-entidades-legadas

**Status:** aberta

**Contexto:** o trabalho de "Organização Judiciária" (Tasks 1-6, `Tribunal`/`Comarca` como entidade real)
migrou território (uf/comarca) de String solta para FK `Comarca` em 5 entidades: `UnidadeJudiciariaCompetencia`,
`Jurisdicao`, `Usuario`, `Processo`, `WorkItem` — mantendo `uf`/`comarca` como fallback String real ao lado
da FK, porque o catálogo `tb_comarca` (Task 1) só cobre 3 dos 27 estados (CE/MG/RN). O teste de arquitetura
novo (`OrganizacaoJudiciariaArchitectureTest`, Task 6) trava qualquer entidade NOVA que reintroduza `uf`/`comarca`
String sem a FK `Comarca` correspondente na mesma classe — mas, ao rodar essa regra contra o projeto inteiro
pela primeira vez, apareceram 23 entidades pré-existentes, fora do escopo original, que já declaravam
`uf`/`comarca` String sem nenhuma FK `Comarca`. Quatro saíram da allowlist depois — `JurisdicaoTerritorial`
logo em seguida, `OrgaoJudiciario`/`PeritoSorteioAudit`/`PeritoDisponibilidade` mais tarde —
ver notas abaixo —, restando **19 entidades pré-existentes**:

`CalendarioForenseEntry`, `AtlasAcessoMunicipio`, `NoFederacaoJudicial`, `EscrituraExtrajudicialRegistro`,
`InqueritoPolicialDigital`, `EventoInstitucional`, `Estados`, `CidadaoProcessoNacionalProjection`, `Municipios`,
`ProcessoZonaEleitoral`, `UnidadeInstituicao`, `CalendarioEleitoral`, `OperationalFunctionCredential`,
`GovServiceRegistry`, `InstitutionalCompetenceRuleSnapshot`, `InstitutionalCatalogUnitSnapshot`,
`InstitutionalCatalogGovernanceSnapshot`, `ProfessionalInstitutionalAccessGrant`, `PainelTribunalMetrica`.

Essas 19 classes foram registradas numa allowlist nomeada (`ENTIDADES_LEGADAS_TERRITORIO_STRING_SEM_FK_COMARCA`)
dentro do próprio teste de arquitetura — a regra continua ativa e bloqueia qualquer entidade nova fora dessa
lista, mas não força a migração retroativa das 19 de uma vez (cada uma pertence a um domínio
diferente — eleitoral, criminal, atlas, extrajudicial, gov, federalismo, snapshots institucionais —
e migrar todas exigiria repetir a mesma investigação e migração individualmente 19 vezes).

`JurisdicaoTerritorial` saiu da allowlist na correção da revisão final: é a tabela de onde `tb_comarca`
é semeada (`V319` lê `municipio_ibge`/`municipio_nome`/`uf`), então a FK `Comarca` resolve por código IBGE com
match exato, sem a ambiguidade de nome que motivou o adiamento das demais.

`OrgaoJudiciario` saiu da allowlist com migração própria (`V329__orgao_judiciario_fk_comarca.sql`): ganhou
`comarcaEntidade` (`@ManyToOne Comarca`, nullable, ao lado dos campos `comarca`/`estado` String que continuam
como fallback), `OrgaoJudiciarioService.aplicarComarcaDoCatalogo` resolve via `ComarcaResolutionService.resolver`
(mesmo padrão nome+UF acento-insensível já usado por `UsuarioService`) em `criar`/`atualizar`, e o backfill da
migration aplica o mesmo match aos registros já existentes. Cobertura:
`OrgaoJudiciarioServiceComarcaTest` (3, resolve/aplica, comarca em branco não resolve, resolver sem candidata
não lança) e `OrganizacaoJudiciariaArchitectureTest` (a regra em si, confirmando que a classe não precisa mais
da allowlist). Cadeia completa de migrations (V1→V329) validada do zero contra Postgres 17 descartável via
Flyway CLI antes do fechamento.

`PeritoSorteioAudit`/`PeritoDisponibilidade` saíram da allowlist juntas numa migração própria
(`V330__perito_disponibilidade_sorteio_fk_comarca.sql`), mesmo padrão de `comarcaEntidade` nullable ao lado do
`comarca` String. Diferença em relação às outras: nenhuma das duas tem campo UF próprio (só `comarca`), então
a resolução usa `ComarcaResolutionService.resolver(comarca, null)` — o resolver já trata UF ausente com
match por nome sozinho, só resolvendo quando há exatamente 1 candidata inequívoca em `tb_comarca` (caso
contrário fica `null`, nunca escolhe errado). O backfill da migration replica essa mesma regra de
desambiguação em SQL puro (`WHERE ... AND (SELECT count(*) ... ) = 1`). Cobertura:
`PeritoDisponibilidadeServiceTest` ganhou 2 testes novos (resolve e aplica `comarcaEntidade` ao registrar
disponibilidade; comarca não informada não resolve nem lança) somados aos 5 já existentes (7/7 verde).

**Estados/Municipios não são candidatos válidos, apesar do que uma versão anterior desta nota sugeria:**
investigação confirmou que `Estados` (PK = `uf`) e `Municipios` (PK = `ibgeCode`) são elas mesmas as tabelas de
catálogo geográfico — não têm um campo "uf/comarca solta" que precise ser comparado contra `tb_comarca` por
identidade, são a própria fonte primária. Não são migráveis pelo mesmo padrão das demais; retirado como
sugestão de próximo alvo.

**Risco:** as mesmas classes de bug que motivaram este trabalho (grafia divergente entre UF/comarca cadastrados
em textos diferentes) continuam presentes nas 19 entidades restantes — nenhuma delas ganhou o benefício da
comparação por identidade real via FK.

**Quando revisitar:** ao planejar o próximo trabalho de território — priorizar por volume de uso real. Cada
migração fecha reduzindo a allowlist em `OrganizacaoJudiciariaArchitectureTest`, nunca alargando.

## D-workitem-fk-comarca-propagacao-parcial

**Status:** aberta

**Contexto:** a revisão final da fatia "Organização Judiciária" achou que nenhum caminho de produção
escrevia a FK `comarcaEntidade` de `Usuario`/`Processo`/`WorkItem` — todo dado novo ficava com `comarca_id`
permanentemente nulo, e a comparação territorial por identidade real (`AssessorGabineteGuardRailService.territoryMatches`)
nunca disparava para dado novo. A rodada de correção ligou a FK nos pontos que efetivamente alimentam essa
comparação: os dois `WorkItem.builder()` de `RitoWorkflowService` (que herdam a FK já resolvida da `Jurisdicao`
do processo), o snapshot de distribuição em `MapaCompetenciaDinamicoEngine`, e os três pontos de resolução
por texto (`UsuarioService.criar/atualizar`, `ApiMarketplaceService.protocolar`, `MniRecepcaoService.receberAutos`,
via `ComarcaResolutionService`).

Uma varredura de `WorkItem.builder()` no restante do projeto (não feita durante a rodada de correção — o
implementador relatou "~12 outros pontos" de memória; a contagem real, feita na re-revisão, é **44 arquivos**
em `pjb-api/src/main` que constroem `WorkItem` setando `.comarca(...)` textual sem `.comarcaEntidade(...)`)
mostra que `RitoWorkflowService` é o único ponto de criação de `WorkItem` com FK — exemplos confirmados:
`RecursalWorkItemMaterializerService`, `TransitoJulgadoArquivamentoEngine`, `DesembargadorColegialdoPainelService`,
`NationalCommunicationFlowFacade`, `OficialJusticaPainelService`, `JuizGabineteDecisionalService`, entre outros,
em domínios de recursal, colegiado, comunicação processual, secretariado e gabinete.

**Risco:** baixo, não é regressão — confirmado por leitura de código e teste (`territoryMatches` após o fix
do achado I2 da mesma revisão final): um `WorkItem` sem `comarcaEntidade` mas com `comarca` textual própria
cai inteiro no caminho de comparação textual normalizada (o comportamento anterior à fatia inteira), nunca
tenta usar a FK do `Processo` no lugar. Os 44 sites simplesmente não ganham o benefício da comparação por
identidade real — não produzem nenhum match incorreto.

**Quando revisitar:** ao planejar a próxima fatia que toque roteamento de `WorkItem` por gabinete/assessoria —
extrair um método `WorkItem.herdarTerritorioDe(Processo)` (ou equivalente) que copie `uf`/`comarca` E
`comarcaEntidade` juntos, e aplicar nos 44 sites incrementalmente, priorizando os que já são usados pelo
guard-rail de território (`AssessorGabineteGuardRailService`) com mais frequência em produção.

## D-classificacao-contextual-default-permissivo

**Status:** aberta

**Contexto:** `classificacaoContextualCoerente` (em `QualifiedDocumentSignatureEnvelopeService.resolveClassificacaoContextualCoerente`)
retorna `true` por padrão, sem verificação, para 2 dos 14 chamadores de assinatura qualificada:

- `OfficialDocumentTemplateService` (ramo `TERMO_ACORDO`/`SEM_INTERESSE_MANIFESTACAO`) — gate é ABAC de
  leitura de processo (`requireReadProcesso`), não de papel. Pode ser intencional: esses 2 templates são
  tipicamente assinados por parte/advogado, não magistratura — mas não foi verificado se o valor `true`
  cego é a decisão de negócio certa ou só um ponto cego.
- `OperationalNotificationProofService` — gate é ABAC de capacidade institucional
  (`ASSINAR_MANIFESTACAO`), cujo motor de afiliação institucional (`InstitutionalAffiliationApplicationService`
  + `CapacidadeCaixaInstitucional`) não foi mapeado. Não se sabe quais `TipoUsuario` efetivamente recebem
  essa capacidade em produção.

**Risco:** baixo — os dois já têm gate próprio (ABAC), então não é ausência de controle de acesso. É
ausência de comparação contra `segmentoInstitucional()`, como os outros 12 chamadores já têm.

**Cobertura de teste:** o comportamento `default -> true` do switch é testado isoladamente (com um
`papelAssinante` sintético, não um dos 2 chamadores reais). Nenhum teste exercita
`OfficialDocumentTemplateService` ou `OperationalNotificationProofService` end-to-end pra confirmar que o
`true` realmente se propaga desses fluxos específicos.

**Quando revisitar:** se o motor de afiliação institucional for mapeado por outro motivo, ou se a política
de assinatura de TERMO_ACORDO precisar de auditoria mais rígida.

## D-domicilio-parte-dois-canais-nao-populam

**Status:** aberta — MNI captura UF, comarca e município seguem sem fonte nesse canal

**Contexto:** `Processo.ufAutor`/`comarcaAutor`/`ufReu`/`comarcaReu` eram populados só pelo canal REST
(via `ProcessoMapper`). **Laiane já foi corrigido**: `EstruturarRequest` captura os 4 campos +
`enderecoReuDesconhecido`, a sessão (`LaianePeticaoInicialDraftSession`, migration V301) os carrega até
`protocolar()`, que os aplica ao `Processo` (flag vence os valores quando o réu é desconhecido).
**Marketplace também foi corrigido** (campo opcional aditivo, sem versionar endpoint):
`MarketplaceProtocoloRequest` ganhou `ufAutor`/`comarcaAutor`/`ufReu`/`comarcaReu`/
`enderecoReuDesconhecido`, propagados através de `MarketplaceSurfaceFacadeService` até o record interno
homônimo de `ApiMarketplaceService`, que aplica a mesma regra de precedência do Laiane em `protocolar()`.
Achado durante a implementação: o contrato público (`model/dto/processo/marketplace/
MarketplaceProtocoloRequest`) e o parâmetro interno de `ApiMarketplaceService` são dois records distintos
com os mesmos 15 campos, mapeados posicionalmente pelo facade — não é duplicação indevida (contrato
público vs. parâmetro interno são propósitos diferentes), mas qualquer campo novo precisa ser adicionado
nos dois records e no mapeamento do facade na mesma edição, senão a aridade diverge e o projeto para de
compilar.

**MNI passou a capturar UF de domicílio** (`MniXmlToProcessoAdapter.resolvePartes`): extrai `estado`
do primeiro `<endereco>` de cada `<pessoa>`, normaliza (trim + maiúsculo) e só grava se resultar em
exatamente 2 letras — valor fora desse formato (nome por extenso, código estranho) fica nulo em vez de
gravado cru, porque `Processo.ufAutor`/`ufReu` são `@Column(length = 2)` e um valor maior quebraria o
INSERT no Postgres. `comarcaAutor`/`comarcaReu` continuam nulos nesse canal — **o MNI não tem elemento
equivalente a comarca** (circunscrição judiciária), e `cidadeAutor`/`cidadeReu` (município) também não
são capturados nesta etapa, embora o schema tenha um elemento `cidade` livre dentro de `tipoEndereco`
(texto sem código, sem cruzamento com o catálogo de jurisdição territorial).

**Nota para não confundir no futuro:** o endereço de parte do MNI 2.2.2 **não tem código IBGE de
município**. Existe um atributo `codigoMunicipioIBGE` no schema, mas ele pertence ao
`complexType tipoOrgaoJulgador` (órgão julgador/tribunal, usado para outra finalidade já resolvida no
adapter), não a `tipoEndereco`/pessoa. Confirmado por busca exaustiva na documentação do schema MNI
2.2.2 (nenhuma outra ocorrência de "municipio"/"IBGE" no documento inteiro). Registrado aqui para que
uma leitura futura desta dívida não presuma que há um código IBGE de domicílio de parte sendo
descartado — não há.

**Risco:** MNI é o único dos 4 canais sem `comarca`/`cidade` de parte capturados; `PoloCompositionPolicy`
continua derivando `comarcaDomicilio`/`municipioDomicilio` como nulos para processos recebidos por esse
canal.

**Quando revisitar:** se `cidade` (município, texto livre) precisar ser capturado por paridade com
REST; e, separadamente, quando o domicílio de parte precisar alimentar `CompetenciaTerritorialResolver`
(que exige `municipioIbge` — hoje nenhum dos 4 canais de produção alimenta esse resolver, é lacuna
transversal, não específica do MNI).

## D-intake-workspace-endereco-nao-wireado

**Status:** aberta

**Contexto:** `PeticionamentoInitialIntakeWorkspaceService` tem `enderecoAutor`/`enderecoReu`
estruturados (com `uf`/`cidade`) em `PeticionamentoSessaoRequest` — inclusive já lê
`getEnderecoAutor().getUf()` para resolver `ufFato` — mas passa `null` para os 4 campos territoriais
de parte do `EstruturarRequest`.

**Risco:** baixo hoje. Wirear `cidade` → `comarca` seria aproximação (comarca é circunscrição
judiciária; município não é comarca — município pequeno pertence à comarca sede vizinha). Decisão
tomada: não aproximar. A resolução correta virá do catálogo de jurisdição territorial chaveado por
código IBGE (iniciativa de competência territorial por rito, Etapa 6 — adapter ViaCEP).

**Quando revisitar:** quando a Etapa 6 entregar CEP → código IBGE; aí o wiring vira exato, não
aproximado.

## D-rito-retificacao-registro-nome-ambiguo

**Status:** aberta

**Contexto:** `RitoProcessual.CIVIL_RETIFICACAO_REGISTRO` não desambigua entre retificação de registro
de imóvel (foro da situação da coisa, CPC art. 47) e retificação de registro civil de pessoa natural
(nome, nascimento — critério territorial distinto). Isso impediu o mapeamento em
`criterioTerritorial()`, que devolve `Optional.empty()` para este rito.

**Risco:** o problema não é a lacuna de mapeamento — é o nome do enum carregar dois institutos
jurídicos diferentes sob um rótulo só. Qualquer regra por rito (documentos exigidos, partes, foro)
herda a mesma ambiguidade. Dividir em dois valores distintos é mudança de vocabulário canônico, com
efeito cascata sobre catálogo e dados já gravados.

**Quando revisitar:** ao mapear o critério territorial dos ritos civis residuais (CPC art. 46), ou se
alguma regra por rito precisar tratar os dois institutos de forma diferente.

## D-vigencia-trt7-e-futuras-regioes-presumida-nao-documentada

**Status:** aberta

**Contexto:** `vigencia_inicio` das 37 unidades do TRT7/CE, das 155 unidades do TRT3/MG e das 20 unidades
do TRT21/RN (e, por padrão, das demais 21 regiões quando carregadas) usa uma data conservadora presumida
(CF/88), não a data real de criação de cada vara.

No TRT7/CE essa presunção era a única opção — o documento-fonte só confirmava jurisdição vigente na
data de publicação, sem histórico individual. No TRT3/MG **a informação real existe**: o documento
(`End03.pdf`) traz "Data de Instalação" individual e verificada para as 158 varas (cobertura 100%,
confirmada por contagem), com casos concretos de dispersão relevante — ex. Belo Horizonte tem varas
instaladas entre 1941 (1ª VT) e 2013 (45ª VT), 72 anos de diferença dentro do mesmo município. Decisão
consciente (não lacuna de pesquisa) foi manter a mesma data presumida do Ceará em vez de usar a data
real, porque o schema atual (`tb_jurisdicao_territorial`) só suporta um `vigencia_inicio` por linha de
município — não representa "este conjunto de varas cresceu ao longo de décadas", só "desde a data X,
todo o conjunto é competente" (a constraint `EXCLUDE` do schema proíbe duas linhas do mesmo município
com intervalos de vigência sobrepostos).

**Risco:** nenhum falso-negativo pra frente (o sistema não nega competência que existe), mas não há
precisão sobre desde quando cada configuração específica de jurisdição vale — se uma vara foi criada em
2015, casos de 2010 continuam resolvendo pra ela mesmo sem essa vara ter existido ainda.

**Quando revisitar:** se algum caso de uso exigir precisão histórica real (ex.: litígio sobre qual vara
era competente numa data específica no passado), considerar redesenho de `vigencia_inicio`/`vigencia_fim`
para `tb_jurisdicao_territorial_unidade` (por vara, não por município) — o TRT3/MG já tem o dado real
pronto pra popular esse redesenho quando ele acontecer, sem nova extração de PDF.

## D-trt3-codigo-unidade-duplicado-fonte

**Status:** aberta

**Contexto:** o problema apareceu duas vezes em duas regiões distintas, cada vez por uma causa
diferente — não é um incidente isolado do TRT3, é um padrão de qualidade do dado que se repete e deve
ser esperado nas próximas regiões.

No TRT3/MG (`End03.pdf`), 3 pares de varas fisicamente distintas compartilham o mesmo "Código atribuído
pelo TRT": `0031` (3ª e 5ª VT de Contagem), `0070` (2ª VT de Ouro Preto e 1ª VT de Passos) e `0142`
(5ª VT de Betim e 2ª VT de Uberaba) — sem causa aparente, parece erro pontual de atribuição.

No TRT21/RN (`End21.pdf`), mais 3 pares: `0011` (1ª VT de Mossoró e 11ª VT de Natal), `0012` (2ª VT de
Mossoró e 12ª VT de Natal) e `0013` (3ª VT de Mossoró e 13ª VT de Natal) — aqui a causa é identificável:
Natal numera suas 13 varas sequencialmente (0001–0013) e Mossoró numera as suas 4 (0011–0014) na mesma
faixa, sem que as duas séries tenham sido unificadas.

Em ambos os casos, confirmado por leitura direta do texto extraído — os registros são completos e bem
formados, com endereço e e-mail institucional distintos entre si; não é artefato de parsing. Carregado
como está nas duas regiões, decisão consciente do usuário nos dois casos.

**Risco:** `TRT3-0070` e `TRT21-0011` (por exemplo) apontam simultaneamente para duas varas físicas
diferentes — identificador de vara ambíguo nesses casos específicos. Onde as duas varas com código
duplicado atendem exatamente o mesmo conjunto de municípios (caso de Contagem no MG), o
`Set<String> unidadesElegiveis` colapsa as duas em uma entrada só — a carga não perde competência
territorial nenhuma, mas perde a informação de que existiam originalmente 2 varas ali com códigos que
deveriam ser distintos. Onde os conjuntos de municípios diferem (caso de Mossoró/Natal no RN), o código
duplicado aparece nas duas linhas de município normalmente, cada uma com seu próprio conjunto de
unidades — a ambiguidade fica restrita a "qual vara física esse código identifica", não à cobertura
territorial.

**Quando revisitar:** se o TST/TRT3/TRT21 publicar uma revisão do documento-fonte corrigindo a
duplicidade, ou se algum fluxo precisar citar univocamente uma dessas varas (ex.: intimação, mandado) —
nesse caso a resolução exige fonte primária adicional (ex.: consulta direta ao tribunal), não inferência.
Ao carregar as próximas regiões, checar duplicidade de código já na primeira rodada de auditoria, não
como complemento posterior.

## D-trt3-municipios-sem-vara-competencia-delegada

**Status:** aberta

**Contexto:** 6 municípios de MG (Capitólio, Doresópolis, Guapé, Piumhi, São Roque de Minas, Vargem
Bonita) não aparecem em nenhuma jurisdição de vara no documento-fonte do TRT3 — confirmado por busca
textual nas 70 páginas do PDF, nenhuma ocorrência. Não foram carregados na V305.

**Risco:** consulta territorial para esses 6 municípios devolve `MunicipioForaDoCatalogo`, quando a
hipótese mais provável é que exista competência trabalhista real por delegação ao juiz de direito da
comarca local (CLT art. 668 c/c CF art. 112 — mecanismo usado onde não há Vara do Trabalho instalada),
não ausência de competência. O documento usado (`End03.pdf`, cadastro de Varas do Trabalho) não cobre
esse tipo de competência delegada por desenho — não é uma lacuna de extração.

**Quando revisitar:** se a Etapa territorial precisar cobrir `modo_competencia = 'DELEGADA_JUIZ_DIREITO'`
(já suportado pelo schema desde a V302) — nesse caso, buscar fonte específica de comarcas com
competência trabalhista delegada, provavelmente no TJMG, não no TST.

## D-trt21-posto-avancado-sem-codigo

**Status:** aberta

**Contexto:** o documento-fonte do TRT21/RN (`End21.pdf`) cadastra 2 unidades do tipo "Posto Avançado",
categoricamente diferentes de Vara do Trabalho, e nenhuma das duas recebe "Código atribuído pelo TRT" —
esse campo, no documento, só existe para VTs.

O "Posto Avançado da Justiça do Trabalho em Pau dos Ferros" tem endereço, e-mail e jurisdição própria e
exclusiva (38 municípios — Pau dos Ferros e mais 37 — confirmados por busca textual como não citados em
nenhuma outra unidade do documento), mas sem código, sem "Criação" e sem "Data de Instalação". Não foi
carregado na V306; os 38 municípios não entram no catálogo.

O "Posto de Atendimento Avançado da Zona Norte" também não tem código, mas sua jurisdição não gera a
mesma lacuna: cobre bairros específicos de Natal (Igapó, Salinas, Potengi, Nossa Senhora da Apresentação,
Lagoa Azul, Pajuçara, Redinha) — granularidade abaixo de município, que `tb_jurisdicao_territorial`
(chaveada por `municipio_ibge`) não tem como representar independente de o código existir ou não — mais
os municípios Extremoz e São Gonçalo do Amarante, que já estão cobertos pelas 13 VTs regulares de Natal
(confirmado: as 13 compartilham jurisdição idêntica). Nenhuma cobertura de município se perde ao não
carregar este segundo Posto.

**Risco:** consulta territorial para os 38 municípios da jurisdição de Pau dos Ferros devolve
`MunicipioForaDoCatalogo`, apesar de existir unidade real, documentada e endereçada atendendo-os —
diferente do caso dos 6 municípios do MG (lá não havia nenhuma unidade documentada), aqui a unidade
existe mas não tem o identificador que o resto do catálogo usa como chave (`unidade_codigo` no
padrão `TRT{N}-{código}`).

**Quando revisitar:** se o TRT21 publicar cadastro com código formal para Postos Avançados, ou se algum
caso de uso exigir cobertura desses 38 municípios — nesse caso, decidir entre buscar o código em fonte
primária adicional (site do TRT21) ou adotar convenção própria de identificador não oficial, com anotação
explícita distinguindo-o de um "Código atribuído pelo TRT" real (decisão de produto, não técnica).

## D-resolve-9-params-posicionais

**Status:** aberta — nota de fragilidade, não bloqueia nada

**Contexto:** `RepresentacaoProcessualPolicyService.resolve(Processo, Usuario, String, Long, String,
boolean, boolean, String, String)` tem 9 parâmetros posicionais. `RecursalValidacaoMinimaService
.elegivelPorJusPostulandi()` chama essa sobrecarga passando `processo, usuario, null, null, null,
false, false, null, null` — 7 `null`/`false` seguidos. Hoje isso funciona porque os 10 call sites
existentes (confirmado por grep: `IAJuridicaV1`, `LaianePeticaoInicialDraftService`,
`LaianeNationalPreflightService`, `LaianeLawyerService` — 2 call sites —, `JuizGabineteDecisionalService`,
`ProcessualParticipacaoAtivaWorkspaceSupport`, `PeticionamentoSessaoFacadeService`,
`RecursalValidacaoMinimaService` e `RecursalFormalizacaoService`) já respeitam a ordem atual, mas o
método não tem nenhuma proteção de tipo entre os `null` posicionais: se a assinatura for reordenada
ou ganhar/perder um parâmetro do mesmo tipo (`String`/`boolean`), o compilador não acusa erro — o
call site continua compilando e passa valor errado para o parâmetro errado, silenciosamente.

**Risco:** regressão silenciosa em qualquer um dos 10 call sites se `resolve()` for refatorado sem
atualizar todos eles em lockstep. É particularmente provável que uma etapa futura sobre
representação processual (ex.: adicionar sinal de "é recurso" para fechar o enforcement do art. 41,
§2º sem depender de allowlist por `LegalAppealType` em `RecursalValidacaoMinimaService`) mexa nesta
assinatura.

**Quando revisitar:** ao tocar `resolve()` de novo — considerar um `record` de request
(`RepresentacaoProcessualPolicyRequest`) ou builder no lugar dos parâmetros posicionais, migrando os
10 call sites de uma vez. Não vale a pena isolado, só quando a assinatura for mexida por outro motivo.

## D-jus-postulandi-recurso-jef-turma-recursal

**Status:** parcialmente atendida — enforcement do comportamento atual agora é verificado por teste;
a pergunta jurídica de fundo (o que a Lei 10.259/2001 realmente exige) segue em aberto,
deliberadamente não respondida nesta etapa.

**Contexto:** `RecursalValidacaoMinimaService.JEF_JUS_POSTULANDI_APPEAL_TYPES` contém apenas
`EMBARGOS_DECLARACAO`. Um CIDADAO com `JUS_POSTULANDI_JEF` fica barrado em dois tipos, mas por
motivos diferentes um do outro:
- **`RECURSO_INOMINADO`** (compartilhado entre JEC estadual e JEF no catálogo `LegalAppealType`) *tem*
  mapeamento processual e passa pela checagem de legitimidade — o bloqueio é enforcement real da
  allowlist, e já tinha teste de regressão mesmo antes desta etapa
  (`cidadaoNoJuizadoEspecialFederalNaoPodeInterporRecursoInominadoSemAdvogado`).
- **`PEDIDO_UNIFORMIZACAO`** (incidente de uniformização à Turma Nacional de Uniformização, específico
  do microssistema federal, sem equivalente no JEC) *não* tem entrada em `toRecursoProcessualTipo()` —
  cai em `"Tipo recursal sem correspondencia processual minima."` para qualquer ator, advogado
  incluído. Esse é o mesmo padrão de bloqueio acidental por lacuna de mapeamento que
  `D-jus-postulandi-recurso-tst` documentou para `RECURSO_REVISTA`, não uma decisão da allowlist.

**Risco (ainda aberto, não resolvido por esta etapa):** o bloqueio de `RECURSO_INOMINADO` foi adotado
por analogia conservadora ao regime do JEC (Lei 9.099/95, art. 41, § 2º), **não** por verificação do
que a Lei 10.259/2001 efetivamente exige. A Lei 10.259/2001 remete subsidiariamente à Lei 9.099/95
(art. 1º), mas tem regime recursal próprio — Turma Recursal Federal e incidente de uniformização
(arts. 14 e 15) não existem no juizado estadual. Se a exigência de advogado no recurso federal for
menos estrita do que a estadual, o sistema está negando um direito processual que a parte teria; se
for igual ou mais estrita, o bloqueio está certo por acidente. Nenhuma das duas hipóteses foi
confirmada contra a lei — decisão explícita de não resolver essa pergunta jurídica nesta etapa
(exige leitura da lei/jurisprudência da TNU, fora do escopo de uma investigação de código).

**Fechamento parcial:** 2 testes novos em `RecursalValidacaoMinimaServiceTest` provam que o par
cidadão/advogado em `PEDIDO_UNIFORMIZACAO` bate no mesmo erro de mapeamento ausente — não é
específico de jus postulandi. O comportamento atual (nega mais do que talvez devesse, nunca
permissivo demais) agora está travado por teste de regressão, não só por composição acidental de
lacunas.

**Quando revisitar:** antes de qualquer promessa de cobertura completa do JEF na banca ou em
produção — verificar o texto da Lei 10.259/2001 (arts. 10, 14 e 15) e a jurisprudência da TNU sobre
capacidade postulatória na fase recursal, e então ou ampliar `JEF_JUS_POSTULANDI_APPEAL_TYPES` com
fundamento explícito, ou registrar o bloqueio atual como enforcement deliberado com base legal
citada. Se `PEDIDO_UNIFORMIZACAO` ganhar mapeamento em `toRecursoProcessualTipo()` no futuro,
adicionar teste explícito confirmando que jus postulandi JEF continua barrado nele (mesmo padrão que
`RECURSO_INOMINADO` já tem).

**Tentativa de pesquisa registrada (não fechou a pergunta):** buscou-se o texto literal dos arts. 10,
14 e 15 da Lei 10.259/2001 e do Regimento Interno da TNU (Resolução CJF nº 586/2019) para responder
definitivamente. Toda tentativa de acesso direto a fonte primária (`planalto.gov.br`, PDFs de
`trf3.jus.br`/`trf1.jus.br`, `cjf.jus.br`) falhou por erro de conexão do ambiente ou por PDF não
extraível como texto — não foi possível citar o artigo exato. Uma busca indireta encontrou um
resultado (não verificado contra o RITNU original) afirmando que capacidade postulatória é exigida
para atuar perante Turmas Recursais e a TRU, o que é consistente com o bloqueio atual — mas por vir
de resumo de busca, não de leitura direta do regimento, não é fundamento citável o suficiente para
mudar o status desta dívida de "parcialmente atendida" para fechada. Permanece em aberto.

## D-custas-jec-isencao-primeiro-grau

**Status:** parcialmente atendida — a política de isenção passou a existir em
`CustaIsencaoPorRitoPolicy`, cobrindo JEC (Lei 9.099/95 art. 54), JEF (Lei 10.259/2001) e JEFP
(Lei 12.153/2009) em primeiro grau, e preservando a regra pré-existente do ramo
`INFANCIA_JUVENTUDE`. A dívida original visava o efeito patrimonial sobre a parte no ajuizamento,
que continua não acontecendo porque o motor `CustaJudicialService` segue desconectado do fluxo —
o que muda a natureza do restante em aberto, agora rastreado por
`D-motor-custas-nao-integrado-ao-ajuizamento`.

**Contexto original (mantido para rastreabilidade):** o art. 54 da Lei 9.099/95 dispensa custas no
acesso ao Juizado Especial em primeiro grau. Grep por `custas`/`gratuidade`/`preparo`/`isenção`
cruzado com o JEC em `service/` e `core/` não retornava nenhuma política que modelasse essa
isenção. `preparoDispensado` existe como parâmetro dos quatro controllers recursais profissionais,
mas nunca é ligado ao fluxo do cidadão; `RecursoProcessualTipo.exigePreparo()` já devolve `false`
para `RECURSO_INOMINADO_JEC`, o que cobre a fase recursal e deixava a inicial descoberta.

**Risco residual:** o cidadão liberado no JEC pelas etapas de jus postulandi ainda depende da
integração do motor de custas ao ajuizamento para que a nova política produza efeito prático.
Enquanto essa integração não acontece, a nova política é defensiva — garante resposta correta
quando alguém ligar o motor, mas não cobra nem isenta ninguém por si só.

**Quando revisitar:** ao encaminhar a integração do motor de custas ao ajuizamento
(`D-motor-custas-nao-integrado-ao-ajuizamento`). Etapas correlatas registradas:
`D-custas-fazenda-publica-pagamento-diferido`, `D-custas-dois-modulos-nao-integrados`,
`D-custas-interface-recebe-string-em-vez-de-enum`.

## D-motor-custas-nao-integrado-ao-ajuizamento

**Status:** aberta — motor pronto, integração pendente por decisão de política de negócio

**Contexto:** o módulo `core/financeiro/custas/` tem `CustaJudicialService` com geração de GRU e
PIX, ledger de auditoria (`CUSTA_GERADA`, `CUSTA_ISENCAO`), tabelas `pjb_custa_judicial` (V196) e
`pjb_custas_processual` (V247), controller admin (`/api/v1/admin/custas`) com nove endpoints e a
nova `CustaIsencaoPorRitoPolicy`. Nenhum dos quatro canais de ajuizamento (REST, Laiane,
Marketplace, MNI) chama esse motor: `AjuizamentoService.ajuizar()` executa `processoRepo.save` +
polos + outbox + evento e encerra, sem passar por `CustaJudicialService.gerarCustas`.

**Risco:** enquanto o motor não é integrado, ninguém é cobrado pelo ajuizamento — incluindo autor
de ação cível comum, que deveria pagar. Quando a integração for feita, ela passa a cobrar todos
os ritos não-isentos ao mesmo tempo. `GruCodigoBarrasGenerator` e `PixPayloadGenerator` atuais
geram valores simulados (hash + payload EMV mock), não conversam com PSP nem tribunal; ligar ao
ajuizamento hoje começaria a emitir guias inválidas para todo mundo.

**Quando revisitar:** só depois de acordo/convênio real com PSP e definição do modelo de repasse
ao tribunal. Não antes. A integração em si é uma linha em `AjuizamentoService.ajuizar()`; o custo
está no que ela expõe, não na chamada.

## D-custas-fazenda-publica-pagamento-diferido

**Status:** aberta — não é isenção; é modelagem de fluxo de cobrança

**Contexto:** o art. 91 do CPC dispõe que as despesas dos atos processuais praticados a
requerimento do Ministério Público ou da Fazenda Pública são pagas ao final pelo vencido. Isso é
**pagamento diferido**, não isenção — a Fazenda não está desobrigada de arcar com o custo se
sucumbente. Modelar isso dentro de `IsentoCustaPolicy` (retornando `isento(true)`) seria erro
jurídico com efeito patrimonial: a Fazenda vencida deixaria de pagar o que deve.

**Risco:** quando o motor de custas for integrado ao ajuizamento
(`D-motor-custas-nao-integrado-ao-ajuizamento`), o fluxo precisa distinguir três casos — isento
por lei, pagamento adiantado obrigatório e pagamento diferido ao final por sucumbência. Hoje o
motor só distingue os dois primeiros (via `IsentoCustaPolicy`).

**Quando revisitar:** junto com a integração do motor. Exige campo próprio no domínio de custas
(`pagamentoDiferido`, `responsavelFinal`) e regra pós-sentença, não vale mexer isolado.

## D-openapi-anotacoes-ausentes-em-controllers

**Status:** aberta — transversal, não bug ativo

**Contexto:** nenhum controller do PJB usa `@Operation` ou `@Tag` (`io.swagger.v3.oas.annotations`)
hoje. O springdoc-openapi gera a spec por análise automática, sem descrições, sem exemplos, sem
nomes de tag consistentes. A superfície unificada `RecursalPeticionamentoController` (Etapa 1 de
`D-recursal-superficie-por-papel`) foi analisada como candidata a receber `@Tag`/`@Operation`
isoladamente e a decisão explícita foi **não fazer**: adicionar anotação Swagger em um único
controller entre 200+ pioraria a consistência do projeto sem resolver a qualidade real da spec.

**Risco:** contrato público sem semântica descritiva, dificultando consumo por integradores futuros
(clientes SDK auto-gerados, portais de terceiros, ferramentas de importação OpenAPI). Não bloqueia
funcionalidade, mas empobrece a documentação executável que o PJB expõe.

**Quando revisitar:** etapa própria de documentação de API, tratando todos os controllers de uma
vez com padrão consistente (tag por área de domínio, `@Operation` com `summary` curto e
`description` mais longo, exemplos em DTOs via `@Schema`), não parcelado por endpoint novo.
Anti-padrão: aplicar caso-a-caso à medida que novos endpoints nascem — cria duas classes de
controllers no mesmo projeto e nunca converge. `RecursalPeticionamentoController` é candidato
natural a primeiro alvo dessa etapa transversal.

## D-salario-minimo-hardcoded-fora-de-gratuidade

**Status:** parcialmente atendida — 3 dos 5 pontos fechados nesta etapa; 2 pontos permanecem
abertos como dívidas próprias (`D-national-rule-pack-engine-sem-data-referencia` e
`D-quadro-credores-recuperacao-marco-nao-pesquisado`).

**Contexto:** investigação transversal em `pjb-api/src/main/java` mapeou 5 pontos que instanciavam
salário mínimo fora do serviço canônico `SalarioMinimoNacionalService`, complementares ao
`D-salario-minimo-hardcoded-em-gratuidade` já FECHADA (que cobria apenas
`JusticaGratuidaVerificadorService`). Os pontos eram: (i) constante literal `"1412.00"` em
`FalenciaDecretacaoService` (limite de impontualidade — Lei 11.101/2005 art. 94 I, que fixa o SM
"na data do pedido de falência"); (ii) constante literal `"1412.00"` em
`QuadroGeralCredoresAssemblerService` (limite trabalhista da falência — Lei 11.101/2005 art. 83 I);
(iii) duas strings literais `"1518.00"` no catálogo de exemplo do frontend em
`CalculoJudicialFrontendCatalogService`; (iv) `valorPorAno(2025)` e `valorPorAno(2026)` literais
no painel comparativo de `CalculoJudicialEconomicReferenceService`; (v) duas chamadas
`multiplicar(..., LocalDate.now())` em `NationalRulePackEngine` para calcular os tetos de
competência do JEC (40 SM) e JEF (60 SM).

**Correção aplicada:**
- **FalenciaDecretacaoService** — injetado `SalarioMinimoNacionalService`, `FalenciaInput`
  recebeu `LocalDate dataPedido` obrigatória (`Objects.requireNonNull`, sem fallback para
  `LocalDate.now()`), constante `VALOR_SALARIO_MINIMO` removida e substituída por
  `salarioMinimoNacionalService.multiplicar(LIMITE_IMPONTUALIDADE_SALARIOS_MINIMOS, dataPedido)`.
  Teste de regressão `limiteDe40SalariosUsaDataDoPedidoNaoDataAtual` prova aritmeticamente com SM
  histórico (2025 → R\$ 1.518,00, limite R\$ 60.720,00) que o valor da data efetivamente determina
  o limite, e teste `dataPedidoNulaFalhaExplicitamenteEmVezDeCairEmDataAtual` prova o `NullPointerException`
  com mensagem `"dataPedido"` — sem fallback silencioso que reintroduziria o hardcode por outro caminho.
- **CalculoJudicialFrontendCatalogService** — injetado `SalarioMinimoNacionalService` (5ª dep no
  construtor, único call site explícito é o próprio teste unitário), 2 literais `"1518.00"`
  substituídos por `valorVigente().toPlainString()`. Teste
  `salarioMinimoReferenciaVemDoServiceCanonicoNaoDeLiteralAntigo` mocka o service com valor
  distinto do antigo hardcode e prova que payloadInicial + requestExemplo do bootstrap
  `FEDERAL_PREVIDENCIARIO_CJF` refletem o valor mockado.
- **CalculoJudicialEconomicReferenceService** — `valorPorAno(2025)` e `valorPorAno(2026)`
  substituídos por `valorPorAno(hoje.getYear() - 1)` e `valorPorAno(hoje.getYear())`, com `hoje`
  já disponível no método. Decisão de janela documentada: (ano anterior + ano corrente) evita
  cair no fallback do próximo ano sem decreto publicado, o que exporia dois valores idênticos
  rotulados como anos diferentes. Constantes de metadata (`FONTE_SALARIO_2026`, `FONTE_INSS_2026`,
  `TETO_INSS_2026`) mantidas — são referências a normas específicas, não valor monetário do SM.
  Teste `janelaComparativaChamaAnoAnteriorEAnoCorrenteDerivadosDeLocalDateNaoLiterais` verifica
  as chamadas por `ArgumentMatchers` derivados de `LocalDate.now().getYear()`, sem fixar anos
  literais que ficariam errados no futuro.
- **DTO `CalculoJudicialSalarioMinimoDto`** — campos ainda nomeados `referencia2025`/`referencia2026`,
  o que ficará semanticamente incorreto no ano seguinte. Não renomeado nesta etapa porque é
  breaking change de contrato consumido pelo frontend; registrado como observação para etapa
  futura de generalização de contrato (`referenciaAnoAnterior`/`referenciaAnoCorrente`).

**Guard de regressão:** `salario_minimo_hardcoded_guard.py` (bridge em `scripts/`, corpo em
`tooling/python/scripts/`) detecta 5 padrões: literal `1XXX.00` próximo a identificador de SM,
literal em entry de Map com chave `salarioMinimo*`, declaração de `static final BigDecimal
SALARIO_MINIMO*`, chamada `valorPorAno(literal)`, e `LocalDate.now()` inline em chamada ao service
canônico. Whitelist explícita do `SalarioMinimoNacionalService.java` (fonte canônica com
`FALLBACK_OFICIAL` legítimo). Sem mecanismo de allowlist inline — nenhuma convenção prévia no
projeto e a etapa optou por não inventar. Exit 1 documentado enquanto as duas dívidas próprias
não forem resolvidas.

**Risco original:** valores monetários congelados em pontos de cálculo relevantes (falência,
recuperação judicial, catálogo de frontend, painel comparativo), com correção requerendo
atualização manual arquivo-a-arquivo todo ano em vez de sincronização automática via
`SalarioMinimoNacionalSyncScheduler`. Impacto direto: cálculo pode negar competência a JEC/JEF em
casos limite, exibir catálogo desatualizado, ou aplicar teto trabalhista/impontualidade com valor
de anos anteriores.

## D-national-rule-pack-engine-sem-data-referencia

**Status:** aberta — dívida arquitetural, não bug ativo (achado transversal de etapa de
`D-salario-minimo-hardcoded-fora-de-gratuidade`, extraído para tratamento próprio)

**Contexto:** `NationalRulePackEngine.inferDynamicRules(ContextoRegra ctx)` chama
`salarioMinimoNacionalService.multiplicar(new BigDecimal("40"), LocalDate.now())` (linha 418, teto
JEC) e `salarioMinimoNacionalService.multiplicar(new BigDecimal("60"), LocalDate.now())` (linha 430,
teto JEF). A regra jurídica pede **data do ajuizamento** (Lei 9.099/95 art. 3º I; Lei 10.259/2001
art. 3º) — o valor da causa deve ser aferido no momento da propositura, não no momento em que a
regra é avaliada. O `record ContextoRegra` (linhas 34-40) carrega `classeTPU`, `assuntoTPU`,
`ramo`, `grau`, `tribunalCodigo`, `extras`, mas **nenhum campo de data**. O `Map<String, Object>
extras` já transporta `valorCausa`; poderia transportar `dataAjuizamento` também, mas hoje não
transporta e o engine cai no `LocalDate.now()` por falta de alternativa disponível.

**Risco:** ao virar de ano, causas ajuizadas em dezembro do ano anterior podem ser reclassificadas
como JEC/JEF por chamada da regra em janeiro do ano corrente com valor de causa que era limítrofe.
Baixa probabilidade, mas mancha o motor com uma decisão temporal errada por construção. Também
mascara o fato de que o SM da data do ajuizamento seria diferente — regra jurídica correta viraria
"foi JEC no momento do ajuizamento" e não "é JEC agora".

**Quando revisitar:** etapa arquitetural própria. Alteração exige adicionar `LocalDate
dataReferencia` ao `record ContextoRegra`, o que cascateia por 28 arquivos consumidores
(`JurimetriaEngine`, `NationalColegiadoEngine`, `CejuscEngine`, `CooperacaoJuridicaEngine`,
`ImpedimentoSuspeicaoEngine`, `NotificacaoInteligentePJB`, `TransparenciaCnjEngine`, `LoadPlan`,
`PluginSnapshot`, `PluginResolucaoTribunalService`, `TribunalRuleEngine`,
`TribunalRulePackSynchronizationSupport`, `TribunalRuleResolutionSupport`, além dos testes). O
guard `salario_minimo_hardcoded_guard.py` detecta as duas ocorrências e permanecerá reportando-as
com `exit=1` documentado até o fechamento desta dívida.

## D-quadro-credores-recuperacao-marco-nao-pesquisado

**Status:** aberta — bloqueio de segurança sobre `QuadroGeralCredoresAssemblerService`, gate
levantado pela Fase 0 da etapa de `D-salario-minimo-hardcoded-fora-de-gratuidade`.

**Contexto:** o service `QuadroGeralCredoresAssemblerService` foi analisado como candidato a
receber `LocalDate dataDecretacao` e passar a consultar o `SalarioMinimoNacionalService` para o
limite trabalhista de 150 SM por credor (Lei 11.101/2005 art. 83 I). **Se o service fosse
exclusivo de falência**, o critério "data da decretação da falência" seria o majoritariamente
aceito pela jurisprudência estadual (ausente precedente do STJ especificamente sobre o marco),
com fundamento na consolidação do quadro geral pelo administrador judicial e no princípio da
par conditio creditorum — essa é a base doutrinária que orientaria a implementação. **Mas o
service não é declaradamente exclusivo de falência**, e a investigação leu o arquivo completo do
assembler e confirmou:

- nenhum parâmetro, campo ou enum distingue falência × recuperação judicial;
- o observation gerado cita apenas "Lei 11.101/2005 arts. 83 e 149" (art. 149 é ordem de pagamento
  pós-realização do ativo, específico de falência);
- o único teste (`quadroGeralOrdenadoPorClasse` em `RecuperacaoJudicialFalenciaTest`) cobre apenas
  ordenação, sem cenário RJ vs falência;
- **zero call sites em produção** (mesmo perfil de `JusticaGratuidaVerificadorService`).

O art. 83 rege falência; em recuperação judicial as classes de credores são reaproveitadas por
remissão via art. 41, mas o evento-marco temporal em RJ **não é "decretação"** (que só existe em
falência) — pode ser deferimento do processamento, concessão da recuperação, ou outra decisão
específica. **Marco temporal para RJ não foi pesquisado nesta etapa.**

**Risco:** como o service não impõe barreira arquitetural contra reuso em RJ (aceita qualquer
`List<Credor>` sem verificar tipo de processo), qualquer implementação futura da Fase 3 com
"data da decretação" hardcoded como semântica única pode ser silenciosamente incorreta em cenário
de RJ. Aplicar critério de falência em RJ, ou vice-versa, reintroduz a mesma ambiguidade que a
etapa atual resolveu para o outro service.

**Quando revisitar:** etapa própria com pesquisa jurídica prévia sobre o marco temporal do SM em
recuperação judicial. Antes de escrever código: (a) pesquisar art. 54 c/c 83 da Lei 11.101/2005 no
contexto de RJ; (b) confirmar precedente ou doutrina sobre marco em RJ; (c) decidir se o service
deve receber enum discriminador (`TipoProcesso.FALENCIA` / `TipoProcesso.RECUPERACAO`) para
resolver marco diferente por caminho, ou se são dois services distintos. O guard
`salario_minimo_hardcoded_guard.py` continua reportando as 2 ocorrências (constante literal +
declaração de constante) até fechamento.

## D-scheduler-salario-minimo-nunca-ativado

**Status:** aberta — decisão de ativação pendente; defasagem agora observável

**Contexto:** `SalarioMinimoNacionalSyncScheduler` existe com cron diário e consumiria a série 1619
do Banco Central, mas está atrás de `@ConditionalOnProperty(pjb.sync.salario-minimo.enabled)` sem
`matchIfMissing`, e a propriedade não é definida em nenhum `application*.yml`. Nenhuma migration
semeia `salario_minimo_nacional`. Consequência: toda consulta cai no `FALLBACK_OFICIAL` estático.

**Corrigido nesta etapa:**

- O fallback para ano além da tabela usava `reduce((a, b) -> b)` sobre `FALLBACK_OFICIAL`, que é um
  `Map.copyOf` — cuja ordem de iteração o javadoc declara **indefinida e sujeita a mudança**. Não era
  bug ativo (probe confirmou que hoje a ordem sai ascendente e o resultado é correto), mas era
  fragilidade latente: acrescentar um ano ou trocar de JDK poderia alterar o valor em silêncio.
  Passou a usar `max(Map.Entry.comparingByKey())`, que é o mesmo critério que
  `anoMaisRecenteConhecido()` já usava na mesma classe.
- `SalarioMinimoStalenessWatchdogService` só registrava `log.warn`. Ganhou o gauge
  `pjb.salario_minimo.defasagem_anos`, que expõe a defasagem corrente mesmo quando ela está **abaixo**
  do limiar de alerta. Inicia em `-1` para distinguir "watchdog ainda não rodou" de "defasagem zero".

**Achado do revisor — interação entre duas dívidas que nenhuma descreve sozinha:**

O limiar `defasagemAnos > 1` do watchdog é decisão documentada e correta em sua premissa: em janeiro
o valor do ano novo pode legitimamente ainda não estar cadastrado enquanto o decreto sai, e alertar
em `defasagem == 1` produziria falso positivo todo início de ano.

Mas essa premissa supõe que **alguém cadastra o valor durante o ano**. Como o sync nunca é ativado e
não há seed, o que seria um transitório de janeiro vira permanente — e a tolerância desenhada para o
transitório passa a cobrir a condição permanente por um ano inteiro sem alerta.

Não alterei o limiar: a decisão está fundamentada e a instrução registrada é não revisitá-la sem
mudança no padrão de publicação do decreto. O gauge resolve o sintoma (a defasagem tolerada agora é
visível); a causa é a ativação da fonte.

**Decisão pendente do Tiago:** ativar o sync contra o Banco Central (exige rede de saída em produção)
ou semear `salario_minimo_nacional` por migration com os valores oficiais. A segunda opção é
determinística e não depende de rede, mas exige atualização manual por decreto.

## D-frontend-delivery-routes-nao-sinaliza-depreciacao

**Status:** aberta — varredura por regex, não sinaliza depreciação de rota

`PjbFrontendDeliveryApplicationService.parseRoutes` escaneia `@PostMapping`/`@GetMapping` via regex e não lê headers HTTP de depreciação — os 4 endpoints recursais legados aparecem no catálogo `/api/v1/frontend/delivery/routes` com o mesmo peso da rota unificada nova, achado ao investigar consumidores antes da Etapa 3.
Revisitar se o catálogo vier a ser consumido por um frontend real: cruzar rota com `RecursalLegacyDeprecationHeaders` ou marcador equivalente antes de expor como pronta para uso.

## D-auditoria-ledger-falha-de-persistencia-sem-retry

**Status:** aberta — observabilidade corrigida; decisão de retry/outbox pendente

`AuditLedgerService.persistSafely` engole exceção de persistência por contrato: são **425 pontos de
chamada** e nenhum ato judicial deve falhar porque a auditoria falhou. Isso permanece correto e não
está em discussão.

**Correção aplicada:** a entrada anterior deste registro afirmava que o `Counter`
`pjb.audit_ledger.persist_failures` era incrementado no `catch`, "tornando a falha observável". Isso
era falso — o contador era declarado e construído, mas **nunca incrementado em lugar nenhum da
classe**. A métrica lia zero permanentemente, o que é pior que não existir: um painel em zero dá
confiança de que nenhuma entrada está se perdendo.

O incremento passou a existir de fato. O log subiu de `WARN` para `ERROR` e passou a registrar
identidade reconstituível — `action`, `resourceType`, `resourceId`, `payloadHash`, `entryHash` e
`prevHash` — sem `description`, `justificativa`, IP ou user agent, para não vazar conteúdo.

**Por que os hashes importam:** `AuditLedgerEntry` tem `prevHash`/`entryHash` e a entrada entra na
cadeia em memória **antes** de persistir. Uma entrada perdida não perde só um registro: deixa a
próxima apontando para um elo ausente. Com os hashes no log, o elo é reconstituível.

**O que continua aberto:** decidir se falha de persistência de evento crítico deve alimentar
retry ou outbox. O projeto tem padrão outbox e o README promete "zero perda de evento em falha de
commit", o que hoje não vale para o ledger de auditoria.

A dificuldade real: os 425 eventos são heterogêneos, de `PDF_WATERMARK_ISSUED` a auditoria de
comunicação institucional. Roteamento indiscriminado para outbox é caro e desnecessário para a
maioria; roteamento seletivo exige classificar criticidade por evento, que é trabalho de domínio,
não de infraestrutura.

**Verificado nesta correção:** varredura em `pjb-api/src/main` não encontrou nenhum outro `Counter`
declarado e nunca incrementado. O problema era isolado, não sistêmico.

## D-marketplace-payload-multiplo-anexo

**Status:** aberta — achado da revisão final da Fase 2 de completude documental

Achado na revisão final de branch inteiro do `D-marketplace-sem-completude-documental` Fase 2. O limite de payload da rota `marketplace-institutional` (`application-api-governance.yml`) foi elevado de 2MB para 8MB — cobre com folga UM anexo no limite documentado de `DocumentContentValidator` (5MB, inflado ~1.33x pelo base64 do JSON). Mas `MarketplaceComplementoDocumentalRequest.documentos` e `MarketplaceProtocoloRequest.documentos` aceitam `List<Attachment>` sem limite de quantidade — um cliente que envie vários anexos grandes na mesma chamada ainda pode estourar o limite de payload antes mesmo de qualquer anexo individual ser validado, recebendo um erro de transporte genérico em vez do `TAMANHO_EXCEDIDO` documentado. Decisão de produto em aberto: limitar quantidade de anexos por chamada, ou elevar o limite de payload proporcionalmente (custo: janela maior para abuso de banda). Não corrigido nesta fatia — corrigir exigiria decidir o número real de anexos esperado por chamada, que não está especificado em nenhum lugar do contrato atual.

## D-marketplace-connectorclientid-sem-backfill-para-janela-entre-commits

**Status:** aberta — janela entre commits sem backfill em V309

Achado na revisão da correção do finding B (checagem de posse). A migração `V309__processo_connector_client_id.sql` adiciona a coluna `connector_client_id` sem backfill. Isso é seguro para dados anteriores ao commit `c5203968` (que introduziu o endpoint `/documentos` inteiro), mas esse mesmo commit já persistia `connectorProtocolReference` no formato `clientId:referencia` — teoricamente, qualquer `Processo` protocolado entre `c5203968` e a correção (`5b1551c9`) fica com `connector_client_id = null` e nunca mais alcança `complementar()` (404 permanente, sem caminho de remediação operacional). Não corrigido porque não há dado real nessa janela: a branch nunca foi implantada em produção entre esses dois commits — ambas as migrações chegam juntas no primeiro deploy real da fatia. Revisitar apenas se algum dia esses dois commits forem implantados separadamente (não é o plano atual).

## D-ha-replica-topology-verifier-sem-replica-real

**Status:** aberta

**Contexto:** achada durante a verificação de boot completo de `D-ha-pgbouncer-prepared-statements` (mesma investigação, causa diferente). `PjbReplicaTopologyVerifier` valida no startup que o datasource de leitura é uma réplica física real (`select pg_is_in_recovery()` deve retornar `true`), gate controlado por `pjb.datasource.routing.verify-topology-on-startup` (default `true`, não sobrescrito em `docker-compose.ha.yml`). Nessa topologia local, `pgbouncer-ro` aponta pro mesmo Postgres single-node que `pgbouncer-rw` (`PJB_PGBOUNCER_RO_DB_HOST:-postgres`, mesmo host) — não existe réplica física de streaming configurada em `docker-compose.ha.yml`. `pg_is_in_recovery()` portanto sempre retorna `false`, e o verifier derruba a aplicação (`IllegalStateException`) poucos segundos depois de `Started BackendApplication`, entrando em loop de restart (`restart: on-failure:5`) até esgotar as tentativas.

**Risco:** alto pra rodar a topologia HA localmente de ponta a ponta (impede estabilidade indefinida do `backend`), mas não afeta produção real se lá houver uma réplica física de verdade — o verifier está fazendo exatamente o que deveria fazer dado o desenho atual do compose local.

**Cobertura de teste:** nenhuma — só descoberto rodando a topologia real, não há IT que suba `docker-compose.ha.yml` de ponta a ponta.

**Quando revisitar:** ao decidir como o dev local vai simular um read-replica de verdade (ex.: segundo Postgres com `pg_basebackup`/streaming replication, ou desabilitar o verifier via `PJB_DB_READ_VERIFY_TOPOLOGY_ON_STARTUP=false` explicitamente só em `docker-compose.ha.yml` como uma escolha deliberada e documentada, não um bug).

## D-funcao-servidor-proferir-nao-implementado

**Status:** aberta

**Contexto:** a fatia que conecta `FuncaoServidorJudiciario` ao motor ABAC real
(`PjbAuthorizationService.requireFuncaoServidorCapability(Processo, AcaoProcessualServidor)`)
fechou os 4 gates que já tinham um fluxo real chamando o motor: `CONCLUIR` (conclusão processual),
`INTIMAR` (intimação de audiência), `ARQUIVAR` e `DISTRIBUIR`. O enum `AcaoProcessualServidor`
também declara `PROFERIR`, e `FuncaoServidorJudiciario.podeProferir()` já existe e é testado
isoladamente (ex.: `DIRETOR_SECRETARIA.podeProferir()` retorna `true`), mas **nenhum endpoint ou
fluxo real do sistema chama `requireFuncaoServidorCapability(processo, AcaoProcessualServidor.PROFERIR)`**
— o caso de uso que essa capacidade representa (despacho de mero expediente praticado por
servidor, sem decisão de mérito, nos termos do art. 93, XIV da CF/88 e do art. 203, §4º do CPC) não
tem nenhuma feature construída no PJB ainda.

Diferente dos outros 4 valores do enum, `PROFERIR` hoje só existe no modelo (enum +
`possuiCapacidade()` no `switch` de `PjbAuthorizationFuncaoServidorFacade` + booleano na entidade
`FuncaoServidorJudiciario`) — não há controller, service ou comando que o invoque. Isso é
esperado e está fora do escopo desta fatia, que conecta capacidades **já existentes** à
autorização real; construir o fluxo de despacho de mero expediente por servidor é uma feature nova,
não uma conexão de fiação já pronta.

**Risco:** nenhum imediato — `PROFERIR` sem chamador não é uma porta aberta (o gate nega por
padrão na ausência de chamada, não existe bypass). O risco é de expectativa: alguém lendo o enum
ou a entidade pode presumir que a capacidade já está em uso.

**Cobertura de teste:** nenhuma direta para o caminho `PROFERIR` fim-a-fim (não existe fim-a-fim
para testar). `possuiCapacidade()` (privado em `PjbAuthorizationFuncaoServidorFacade`, chaveado por
`AcaoProcessualServidor`) é coberto isoladamente por `PjbAuthorizationFuncaoServidorFacadeTest` —
`FuncaoServidorApplicationServiceTest` **não** o toca, apesar do que a versão anterior desta
entrada afirmava. `podeProferir()` (o booleano do enum `FuncaoServidorJudiciario` em si, não o
`switch` do facade) é, esse sim, coberto diretamente por `FuncaoServidorApplicationServiceTest`
(`diretorSecretariaPoderProferirTrue`/`tecnicoJudiciarioPoderProferirFalse`, linhas 87-93), que
também cobre `verificarPermissao(String)` do próprio `FuncaoServidorApplicationService` — ver
`D-duas-tabelas-verdade-capacidade-servidor` abaixo para a duplicação entre esse método e
`possuiCapacidade()`.

## D-duas-tabelas-verdade-capacidade-servidor

**Status:** aberta

**Contexto:** a regra de negócio "quais ações um `FuncaoServidorJudiciario` pode praticar" — os 5
booleanos do enum (`podeProferir`, `podeConcluir`, `podeIntimar`, `podeDistribuir`, `podeArquivar`)
— está codificada em dois lugares paralelos:

1. `PjbAuthorizationFuncaoServidorFacade.possuiCapacidade(FuncaoServidorJudiciario, AcaoProcessualServidor)`
   (privado, chaveado pelo enum `AcaoProcessualServidor`) — é o caminho real, chamado por
   `PjbAuthorizationService.requireFuncaoServidorCapability(...)` em produção.
2. `FuncaoServidorApplicationService.verificarPermissao(FuncaoServidorJudiciario, String)` (privado,
   chaveado por `String` solto) — chamado apenas por `podeExecutar(...)`, que por sua vez não tem
   nenhum chamador real em produção, só uso em `FuncaoServidorApplicationServiceTest`. Foi mantido
   deliberadamente como API pública do service (base potencial para um endpoint administrativo
   futuro de consulta de permissão), não é código morto para remover sem decisão de produto.

**Risco:** os dois `switch` fazem o mesmo mapeamento função→ação hoje, mas nada os mantém
sincronizados. Se a regra de capacidade mudar (novo cargo no enum, nova ação em
`AcaoProcessualServidor`), quem alterar `possuiCapacidade()` pode esquecer de atualizar
`verificarPermissao()` (ou vice-versa) — a segunda tabela-verdade ficaria desatualizada em
silêncio, já que não é exercitada por nenhum fluxo real hoje.

**Cobertura de teste:** cada `switch` é coberto isoladamente por sua própria suíte
(`PjbAuthorizationFuncaoServidorFacadeTest` para o primeiro, `FuncaoServidorApplicationServiceTest`
para o segundo) — não existe teste que prove que os dois concordam entre si.

**Não revisitar sem decisão de produto:** consolidar os dois em uma única fonte de verdade (ex.:
`FuncaoServidorApplicationService` delegando ao facade, ou ambos delegando a um método único no
enum) é uma limpeza estrutural legítima, mas está fora do escopo de correção pontual — depende de
decidir se `verificarPermissao`/`podeExecutar` seguem como API pública do service ou são removidos.

## D-ponte-unidade-instituicao-sem-backfill

**Status:** aberta

**Contexto:** a fatia de designação institucional (`docs/superpowers/plans/2026-08-14-designacao-institucional-servidor.md`)
adicionou `unidade_instituicao_id` (nullable) em `tb_unidade_judiciaria_competencia`, mas nenhuma
`UnidadeJudiciariaCompetencia` existente teve a coluna preenchida — foi decisão explícita de escopo
(problema de dados, não desta fatia). Enquanto a ponte não for preenchida linha a linha, toda
designação feita numa unidade existente materializa `FuncaoServidorJudiciarioEntity` normalmente (os
gates ABAC funcionam) mas não materializa `LotacaoInstituicao` — a lacuna é aceita por design, não é
bug, mas significa que `ContextoInstitucionalResolver`/`LotacaoVisibilityPolicy` seguem sem dado real
pra essas unidades até alguém rodar o backfill.

**Risco:** nenhum gate quebra; a visibilidade institucional baseada em `LotacaoInstituicao` fica
incompleta silenciosamente até o backfill acontecer.

**Não revisitar sem decisão de produto:** decidir se o backfill é automático (matching por
nome/comarca, com risco de erro) ou manual (mais lento, mais seguro) é escopo de outra fatia.

## D-encerrar-designacao-nao-sincroniza-lotacao

**Status:** aberta

**Contexto:** `FuncaoServidorAdminController.encerrar` delega direto pra
`FuncaoServidorApplicationService.encerrar(...)` (existente, sem mudança), que encerra só a
`FuncaoServidorJudiciarioEntity`. `FuncaoServidorDesignacaoService.designarComLotacao` materializa
`LotacaoInstituicao` na designação, mas não existe caminho simétrico que a encerre — se um servidor
tiver a função encerrada, `LotacaoInstituicao.fim` permanece `null` (lotação continua "ativa" pra
`ContextoInstitucionalResolver`/`LotacaoVisibilityPolicy` mesmo sem função real na unidade).

**Risco:** visibilidade institucional pode conceder acesso baseado numa lotação que já deveria ter
terminado.

**Não revisitar sem decisão de produto:** exige decidir se `encerrar()` deve sempre encerrar a
`LotacaoInstituicao` correspondente (pode ser incorreto se o servidor tiver outra função ativa na
mesma unidade) ou se precisa de uma consulta adicional antes de decidir.

## D-secretariat-visibility-scope-nunca-populado

**Status:** aberta

**Contexto:** `SecretariatInstitutionalVisibilityService.resolveActorScope` deriva o recorte
territorial/institucional do ator (tribunal, instância, ramo, unidade) por parsing de regex sobre
`Usuario.perfil`, `Usuario.registroProfissional` e `Usuario.especialidades`, e compara UF/comarca
direto pelas colunas `Usuario.uf`/`Usuario.comarca`. Nenhum desses cinco campos é preenchido em
qualquer fluxo de cadastro ou atualização de conta hoje — busca completa por `.setUf(`, `.setComarca(`,
`.setPerfil(` e `.setRegistroProfissional(` sobre `Usuario` em todo o `pjb-api/src/main` não encontrou
nenhuma chamada. Na prática, para a maioria das contas reais de servidor/magistrado, `resolveActorScope`
resolve com o mínimo de sinal disponível (ou nenhum), e as comparações de eixo em `requireRoutingAccess`
(UF, comarca, tribunal, instância, ramo) ficam com efetividade parcial — cada eixo só compara quando os
dois lados têm valor, e o lado do ator normalmente não tem.

`SecretariaEspecializadaRoutingService.malhaProcesso` ganhou uma camada adicional de defesa
(`PjbAuthorizationInstitutionalMalhaAccessFacade.requireVinculoInstitucionalComProcesso`, vínculo real
por `WorkItem`) para o caso de magistratura, que já segue o mesmo padrão usado nos painéis de
Desembargador/Ministro/Delegado. Servidor (`SERVIDOR`/`SERVIDOR_FORUM`) foi deliberadamente deixado fora
dessa camada extra: a maior parte do uso legítimo desse endpoint é justamente a triagem de processos
ainda não atribuídos a um `WorkItem` do servidor, e negar por padrão quando o vínculo não existe ainda
bloquearia esse fluxo real. Endurecer os eixos UF/comarca dentro de `requireRoutingAccess` também não é
seguro sem essa investigação adicional: como esses campos nunca são preenchidos hoje, qualquer regra que
passe a negar quando `Usuario.uf`/`comarca` estiverem nulos bloquearia acesso de praticamente toda conta
de servidor/magistrado em produção.

**Risco:** para atores do tipo `SERVIDOR`/`SERVIDOR_FORUM`, a verificação territorial de
`requireRoutingAccess` está funcionalmente próxima de um no-op — a maior parte das comparações de eixo
não tem dado do lado do ator para comparar.

**Não revisitar sem decisão de produto:** exige decidir onde e quando a afiliação institucional real do
servidor (tribunal, unidade, ramo, instância) passa a ser capturada no cadastro/lotação — hoje não existe
nenhum fluxo que grave isso. Só depois de existir esse fluxo faz sentido decidir se `requireRoutingAccess`
deve negar por padrão quando o escopo do ator não resolve, ou se deve continuar comparando só o que
existe.

## D-fracionary-organ-routing-catalogo-inexistente

**Status:** aberta

**Contexto:** `UnidadeJudiciariaCompetencia` ganhou os campos `grau` (`GrauJurisdicao`) e
`tipoTurmaRecursal` (`TipoTurmaRecursal`, novo enum), permitindo catalogar uma turma recursal real
(2º grau dos Juizados Especiais) com a mesma matéria já usada para varas de 1º grau (`tipoVara`).
Investigação (via fork dedicado) confirmou que isso NÃO fecha, sozinho, a separação por matéria no
roteamento recursal: `FracionaryOrganRoutingResolver.resolveSecondInstanceProfile`
(`core/processual/routing/FracionaryOrganRoutingResolver.java:265-266`), junto com
`TribunalInternalOrganCatalog.resolve()` (`core/processual/routing/TribunalInternalOrganCatalog.java:32-226`)
e `CollegiateOrganCatalog.resolve()` (`core/processual/routing/CollegiateOrganCatalog.java:13-64`), já
diferenciam civil de criminal — mas só no nível do **rótulo textual** (`"CAMARA_TJ_CRIMINAL"` vs
`"CAMARA_TJ_CIVEL"` etc., montado por concatenação de String). Nenhuma das três classes tem `@Entity`,
repository ou qualquer persistência — são funções puras que sintetizam texto descritivo
(`orgaoJulgadorSugerido`, `internalOrganLabel`, os campos `fracionary.internalOrgan.*` consumidos por
`RecursalAutuacaoDestinoService.buildFracionaryProjection`). Nenhuma delas referencia
`UnidadeJudiciariaCompetencia` — confirmado por leitura completa dos três arquivos, zero import/campo/chamada.

**Risco:** não existe risco de um recurso cível ser **atribuído** a uma câmara/turma criminal, porque
não existe atribuição real nenhuma hoje — o "destino" mostrado ao usuário é metadado descritivo/de
exibição, não uma decisão de distribuição persistida e auditável como já existe para 1º grau via
`MapaCompetenciaDinamicoEngine` + `UnidadeJudiciariaCompetencia`. O risco real é a lacuna inversa: o
catálogo novo de turma recursal (`grau`/`tipoTurmaRecursal`) não tem nenhum consumidor ainda — motor de
roteamento recursal e catálogo de câmaras/turmas seguem desconectados.

**Não revisitar sem decisão de produto:** fechar isso de verdade exige decidir se `FracionaryOrganRoutingResolver`/
`TribunalInternalOrganCatalog`/`CollegiateOrganCatalog` devem ser reescritos para consultar
`UnidadeJudiciariaCompetencia` (exigindo seed real de câmaras/turmas por tribunal, hoje inexistente) ou se
o papel desses três continua sendo só rotulagem informativa, com uma camada de distribuição recursal
real construída à parte, análoga ao `MapaCompetenciaDinamicoEngine`. Qualquer uma das duas rotas é maior
que uma correção pontual.
