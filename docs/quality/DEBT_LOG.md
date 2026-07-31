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

## D-jus-postulandi-recurso-tst

**Status:** aberta — bloqueio hoje é efeito colateral, não enforcement intencional

**Contexto:** `RecursalValidacaoMinimaService.elegivelPorJusPostulandi()` restringe jus postulandi
trabalhista a `RECURSO_ORDINARIO_TRABALHISTA` e `EMBARGOS_DECLARACAO` via allowlist de
`LegalAppealType`. `RECURSO_REVISTA` e `AGRAVO_RECURSO_REVISTA` (recursos de competência do TST,
onde a Súmula 425/TST expressamente veda jus postulandi) não estão na allowlist — mas também não
têm entrada em `RecursalValidacaoMinimaService.toRecursoProcessualTipo()` (confirmado por leitura do
switch: caem no `default -> null`), então `validar()` já lança
`"Tipo recursal sem correspondencia processual minima."` antes de chegar em qualquer checagem de
legitimidade, para qualquer ator — advogado incluído.

**Risco:** o bloqueio de jus postulandi no TST hoje existe por acidente (o tipo recursal nem é
processável nesta service), não por uma regra que leia o tribunal de destino. Se
`toRecursoProcessualTipo()` ganhar uma entrada para `RECURSO_REVISTA`/`AGRAVO_RECURSO_REVISTA` no
futuro (para permitir que advogados formalizem esses recursos por aqui), a allowlist atual passa a
ser a única proteção contra jus postulandi indevido no TST — e ela protege corretamente, porque
`RECURSO_REVISTA` não está nela. Mas isso não foi testado nem verificado neste momento; é proteção
por composição de duas lacunas independentes, não por design.

**Quando revisitar:** ao mapear `RECURSO_REVISTA`/`AGRAVO_RECURSO_REVISTA` em
`toRecursoProcessualTipo()` — adicionar teste explícito confirmando que jus postulandi trabalhista
continua barrado nesses dois tipos após o mapeamento, não presumir que a allowlist já cobre.

## D-completude-documental-sem-jus-postulandi

**Status:** FECHADA — corrigida no canal REST; o achado colateral do Marketplace segue aberto em
`D-marketplace-sem-completude-documental`

**Correção:** `CompletudeDocumentalPolicyService.diagnosticar` ganhou sobrecarga que recebe o
`InstrumentoRepresentacaoProcessual` resolvido para o ator; quando o instrumento é regime de jus
postulandi, `PROCURACAO` sai da lista de documentos obrigatórios — e apenas ela, sem afetar nenhum
outro requisito do catálogo do rito. A assinatura de dois argumentos foi mantida como delegação,
então os call sites e testes anteriores não mudaram. `AjuizarProcessoCommand` resolve o instrumento
via `RepresentacaoProcessualPolicyService` antes de chamar a checagem. Trava defensiva: quando a
política resolve a representação como irregular, o método devolve `null` e a exigência de procuração
permanece — irregularidade nunca vira dispensa. Quatro testes novos em
`CompletudeDocumentalPolicyServiceTest` (11/11 verde) cobrem a dispensa nos três regimes de jus
postulandi, a manutenção da exigência em `MANDATO_AD_JUDICIA` e a garantia de que documento
obrigatório diverso da procuração continua bloqueando.

**Contexto original (mantido para rastreabilidade):** achado durante investigação da Fatia 2,
pré-existente às fatias de jus postulandi, não criado por elas.

**Contexto:** `POST /api/v1/processos/ajuizar` (`ProcessoCommandController`, `@PreAuthorize
("isAuthenticated()")` — aberto a qualquer usuário autenticado, incluindo CIDADAO) roteia para
`AjuizarProcessoCommand`, que usa `CompletudeDocumentalPolicyService.diagnosticar()` para checar
documentos obrigatórios. Essa checagem lê `ProceduralCatalogSupport.snapshot(rito).documents()` —
um catálogo estático por rito (`ProceduralCatalogDefinitionSupport`) que marca `PROCURACAO` como
`required=true` para `TRABALHISTA_ORDINARIO`/`TRABALHISTA_SUMARIO_ALCADA` (via `trabalhistaIndividual`/
`trabalhistaAlcada`) e para `JUIZADO_ESPECIAL_CIVEL` (cai no `default -> civilGeral(rito)`, que
também exige `PROCURACAO`). Esse catálogo é totalmente independente de
`RepresentacaoProcessualPolicyService` — não sabe o que é jus postulandi.

**Risco:** um CIDADAO que ajuíze via `/api/v1/processos/ajuizar` (não via Laiane) para JEC ou
qualquer rito trabalhista do jus postulandi esbarra em `"Ajuizamento bloqueado por incompletude
documental... PROCURACAO"` — o mesmo bug que a Fatia 1/Fatia 2 corrigiram no fluxo do Laiane, intacto
neste segundo caminho de ajuizamento. Confirmado por leitura de código: `grep` por
`CompletudeDocumentalPolicyService`/`completudeDocumentalPolicyService` no `pjb-api/src/main` só
retorna `AjuizarProcessoCommand` como consumidor — `LaianePeticaoInicialDraftService` nunca chama
essa classe, por isso a Fatia 2 não precisou de correção condicional nela (ver decisão registrada no
prompt da Fatia 2, item "PRIMEIRO").

**Quando revisitar:** aplicar a mesma correção condicional que
`RepresentacaoProcessualPolicyService.addDocumentosBase()` já tem: `PROCURACAO` deixa de ser
`required` quando o instrumento resolvido é `isJusPostulandi()`. Fora de escopo da Fatia 2 por
instrução explícita do prompt (investigar e reportar, não corrigir).

**Consumidores mapeados (grep, 2026-07-25):** `AjuizarProcessoCommand` — e portanto a checagem de
`CompletudeDocumentalPolicyService` — é alcançado por exatamente um caminho:
`ProcessoCommandController` (`POST /api/v1/processos/ajuizar`) → `ProcessoCommandSurfaceFacadeService`
→ `AjuizarProcessoCommand`. Os outros dois canais de ajuizamento **não** passam por ele: tanto
`LaianePeticaoInicialDraftService` quanto `ApiMarketplaceService` chamam `AjuizamentoService.ajuizar()`
diretamente, pulando o command e sua checagem de completude. O Laiane tem gate próprio
(`ProtocoloCompletudeValidator` + `tb_requisito_documental`, onde a exigência de `PROCURACAO` já está
corretamente escopada para `ADVOGADO_PRIVADO`/`ADVOGADO_CONSTITUIDO`); o Marketplace não tem
checagem equivalente — ver `D-marketplace-sem-completude-documental`.

**Urgência:** o endpoint REST não tem consumidor interno no `pjb-api/src/main` — é superfície
externa (frontend/integrador), governada em `application-api-governance.yml` como
`processo-ajuizamento` com rate limit de 30 req/min. Não há como afirmar por leitura de código se
CIDADAO o consome em produção; a única barreira hoje é `@PreAuthorize("isAuthenticated()")`, que
não filtra perfil. Portanto a urgência é **média, não baixa**: o caminho está aberto a CIDADAO por
construção, só não há prova de uso.

## D-marketplace-sem-completude-documental

**Status:** Fase 1 aplicada — sinal síncrono + assíncrono de completude documental no canal
marketplace; consolidação dos três canais numa política única segue como Fase 2, não implementada.

**Contexto original (mantido para rastreabilidade):** existem três canais que criam processo no
PJB, e cada um validava completude documental de um jeito diferente (ou não validava). `POST
/api/v1/processos/ajuizar` passa por `AjuizarProcessoCommand` e usa `CompletudeDocumentalPolicyService`
contra o catálogo estático `ProceduralCatalogDefinitionSupport`. `LaianePeticaoInicialDraftService.protocolar()`
usa `ProtocoloCompletudeValidator` contra a tabela `tb_requisito_documental` (migration `V284`), com
severidade, condicionalidade por representante e registro de pendência. `ApiMarketplaceService.protocolar()`
não fazia nenhuma das duas: chamava `ajuizamentoService.ajuizar(processo)` direto, sem consultar
catálogo nem tabela de requisitos.

**Correção (Fase 1):** `MarketplaceProtocoloRequest` ganhou campo opcional `documentos` (`List<Attachment>`,
aditivo — clientes que não migraram continuam funcionando). `ApiMarketplaceService.protocolar()` reaproveita
`CompletudeDocumentalPolicyService.diagnosticar(rito, documentos)` sem alterar o service. Quando bloqueante,
`Processo.connectorSubmissionStatus` grava `PENDENTE_DOCUMENTACAO` em vez de `RECEBIDO_MARKETPLACE` —
decisão deliberada de não introduzir valor novo em `StatusProcesso` (enum compartilhado por
distribuição/prazo/analytics): o sinal vive no campo já dedicado ao canal conector, raio de explosão zero
sobre os demais bounded contexts. A resposta HTTP síncrona (`MarketplaceProtocoloResponse`) ganhou
`documentacaoCompleta`/`documentosFaltantes` — sinal que não depende de o cliente ter configurado webhook,
o que cobre 100% dos integradores hoje (nenhum tinha motivo pra configurar webhook para um evento que não
existia). `MarketplaceGovernanceService.publicarEventoPendenciaDocumental` (novo, espelha
`publicarEventoProtocolo`) dispara `PROCESSO_PENDENTE_DOCUMENTACAO` adicionalmente — nunca em substituição —
ao `PROCESSO_PROTOCOLADO`, que continua disparando sempre: o protocolo aconteceu de fato, completo ou não.

**Hardcode de rito corrigido na mesma fatia, não documentado como ruído:** `processo.setRito(RitoProcessual.COMUM_ORDINARIO)`
incondicional foi substituído por `ProceduralCatalogSupport.tryResolveRito(null, request.ramoDireito(),
request.classeProcessual())` — utilitário estático leve já usado por `AjuizarProcessoCommand` como fallback
sobre o roteamento pesado (`NationalProcessRoutingService`), sem puxar esse motor pesado para dentro do
marketplace. `MarketplaceProtocoloRequest` já carregava os dois sinais (`ramoDireito`, `classeProcessual`)
sem precisar de campo novo. Fallback idêntico ao comportamento anterior quando nada casa (`COMUM_ORDINARIO`),
resolução real quando casa — decisão tomada porque ligar completude documental sem corrigir o rito produziria
sinal de pendência poluído por rito errado desde o primeiro dia, o oposto do que a fatia promete entregar.

**Testes:** `ApiMarketplaceServiceCompletudeDocumentalUnitTest` (3, Mockito puro, sem Docker) e
`ApiMarketplaceServiceCompletudeDocumentalTest` (3, IT com Postgres real via Testcontainers) — ambos verdes,
cobrindo cliente sem campo `documentos` (nome do teste prova a negação central: sinalização pendente, não
aceitação silenciosa), cliente completo e cliente parcial. Regressão de `ApiMarketplaceServicePoloMaterializacaoTest`
(4) confirmada sem alteração.

**Fase 2 (não implementada, registrada apenas por nome):** endpoint dedicado
`POST /processos/{id}/documentos` para complementação documental pós-protocolo, disparando evento
`PROCESSO_DOCUMENTACAO_COMPLETADA` reservado neste texto para evitar renomear webhook já em produção quando
a Fase 2 for implementada. Consolidação das três políticas de completude (catálogo estático, tabela
`tb_requisito_documental`, e a nova checagem do marketplace) numa única fonte segue em aberto — candidato
natural continua sendo `ProtocoloCompletudeValidator`, por ser orientado a dado versionado.

**Achados colaterais registrados sem virar entrada própria:** duplicação de `MarketplaceProtocoloRequest`/
`MarketplaceProtocoloResponse` (DTO público em `model.dto.processo.marketplace` vs. record aninhado em
`ApiMarketplaceService`, sincronizados manualmente por `MarketplaceSurfaceFacadeService`) — pré-existente,
apenas mais um campo a manter nos dois lados a partir de agora.

## D-jus-postulandi-recurso-jef-turma-recursal

**Status:** aberta — bloqueio por conservadorismo deliberado, não por enforcement verificado

**Contexto:** `RecursalValidacaoMinimaService.JEF_JUS_POSTULANDI_APPEAL_TYPES` contém apenas
`EMBARGOS_DECLARACAO`. Isso significa que um CIDADAO com `JUS_POSTULANDI_JEF` fica barrado em
`RECURSO_INOMINADO` (que no catálogo `LegalAppealType` é compartilhado entre JEC estadual e JEF — não
existe tipo recursal federal separado) e em `PEDIDO_UNIFORMIZACAO` (incidente de uniformização à
Turma Nacional de Uniformização, específico do microssistema federal e sem equivalente no JEC).

**Risco:** esse bloqueio foi adotado por analogia conservadora ao regime do JEC (Lei 9.099/95,
art. 41, § 2º), **não** por verificação do que a Lei 10.259/2001 efetivamente exige. A Lei
10.259/2001 remete subsidiariamente à Lei 9.099/95 (art. 1º), mas tem regime recursal próprio —
Turma Recursal Federal e incidente de uniformização (arts. 14 e 15) não existem no juizado
estadual. Se a exigência de advogado no recurso federal for menos estrita do que a estadual, o
sistema está negando um direito processual que a parte teria; se for igual ou mais estrita, o
bloqueio está certo por acidente. Nenhuma das duas hipóteses foi confirmada contra a lei.

**Quando revisitar:** antes de qualquer promessa de cobertura completa do JEF na banca ou em
produção — verificar o texto da Lei 10.259/2001 (arts. 10, 14 e 15) e a jurisprudência da TNU sobre
capacidade postulatória na fase recursal, e então ou ampliar `JEF_JUS_POSTULANDI_APPEAL_TYPES` com
fundamento explícito, ou converter o bloqueio atual em enforcement documentado com teste próprio.
Enquanto isso, o comportamento é seguro (nega mais do que talvez devesse), nunca permissivo demais.

## D-recursal-superficie-por-papel

**Status:** parcialmente atendida — superfície única aditiva criada em Fatia 1; os quatro controllers
originais permanecem intactos por coexistência, e as etapas de deprecação/remoção seguem abertas.

**Contexto:** o módulo recursal expõe quatro controllers (`AdvogadoCockpitController`,
`DefensorPublicoPainelController`, `MinisterioPublicoPainelController`,
`ProcuradoriaOperacionalController`) que chamam a mesma facade com os mesmos parâmetros, diferindo
apenas no `@PreAuthorize`. Isso contradiz o precedente firmado em `5d500ee` para o peticionamento
inicial, onde a capacidade postulatória é resolvida por perfil no cadastro e por motor de política,
com `isAuthenticated()` na porta e o gate na camada de serviço — padrão que `PeticionamentoController`
(`/api/v1/peticionamento`) segue. Como consequência direta desta jornada:
`RecursalValidacaoMinimaService.elegivelPorJusPostulandi()` passou a autorizar o cidadão em jus
postulandi a opor embargos de declaração, mas não existe superfície pela qual ele exerça isso — o
motor responde sim e não há porta correspondente.

**Risco:** cristaliza o desenho de superfície-por-papel. Qualquer perfil novo que ganhe capacidade
recursal exige uma quinta cópia do mesmo controller, e a regra de quem pode recorrer permanece
dispersa em quatro lugares em vez de um. O motor de admissibilidade já concentra a decisão; a
superfície é que não confia nele.

**Fatia 1 aplicada — superfície unificada aditiva:** `RecursalPeticionamentoController`
(`POST /api/v1/recursal/processos/{processoId}/recurso`) publica a superfície única de interposição
recursal, autorizada por `@PreAuthorize` combinado que cobre as 13 roles legítimas hoje
(advocacia, defensoria pública, ministério público e procuradorias). A decisão de qual
service-de-perfil chamar acontece em `RecursalPeticionamentoPerfilRouter`, que resolve pelo
`TipoUsuario` do usuário autenticado (`isAdvocacia`/`isDefensoriaPublica`/`isMinisterioPublico`/
`isProcuradoria`) e delega ao mesmo intermediate service que os controllers atuais consomem
(`AdvogadoCockpitService`, `DefensorPublicoPainelService`, `MinisterioPublicoPainelService`,
`ProcuradoriaOperacionalService`), preservando 100% dos guards materiais por perfil já existentes
(`InstitutionalMaterialActionGuardService.MaterialAction.{DEFENSORIA|MINISTERIO_PUBLICO|PROCURADORIA}_RECURSO`
e a governança de escritório da advocacia via `OfficeGovernedProcessOperationService`). O rate limit
usa uma capability única (`recursal_peticionamento_recurso`), com o domínio (`LAWYER` × `INSTITUCIONAL`)
resolvido pelo próprio perfil. A resposta é envelopada em `SurfaceActionResponse` canônica com scope
`recursal.peticionamento.<perfil>`. DTO nova (`RecursalPeticionamentoRequest`, mesma shape byte-a-byte
das anteriores). Os quatro controllers antigos ficam intactos por período de coexistência: nada foi
removido nesta fatia. Cobertura: `RecursalPeticionamentoPerfilRouterTest` (11 testes: roteamento por
`TipoUsuario` — inclusive PGR rumo ao MP pela classificação canônica do enum —, mapeamento de
rate-limit domain, delegação por perfil, rejeição de perfil sem habilitação),
`RecursalPeticionamentoControllerTest` (4 testes MockMvc: happy path por família de perfil, scope
canônico, escolha correta do `CapabilityRateLimitDomain`) e `RecursalPeticionamentoControllerIT`
(7 testes contra Postgres real com Spring Security completo: anônimo negado sem tocar o router,
`ROLE_JUIZ` recebendo 403 via `@PreAuthorize`, e as quatro famílias legítimas mais o PGR chegando
ao router com o `Perfil` esperado). A DTO reusa `InstitutionalRecursoRequest` já existente em vez
de introduzir uma terceira cópia idêntica; `AdvogadoRecursoRequest` ganhou javadoc apontando para
a superfície canônica e para a Fatia 3 de remoção. Guards `constructor_injection_guard.py` e
`spring_ambiguous_constructor_guard.py` verdes com a nova classe já contabilizada (2310→2311
arquivos com estereótipo Spring escaneados, 0 findings).

**Divergência de roteamento PGR (registrada, não corrigida):** o enum `TipoUsuario` classifica
`PROCURADOR_GERAL_REPUBLICA` como Ministério Público (`isMinisterioPublico()` inclui PGR;
`isProcuradoria()` não). O router segue essa classificação canônica e roteia PGR ao
`MinisterioPublicoPainelService` — coberto por teste explícito
(`resolverPerfilAtivo_procuradorGeralRepublica_retornaMinisterioPublicoPorClassificacaoDoEnum`). O
`ProcuradoriaOperacionalController` legado, porém, aceita PGR no seu `@PreAuthorize`, o que
significa que um PGR autenticado hoje pode bater no endpoint da Procuradoria e disparar
`MaterialAction.PROCURADORIA_RECURSO` em vez de `MINISTERIO_PUBLICO_RECURSO`. Na superfície
unificada, esse mesmo PGR passa pelo guard de MP. Não é bug da Fatia 1 — é divergência
preexistente na modelagem de roles do PGR entre `TipoUsuario` (MP-centric) e o `@PreAuthorize` do
controller legado (MP+Procuradoria). A fatia de deprecação (2) deve alinhar os dois lados; até lá,
preferir a nova superfície faz o PGR ficar sempre no caminho canônico do enum.

**Divergência de role `DEFENSOR_DISTRITAL` (não introduzida, apenas não replicada):** o
`@PreAuthorize` do `DefensorPublicoPainelController` legado inclui `DEFENSOR_DISTRITAL`, valor que
não existe em `TipoUsuario` — code-path morto que nunca casa em runtime. A superfície unificada
não replica esse literal; se algum dia a role for adicionada ao enum, ambos os lados precisam ser
atualizados.

**Fatias restantes (abertas):** (2) deprecar as quatro URLs antigas com header `Deprecation`/`Sunset`,
redirecionando internamente à nova superfície; (3) remover os três métodos `interporRecurso` dos
surfaces intermediários (`AdvogadoSurfaceFacadeService`, `InstitutionalPainelSurfaceFacadeService`,
`ProcuradoriaOperationalSurfaceFacadeService`) e as quatro URLs antigas depois de zerar consumidores;
(4) habilitar jus postulandi de parte na mesma URL — expandir `@PreAuthorize` para incluir role de
cidadão-parte quando `elegivelPorJusPostulandi()` permitir, fechando a lacuna do motor recursal que
já responde “sim” para embargos de declaração no jus postulandi mas hoje não tem porta correspondente.

**Fatia 4 é bloqueadora de Fatias 2 e 3.** Deprecar (Fatia 2) ou remover (Fatia 3) as quatro URLs
legadas antes de habilitar jus postulandi de parte na nova superfície (Fatia 4) deixaria o
cidadão-parte permanentemente sem porta para exercer a capacidade que
`RecursalValidacaoMinimaService.elegivelPorJusPostulandi()` já autoriza — o oposto do objetivo do
debt. A ordem operacional obrigatória é 1 → 4 → 2 → 3.

**Quando revisitar:** em fatia própria de convergência recursal — superfície única autorizada por
`elegivelPorJusPostulandi()` somada à legitimidade profissional, no mesmo padrão do peticionamento.
Exige período de coexistência com os quatro controllers atuais por causa de consumidores de frontend
e testes; a remoção é etapa posterior, não simultânea. Não fazer dentro de fatia de jus postulandi.

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

**Risco residual:** o cidadão liberado no JEC pelas fatias de jus postulandi ainda depende da
integração do motor de custas ao ajuizamento para que a nova política produza efeito prático.
Enquanto essa integração não acontece, a nova política é defensiva — garante resposta correta
quando alguém ligar o motor, mas não cobra nem isenta ninguém por si só.

**Quando revisitar:** ao encaminhar a integração do motor de custas ao ajuizamento
(`D-motor-custas-nao-integrado-ao-ajuizamento`). Fatias correlatas registradas:
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

## D-custas-calculator-fazenda-classificada-como-isenta-cita-cpc-91-errado

**Status:** FECHADA — a fatia de unificação removeu o `CustasProcessuaisCalculatorService` por
completo, junto com seu enum `TipoCusta` paralelo e o teste próprio. O erro jurídico deixa de
existir porque a classe deixa de existir; a decisão foi remover em vez de migrar corrigindo porque
os percentuais hardcoded (`2%` preparo, `1%` multa art. 1.026, `10%` má-fé) não têm base legal
universal — variam por regimento de custas estadual (TJ) ou resolução do CJF, então plantar esses
números como se fossem autoritativos era ruído maior do que corrigir a citação errada do CPC art.
91. Se cálculo de preparo/multa virar necessidade real, será fatia própria com tabela por
tribunal, seguindo o padrão de `tb_jurisdicao_territorial`.

**Contexto original (mantido para rastreabilidade):** `CustasProcessuaisCalculatorService`, no
módulo paralelo `service/custas/` que não tinha consumidor no projeto, tratava Fazenda Pública e
Ministério Público como isentos e citava o CPC art. 91 como fundamento
(`"Fazenda Pública/MP isentos de custas (CPC, art. 91)."`). O art. 91 não é fundamento de isenção
— trata de pagamento diferido, como descrito em `D-custas-fazenda-publica-pagamento-diferido`.

## D-custas-dois-modulos-nao-integrados

**Status:** FECHADA — o módulo vivo é agora fonte única de verdade. `TipoCusta` foi movido para
`core/financeiro/custas/domain/TipoCusta.java` com os predicados originais (`eMulta`,
`requerDespacho`) preservados e três complementos legítimos adicionados
(`aplicaAoAjuizamentoInicial`, `aplicaAoRecursal`, `fundamentoLegal` — este último cobre os nove
valores do enum com base legal explícita por tipo). O pacote `service/custas/` foi deletado
inteiro: `CustasProcessuaisCalculatorService`, `TipoCusta` (versão antiga) e o teste do calculator.
A interface `IsentoCustaPolicy` foi renomeada para `CustaIsencaoPolicy` para seguir o padrão de
nomes do módulo (substantivo antes de qualificador, como `CustaJudicialService`).

**Contexto original (mantido para rastreabilidade):** o projeto tinha dois módulos de custas que
não se falavam. `core/financeiro/custas/` era o vivo: interface `IsentoCustaPolicy`,
`CustaJudicialService`, geradores de GRU e PIX, ledger, controller admin, migrations `V196` e
`V247`. `service/custas/` era o morto: `enum TipoCusta` com nove valores tipados,
`CustasProcessuaisCalculatorService` com percentuais de preparo e multa, sem nenhum call site em
`main`. O módulo vivo recebia `String tipoCusta` na interface enquanto o enum certo morava no
módulo morto.

## D-custas-interface-recebe-string-em-vez-de-enum

**Status:** FECHADA — a interface `CustaIsencaoPolicy.verificar(Processo, TipoCusta)` passou a
receber `TipoCusta` diretamente. Os cinco call sites foram migrados em conjunto:
`CustaIsencaoPorRitoPolicy` (troca comparação `"CUSTAS_INICIAIS".equals(tipoCusta)` por
`tipoCusta.aplicaAoAjuizamentoInicial()`), `GerarCustaJudicialCommand` (record com
`TipoCusta tipoCusta`), `CustaJudicialService.gerarCustas(Long, TipoCusta, BigDecimal)`,
`CustasApplicationService.gerar(Long, TipoCusta, BigDecimal)`, `AdminCustasController.gerar` (usa
`@RequestParam("tipo") TipoCusta tipo` — Spring converte string do request para enum
automaticamente, valor inválido devolve 400 sem código adicional). A entity `CustaJudicial.tipo`
foi migrada para `TipoCusta` com `@Enumerated(EnumType.STRING)`, mantendo compatibilidade binária
com a coluna `VARCHAR(64)` já existente (sem migration nova). DTOs de saída (`CustaJudicialView`,
`CustaConsultaResult`, `CustaTimelineEntry`) continuam expondo `String` no contrato público,
convertidos com `.name()` no boundary — a tipagem forte é escolha interna, não vaza para clientes.

**Ganho colateral no ledger:** `CUSTA_ISENCAO` e `CUSTA_GERADA` no `AuditLedgerService` passaram a
gravar `fundamento=` extraído de `TipoCusta.fundamentoLegal()`, além do `tipo=` já existente. A
rastreabilidade jurídica de cada isenção deixa de depender só do texto do motivo — agora tem
base legal explícita no evento.

**Contexto original (mantido para rastreabilidade):** `IsentoCustaPolicy.verificar(Processo,
String tipoCusta)` recebia string livre. Não havia enum na assinatura nem validação por parte do
consumidor — o service confiava que o chamador passaria o valor certo. Mesmo padrão frágil já
corrigido em outros pontos do projeto (`LaianeLawyerService`, `JuizGabineteDecisionalService`,
que trocaram comparação de string por enum tipado).

## D-salario-minimo-hardcoded-em-gratuidade

**Status:** FECHADA — a constante `SALARIO_MINIMO_2026 = 1518` foi removida;
`JusticaGratuidaVerificadorService` agora injeta `SalarioMinimoNacionalService` e consulta
`valorVigente()` a cada avaliação. O motor de salário mínimo já registra 2026 → R\$ 1.621,00 no
`fallbackOficial()`, e a base ativa via `SalarioMinimoNacionalRepository` sobrescreve o fallback
quando houver.

**Correção:** o teto de presunção passou a ser `valorVigente().multiply(5)` calculado a cada
chamada de `avaliar`, em vez de constante compilada. A regra fixa dos "cinco salários mínimos"
(consolidada por jurisprudência majoritária ao lado do CPC art. 99, § 3º) segue como constante
`TETO_PRESUNCAO_SM = 5` — é multiplicador legal, não valor monetário. Sete testes cobrem os
caminhos do `avaliar`, incluindo o teto exato em 2026 (R\$ 1.621 × 5 = R\$ 8.105) e a rejeição
por um centavo acima. A verificação de wiring da pré-condição confirmou que a classe não tinha
consumidor em `main` nem construção manual em testes, então adicionar construtor com o novo
service não quebrou nada.

**Contexto original (mantido para rastreabilidade):** `JusticaGratuidaVerificadorService` mantinha
a constante `private static final BigDecimal SALARIO_MINIMO_2026 = new BigDecimal("1518")`. O
valor de 2026 é R\$ 1.621,00 — registrado corretamente em
`SalarioMinimoNacionalService.fallbackOficial()`, que tem entradas por ano (2023 → 1320,
2024 → 1412, 2025 → 1518, 2026 → 1621). Ou seja, a constante estava com o valor de 2025 sob o
nome de 2026, e o motor certo já existia e retornava o valor certo.

**Risco original:** cálculo de hipossuficiência em 2026 usava referência R\$ 103 menor do que a
legal (`renda ≤ 5 * 1518 = 7.590` em vez de `5 * 1621 = 8.105`), potencialmente negando
gratuidade a quem tem direito. Efeito era latente porque a classe segue sem consumidor externo,
como o resto dos motores de custas — mas o valor errado deixou o registro no código como se fosse
autoritativo.

**Extensão da correção — atualização automática do salário mínimo:** o problema estrutural por trás
do valor errado hardcoded era a ausência de mecanismo de atualização anual. `SalarioMinimoBcbClient`
e `SalarioMinimoNacionalSyncScheduler` fecham essa lacuna. O client consulta a série 1619 do Banco
Central do Brasil (`https://api.bcb.gov.br/dados/serie/bcdata.sgs.1619/dados/ultimos/1`), API pública
com contrato estável, e devolve `Optional<SnapshotSalarioMinimo>` com data e valor. O scheduler roda
diariamente às 03:00 (cron configurável via `pjb.sync.salario-minimo.cron`), compara com o valor
persistido pelo ano da referência retornada e só chama `salvarOuAtualizar` quando difere. Payload
inválido, HTTP fora do ar ou exceção inesperada não propagam — o `FALLBACK_OFICIAL` do service
segue como muleta e a próxima execução do cron tenta de novo.

Segue o padrão do projeto para integrações federais: `RestClient` do Spring 6 sobre o
`pjbSharedHttpClient` já compartilhado por `CnjTpuSyncService`, `ResilientGovRegistryClient`,
`InfojudHttpClient` e outros; `@ConditionalOnProperty(name = "pjb.sync.salario-minimo.enabled",
havingValue = "true")` mantém a integração **desligada por default**, alinhado à convenção do
`IbgeSyncService` — o operador habilita explicitamente em produção quando decidir. Doze testes
cobrem o parser (payload BCB válido, com múltiplas entradas, vazio, nulo, em branco, não-array,
campos ausentes, data inválida, valor negativo/zero, JSON inválido); cinco cobrem o scheduler
(valor diferente dispara persistência, valor igual é no-op, snapshot vazio é no-op, comparação por
`compareTo` tolera diferença de scale, exceção do service não propaga).

## D-openapi-anotacoes-ausentes-em-controllers

**Status:** aberta — transversal, não bug ativo

**Contexto:** nenhum controller do PJB usa `@Operation` ou `@Tag` (`io.swagger.v3.oas.annotations`)
hoje. O springdoc-openapi gera a spec por análise automática, sem descrições, sem exemplos, sem
nomes de tag consistentes. A superfície unificada `RecursalPeticionamentoController` (Fatia 1 de
`D-recursal-superficie-por-papel`) foi analisada como candidata a receber `@Tag`/`@Operation`
isoladamente e a decisão explícita foi **não fazer**: adicionar anotação Swagger em um único
controller entre 200+ pioraria a consistência do projeto sem resolver a qualidade real da spec.

**Risco:** contrato público sem semântica descritiva, dificultando consumo por integradores futuros
(clientes SDK auto-gerados, portais de terceiros, ferramentas de importação OpenAPI). Não bloqueia
funcionalidade, mas empobrece a documentação executável que o PJB expõe.

**Quando revisitar:** fatia própria de documentação de API, tratando todos os controllers de uma
vez com padrão consistente (tag por área de domínio, `@Operation` com `summary` curto e
`description` mais longo, exemplos em DTOs via `@Schema`), não parcelado por endpoint novo.
Anti-padrão: aplicar caso-a-caso à medida que novos endpoints nascem — cria duas classes de
controllers no mesmo projeto e nunca converge. `RecursalPeticionamentoController` é candidato
natural a primeiro alvo dessa fatia transversal.

## D-salario-minimo-hardcoded-fora-de-gratuidade

**Status:** parcialmente atendida — 3 dos 5 pontos fechados nesta fatia; 2 pontos permanecem
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
  o que ficará semanticamente incorreto no ano seguinte. Não renomeado nesta fatia porque é
  breaking change de contrato consumido pelo frontend; registrado como observação para fatia
  futura de generalização de contrato (`referenciaAnoAnterior`/`referenciaAnoCorrente`).

**Guard de regressão:** `salario_minimo_hardcoded_guard.py` (bridge em `scripts/`, corpo em
`tooling/python/scripts/`) detecta 5 padrões: literal `1XXX.00` próximo a identificador de SM,
literal em entry de Map com chave `salarioMinimo*`, declaração de `static final BigDecimal
SALARIO_MINIMO*`, chamada `valorPorAno(literal)`, e `LocalDate.now()` inline em chamada ao service
canônico. Whitelist explícita do `SalarioMinimoNacionalService.java` (fonte canônica com
`FALLBACK_OFICIAL` legítimo). Sem mecanismo de allowlist inline — nenhuma convenção prévia no
projeto e a fatia optou por não inventar. Exit 1 documentado enquanto as duas dívidas próprias
não forem resolvidas.

**Risco original:** valores monetários congelados em pontos de cálculo relevantes (falência,
recuperação judicial, catálogo de frontend, painel comparativo), com correção requerendo
atualização manual arquivo-a-arquivo todo ano em vez de sincronização automática via
`SalarioMinimoNacionalSyncScheduler`. Impacto direto: cálculo pode negar competência a JEC/JEF em
casos limite, exibir catálogo desatualizado, ou aplicar teto trabalhista/impontualidade com valor
de anos anteriores.

## D-national-rule-pack-engine-sem-data-referencia

**Status:** aberta — dívida arquitetural, não bug ativo (achado transversal de fatia de
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

**Quando revisitar:** fatia arquitetural própria. Alteração exige adicionar `LocalDate
dataReferencia` ao `record ContextoRegra`, o que cascateia por 28 arquivos consumidores
(`JurimetriaEngine`, `NationalColegiadoEngine`, `CejuscEngine`, `CooperacaoJuridicaEngine`,
`ImpedimentoSuspeicaoEngine`, `NotificacaoInteligentePJB`, `TransparenciaCnjEngine`, `LoadPlan`,
`PluginSnapshot`, `PluginResolucaoTribunalService`, `TribunalRuleEngine`,
`TribunalRulePackSynchronizationSupport`, `TribunalRuleResolutionSupport`, além dos testes). O
guard `salario_minimo_hardcoded_guard.py` detecta as duas ocorrências e permanecerá reportando-as
com `exit=1` documentado até o fechamento desta dívida.

## D-quadro-credores-recuperacao-marco-nao-pesquisado

**Status:** aberta — bloqueio de segurança sobre `QuadroGeralCredoresAssemblerService`, gate
levantado pela Fase 0 da fatia de `D-salario-minimo-hardcoded-fora-de-gratuidade`.

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
específica. **Marco temporal para RJ não foi pesquisado nesta fatia.**

**Risco:** como o service não impõe barreira arquitetural contra reuso em RJ (aceita qualquer
`List<Credor>` sem verificar tipo de processo), qualquer implementação futura da Fase 3 com
"data da decretação" hardcoded como semântica única pode ser silenciosamente incorreta em cenário
de RJ. Aplicar critério de falência em RJ, ou vice-versa, reintroduz a mesma ambiguidade que a
fatia atual resolveu para o outro service.

**Quando revisitar:** fatia própria com pesquisa jurídica prévia sobre o marco temporal do SM em
recuperação judicial. Antes de escrever código: (a) pesquisar art. 54 c/c 83 da Lei 11.101/2005 no
contexto de RJ; (b) confirmar precedente ou doutrina sobre marco em RJ; (c) decidir se o service
deve receber enum discriminador (`TipoProcesso.FALENCIA` / `TipoProcesso.RECUPERACAO`) para
resolver marco diferente por caminho, ou se são dois services distintos. O guard
`salario_minimo_hardcoded_guard.py` continua reportando as 2 ocorrências (constante literal +
declaração de constante) até fechamento.

## D-scheduler-salario-minimo-nunca-ativado

**Status:** aberta — dívida operacional, não bug ativo (achado transversal da investigação de
`D-salario-minimo-hardcoded-fora-de-gratuidade`)

**Contexto:** `SalarioMinimoNacionalSyncScheduler` (`@Scheduled(cron = "${pjb.sync.salario-minimo.cron:0 0 3 * * *}")`)
existe com cron diário às 03:00 UTC e consumiria a série 1619 do Banco Central via
`SalarioMinimoBcbClient`. Está protegido por dois gates: `@Profile("!test")` e
`@ConditionalOnProperty(name = "pjb.sync.salario-minimo.enabled", havingValue = "true")` — sem
`matchIfMissing=true`. **A propriedade `pjb.sync.salario-minimo.enabled` não está setada em
nenhum `application*.yml`/`.properties` de `pjb-api/src/main/resources`.** Nenhuma migration
popula a tabela `salario_minimo_nacional` como seed. Consequência: em todo ambiente, toda consulta
a `SalarioMinimoNacionalService.valorPorAno(ano)` cai no `FALLBACK_OFICIAL` estático (2023=1320,
2024=1412, 2025=1518, 2026=1621), e o último recurso do fallback devolve `1621` fixo para qualquer
ano ≥ 2026. Quando 2027 chegar, o service devolverá 1621 para 2027 sem intervenção humana — valor
de 2026 congelado como default eterno.

**Risco:** a plataforma parece dinâmica (consulta service canônico, propaga data de referência,
guard anti-hardcode ativo) mas a fonte por trás é estática e envelhece silenciosamente. Correção
dos 3 hardcodes da fatia atual (Falencia + FrontendCatalog + EconomicReference) melhora o desenho
mas não elimina a dívida de fonte: enquanto o scheduler não subir, a atualização anual do salário
mínimo continua manual (via PR editando `FALLBACK_OFICIAL`).

**Quando revisitar:** decisão operacional de deploy + segurança. Ativar o scheduler exige (a)
setar `pjb.sync.salario-minimo.enabled=true` no perfil de produção, (b) confirmar que a chamada
externa ao BCB é aceitável no ambiente (whitelist de saída, rate limit), (c) monitorar as
primeiras execuções via log ou métrica dedicada (o scheduler não escreve em `AuditLedgerService`
hoje), (d) avaliar se cabe seed inicial via migration para garantir base populada mesmo antes da
primeira execução. Não integrar essa fatia com a de fixes atuais — é decisão operacional de
outra natureza.

## D-titularidade-cidadao-duplicada-dois-guards

`PjbAuthorizationService.requireReadProcessoAsCidadaoParte` e `PersonalProcessAccessGuardService.requireCurrentUserAsParty` implementam a mesma checagem de CPF (parte autora/ré/usuário do processo) em dois arquivos distintos, achado ao cablear a Fatia 4 de `D-recursal-superficie-por-papel`.
Revisitar ao tocar qualquer um dos dois: unificar num único método antes de corrigir bug ou adicionar caso novo em só um lado.

## D-peticionamento-controller-domain-lacuna-cidadao

`PeticionamentoController.resolveDomain()` não reconhece `CIDADAO` e recai em `CapabilityRateLimitDomain.LAWYER` por omissão — inconsistente com o resto do projeto, que usa `CITIZEN` para ação/leitura de cidadão (achado ao cablear a Fatia 4 de `D-recursal-superficie-por-papel`).
Revisitar em fatia própria: adicionar branch explícito para `CIDADAO` retornando `CITIZEN`.

## D-cidadao-parte-guard-sem-teste-rejeicao

`PjbAuthorizationService.requireReadProcessoAsCidadaoParte` não tem teste dedicado que prove a rejeição real por CPF divergente em nenhum dos 9 consumidores em produção — lacuna pré-existente, mais antiga que a Fatia 4 de `D-recursal-superficie-por-papel`, só encontrada ao cablear esta fatia.
Revisitar em fatia própria: IT com Spring Security real provando 403 para CIDADAO cujo CPF não bate com nenhuma parte do processo.

## D-controllers-recursais-legados-sem-teste-dedicado

Os 4 controllers recursais legados (`AdvogadoCockpitController`, `DefensorPublicoPainelController`, `MinisterioPublicoPainelController`, `ProcuradoriaOperacionalController`) não tinham nenhuma classe de teste dedicada antes da Fatia 2 de `D-recursal-superficie-por-papel` — só os headers de depreciação de `interporRecurso` ganharam cobertura mínima, os demais endpoints seguem sem teste próprio.
Revisitar em fatia própria: cobertura completa (sucesso, validação, autorização real) dos 4 controllers antes da remoção na Fatia 3.

## D-frontend-delivery-routes-nao-sinaliza-depreciacao

`PjbFrontendDeliveryApplicationService.parseRoutes` escaneia `@PostMapping`/`@GetMapping` via regex e não lê headers HTTP de depreciação — os 4 endpoints recursais legados aparecem no catálogo `/api/v1/frontend/delivery/routes` com o mesmo peso da rota unificada nova, achado ao investigar consumidores antes da Fatia 3.
Revisitar se o catálogo vier a ser consumido por um frontend real: cruzar rota com `RecursalLegacyDeprecationHeaders` ou marcador equivalente antes de expor como pronta para uso.

## D-tribunal-rule-engine-wiring-manual-de-colaborador

`TribunalRuleEngine` constrói `TribunalRuleResolutionSupport` e `TribunalRulePackSynchronizationSupport` com `new` no próprio construtor em vez de injetá-los via Spring — oposto ao padrão de constructor injection do resto do projeto. Achado ao confirmar que `SalarioMinimoNacionalService` não era dependência morta ali (é repassada pra materializar `resolutionSupport`, que a usa de fato); não corrigido por estar fora do escopo da fatia de observabilidade de `D-scheduler-salario-minimo-nunca-ativado`.
Revisitar em fatia própria: avaliar se os dois colaboradores deveriam virar `@Component` injetados via construtor, mapeando todo consumidor de `TribunalRuleEngine` antes.

## D-auditoria-salario-minimo-sem-garantia-de-persistencia

`AuditLedgerService.persistSafely` envolve `auditLedgerRepository.save(entry)` num `try/catch` que loga e nunca propaga nem tenta de novo — comportamento pré-existente da classe, exposto de novo pela auditoria da escrita manual de salário mínimo (`IntelligenceOperationalSurfaceFacadeService.salvarSalarioMinimo`). `payload_hash=null` passado ao `appendSafely` não chega nulo ao banco: `safePayloadHash()` sintetiza um SHA-256 a partir de `eventCode`/`resourceType`/`resourceId`/`description`/timestamp antes de persistir — confirmado por IT real contra Postgres (`AuditLedgerServicePayloadHashNuloIT`, que originalmente esperava `null` e falhou, expondo esse comportamento). Não é a causa de falha silenciosa aqui, mas qualquer outra falha de persistência (banco fora, pool esgotado) ainda passaria despercebida, sem alerta associado ao `log.warn`.
Revisitar: decidir se falha de persistência de evento crítico deveria propagar ou alimentar retry/outbox em vez de só logar — mudança em classe usada por dezenas de call sites, não é correção pontual.

## D-testes-it-contaminacao-em-lote-amplo-service-package

`mvnw test -Dtest="com.tcc.pjb.backend.service.**"` produziu 24 falhas em `Trt7CearaJurisdicaoCargaIT` (município resolvendo para `MunicipioForaDoCatalogo` em vez de `Resolvida`); a mesma classe isolada (`-Dtest=Trt7CearaJurisdicaoCargaIT`, mesmo HEAD) deu 9/9 verde — contaminação real de estado entre classes de IT quando agrupadas amplamente fora do `verify` padrão, não regressão de código. `D-ci-heap` já registra instabilidade de execução conhecida no CI; se o GitHub Actions algum dia agrupar essas classes de forma parecida, o resultado é falso-negativo pra quem não tiver este contexto.
Revisitar: identificar o dado/estado que vaza entre `Trt7CearaJurisdicaoCargaIT` e as demais classes do lote (mesmo padrão de `D-consultapublica-flaky` e `D-pjbflowitbase-cleanup-only-beforeeach`); não usar `-Dtest="pacote.**"` como atalho de validação de regressão ampla até resolver — usar `verify` oficial ou lotes menores deliberadamente compostos.

## D-salario-minimo-watchdog-limiar-sem-base-documentada

`SalarioMinimoStalenessWatchdogService` usa limiar default de 1 ano (`pjb.observability.salario-minimo.staleness-limiar-anos:1`) escolhido por julgamento de engenharia no momento da implementação, sem SLA, ADR ou requisito citado — registrado explicitamente para não virar decisão calibrada por presunção de quem ler o config depois.
Revisitar: se houver critério real (tempo histórico entre decretos, tolerância operacional acordada), substituir o valor e esta entrada por uma nota de fundamento.

## D-anomaisrecenteconhecido-divergia-da-resolucao-real-de-valorPorAno

**FECHADA nesta mesma fatia.** `SalarioMinimoNacionalService.anoMaisRecenteConhecido()` usava `findTopByAtivoTrueOrderByAnoReferenciaDesc()` (máximo irrestrito do banco) e só considerava a persistência quando o ano superava o teto do fallback — divergindo de `valorPorAno()`, que prioriza qualquer registro do banco de forma incondicional, mesmo mais antigo que o fallback. Cenário real: banco só com registro de 2023, fallback até 2026 — o watchdog reportava "sem defasagem" enquanto `valorPorAno(anoAtual)` de fato servia o valor de 2023.
Corrigido reusando a mesma query e cadeia de resolução de `valorPorAno` (`findTopByAnoReferenciaLessThanEqualAndAtivoTrueOrderByAnoReferenciaDesc`), retornando o ano que efetivamente governa o valor servido. 3 testes cobrem banco vazio, banco mais antigo que o fallback (o cenário real do achado) e banco no ano corrente.

## D-mutableclock-duplicado-em-3-testes

`MutableClock` (implementação de `java.time.Clock` mutável para teste) existia como classe privada copiada em 4 arquivos antes desta fatia; extraída para `com.tcc.pjb.backend.support.MutableClock` (pública, reusável) e consolidada em `SalarioMinimoMetricsTest` e `PjbCodebaseLearningApplicationServiceCacheTest` — os dois nomeados no pedido de correção. `PjbCodebaseSanityApplicationServiceCacheTest`, `PjbWriteFailoverTrackerTest` e `AcordoProcessualApplicationServiceTest` continuam com cópia privada própria, não tocados por estarem fora do escopo desta fatia.
Revisitar: migrar os 3 restantes para a classe compartilhada quando algum deles for tocado por outro motivo, ou numa fatia dedicada de higiene de teste.
