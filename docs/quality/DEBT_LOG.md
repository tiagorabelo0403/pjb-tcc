# Débitos Técnicos — Registro Aberto

Registro de dívidas técnicas conhecidas e ainda não fechadas. Diferente da seção de Testes do
`README.md` (que narra dívidas já resolvidas), este arquivo documenta lacunas abertas — sem bloquear
nenhuma entrega em andamento — para que não fiquem só na memória de quem investigou.

Cada entrada sai daqui quando a dívida é fechada; o fechamento é então narrado no `README.md`, seguindo
o padrão já em uso (ex.: D-routing-preprotocolo, D-d25-testes-anexo).

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
são capturados nesta fatia, embora o schema tenha um elemento `cidade` livre dentro de `tipoEndereco`
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

## D-mni-litisconsorcio-primeira-pessoa

**Status:** aberta

**Contexto:** `MniXmlToProcessoAdapter.resolvePartes` captura apenas a primeira `<pessoa>` de cada
`<polo>` (via `firstDescendant`, que retorna o primeiro match). Esse era o comportamento pré-existente
para nome/documento e agora também vale para a UF de domicílio recém-capturada. Em litisconsórcio
(múltiplos autores ou múltiplos réus no mesmo polo), as demais pessoas do polo são ignoradas por
completo — nome, documento e domicílio só da primeira.

**Risco:** processo com litisconsórcio recebido via MNI perde as partes além da primeira de cada polo.
Não é regressão desta fatia (o comportamento já existia para nome/documento antes de qualquer trabalho
em domicílio) — mas tocar o método tornou a limitação mais visível e vale documentar explicitamente
em vez de deixar só implícita no código.

**Quando revisitar:** ao modelar múltiplas partes por polo no canal MNI — depende de estender
`PoloProcessual`/`resolvePartes` para agregar uma lista, não um único nome/documento/UF por polo;
fatia própria, maior que ajuste pontual no adapter.

## D-intake-workspace-endereco-nao-wireado

**Status:** aberta

**Contexto:** `PeticionamentoInitialIntakeWorkspaceService` tem `enderecoAutor`/`enderecoReu`
estruturados (com `uf`/`cidade`) em `PeticionamentoSessaoRequest` — inclusive já lê
`getEnderecoAutor().getUf()` para resolver `ufFato` — mas passa `null` para os 4 campos territoriais
de parte do `EstruturarRequest`.

**Risco:** baixo hoje. Wirear `cidade` → `comarca` seria aproximação (comarca é circunscrição
judiciária; município não é comarca — município pequeno pertence à comarca sede vizinha). Decisão
tomada: não aproximar. A resolução correta virá do catálogo de jurisdição territorial chaveado por
código IBGE (iniciativa de competência territorial por rito, Fatia 6 — adapter ViaCEP).

**Quando revisitar:** quando a Fatia 6 entregar CEP → código IBGE; aí o wiring vira exato, não
aproximado.

## D-advisory-modos-nao-implementados

**Status:** aberta (não bloqueia nada — documentação corrigida para refletir o comportamento real)

**Contexto:** `LaianeJudicialDecisionAdvisoryService` sempre bloqueia publicação e exige revisão humana
(`publicationLocked`/`reviewRequired` sempre `true`, por política de segurança deliberada — não é bug) e
sempre opera em modo único (`advisoryMode = "ADVISORY_DRAFT_ONLY"`). Os 3 modos originalmente
documentados (`SUGESTIVO`/`RESTRITIVO`/`BLOQUEADOR`) nunca foram implementados.

**Quando revisitar:** se o produto decidir que a Laiane deve diferenciar níveis de consultoria (ex.:
permitir publicação sem revisão em casos de baixíssimo risco) — isso exigiria definir critério jurídico
de classificação por template, trabalho substantivo, não uma correção pontual.

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

**Quando revisitar:** se a Fatia territorial precisar cobrir `modo_competencia = 'DELEGADA_JUIZ_DIREITO'`
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

## D-drain-coordinator-fork-exit-sem-guarda-regressao

**Status:** aberta (fix aplicado e validado 2x; falta blindagem contra regressão)

**Contexto:** `PjbRuntimeDrainCoordinator` (`SmartLifecycle`, fase `Integer.MAX_VALUE`) dorme
`pjb.runtime.lifecycle.drain-quiet-period` (default de produção: 20s) a cada fechamento de contexto
Spring, inclusive em JVMs de teste. Numa rodada completa de `verify`/`test` (fork único reutilizado,
`reuseForks=true`), esse sleep somado à pressão de GC acumulada estourava o watchdog de 30s do próprio
Surefire (`forkedProcessExitTimeoutInSeconds`), matando a JVM forkada à força no encerramento — confirmado
por thread dump (`main` preso em `ApplicationShutdownHooks.runHooks()` → `SpringApplicationShutdownHook`
→ `DefaultLifecycleProcessor$LifecycleGroup.stop()` → `CountDownLatch.await()`, com a thread
`pjb-drain-coordinator` ainda em `Thread.sleep()`). Corrigido via
`-Dpjb.runtime.lifecycle.drain-quiet-period=10ms` no `argLine` de Surefire e Failsafe (`pom.xml`).

**Risco:** o fix depende de duas linhas de `argLine` no `pom.xml` permanecerem intactas — sem elas, o
sintoma volta. É silencioso em rodadas curtas ou isoladas (uma classe sozinha nunca acumula GC suficiente
pra estourar os 30s) e só se manifesta em `verify`/`test` completo sob carga.

`PjbRuntimeDrainService.sanitizeDuration()` trata `Duration.ZERO` como valor inválido e substitui
silenciosamente pelo fallback de produção (20s/30s) — `-Dpjb.runtime.lifecycle.drain-quiet-period=0s`
não gera erro nem log, simplesmente não tem efeito algum; só um valor pequeno e não-zero (ex.: `10ms`)
neutraliza a espera de fato.

**Cobertura de teste:** nenhuma. Não existe teste que falhe se a flag for removida do `pom.xml`, nem
teste que exercite `sanitizeDuration()` com `Duration.ZERO` pra documentar o comportamento de fallback
silencioso.

**Quando revisitar:** ao mexer no `<argLine>` do Surefire/Failsafe por qualquer outro motivo — conferir
que a flag continua presente. Candidato a guard Python dedicado (verifica que o argLine de teste sempre
inclui esse override), já que a ausência da flag só se manifesta em rodada completa, nunca em execução
isolada de uma classe.

## D-transactional-hotspot-guard-49-achados-nao-triados

**Status:** fechada — `--fail-on-findings` passa limpo (`hotspotCount: 51, unreviewed: 0, missingBudgets: 0`)

**Contexto:** `transactional_hotspot_guard.py --fail-on-findings` chegou a acusar 750 métodos
`@Transactional` com token suspeito (`findAll(`, `saveAll(`, `outboxPublisher.enqueue(`,
`processUnified(`, `iaOrchestrator.`, e — antes do ajuste abaixo — `.stream()`/`for (`/`appendSafely(`
mesmo sozinhos, sem nenhum outro sinal de I/O real). Triagem manual de uma amostra de 27 achados
(os de maior contagem de sinais) confirmou 6 riscos reais (N+1 de banco, full-table-scan pra contar
linha) e uma quantidade grande de ruído puro: `.stream()`/`for (` sobre coleção já carregada em
memória, e `appendSafely(` sozinho (uma escrita de auditoria isolada, não um loop).

Os 6 riscos reais foram corrigidos (bulk pre-check em vez de query por item, `COUNT` no banco em vez
de `findAll` + filtro em Java). Três outros achados do mesmo grupo (`issueBatch`/
`issueBatchFromTemplate`, `DigitalCustodyChainLedgerService.persist`, `reconcileVisibility`) têm
padrão deliberado (isolamento de erro por item em lote administrativo, lock otimista sob
concorrência, N já limitado por página) — marcados com `@PjbTransactionalBudget` em vez de
reestruturados, e o guard passou a respeitar essa anotação também em `--fail-on-findings` (antes só
valia pra `--fail-on-missing-budgets` nos 4 pacotes hotspot).

O heurístico do guard foi refinado: `.stream()`, `for (` e `appendSafely(` só contam como sinal
quando aparecem junto de `findAll(`/`saveAll(`/`outboxPublisher.enqueue(`/`processUnified(`/
`iaOrchestrator.` — isso derrubou o total de 750 para 51 achados, eliminando o ruído comprovado.

**Fechamento:** os 49 achados foram lidos método por método (leitura completa do corpo, não só do
trecho do regex). Um resultou em correção real: `UsuarioService.listarTodosUsuarios` fazia
`findAll()` sem paginação nenhuma num endpoint admin (`GET /api/v1/usuarios`, rate-limited mas sem
limite de tamanho por chamada) — corrigido pra `Page<UsuarioResponse> listarTodosUsuarios(Pageable)`,
espelhando o padrão já usado em `JurisdicaoService.listarPaginado`. Essa é uma mudança de contrato
público (`List<UsuarioResponse>` → `Page<UsuarioResponse>`); nenhum teste ou cliente no repositório
depende do formato antigo (frontend ainda não existe — ver README, seção "Frontend em análise e
planejamento") e nenhum outro caller da mesma classe precisou mudar.

Os outros 48 (incluindo o próprio `listarTodosUsuarios` já corrigido, que continua batendo no regex
via `findAll(pageable)`) foram confirmados falso-positivo ou risco aceitável por leitura: tabela de
referência pequena (planos de marketplace, clientes OAuth2, órgãos judiciários, planos de partição),
já em cache com TTL (`FederalismoJudicialEngine.listarNos`/`healthFederacao`,
`PainelNacionalJusticaService.gerarSnapshot`), já paginado/limitado corretamente
(`OabInstitucionalService.listarSeccional`, `BulkUploadService.finalizeBatch`,
`AdvClienteCanonicalizeSensitiveService.canonicalizeBatch`), escopo de um processo/thread só, ou
já é o padrão correto de lote paginado + `saveAll` único (`CienciaProcessualApplicationService.
processarExpirados`, `ConclusaoProcessualApplicationService.processarExpiradas`,
`AdministradorNacionalGovernanceService.executarReconciliacaoGlobal`). Dois casos ficaram como
"moderado, não bloqueante" — `NationalForumMeshGovernanceService.reconcile` e
`ConfiguracaoDistribuicaoVaraService.recarregarDoRepositorio` fazem `findAll()` real em
`UnidadeJudiciariaCompetencia` (varas/unidades — nacionalmente na casa de milhares, não milhões),
mas são jobs de governança/cache, não hot path.

Achado colateral de heurística: `ComunicacaoJudicialStateStore.findAll(String domain, Class<T> type)`
foi flagado só porque o **nome do próprio método** contém a substring `findAll(` — o corpo é um
lookup em cache filtrado por domínio, não um scan. O guard casa contra o texto do bloco inteiro,
inclusive a assinatura do método, e não distingue `repository.findAll()` de
`repository.findAllByX(...)` (convenção Spring Data pra query filtrada) nem do nome do método
hospedeiro — não corrigido nesta rodada, fica como limitação conhecida do heurístico.

**Cobertura de teste:** `TemaRecursoRepetitivoService.aplicarResultado` e
`ProcessoCumprimentoOperacionalApplicationService.materializar` continuam sem teste unitário ou de
integração no projeto; as correções foram validadas só por leitura de código e `test-compile`.
`PainelNacionalJusticaService.registrarAlertaPrazo` e o novo `UsuarioService.listarTodosUsuarios(Pageable)`
também não têm teste direto. Escrever regressão pra esses 4 fica como trabalho futuro, não bloqueia
o fechamento desta dívida (o guard, que é o que bloqueava o CI, já está limpo).

**Quando revisitar:** se o guard voltar a acusar um achado genuinamente novo (código novo, não
`@PjbTransactionalBudget` residual), ou se `UnidadeJudiciariaCompetencia` crescer o suficiente pra
`reconcile`/`recarregarDoRepositorio` deixarem de ser "moderado" e virarem risco real — nesse caso
paginar como foi feito em `UsuarioService`.

## D-peticionamento-pessoal-teste-nao-cobre-timing-de-repositorio

**Status:** aberta — nota de cobertura, não bloqueia nada

**Contexto:** `LaianePeticaoInicialDraftService.rejeitarProcessoIdParaPeticionantePessoal` roda antes
de `resolveProcesso` em `estruturar()`/`salvar()`, evitando por construção que um peticionante
pessoal consiga fazer o serviço buscar no repositório um `Processo` de terceiro a partir de um
`processoId` arbitrário. O teste `cidadaoComProcessoIdDeTerceiroEBloqueadoAntesDeCarregarOProcesso`
prova que a exceção é lançada e que nenhum dado do processo alheio chega ao chamador — mas
`processoRepository` neste teste é um bean real (`@Autowired`, `PjbIntegrationTestBase`), não um
mock/spy, então o teste não confirma que `processoRepository.findById` deixa de ser chamado.

**Risco:** a garantia de que não há chamada ao repositório existe hoje só por leitura de código
(a ordem das chamadas no método), não por teste. Isso importa porque, mesmo sem vazamento de dado,
uma chamada ao repositório antes do bloqueio poderia, em tese, vazar existência de `processoId` por
diferença de timing entre "processo existe, barrado depois" e "processo não existe, erro mais
rápido" — canal lateral de enumeração, não a ausência de dado que o teste atual cobre.

**Quando revisitar:** se este padrão de trava (`rejeitar antes de resolver`) for replicado em canal
com superfície de ataque maior que o Laiane (ex.: endpoint público REST sem autenticação de
profissional), vale reforçar com `@SpyBean`/verificação de invocação — não é urgente aqui, porque o
Laiane já exige usuário autenticado e o request de peticionante pessoal é de baixo volume.

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
atualizar todos eles em lockstep. É particularmente provável que uma fatia futura sobre
representação processual (ex.: adicionar sinal de "é recurso" para fechar o enforcement do art. 41,
§2º sem depender de allowlist por `LegalAppealType` em `RecursalValidacaoMinimaService`) mexa nesta
assinatura.

**Quando revisitar:** ao tocar `resolve()` de novo — considerar um `record` de request
(`RepresentacaoProcessualPolicyRequest`) ou builder no lugar dos parâmetros posicionais, migrando os
10 call sites de uma vez. Não vale a pena isolado, só quando a assinatura for mexida por outro motivo.
