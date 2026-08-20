# Débitos Técnicos — Registro Aberto

Registro de dívidas técnicas conhecidas e ainda não fechadas. Diferente da seção de Testes do
`README.md` (que narra dívidas já resolvidas), este arquivo documenta lacunas abertas — sem bloquear
nenhuma entrega em andamento — para que não fiquem só na memória de quem investigou.

Cada entrada sai daqui quando a dívida é fechada; o fechamento é então narrado no `README.md`, seguindo
o padrão já em uso (ex.: D-routing-preprotocolo, D-d25-testes-anexo).

## D-territorio-string-solta-entidades-legadas

**Status:** aberta

**Contexto:** a fatia "Organização Judiciária" (Tasks 1-6, `Tribunal`/`Comarca` como entidade real)
migrou território (uf/comarca) de String solta para FK `Comarca` em 5 entidades: `UnidadeJudiciariaCompetencia`,
`Jurisdicao`, `Usuario`, `Processo`, `WorkItem` — mantendo `uf`/`comarca` como fallback String real ao lado
da FK, porque o catálogo `tb_comarca` (Task 1) só cobre 3 dos 27 estados (CE/MG/RN). O teste de arquitetura
novo (`OrganizacaoJudiciariaArchitectureTest`, Task 6) trava qualquer entidade NOVA que reintroduza `uf`/`comarca`
String sem a FK `Comarca` correspondente na mesma classe — mas, ao rodar essa regra contra o projeto inteiro
pela primeira vez, apareceram 23 entidades pré-existentes, fora do escopo desta fatia, que já declaravam
`uf`/`comarca` String sem nenhuma FK `Comarca`. Uma delas (`JurisdicaoTerritorial`) saiu da allowlist ainda
nesta fatia — ver nota abaixo —, restando **22 entidades pré-existentes**:

`CalendarioForenseEntry`, `AtlasAcessoMunicipio`, `NoFederacaoJudicial`, `EscrituraExtrajudicialRegistro`,
`InqueritoPolicialDigital`, `EventoInstitucional`, `Estados`, `CidadaoProcessoNacionalProjection`, `Municipios`,
`ProcessoZonaEleitoral`, `UnidadeInstituicao`, `CalendarioEleitoral`, `OperationalFunctionCredential`,
`GovServiceRegistry`, `InstitutionalCompetenceRuleSnapshot`, `InstitutionalCatalogUnitSnapshot`,
`InstitutionalCatalogGovernanceSnapshot`, `ProfessionalInstitutionalAccessGrant`, `PeritoSorteioAudit`,
`PeritoDisponibilidade`, `PainelTribunalMetrica`, `OrgaoJudiciario`.

Essas 22 classes foram registradas numa allowlist nomeada (`ENTIDADES_LEGADAS_TERRITORIO_STRING_SEM_FK_COMARCA`)
dentro do próprio teste de arquitetura — a regra continua ativa e bloqueia qualquer entidade nova fora dessa
lista, mas não força a migração retroativa das 22 nesta fatia (fora de escopo: cada uma pertence a um domínio
diferente — eleitoral, criminal, atlas, perícia, extrajudicial, gov, federalismo, snapshots institucionais —
e migrar todas exigiria repetir o ciclo completo desta fatia 22 vezes).

`JurisdicaoTerritorial` saiu da allowlist na rodada de correção da revisão final: é a tabela de onde `tb_comarca`
é semeada (`V319` lê `municipio_ibge`/`municipio_nome`/`uf`), então a FK `Comarca` resolve por código IBGE com
match exato, sem a ambiguidade de nome que motivou o adiamento das demais.

**Risco:** as mesmas classes de bug que motivaram esta fatia (grafia divergente entre UF/comarca cadastrados
em textos diferentes) continuam presentes nessas 22 entidades — nenhuma delas ganhou o benefício da comparação
por identidade real via FK.

**Quando revisitar:** ao planejar a próxima fatia de território — priorizar por volume de uso real
(`Estados`/`Municipios`/`OrgaoJudiciario` parecem candidatos de alto impacto por serem catálogos amplamente
referenciados). Cada migração fecha reduzindo a allowlist em `OrganizacaoJudiciariaArchitectureTest`,
nunca alargando.

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

## D-mni-litisconsorcio-primeira-pessoa

**Status:** fechada

**Contexto:** `MniXmlToProcessoAdapter.resolvePartes` capturava apenas a primeira `<pessoa>` de cada
`<polo>` (via `firstDescendant`, que retornava o primeiro match). Em litisconsórcio (múltiplos autores
ou múltiplos réus no mesmo polo), as demais pessoas do polo eram ignoradas — nome, documento e
domicílio perdidos silenciosamente.

**Correção:** `resolvePartes` agora itera TODAS as `<pessoa>` de cada `<polo>` via `allDescendants`.
O adapter retorna `MniAdapterResult(Processo, List<MniParteParsed>)` — a primeira pessoa de cada polo
ainda popula os campos planos do `Processo` (backward compat); `MniRecepcaoService.materializarPolosIniciais`
materializa TODAS as pessoas como `PoloProcessual`, usando o `TipoParte` rito-aware do
`PoloCompositionPolicy` para as primeiras e replicando o mesmo `TipoParte` para as demais do mesmo polo.
Testes de litisconsórcio no adapter (4 pessoas, 2 polos) e no service (4 `incluir`, `TipoParte`
rito-aware preservado) validam a correção.

## D-mni-terceiro-pj-interesse-publico

**Status:** fechada

**Contexto:** ao investigar a mesma etapa de litisconsórcio, identificamos que o adapter MNI também
descartava três categorias de parte: (1) terceiro interessado — o schema MNI usa `polo="TC"`/`"TJ"`,
mas o mapeamento antigo era binário (`"AT"` → ATIVO, qualquer outra coisa → PASSIVO), então terceiro
virava réu; (2) pessoa jurídica — o atributo `tipoPessoa="juridica"` da `<pessoa>` não era lido e
`razaoSocial` nunca era populado no `PoloProcessual`; (3) parte institucional sem `<pessoa>` — o
schema MNI permite `<interessePublico>` (texto livre) em vez de `<pessoa>` para casos como Fazenda
Pública/INSS/União, e o adapter só buscava `<pessoa>`, descartando a parte silenciosamente.

**Correção:** `MniParteParsed` ganhou o campo `tipoPessoa`. `resolvePartes` agora também itera
`<interessePublico>` dentro de cada `<polo>`, marcando `tipoPessoa="interesse_publico"`.
`MniRecepcaoService.mapMniPoloCode` mapeia `"TC"/"TJ"` → `TipoPolo.TERCEIRO` e `"FL"` →
`TipoPolo.MINISTERIO_PUBLICO` (com `TERCEIRO` como default seguro em vez de `PASSIVO`);
`defaultTipoParteForPolo` dá o `TipoParte` correto por polo quando `PoloCompositionPolicy` não tem
entrada para aquele tipo. `PoloProcessual` ganhou a coluna `razao_social` (migration V297) e um
overload de `incluir` que a recebe; `razaoSocial` é populado quando `tipoPessoa` é `"juridica"` ou
`"interesse_publico"`. Testes no adapter cobrem os 5 casos (pessoa física, PJ, terceiro, interesse
público, Ministério Público) validando `tipoPolo`/`nome`/`documento`/`tipoPessoa` de cada parte parseada.

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

## D-verify-instabilidade-forks-orfaos

**Status:** FECHADA — guard reaper dedicado + documentação no README.

**Contexto:** durante longas sessões de teste, rodadas de `mvnw test`/`verify` passaram a ser mortas
logo no início. A causa raiz NÃO era flag de JVM mal configurada (o `-Xmx4g` por fork e o launcher
Maven estão corretos e deliberados — ver `D-ci-heap-correcao`), e sim **JVM de teste órfã**: quando um
`mvnw` em background é interrompido abruptamente (SIGKILL do ambiente/CI), a JVM forkada do
Surefire/Failsafe (`surefirebooter`, `-Xmx4g`) sobrevive sem processo pai que a reape e vai acumulando —
observado ao vivo caindo para ~1,5 GB livres de 24 GB, com múltiplos forks `-Xmx4g` zumbis segurando
~7 GB; encerrá-los restaurava 8-9 GB e destravava os runs seguintes. Processo zumbi, não heap errado.

**Fechamento:** `scripts/reap_orphan_test_jvms.py` — guard multiplataforma (Windows via CIM/PowerShell,
Linux/macOS via `ps`, somente stdlib) que detecta JVMs de teste órfãs (fork do surefire/failsafe cujo
processo pai não está mais vivo, ou reparentado para PID 1) e as encerra com `--kill` (report-only por
padrão, exit ≠ 0 se achar — sinal útil em CI). Não amarrado ao build automaticamente (auto-kill em toda
rodada poderia matar um run paralelo legítimo); é executado sob demanda ao notar instabilidade.
Documentado no README (seção Testes, "Se o test/verify começar a cair sem motivo aparente"). Padrão
operacional: reapear órfãs antes de uma rodada longa mantém a memória saudável.

## D-drain-coordinator-fork-exit-sem-guarda-regressao

**Status:** FECHADA — guard dedicado e testes de `sanitizeDuration()` adicionados.

**Contexto:** `PjbRuntimeDrainCoordinator` (`SmartLifecycle`, fase `Integer.MAX_VALUE`) dorme
`pjb.runtime.lifecycle.drain-quiet-period` (default de produção: 20s) a cada fechamento de contexto
Spring, inclusive em JVMs de teste. Numa rodada completa de `verify`/`test` (fork único reutilizado,
`reuseForks=true`), esse sleep somado à pressão de GC acumulada estourava o watchdog de 30s do próprio
Surefire (`forkedProcessExitTimeoutInSeconds`), matando a JVM forkada à força no encerramento — confirmado
por thread dump (`main` preso em `ApplicationShutdownHooks.runHooks()` → `SpringApplicationShutdownHook`
→ `DefaultLifecycleProcessor$LifecycleGroup.stop()` → `CountDownLatch.await()`, com a thread
`pjb-drain-coordinator` ainda em `Thread.sleep()`). Corrigido via
`-Dpjb.runtime.lifecycle.drain-quiet-period=10ms` no `argLine` de Surefire e Failsafe (`pom.xml`).

**Risco original:** o fix dependia de duas linhas de `argLine` no `pom.xml` permanecerem intactas —
sem elas, o sintoma volta. É silencioso em rodadas curtas ou isoladas (uma classe sozinha nunca
acumula GC suficiente pra estourar os 30s) e só se manifesta em `verify`/`test` completo sob carga.

`PjbRuntimeDrainService.sanitizeDuration()` trata `Duration.ZERO` (ou negativo) como valor inválido e
substitui silenciosamente pelo fallback de produção (20s/30s) — `-Dpjb.runtime.lifecycle.drain-quiet-period=0s`
não gera erro nem log, simplesmente não tem efeito algum; só um valor pequeno e não-zero (ex.: `10ms`)
neutraliza a espera de fato.

**Fechamento:**
- `scripts/drain_quiet_period_argline_guard.py` — guard Python dedicado que lê `pom.xml`, localiza os
  blocos reais de configuração (não o `<pluginManagement>`, que só fixa versão) do Surefire e do
  Failsafe, e falha se o `<argLine>` de qualquer um dos dois não tiver
  `-Dpjb.runtime.lifecycle.drain-quiet-period=<valor>` com um valor não-zero. Validado tanto contra o
  `pom.xml` real (passa) quanto contra cópias mutadas simulando a flag removida e a flag zerada (falha
  nos dois casos, com a causa raiz explicada na mensagem).
- `PjbRuntimeDrainServiceTest` ganhou 4 testes novos documentando o comportamento de
  `sanitizeDuration()`: `Duration.ZERO` e `Duration` negativo em `drainQuietPeriod()`/`shutdownAwaitTimeout()`
  caem no fallback de produção (20s/30s) silenciosamente, e um valor pequeno não-zero (`10ms`) é
  respeitado sem fallback. Suite completa da classe: 6/6 verdes.

**Quando revisitar:** ao mexer no `<argLine>` do Surefire/Failsafe por qualquer outro motivo, rodar
`python scripts/drain_quiet_period_argline_guard.py` — ele já acusa se a flag sumir ou for zerada.

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

**Status:** FECHADA — teste unitário isolado prova a ausência de chamada ao repositório.

**Contexto original (mantido para rastreabilidade):** `LaianePeticaoInicialDraftService.rejeitarProcessoIdParaPeticionantePessoal`
roda antes de `resolveProcesso` em `estruturar()`/`salvar()`, evitando por construção que um
peticionante pessoal consiga fazer o serviço buscar no repositório um `Processo` de terceiro a
partir de um `processoId` arbitrário. O teste existente (`OabLegitimidadePeticionamentoTest.
cidadaoComProcessoIdDeTerceiroEBloqueadoAntesDeCarregarOProcesso`) provava a exceção e a ausência
de dado no chamador, mas `processoRepository` ali é um bean real (`@Autowired`, `PjbIntegrationTestBase`),
não um mock/spy — o teste não confirmava que `processoRepository.findById` deixava de ser chamado.

**Tentativa descartada:** converter o `processoRepository` compartilhado do arquivo IT para
`@MockitoSpyBean` quebrou o boot inteiro do `ApplicationContext` da classe (28/28 erros): o bean
é interceptado por AOP relacionado a RLS de sigilo (`PjbProcessoSigiloRlsFilter`/
`ProcessoSigiloRlsEnvelopeService`), e o CGLIB do Spring não consegue gerar proxy em cima do proxy
já gerado pelo Mockito para o spy. Revertido integralmente.

**Fechamento:** `LaianePeticaoInicialDraftServiceTimingTest` (teste unitário puro, sem Spring, sem
Postgres) constrói o service manualmente com os 14 colaboradores como mocks Mockito isolados —
sem o bean gerenciado pelo Spring, o `ProcessoRepository` mockado nunca passa pelo pós-processamento
de AOP que quebrava o spy. `verifyNoInteractions(processoRepository)` depois da chamada que lança
`AccessDeniedPjbException` prova que `rejeitarProcessoIdParaPeticionantePessoal` bloqueia antes de
`resolveProcesso` tocar o repositório — não só por leitura de código. 1/1 verde, 3,8s (contra os
~200s do IT completo).

**Quando revisitar:** se este padrão de trava (`rejeitar antes de resolver`) for replicado em canal
com superfície de ataque maior que o Laiane (ex.: endpoint público REST sem autenticação de
profissional), o mesmo padrão de teste unitário isolado (não IT) se aplica — construir o service
manualmente com mocks evita a incompatibilidade AOP/CGLIB descoberta aqui.

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

## D-jus-postulandi-recurso-tst

**FECHADA — correção do diagnóstico original, não implementação de regra nova.** A premissa de que
`RECURSO_REVISTA` e `AGRAVO_RECURSO_REVISTA` "não têm entrada em `toRecursoProcessualTipo()`" estava
errada para `AGRAVO_RECURSO_REVISTA` desde antes desta dívida ser escrita — confirmado por
`git log -p`/`git show` no commit imediatamente anterior ao que registrou esta entrada (24/07 21:22,
oito minutos antes): `case AGRAVO_INSTRUMENTO, AGRAVO_RESP_RE, AGRAVO_RECURSO_REVISTA ->
RecursoProcessualTipo.AGRAVO_DE_INSTRUMENTO` já existia desde 25/05. Os dois tipos têm destinos
diferentes de verdade:
- **`AGRAVO_RECURSO_REVISTA`** *tem* mapeamento processual e passa pela checagem de legitimidade.
  Como não está em `TRABALHISTA_JUS_POSTULANDI_APPEAL_TYPES` (só `RECURSO_ORDINARIO_TRABALHISTA` e
  `EMBARGOS_DECLARACAO`), a Súmula 425/TST **já era aplicada de verdade** — não por acidente, por
  enforcement ativo da allowlist —, só nunca tinha teste de regressão provando isso.
- **`RECURSO_REVISTA`** (sem "Agravo") de fato não tem entrada no switch e cai em
  `"Tipo recursal sem correspondencia processual minima."` para qualquer ator, advogado incluído —
  esse sim é o bloqueio acidental que a dívida original descrevia, mas só se aplica a este tipo.

**Fechamento:** 4 testes novos em `RecursalValidacaoMinimaServiceTest` — cidadão trabalhista barrado
em `AGRAVO_RECURSO_REVISTA` por ilegitimidade (prova o enforcement real da Súmula 425), advogado
segue legítimo no mesmo tipo (prova que a restrição é só de jus postulandi), e o par cidadão/advogado
em `RECURSO_REVISTA` provando que os dois batem no mesmo erro de mapeamento ausente — não é
específico de jus postulandi, então não precisa de allowlist nova. 13/13 verde na classe inteira.

**Quando revisitar:** só se `RECURSO_REVISTA` ganhar mapeamento em `toRecursoProcessualTipo()` no
futuro — nesse momento, adicionar teste explícito confirmando que jus postulandi trabalhista continua
barrado nele (mesmo padrão que `AGRAVO_RECURSO_REVISTA` já tem agora).

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

**Contexto original (mantido para rastreabilidade):** achado durante investigação da Etapa 2,
pré-existente às etapas de jus postulandi, não criado por elas.

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
documental... PROCURACAO"` — o mesmo bug que a Etapa 1/Etapa 2 corrigiram no fluxo do Laiane, intacto
neste segundo caminho de ajuizamento. Confirmado por leitura de código: `grep` por
`CompletudeDocumentalPolicyService`/`completudeDocumentalPolicyService` no `pjb-api/src/main` só
retorna `AjuizarProcessoCommand` como consumidor — `LaianePeticaoInicialDraftService` nunca chama
essa classe, por isso a Etapa 2 não precisou de correção condicional nela (ver decisão registrada no
prompt da Etapa 2, item "PRIMEIRO").

**Quando revisitar:** aplicar a mesma correção condicional que
`RepresentacaoProcessualPolicyService.addDocumentosBase()` já tem: `PROCURACAO` deixa de ser
`required` quando o instrumento resolvido é `isJusPostulandi()`. Fora de escopo da Etapa 2 por
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

**Status:** FECHADA. Fase 1 aplicada — sinal síncrono + assíncrono de completude documental no
canal marketplace. Fase 2 implementada e mergeada em `master` (commit `c34e2db3`, PR #9,
branch `worktree-marketplace-completude-fase2`): endpoint `POST /api/marketplace/v1/processos/{id}/documentos`,
`MarketplaceDocumentoComplementarService` com `MarketplaceDocumentoPersistenceService` compartilhado entre
`protocolar()` (leniente) e `complementar()` (estrito) — validação, deduplicação por SHA-256, classificação
de sigilo/categoria, escrita em `ObjectStoragePort`, persistência de `DocumentoProcessual`. Ownership resolvido
por coluna dedicada `connector_client_id`, substituindo a checagem ambígua original baseada em `startsWith`/
split por `:` (o `clientId` podia conter `:` no próprio valor). Suíte: 4430/4430 testes unitários, 0 falhas
na suíte completa de IT (95 classes). Três dívidas novas registradas fora do escopo da Fase 2:
`D-marketplace-payload-multiplo-anexo`, `D-marketplace-scope-oauth-nao-checado-no-path-primario`,
`D-marketplace-connectorclientid-sem-backfill-para-janela-entre-commits`.

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

**Hardcode de rito corrigido na mesma etapa, não documentado como ruído:** `processo.setRito(RitoProcessual.COMUM_ORDINARIO)`
incondicional foi substituído por `ProceduralCatalogSupport.tryResolveRito(null, request.ramoDireito(),
request.classeProcessual())` — utilitário estático leve já usado por `AjuizarProcessoCommand` como fallback
sobre o roteamento pesado (`NationalProcessRoutingService`), sem puxar esse motor pesado para dentro do
marketplace. `MarketplaceProtocoloRequest` já carregava os dois sinais (`ramoDireito`, `classeProcessual`)
sem precisar de campo novo. Fallback idêntico ao comportamento anterior quando nada casa (`COMUM_ORDINARIO`),
resolução real quando casa — decisão tomada porque ligar completude documental sem corrigir o rito produziria
sinal de pendência poluído por rito errado desde o primeiro dia, o oposto do que a etapa promete entregar.

**Testes:** `ApiMarketplaceServiceCompletudeDocumentalUnitTest` (3, Mockito puro, sem Docker) e
`ApiMarketplaceServiceCompletudeDocumentalTest` (3, IT com Postgres real via Testcontainers) — ambos verdes,
cobrindo cliente sem campo `documentos` (nome do teste prova a negação central: sinalização pendente, não
aceitação silenciosa), cliente completo e cliente parcial. Regressão de `ApiMarketplaceServicePoloMaterializacaoTest`
(4) confirmada sem alteração.

**Correção (Fase 2 — retrofit do jus postulandi na Fase 1):** a Fase 1 checava completude documental
contra o catálogo do rito sem saber se o ator dispensava `PROCURACAO` por jus postulandi — o mesmo bug
que `D-completude-documental-sem-jus-postulandi` já havia corrigido no canal REST, mas que a Fase 1 do
marketplace reintroduziu por não existir ainda quando aquela fatia foi escrita. `MarketplaceProtocoloRequest`
ganhou campo opcional `perfilAtor` (aditivo — sem ele o comportamento não muda). `ApiMarketplaceService.protocolar()`
resolve o `InstrumentoRepresentacaoProcessual` via `MarketplaceRepresentacaoResolver` (novo — encapsula a
sobrecarga de `RepresentacaoProcessualPolicyService` que trabalha com primitivos, sem exigir `Usuario`
carregado, que o canal marketplace não tem) e passa o instrumento resolvido para
`CompletudeDocumentalPolicyService.diagnosticar`, dispensando `PROCURACAO` corretamente quando o regime é
jus postulandi.

**Correção (Fase 2 — endpoint de complementação documental):** novo `POST
/api/marketplace/v1/processos/{id}/documentos` (`MarketplaceDocumentoComplementarService`, exposto via
`MarketplaceSurfaceFacadeService` + `ApiMarketplaceController`) para complementação documental pós-protocolo,
com storage real via `ObjectStoragePort` — ao contrário da Fase 1, que usava os documentos apenas
transientemente para diagnóstico, sem persistir nada. O endpoint faz checagem de posse (404 se o processo
não pertence ao cliente chamador), guarda de estado (409 se `connectorSubmissionStatus` não é
`PENDENTE_DOCUMENTACAO`), validação de conteúdo via `DocumentContentValidator` (extraído de
`PastaDigitalService`, que passou a delegar para ele sem mudança de comportamento) e classificação de
sigilo com texto extraído real da amostra do documento. Ao completar a exigência documental, dispara o
evento `PROCESSO_DOCUMENTACAO_COMPLETADA` — reservado por nome desde a Fase 1 para não renomear webhook
já em produção.

Para que o endpoint novo tivesse o que completar, `ApiMarketplaceService.protocolar()` também passou a
persistir os documentos declarados como `DocumentoProcessual` reais (via `MarketplaceDocumentoPersistenceService`,
novo, compartilhado pelos dois pontos de entrada) — achado de arquitetura no meio da implementação, aprovado
antes de aplicar: sem persistência real na Fase 1, o recálculo de completude do endpoint novo nunca teria o
que somar, porque só soma linhas persistidas. `CompletudeDocumentalPolicyService` ganhou sobrecarga que aceita
`Set<TipoDocumento>` diretamente (em vez de só a lista de anexos), para o endpoint novo recalcular completude
sem precisar reconstruir anexos.

**Dois bugs reais encontrados e corrigidos durante a Fase 2 (nenhum introduzido por ela):**
`MarketplaceDocumentoPersistenceService.persistirSeNovo` nasceu `@Transactional`, o que quebrava o
try/catch deliberadamente tolerante de `protocolar()` (um anexo ruim não deve abortar o protocolo inteiro)
— o Spring propaga a transação compartilhada para rollback-only e a chamada externa nunca via a exceção
isolada. Corrigido removendo o `@Transactional` interno; os dois chamadores já têm transação própria. Mais
grave: `tb_documento_processual.categoria` é `NOT NULL` desde a migration `V19`, mas
`DocumentoSigiloClassifier.suggestedCategoria()` devolve `null` para qualquer documento comum (sem sinal de
sensibilidade) — todo insert de documento normal pelo marketplace teria estourado
`DataIntegrityViolationException` em produção. Só foi descoberto porque a IT nova (`MarketplaceDocumentoComplementarServiceIT`)
é o primeiro teste real deste projeto a tocar Postgres de verdade num insert de `DocumentoProcessual` pelo
canal marketplace. Corrigido em `MarketplaceDocumentoPersistenceService` com fallback para
`DocumentoCategoria.PUBLICO` quando o classificador não sugere nada — mesma escolha que o backfill da
própria `V19` já fazia e que a camada ABAC já normaliza em outros pontos. Uma hipótese inicial (incorreta)
assumiu que `PastaDigitalService` (canal interno de documentos) tinha o mesmo bug e recebeu o mesmo fix por
engano; revisão de código independente identificou que a variável `categoria` ali já vem normalizada por
`DocumentoCategoria.fromString(...)`, que nunca devolve `null` — o fix nesse arquivo foi revertido, o bug
nunca existiu ali.

**Testes (Fase 2):** 23 testes unitários novos (7 `PastaDigitalServiceTest`, 2
`CompletudeDocumentalPolicyServiceTest`, 4 `MarketplaceRepresentacaoResolverTest`, 2
`ApiMarketplaceServiceCompletudeDocumentalUnitTest`, 1 `MarketplaceGovernanceServiceDocumentacaoCompletadaTest`,
7 `MarketplaceDocumentoComplementarServiceTest`) e 1 IT nova (`MarketplaceDocumentoComplementarServiceIT`,
Testcontainers Postgres real, prova round-trip de storage) — todos verdes. Suíte completa do projeto
reconfirmada ao final: **4.421 testes unitários, 0 falhas, 0 erros** (`mvn test` completo, não estimativa).
Consolidação das três políticas de completude (catálogo estático, tabela `tb_requisito_documental`, e a
checagem do marketplace) numa única fonte segue fora de escopo desta fatia — candidato natural continua
sendo `ProtocoloCompletudeValidator`, por ser orientado a dado versionado.

**Achados colaterais registrados sem virar entrada própria:** duplicação de `MarketplaceProtocoloRequest`/
`MarketplaceProtocoloResponse` (DTO público em `model.dto.processo.marketplace` vs. record aninhado em
`ApiMarketplaceService`, sincronizados manualmente por `MarketplaceSurfaceFacadeService`) — pré-existente,
apenas mais um campo a manter nos dois lados a partir de agora.

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

## D-recursal-opa-critical-path-nao-atualizado

**Status:** FECHADA — `critical_paths` do OPA ext-authz atualizado para cobrir a superfície unificada.

**Contexto:** `infra/k8s/overlays/prod-sovereign-opa-ext-authz/opa-policy-configmap.yaml` é uma política
Envoy/OPA real (`default allow := false`), um dos 4 overlays principais validados por schema no
próprio CI (ver README, seção "Validação de manifestos Kubernetes"). Seu conjunto `critical_paths`
(exige header `x-pjb-affiliation-id` e `not read_only`) listava só `/api/v1/mp/recurso/` — o path
legado do MP — desde antes da Etapa 1 de `D-recursal-superficie-por-papel`. Quando a Etapa 1 publicou
a superfície unificada `/api/v1/recursal/processos/{id}/recurso` (semanas atrás), essa política nunca
foi atualizada: o path novo não começa com `/api/v1/institucional/` nem com nenhum `critical_paths`
existente, então em qualquer deploy real usando este overlay o endpoint unificado roda **sem nenhuma
proteção de critical-path** desde que a Etapa 1 foi ao ar — achado ao investigar pré-requisitos da
Etapa 3, não introduzido por ela.

**Risco se não corrigido antes da Etapa 3:** remover o path legado `/api/v1/mp/recurso/` (que a Etapa 3
prevê) sem primeiro cobrir `/api/v1/recursal/` deixaria as operações de recurso do MP **sem nenhuma**
proteção de critical-path neste overlay — regressão de segurança real, não cosmética.

**Fechamento:** `critical_paths` ganhou `/api/v1/recursal/` mantendo `/api/v1/mp/recurso/` por enquanto
(será removido do conjunto quando o path legado for de fato apagado do código, na mesma Etapa 3).
`python infra/k8s_schema_validate.py` confirmado OK nos 4 overlays após a mudança.

**Quando revisitar:** ao concluir a remoção do path `/api/v1/mp/recurso/` no código (Etapa 3), remover
também a entrada `/api/v1/mp/recurso/` deste `critical_paths` — path morto, nunca mais alcançável.

## D-institutional-gate-filter-roda-antes-da-auth

**Status:** FECHADA — bug sistêmico de ordem de filtro corrigido e provado por IT com JWT real.

**Contexto (bug):** `InstitutionalCriticalActionHttpGuardFilter` — o filtro que aplica o gate documental
institucional (`InstitutionalDocumentSecurityGateApplicationService.enforce`, ato sensível conforme o
path) a ~30 operationCodes reais (senteça, despacho, manifestação/parecer/requisição do MP, ofício e
resposta de ofício do oficial de justiça, laudo do perito, parecer psicossocial, redistribuição,
lavratura de escritura, etc.) — era `@Component` com `@Order(Ordered.HIGHEST_PRECEDENCE + 35)` e nunca
foi adicionado à cadeia do Spring Security via `http.addFilter*`. `HIGHEST_PRECEDENCE + 35`
(`Integer.MIN_VALUE + 35`) registra o filtro no servlet chain **antes** do `DelegatingFilterProxy` do
Spring Security, cujo order é `SecurityProperties.DEFAULT_FILTER_ORDER = -100` (valor lido diretamente
do `spring-boot-autoconfigure-3.5.12.jar`, não presumido). Resultado: quando o filtro rodava, o
`SecurityContextHolder` ainda estava vazio, então `enforce()` → `CurrentUserService.getRequired()`
lançava `IllegalStateException` (não capturada — o filtro só tratava `RegraNegocioException`) →
**HTTP 500 em todo POST protegido**, em produção. O gate era, na prática, um controle de segurança
dormente/quebrado desde que foi introduzido.

**Prova empírica:** IT temporária `InstitutionalGateFilterOrderingProbeIT` (removida após confirmar)
com usuário seedado + JWT real reproduziu o 500; o stack trace mostrou o throw exatamente em
`getRequired`, chamado pelo filtro, com a cadeia do Spring Security ausente entre os filtros que o
envolviam — confirmando que ele rodava antes da autenticação. Nenhuma IT do projeto exercitava
qualquer path desse filtro via HTTP real antes desta etapa (lacuna de cobertura que escondia o bug).

**Correção (3 camadas):**
1. **Ordem:** removido o `@Order(HIGHEST_PRECEDENCE + 35)` (mantido `@Component`, exatamente como
   `MinisterStepUpFilter`/`DecisionStepUpFilter`, que são `@Component` sem `@Order` e adicionados à
   cadeia) e registrado via `http.addFilterAfter(institutionalCriticalActionHttpGuardFilter, AuthorizationFilter.class)`
   em `SecurityConfig`. `AuthorizationFilter` é o último filtro de segurança — garante execução após
   autenticação E autorização, com o usuário resolvido. `OncePerRequestFilter` deduplica a
   auto-registração residual (que agora cai em `LOWEST_PRECEDENCE`, depois da cadeia de segurança).
2. **Null-safety (defesa em profundidade):** `InstitutionalDocumentSecurityGateApplicationService.avaliar()`
   trocou `currentUserService.getRequired()` por `getOrNull()`; usuário não resolvível vira `nomination == null`
   e segue o mesmo `allowLegacyFallback` já existente — nunca mais 500, decisão determinística. Não muda
   nada para usuário real resolvido (getOrNull devolve o mesmo que getRequired devolveria).
3. **Cobertura recursal:** `/api/v1/recursal/processos/*/recurso` adicionado ao `resolvePolicy` do filtro
   (agora funcional), com `operationCode="RECURSAL_UNIFICADO"` e ato `PETICIONAR_EM_NOME_DO_ORGAO`,
   restaurando o gate institucional que a superfície recursal perdeu na Etapa 1 de
   `D-recursal-superficie-por-papel`.

**Cobertura de teste nova:** `InstitutionalDocumentSecurityGateApplicationServiceTest` (5, unit:
null-safety com/sem fallback legado, no-nomination allow/block, generatedAt); teste novo em
`InstitutionalCriticalActionHttpGuardFilterTest` para o path recursal unificado; `InstitutionalRecursalGateIT`
(2, `PjbFlowItBase` + JWT real contra Postgres: usuário MP resolvido passa pelo gate e chega ao router
com header `X-PJB-Institutional-Gate-Operation=RECURSAL_UNIFICADO`; usuário com uid não materializado no
banco NÃO estoura 500). `RecursalPeticionamentoControllerIT` (8/8) revalidada verde com o filtro já
ativo na cadeia (regressão do path recém-protegido via `@WithMockUser`).

**Escopo assumido conscientemente:** o mesmo bug de ordem afetava ~30 operationCodes; a correção de
ordem os conserta TODOS de uma vez (o filtro inteiro passou a rodar após a auth). Risco real da
mudança é baixo: os endpoints hoje já davam 500/nunca foram exercitados (TCC pré-produção, sem tráfego
real, sem IT cobrindo-os), então ativar o gate corretamente é estritamente melhoria. As demais famílias
(oficial de justiça, perito, psicossocial, etc.) não ganharam IT dedicada nesta etapa — o gate delas
agora roda pela mesma correção de ordem, mas a prova end-to-end por família fica registrada como
extensão natural de cobertura futura, não como bug aberto.

**Atualização:** as 11 famílias restantes (MP, extrajudicial, psicossocial, distribuição,
secretaria especializada, procuradoria, perito, oficial de justiça, delegado, juiz —
gabinete de decisões, e trânsito em julgado) ganharam prova E2E dedicada, mesmo padrão de
`InstitutionalRecursalGateIT`/`InstitutionalMagistraturaGateIT`: `InstitutionalMpGateIT`,
`InstitutionalExtrajudicialGateIT`, `InstitutionalPsicossocialGateIT`,
`InstitutionalDistribuicaoGateIT`, `InstitutionalSecretariaGateIT`,
`InstitutionalProcuradoriaGateIT`, `InstitutionalPeritoGateIT`,
`InstitutionalOficialJusticaGateIT`, `InstitutionalDelegadoGateIT`,
`InstitutionalJuizGabineteGateIT`, `InstitutionalTransitoJulgadoGateIT`. Todas as ~30
`operationCode` do filtro agora têm pelo menos uma família com prova end-to-end própria —
extensão de cobertura fechada.

**Quando revisitar:** ao adicionar novas famílias de ato sensível ao filtro, cobrir com IT via JWT
real (padrão de `InstitutionalRecursalGateIT`). Ao seedar nomeação institucional em teste, usar
`InstitutionalNominationStateRepository.save(...)` para exercitar o caminho de bloqueio real do gate.

## D-recursal-superficie-por-papel

**Status:** FECHADA — Etapas 1, 2, 3 e 4 concluídas. Os 4 controllers legados seguem existindo (têm
outros endpoints ativos além do recurso), mas o endpoint `interporRecurso` e as facades correspondentes
foram removidos; toda interposição de recurso passa exclusivamente pela superfície unificada.

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

**Etapa 1 aplicada — superfície unificada aditiva:** `RecursalPeticionamentoController`
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
removido nesta etapa. Cobertura: `RecursalPeticionamentoPerfilRouterTest` (11 testes: roteamento por
`TipoUsuario` — inclusive PGR rumo ao MP pela classificação canônica do enum —, mapeamento de
rate-limit domain, delegação por perfil, rejeição de perfil sem habilitação),
`RecursalPeticionamentoControllerTest` (4 testes MockMvc: happy path por família de perfil, scope
canônico, escolha correta do `CapabilityRateLimitDomain`) e `RecursalPeticionamentoControllerIT`
(7 testes contra Postgres real com Spring Security completo: anônimo negado sem tocar o router,
`ROLE_JUIZ` recebendo 403 via `@PreAuthorize`, e as quatro famílias legítimas mais o PGR chegando
ao router com o `Perfil` esperado). A DTO reusa `InstitutionalRecursoRequest` já existente em vez
de introduzir uma terceira cópia idêntica; `AdvogadoRecursoRequest` ganhou javadoc apontando para
a superfície canônica e para a Etapa 3 de remoção. Guards `constructor_injection_guard.py` e
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
unificada, esse mesmo PGR passa pelo guard de MP. Não é bug da Etapa 1 — é divergência
preexistente na modelagem de roles do PGR entre `TipoUsuario` (MP-centric) e o `@PreAuthorize` do
controller legado (MP+Procuradoria). A etapa de deprecação (2) deve alinhar os dois lados; até lá,
preferir a nova superfície faz o PGR ficar sempre no caminho canônico do enum.

**Divergência de role `DEFENSOR_DISTRITAL` (não introduzida, apenas não replicada):** o
`@PreAuthorize` do `DefensorPublicoPainelController` legado inclui `DEFENSOR_DISTRITAL`, valor que
não existe em `TipoUsuario` — code-path morto que nunca casa em runtime. A superfície unificada
não replica esse literal; se algum dia a role for adicionada ao enum, ambos os lados precisam ser
atualizados.

**Etapa 4 aplicada** (commit `1b15fc4`): `RecursalPeticionamentoPerfilRouter` ganhou o quinto perfil
(`CIDADAO`), resolvido via `TipoUsuario.isPeticionantePessoal()`, fechando a lacuna do jus postulandi
para embargos de declaração.

**Etapa 2 aplicada** (commits `9ffdf4a`/`343dae2`): os 4 controllers legados passaram a expor headers
RFC 8594 (`Deprecation: true`, `Sunset`, `Link: successor-version`) no endpoint `interporRecurso`,
e a coleção Postman de integração foi sincronizada com a superfície unificada.

**Pré-requisito de Etapa 3 fechado em `D-controllers-recursais-legados-sem-teste-dedicado`:**
cobertura completa (sucesso, validação, autorização real) dos 4 controllers antes de remover
qualquer endpoint, para garantir que nenhum outro comportamento deles fosse afetado pela remoção.

**Gap de infraestrutura achado e corrigido antes da Etapa 3, registrado em
`D-recursal-opa-critical-path-nao-atualizado`:** a política OPA de um overlay de produção real
(`prod-sovereign-opa-ext-authz`) nunca foi atualizada para cobrir `/api/v1/recursal/` desde que a
Etapa 1 foi ao ar — corrigido antes de remover o path legado de MP que a política protegia.

**Etapa 3 aplicada — remoção do endpoint legado:** o método `interporRecurso` (e o `@PostMapping`
correspondente) foi removido dos 4 controllers legados (`AdvogadoCockpitController`,
`DefensorPublicoPainelController`, `MinisterioPublicoPainelController`,
`ProcuradoriaOperacionalController`) — os controllers continuam existindo, com seus demais endpoints
intactos (snapshot, painel, petição, parecer, etc.). Os métodos de facade correspondentes foram
removidos (`AdvogadoSurfaceFacadeService.interporRecurso`,
`InstitutionalPainelSurfaceFacadeService.defensorInterporRecurso`/`.ministerioPublicoInterporRecurso`,
`ProcuradoriaOperationalSurfaceFacadeService.interporRecurso`) — confirmado, via grep, que nenhum
tinha consumidor além do próprio controller legado que os chamava; a camada de serviço subjacente
(`AdvogadoCockpitService.interprorRecurso`, `DefensorPublicoPainelService.interporRecurso`,
`MinisterioPublicoPainelService.interporRecurso`, `ProcuradoriaOperacionalService.interporRecurso`)
foi preservada intacta, pois é exatamente o que `RecursalPeticionamentoPerfilRouter` chama
diretamente. `AdvogadoRecursoRequest` (DTO exclusiva do endpoint legado) e
`RecursalLegacyDeprecationHeaders` (helper de headers RFC 8594, sem mais nenhum chamador) foram
deletados por inteiro. A coleção Postman perdeu a pasta "legado (depreciado, remover apos
28/10/2026)" com os 4 requests órfãos; o contrato estático `docs/openapi/public-api.yaml` perdeu os
4 blocos de path correspondentes (validado com `yaml.safe_load`, 825→821 paths). Testes órfãos dos
4 controllers (headers de depreciação, validação do corpo do recurso) foram removidos das classes
de teste; os testes de sucesso dos demais endpoints permanecem intactos e verdes — nenhuma
regressão nos 63 testes fechados em `D-controllers-recursais-legados-sem-teste-dedicado`, nem nos
testes da própria superfície unificada (`RecursalPeticionamentoControllerTest`/`IT`,
`RecursalPeticionamentoPerfilRouterTest`).

**Consumidor real quase esquecido — 3 rodadas de varredura, cada uma achou mais (revisão pedida
antes de commitar):** a primeira varredura de consumidores só cobriu extensões não-Java
(`.json`/`.md`/`.ts`/`.js`/`.yaml`/`.yml`). Uma segunda varredura sem filtro de extensão encontrou
dois arquivos Java de produção com as URLs legadas hardcoded como string literal:
`RecursalWorkbenchSurfaceCatalog.ministerioPublicoRecurso()`/`.procuradoriaRecurso()` (catálogo de
URLs usado por 5 blueprints de experiência do workbench institucional para montar cards/atalhos de
ação reais) e `InstitutionalWorkbenchProjectionService.actionBlueprints()` (3 entradas — MP,
Defensoria, Procuradoria — ligadas a `MaterialActionCode` real). Se não corrigido, o workbench
continuaria oferecendo ao usuário um botão "Interpor recurso" apontando para uma URL 404.

Uma terceira varredura, ainda mais ampla (regex por qualquer string `/api/v1/...recurso.../`, sem
qualquer filtro de contexto), achou dois problemas mais sérios que os anteriores:

- **`InstitutionalCriticalActionHttpGuardFilter` rodava ANTES da autenticação — bug sistêmico de
  ordem de filtro, agora CORRIGIDO e provado (ver `D-institutional-gate-filter-roda-antes-da-auth`):**
  o filtro era `@Component` com `@Order(Ordered.HIGHEST_PRECEDENCE + 35)` e NÃO era adicionado à cadeia
  do Spring Security. `HIGHEST_PRECEDENCE + 35` (= `Integer.MIN_VALUE + 35`) registra o filtro no
  servlet chain ANTES do `DelegatingFilterProxy` do Spring Security (`SecurityProperties.DEFAULT_FILTER_ORDER = -100`,
  lido do jar 3.5.12), então `SecurityContextHolder` estava vazio quando o filtro rodava e
  `InstitutionalDocumentSecurityGateApplicationService.enforce()` → `CurrentUserService.getRequired()`
  lançava `IllegalStateException` → **HTTP 500 em todo POST institucional protegido** (senteça,
  despacho, manifestação MP, ofício, laudo, ~30 operationCodes). Provado por IT (`InstitutionalGateFilterOrderingProbeIT`,
  temporária, removida após confirmar) cujo stack trace mostrou o throw exatamente nesse ponto, com a
  cadeia do Spring Security ausente entre os filtros que o envolviam. **Correção:** removido o `@Order`
  (mantido `@Component`, seguindo a mesma convenção de `MinisterStepUpFilter`/`DecisionStepUpFilter`,
  que são `@Component` sem `@Order` e adicionados à cadeia) e registrado via
  `http.addFilterAfter(filtro, AuthorizationFilter.class)` em `SecurityConfig` — roda depois de toda
  autenticação e autorização, com o usuário resolvido; `OncePerRequestFilter` deduplica a
  auto-registração residual em `LOWEST_PRECEDENCE`. Como defesa em profundidade, `avaliar()` trocou
  `getRequired()` por `getOrNull()`: usuário não resolvível é tratado como "sem nomeação", seguindo o
  `allowLegacyFallback` (nunca mais 500). E `/api/v1/recursal/processos/*/recurso` foi adicionado ao
  filtro (agora funcional), restaurando o gate institucional que o recursal perdeu na Etapa 1.
  Cobertura: `InstitutionalDocumentSecurityGateApplicationServiceTest` (5, unit, prova null-safety +
  bloqueio estrito), `InstitutionalCriticalActionHttpGuardFilterTest` (4, incl. o path recursal
  unificado) e `InstitutionalRecursalGateIT` (2, JWT real contra Postgres: usuário MP resolvido passa
  pelo gate e chega ao router; usuário não materializado no banco não estoura 500). Regressão:
  `RecursalPeticionamentoControllerIT` (8/8) revalidada verde com o filtro já ativo na cadeia. As 4 ITs
  dos controllers legados só fazem GET (não tocam path protegido por POST) — `shouldNotFilter` pula o
  filtro nelas, sem risco de regressão.
- **`PainelActionSurfaceCompositionService`/`PainelExecutionSurfaceCompositionService`** — dois services
  reais consumidos por `MinisterioPublicoPainelService`/`DefensorPublicoPainelService`/etc. (confirmado
  via grep de chamador) montam o `actionSurface`/`nativeComposition` retornado pelos paineis reais.
  Tinham 4 entradas de "preparar/abrir recurso" apontando pra `/api/v1/mp/recursos` e
  `/api/v1/defensoria/recursos` — **URLs que nunca existiram como endpoint real**, nem antes nem depois
  desta etapa (achado pré-existente, não introduzido aqui, mas na mesma área e mesma ação). As 4 foram
  corrigidas pra `/api/v1/recursal/processos/{processoId}/recurso`. Uma entrada não relacionada
  (`/api/v1/colegiado/recursos/comando`, atalho de desembargador pra revisar recursos recebidos, não
  pra interpor um) foi deixada intacta — semântica diferente, fora do escopo desta etapa.

As strings dos dois `PainelXSurfaceCompositionService` e do `RecursalWorkbenchSurfaceCatalog`/
`InstitutionalWorkbenchProjectionService` foram atualizadas para
`/api/v1/recursal/processos/{processoId}/recurso` — a interposição de recurso é auto-roteada por
perfil no motor unificado, então o mesmo path serve todos os papéis igualmente (o filtro HTTP foi
efetivamente corrigido e religado ao path unificado — ver o item acima). Nenhum teste dos arquivos
alterados assertava o literal antigo (confirmado por grep antes de cada troca); todos os testes
relevantes rodados após as correções: 4/4 `InstitutionalCriticalActionHttpGuardFilterTest` (incl. o
path recursal unificado), 5/5 `InstitutionalDocumentSecurityGateApplicationServiceTest`, 2/2
`InstitutionalRecursalGateIT` (JWT real contra Postgres), 2/2 `PainelActionSurfaceCompositionServiceTest`,
2/2 `PainelExecutionSurfaceCompositionServiceTest`, 4/4 `PainelCompositionNullSafetyTest`, 2/2
`DelegadoPainelServiceInstitucionalTest`, 2/2 `InstitutionalWorkbenchProjectionServiceTest`, 2/2
`InstitutionalWorkbenchServiceTest`, 2/2 `PjbInstitutionalWorkbenchSurfaceArchitectureTest` — 0
falhas. Advogado nunca teve entrada equivalente em nenhum desses arquivos (não é fluxo
"institucional"), então não havia nada a corrigir para esse perfil aqui. `RecursoController`
(`/api/v1/recurso/*`) foi lido e confirmado como feature totalmente diferente e não afetada
(admissibilidade/tempestividade/deserção/readiness — análise, não interposição).

**Gap residual (não fechado nesta etapa):** `docs/openapi/public-api.yaml` também nunca ganhou o
path `/api/v1/recursal/processos/{id}/recurso` desde a Etapa 1 — o contrato vivo (`/v3/api-docs`,
gerado em runtime pelo springdoc) está correto; só o export estático ficou defasado. Não reconstruído
à mão nesta etapa para não arriscar inventar um shape que não bate com o gerado de verdade — revisitar
quando o export estático for regenerado por processo real, não editado manualmente.

**Quando revisitar:** dívida fechada. Se algum dia os 4 controllers legados perderem também seus
demais endpoints (não só o recurso), essa é outra etapa — fora do escopo desta.

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

## D-custas-calculator-fazenda-classificada-como-isenta-cita-cpc-91-errado

**Status:** FECHADA — a etapa de unificação removeu o `CustasProcessuaisCalculatorService` por
completo, junto com seu enum `TipoCusta` paralelo e o teste próprio. O erro jurídico deixa de
existir porque a classe deixa de existir; a decisão foi remover em vez de migrar corrigindo porque
os percentuais hardcoded (`2%` preparo, `1%` multa art. 1.026, `10%` má-fé) não têm base legal
universal — variam por regimento de custas estadual (TJ) ou resolução do CJF, então plantar esses
números como se fossem autoritativos era ruído maior do que corrigir a citação errada do CPC art.
91. Se cálculo de preparo/multa virar necessidade real, será etapa própria com tabela por
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
dos 3 hardcodes da etapa atual (Falencia + FrontendCatalog + EconomicReference) melhora o desenho
mas não elimina a dívida de fonte: enquanto o scheduler não subir, a atualização anual do salário
mínimo continua manual (via PR editando `FALLBACK_OFICIAL`).

**Quando revisitar:** decisão operacional de deploy + segurança. Ativar o scheduler exige (a)
setar `pjb.sync.salario-minimo.enabled=true` no perfil de produção, (b) confirmar que a chamada
externa ao BCB é aceitável no ambiente (whitelist de saída, rate limit), (c) monitorar as
primeiras execuções via log ou métrica dedicada (o scheduler não escreve em `AuditLedgerService`
hoje), (d) avaliar se cabe seed inicial via migration para garantir base populada mesmo antes da
primeira execução. Não integrar essa etapa com a de fixes atuais — é decisão operacional de
outra natureza.

## D-titularidade-cidadao-duplicada-dois-guards

**FECHADA.** Extraído `ProcessoPartyCpfMatcher` (novo, `core/security/access/`) com resultado tipado via `sealed interface PartyMatchResult` (`Matched(PartyRole role)` / `NotMatched`) — elimina a comparação de CPF duplicada byte a byte entre `PjbAuthorizationService.requireReadProcessoAsCidadaoParte` e `PersonalProcessAccessGuardService.requireCurrentUserAsParty`. Os dois métodos de alto nível continuam existindo com suas políticas distintas (o primeiro só age para `CIDADAO` e roda ABAC antes; o segundo age para qualquer autenticado, sem ABAC prévio) — só o predicado interno foi unificado, nenhum dos 11 call sites (10 do primeiro + 1 do segundo) muda de comportamento.
**Fecha por dedup, não por auditabilidade equivalente:** os dois métodos passaram a auditar suas decisões, mas por convenções assimétricas e deliberadamente diferentes — `requireReadProcessoAsCidadaoParte` grava `AUTHZ_CIDADAO_PARTE_ALLOW/DENY` na trilha ABAC real (`PjbAuthorizationTrailAssembler`/`PjbAuthorizationAuditFacade`, com ator/motivo/risco), `requireCurrentUserAsParty` grava um par mais simples `PERSONAL_ACCESS_ALLOW/DENY` direto via `AuditLedgerService` (esse método nunca teve acesso à máquina ABAC, que é `package-private` a `core.security.abac`). As duas trilhas usam espaços de `resourceId` diferentes sob o mesmo `resourceType="PROCESSO"` (`numeroUnificado` vs. id numérico) — não são joináveis entre si por design, achado confirmado na revisão final de branch inteira.

## D-peticionamento-controller-domain-lacuna-cidadao

**FECHADA.** O bug era mais estrutural do que o achado original sugeria: `PeticionamentoController.resolveDomain()` era uma de 3 reimplementações independentes da mesma regra `Authentication`→`CapabilityRateLimitDomain` (a segunda, `ProcessualParticipacaoControllerRateLimitSupport`, tinha o mesmo bug; a terceira, `UserCalendarController`, parecia correta por ter um branch explícito para `CIDADAO`). Corrigido criando `CapabilityRateLimitDomainResolver` (novo `@Component` único) e migrando os 4 controllers pra ele, eliminando as 3 reimplementações de uma vez.
**Achado real durante a revisão final, não durante a implementação:** a versão inicial do resolver copiou `UserCalendarController.resolveDomain` como "referência correta", mas essa referência tinha ela mesma um bug latente desde antes desta fatia — `PjbGrantedAuthorityFactory` concede `ROLE_USER` a *todo* usuário autenticado, não só a `CIDADAO`, então o check `ROLE_CIDADAO || ROLE_USER` pro domínio `CITIZEN` na verdade casava com qualquer um, tornando `INSTITUCIONAL` inalcançável em produção pros 4 controllers migrados (juiz, defensor, procurador, perito etc. caindo silenciosamente em `CITIZEN`). Corrigido removendo `ROLE_USER` do check (só `ROLE_CIDADAO` identifica cidadão de forma confiável); suíte de teste do resolver reconstruída usando `PjbGrantedAuthorityFactory.authoritiesFor(tipo, ente)` real em vez de fixtures de authority isolada, que era estruturalmente incapaz de pegar esse bug.

## D-cidadao-parte-guard-sem-teste-rejeicao

**FECHADA.** `CidadaoInstanciasControllerCpfMismatchIT` (novo, Testcontainers Postgres + JWT real, sem mocks no caminho de autorização) prova 403 para `CIDADAO` cujo CPF não bate com nenhuma parte do processo, e que a decisão gera a entrada `AUTHZ_CIDADAO_PARTE_DENY` real no ledger de auditoria em vez de negação silenciosa; também prova 200 quando o CPF bate com a parte autora. Escopo da dívida original era `requireReadProcessoAsCidadaoParte` especificamente — fechada como tal.
**Achado durante a revisão final, registrado à parte por ser um método diferente:** `requireCurrentUserAsParty` (o guard irmão de `D-titularidade-cidadao-duplicada-dois-guards`) tinha zero cobertura de teste mesmo depois de reescrito para usar o novo predicado compartilhado e ganhar auditoria — fechado na mesma fatia com 2 testes unitários novos (match e no-match, ambos com `verify` no mock de `AuditLedgerService`), mas via teste unitário, não IT (não precisa: `appendSafely` é chamada direta sem dependência de `RequestContext`, ao contrário do caminho ABAC).

## D-controllers-recursais-legados-sem-teste-dedicado

**Status:** FECHADA — cobertura completa (sucesso, validação, autorização real) adicionada aos 4 controllers.

Os 4 controllers recursais legados (`AdvogadoCockpitController`, `DefensorPublicoPainelController`, `MinisterioPublicoPainelController`, `ProcuradoriaOperacionalController`) não tinham nenhuma classe de teste dedicada antes da Etapa 2 de `D-recursal-superficie-por-papel` — só os headers de depreciação de `interporRecurso` ganharam cobertura mínima, os demais endpoints seguem sem teste próprio.

**Fechamento:** cada controller ganhou (a) testes unitários `MockMvc` standalone cobrindo sucesso de todo endpoint restante e falha de validação (400) para todo DTO com constraint real, e (b) uma classe `*IT` nova (`AdvogadoCockpitControllerIT`, `DefensorPublicoPainelControllerIT`, `MinisterioPublicoPainelControllerIT`, `ProcuradoriaOperacionalControllerIT`) contra Postgres real com Spring Security completo, provando anônimo negado (401/403), role fora da lista negada (403) e cada role legítima do `@PreAuthorize` autorizada (200/201) — mesmo padrão de `RecursalPeticionamentoControllerIT`. Totais confirmados via execução real: Advogado 9 unit + 4 IT, Defensor 11 unit + 3 IT, MP 13 unit + 6 IT, Procuradoria 10 unit + 7 IT — 63 testes novos, 0 falhas.

**Dois achados reais descobertos ao escrever as ITs, ambos sem impacto em produção porque `PjbGrantedAuthorityFactory` já concede a combinação certa a qualquer usuário real:**
- `OAB_PRESIDENTE_SECCIONAL`, `PROMOTOR_ELEITORAL` e `PROMOTOR_TRABALHISTA` nunca chegam sozinhos em produção — a fábrica sempre concede `ROLE_ADVOGADO` junto ao primeiro (por ser also-advocacia) e `ROLE_MINISTERIO_PUBLICO`/`ROLE_MEMBRO_MINISTERIO_PUBLICO` junto aos outros dois (por `isMinisterioPublico()==true`). Testar esses papéis isolados nas ITs gerava 403 falso-negativo; corrigido replicando a combinação real de authorities.
- A IT do Defensor não testa sucesso para `DEFENSOR_DISTRITAL` — esse literal aparece no `@PreAuthorize` legado mas não existe como valor de `TipoUsuario` (já registrado acima como divergência); não faz sentido provar "sucesso" para um papel que a fábrica jamais concede a ninguém.

## D-frontend-delivery-routes-nao-sinaliza-depreciacao

`PjbFrontendDeliveryApplicationService.parseRoutes` escaneia `@PostMapping`/`@GetMapping` via regex e não lê headers HTTP de depreciação — os 4 endpoints recursais legados aparecem no catálogo `/api/v1/frontend/delivery/routes` com o mesmo peso da rota unificada nova, achado ao investigar consumidores antes da Etapa 3.
Revisitar se o catálogo vier a ser consumido por um frontend real: cruzar rota com `RecursalLegacyDeprecationHeaders` ou marcador equivalente antes de expor como pronta para uso.

## D-tribunal-rule-engine-wiring-manual-de-colaborador

FECHADA. `TribunalRuleResolutionSupport` e `TribunalRulePackSynchronizationSupport` viraram `@Component` e passaram a ser injetados via construtor em `TribunalRuleEngine`, eliminando os dois `new` internos — único consumidor mapeado (`TribunalRuleEngineTest`, `TribunalRuleEngineBehaviorTest`, 2 sites de construção) e atualizado para montar os colaboradores explicitamente antes de passar ao engine. `mvnw test-compile -pl pjb-api` limpo após a mudança.
Não revisitar — alinhado ao padrão de constructor injection do resto do projeto, nenhum comportamento mudou.

## D-auditoria-salario-minimo-sem-garantia-de-persistencia

FECHADA parcialmente. `AuditLedgerService.persistSafely` continua engolindo exceção em `try/catch` (contrato de "nunca lança" preservado — dezenas de call sites dependem disso), mas agora incrementa `Counter` Micrometer `pjb.audit_ledger.persist_failures` no catch, tornando a falha observável sem mudar o comportamento. `MeterRegistry` injetado via construtor único (sem overload, respeitando `spring_ambiguous_constructor_guard`); 6 testes que construíam a classe manualmente (`BnmpIntegracaoServiceRegistrarBranchesTest`, `IcpBrasilChainValidatorTest`, `RecursalFormalizacaoServiceTest`, `RecursalPdfArtifactValidationServiceTest`, `RecursalPdfLongTermValidationServiceTest`, `RecursalPdfNativeSignatureServiceTest`) atualizados para passar `SimpleMeterRegistry`. `payload_hash=null` sintetizando SHA-256 via `safePayloadHash()` permanece como estava, apenas documentado corretamente.
Revisitar: decidir se falha de persistência de evento crítico deveria propagar ou alimentar retry/outbox em vez de só logar+contar — mudança em classe usada por dezenas de call sites, fora do escopo desta correção pontual.

## D-testes-it-contaminacao-em-lote-amplo-service-package

FECHADA. Causa raiz real (não suposição): sem `forkCount`/`reuseForks` no pom, Failsafe roda todas as ITs do lote na mesma JVM/mesmo banco (`PjbIntegrationTestBase`). `PjbFlowItBase.truncateDatabaseBeforeEach()` autodescobria e truncava TODAS as tabelas de `public` a cada `@BeforeEach`, incluindo `tb_jurisdicao_territorial`/`tb_jurisdicao_territorial_unidade` — catálogos semeados uma única vez pelo Flyway (V304/V305/V306), nunca recriados depois. Qualquer uma das 11 classes que herdam `PjbFlowItBase` rodando antes de `Trt7CearaJurisdicaoCargaIT` no mesmo fork apaga o catálogo para o resto da execução, explicando o sintoma exato (`MunicipioForaDoCatalogo` em vez de `Resolvida`) e por que a classe isolada sempre dava 9/9 verde. Reproduzido deliberadamente com `-Dit.test=AjuizamentoServiceFlowIT,Trt7CearaJurisdicaoCargaIT` (9 falhas) e novamente após o fix (12/12 verde) — não é suposição, é reprodução controlada nos dois sentidos.
Corrigido em duas camadas: (1) excluídos os dois catálogos do TRUNCATE autodescoberto de `PjbFlowItBase`, documentado em Javadoc; (2) `AjuizamentoServiceFlowIT` tinha um `@AfterAll truncateAfterAll()` próprio com a MESMA query copiada e colada, sem a exclusão — causa do fix inicial não bastar sozinho. Extraído `truncateAllTrackedTables()` protected em `PjbFlowItBase`, reutilizado pelo `@AfterAll` em vez de duplicar a SQL, eliminando a duplicação que causou a divergência. `-Dtest="pacote.**"` continua desaconselhado como atalho de regressão ampla (lote compartilhado é característica do design, não bug), mas o vazamento específico que gerava falso-negativo está eliminado e comprovado.

## D-salario-minimo-watchdog-limiar-sem-base-documentada

**FECHADA.** O limiar default de 1 ano (`pjb.observability.salario-minimo.staleness-limiar-anos:1`) foi fundamentado a posteriori com evidência do próprio projeto: `FALLBACK_OFICIAL` registra 2023→2024→2025→2026 sem nenhuma lacuna, confirmando que o reajuste anual do salário mínimo nacional é cadência histórica sem exceção conhecida no período coberto. `defasagemAnos > limiarAnos` (estritamente maior, não `>=`) foi escolha deliberada, não sobra: em janeiro de cada ano o valor do ano novo pode legitimamente ainda não ter sido cadastrado enquanto o decreto está saindo, o que produziria `defasagemAnos == 1` de forma normal e não anômala — usar `> 1` exige que um ciclo anual inteiro tenha sido perdido antes de alertar, evitando falso-positivo recorrente todo início de ano sem deixar de capturar o caso real (dois anos ou mais sem atualização).
Não revisitar por falta de critério — o critério agora é a própria cadência histórica registrada no código; revisitar apenas se o padrão de publicação do decreto mudar (ex.: atraso legislativo real documentado).

## D-anomaisrecenteconhecido-divergia-da-resolucao-real-de-valorPorAno

**FECHADA nesta mesma etapa.** `SalarioMinimoNacionalService.anoMaisRecenteConhecido()` usava `findTopByAtivoTrueOrderByAnoReferenciaDesc()` (máximo irrestrito do banco) e só considerava a persistência quando o ano superava o teto do fallback — divergindo de `valorPorAno()`, que prioriza qualquer registro do banco de forma incondicional, mesmo mais antigo que o fallback. Cenário real: banco só com registro de 2023, fallback até 2026 — o watchdog reportava "sem defasagem" enquanto `valorPorAno(anoAtual)` de fato servia o valor de 2023.
Corrigido reusando a mesma query e cadeia de resolução de `valorPorAno` (`findTopByAnoReferenciaLessThanEqualAndAtivoTrueOrderByAnoReferenciaDesc`), retornando o ano que efetivamente governa o valor servido. 3 testes cobrem banco vazio, banco mais antigo que o fallback (o cenário real do achado) e banco no ano corrente.

## D-mutableclock-duplicado-em-3-testes

FECHADA. Os 3 restantes (`PjbCodebaseSanityApplicationServiceCacheTest`, `PjbWriteFailoverTrackerTest`, `AcordoProcessualApplicationServiceTest`) migrados para `com.tcc.pjb.backend.support.MutableClock`, zerando as 4 cópias originais. `AcordoProcessualApplicationServiceTest` usava acesso direto a campo (`fx.clock.now = ...`), incompatível com a classe compartilhada — adicionado `set(Instant)` a `MutableClock` e os 3 sites de uso migrados para `fx.clock.set(...)`. `mvnw test-compile -pl pjb-api` limpo após a migração.
Não revisitar — nenhuma cópia privada de `MutableClock` restante no módulo.

## D-marketplace-payload-multiplo-anexo

Achado na revisão final de branch inteiro do `D-marketplace-sem-completude-documental` Fase 2. O limite de payload da rota `marketplace-institutional` (`application-api-governance.yml`) foi elevado de 2MB para 8MB — cobre com folga UM anexo no limite documentado de `DocumentContentValidator` (5MB, inflado ~1.33x pelo base64 do JSON). Mas `MarketplaceComplementoDocumentalRequest.documentos` e `MarketplaceProtocoloRequest.documentos` aceitam `List<Attachment>` sem limite de quantidade — um cliente que envie vários anexos grandes na mesma chamada ainda pode estourar o limite de payload antes mesmo de qualquer anexo individual ser validado, recebendo um erro de transporte genérico em vez do `TAMANHO_EXCEDIDO` documentado. Decisão de produto em aberto: limitar quantidade de anexos por chamada, ou elevar o limite de payload proporcionalmente (custo: janela maior para abuso de banda). Não corrigido nesta fatia — corrigir exigiria decidir o número real de anexos esperado por chamada, que não está especificado em nenhum lugar do contrato atual.

## D-marketplace-scope-oauth-nao-checado-no-path-primario

Achado na revisão final de branch inteiro. `ApiMarketplaceController.complementarDocumentos` (e o `protocolar` já existente, que segue o mesmo padrão) resolve `clientId` de duas formas: via `Authentication` já populada pelo filtro de segurança (path primário) ou via `marketplaceOAuth2Service.authorizeHttpRequest(...)` como fallback. O escopo (`processos:documentos`/`processos:protocolar`) só é checado no path de fallback — no path primário, qualquer cliente autenticado alcança o endpoint independente do escopo que possui. `MarketplaceClientApp`/`MarketplaceOAuth2Service.joinScopes` foram corrigidos nesta fatia para provisionar `processos:documentos` por padrão em clientes novos, e a migração `V310__marketplace_client_backfill_scope_documentos.sql` estendeu o escopo aos clientes já existentes (fechando a lacuna de "escopo nunca provisionável" também para o passado). Mas a ausência de checagem no path primário é um padrão pré-existente, compartilhado com `protocolar()`, e não foi tocado — mexer nisso é uma mudança no modelo de autenticação já em produção, fora do escopo desta fatia. Candidato a fatia própria: checar escopo nos dois paths de forma simétrica.
Nuance achada na correção da checagem de posse (mesma revisão): `Processo.connectorClientId` (coluna dedicada que substituiu o parsing ambíguo de `connectorProtocolReference`) é preenchido com `authentication.getName()` no path primário — que pode vir de QUALQUER principal autenticado no `SecurityContext`, não só de um `MarketplaceClientApp`. Como o path primário nunca valida escopo nem que o principal é de fato um cliente marketplace registrado, um principal de outro subsistema cujo nome colida com um `client_id` de marketplace passaria a checagem de posse por igualdade de string, sem checagem de escopo alguma. Mesma causa raiz do parágrafo acima (nenhuma validação de escopo/identidade no path primário), agravada pela ausência de um discriminador de namespace na coluna nova. Mesma decisão: fora do escopo desta fatia, mesmo candidato de correção.

## D-marketplace-connectorclientid-sem-backfill-para-janela-entre-commits

Achado na revisão da correção do finding B (checagem de posse). A migração `V309__processo_connector_client_id.sql` adiciona a coluna `connector_client_id` sem backfill. Isso é seguro para dados anteriores ao commit `c5203968` (que introduziu o endpoint `/documentos` inteiro), mas esse mesmo commit já persistia `connectorProtocolReference` no formato `clientId:referencia` — teoricamente, qualquer `Processo` protocolado entre `c5203968` e a correção (`5b1551c9`) fica com `connector_client_id = null` e nunca mais alcança `complementar()` (404 permanente, sem caminho de remediação operacional). Não corrigido porque não há dado real nessa janela: a branch nunca foi implantada em produção entre esses dois commits — ambas as migrações chegam juntas no primeiro deploy real da fatia. Revisitar apenas se algum dia esses dois commits forem implantados separadamente (não é o plano atual).

## D-reprocessamento-unidade-nova-mesma-transacao-fk-invisivel

**FECHADA.** Achada durante a rodada de correção da revisão formal de `secretarias institucionais diferenciadas` (Important #1), ao escrever o teste real que a própria correção exigia. `UnidadeInstitucionalAdminService.criarUnidade` chamava `unidadeRepository.save(unidade)` (INSERT imediato, `GenerationType.IDENTITY`) e, na MESMA transação (`@Transactional` padrão, `REQUIRED`), chamava `SecretariaInstitucionalEnfileiramentoService.reprocessarSemUnidade(tipo)`, que resolve itens presos via `resolverUnidade(...)` e persiste cada um via `SecretariaInstitucionalItemGravador.gravar(item)`, propositalmente `@Transactional(propagation = REQUIRES_NEW)` (para isolar conflito de índice único por item, ver comentário na própria classe). Quando `resolverUnidade` encontrava a unidade RECÉM-CRIADA na mesma transação externa (ainda não commitada), o `REQUIRES_NEW` rodava em conexão física separada, que só enxerga dados já commitados — a `UPDATE ... unidade_institucional_id = <nova unidade>` falhava com violação de FK (`secretaria_institucional_item_unidade_institucional_id_fkey`), silenciosamente engolida pelo mesmo `catch (DataIntegrityViolationException)` que trata a corrida do índice único. Reproduzido de forma determinística contra um Testcontainers Postgres limpo (não era artefato de dado residual de teste).

**Correção real aplicada** (aprovada explicitamente pelo dono do projeto, depois de registrada como dívida): `UnidadeInstitucionalAdminService.criarUnidade` separado em dois métodos `@Transactional` distintos — `criarUnidade(...)` faz só a criação/persistência/auditoria da unidade e retorna (commita ao retornar); `reprocessarBacklogAposCriacaoDeUnidade(UnidadeInstituicao unidade)` (novo) faz só a chamada a `reprocessarSemUnidade` + auditoria do lote, em transação própria. `UnidadeInstitucionalAdminController.criarUnidade` (que não é transacional) chama os dois métodos em sequência, um depois do outro — nunca via self-invocation dentro da mesma classe (o proxy `@Transactional` do Spring não intercepta chamadas internas `this.metodo()`, mesma armadilha já identificada na investigação do `EquipeSwitchInterceptor`). Como as duas chamadas partem do controller (bean externo), cada uma passa pelo proxy do Spring e abre sua própria transação física — quando `reprocessarBacklogAposCriacaoDeUnidade` começa, a unidade já está commitada e visível para o `REQUIRES_NEW` do gravador. `adicionarAbrangencia` foi confirmado como não afetado (lido de novo antes de concluir): ele referencia uma `UnidadeInstituicao` já existente e commitada, nunca cria uma nova, então o alvo do FK já está sempre visível.

Verificação real: `SecretariaInstitucionalReprocessamentoEntidadeSujaIT` ganhou um segundo teste (`criarUnidadeResolveBacklogDeVerdadeQuandoAUnidadeNovaEAQueOsItensPresosEsperavam`) reproduzindo exatamente o cenário real antes evitado — unidade nova = unidade alvo do backlog — provando que o item preso é resolvido de verdade (status `PENDENTE`, `unidadeInstitucionalId` apontando pra unidade recém-criada) depois da correção. `UnidadeInstitucionalAdminServiceTest`/`UnidadeInstitucionalAdminControllerTest` atualizados para provar a ordem das duas chamadas (`InOrder`) e que `criarUnidade` sozinho não reprocessa mais nada. Rodado 2x seguidas contra Testcontainers Postgres limpo, 0 falhas nas duas vezes.
Não revisitar — o corte de transação está estrutural, não é um workaround pontual.

## D-ha-pgbouncer-prepared-statements

**FECHADA.** Achada durante o round de verificação de boot completo da topologia HA (registrada como dívida aberta em `secretarias-institucionais/fix-round-2-report.md` e narrada no README). `backend`/`backend-a` nunca conseguia subir em `docker-compose.ha.yml`: o Flyway quebrava no boot com `ERROR: prepared statement "S_n" does not exist`. Causa raiz confirmada contra containers reais: `pgbouncer-rw`/`pgbouncer-ro` dessa topologia rodam a imagem 1.18.0 em `pool_mode = transaction`; suporte a prepared statements em modo de pooling por transação (`max_prepared_statements`) só existe a partir do PgBouncer 1.21. Em `pool_mode = transaction` o pgbouncer pode entregar uma conexão física diferente a cada transação — um prepared statement nomeado que o driver pgjdbc cria do lado servidor depois da 5ª execução da mesma query na mesma conexão lógica (`prepareThreshold=5`, default do driver) deixa de existir na física seguinte.

**Correção real aplicada:** `prepareThreshold=0` nas propriedades de datasource (`spring.datasource.hikari.data-source-properties.prepareThreshold` para o caminho de escrita, `pjb.datasource.routing.replica.data-source-properties.prepareThreshold` para o de leitura), desabilitando prepared statements do lado servidor via novo parâmetro `PJB_DB_PREPARE_THRESHOLD` (default `5` = comportamento nativo inalterado em todo lugar que não define a env var — dev, prod, `docker-compose.yml` base, Testcontainers). `docker-compose.ha.yml` passa a fixar `PJB_DB_PREPARE_THRESHOLD: "0"` só em `backend`/`backend-b`. Validado contra a documentação real antes de aplicar: o próprio FAQ do PgBouncer recomenda `prepareThreshold=0` como correção oficial para JDBC nessa combinação (`pool_mode=transaction` + versão sem `max_prepared_statements`); a documentação do driver pgjdbc confirma que o parâmetro desabilita completamente prepared statements do lado servidor. Das 3 alternativas identificadas na investigação anterior (mudar `pool_mode` pra `session`, `prepareThreshold=0`, ou atualizar a imagem do pgbouncer pra ≥1.21), esta foi escolhida por ser a única que não muda o modo de pooling que a topologia HA foi desenhada pra ter nem exige trocar a imagem Docker — menor blast radius, reversível com uma env var.

**Trade-off aceito e documentado:** com `prepareThreshold=0`, o driver nunca usa prepared statement nomeado do lado servidor nessa topologia — perde reuso de plano de execução, binary transfer de parâmetros/resultado, e reenvia o SQL completo a cada execução. Custo aceitável porque é estritamente melhor que o estado anterior (`backend` não subia de jeito nenhum) e porque `pool_mode=transaction` já impunha esse teto de qualquer forma — não há como ter prepared statements nomeados persistentes de verdade sob esse modo de pooling sem subir o pgbouncer pra ≥1.21 (não feito nesta correção).

**Verificação real:** imagem `pjb-backend:local` reconstruída com as mudanças; topologia HA completa subida via `docker compose -f docker-compose.yml -f docker-compose.ha.yml --profile app --profile ha up -d` em projeto Docker isolado (`-p pjb_ha_pgbouncerfix`, volumes novos, `down -v` ao final). `backend` completou as 280 migrations Flyway via `pgbouncer-rw` e alcançou `healthy` (`docker inspect .State.Health.Status`) em **5 boots consecutivos** — zero ocorrência de "prepared statement ... does not exist" em qualquer um. O cenário de >5 execuções da mesma query na mesma conexão lógica foi exercitado de sobra: 280 migrations sequenciais mais o próprio bookkeeping do Flyway em `flyway_schema_history` (INSERT/SELECT repetidos dezenas de vezes) na mesma pool Hikari por trás do `pgbouncer-rw`.

**Achado incidental durante a mesma verificação, não corrigido (fora de escopo):** com o bug de prepared statements resolvido, o boot avança o suficiente para expor uma segunda falha, pré-existente e desta vez encontrada pela primeira vez porque ninguém tinha chegado tão longe: `PjbReplicaTopologyVerifier` (`pjb.datasource.routing.verify-topology-on-startup`, default `true`) executa `select pg_is_in_recovery()` no datasource de leitura e falha com `IllegalStateException: Datasource de leitura padrão: não confirmou réplica PostgreSQL física` — porque `pgbouncer-ro` desta topologia local aponta pro MESMO Postgres que `pgbouncer-rw` (não há réplica física de verdade em `docker-compose.ha.yml`), então `pg_is_in_recovery()` sempre retorna `false`. Isso derruba `backend`/`backend-a` poucos segundos depois de `Started BackendApplication` (confirmado presente já no primeiro boot limpo, antes de qualquer mudança de `backend-b` — não é efeito colateral desta correção). Ver `D-ha-replica-topology-verifier-sem-replica-real` (nova entrada, aberta).

Não revisitar a parte de prepared statements — a causa está eliminada estruturalmente (parâmetro de conexão, não workaround de dado).

## D-ha-replica-topology-verifier-sem-replica-real

**Status:** aberta

**Contexto:** achada durante a verificação de boot completo de `D-ha-pgbouncer-prepared-statements` (mesma investigação, causa diferente). `PjbReplicaTopologyVerifier` valida no startup que o datasource de leitura é uma réplica física real (`select pg_is_in_recovery()` deve retornar `true`), gate controlado por `pjb.datasource.routing.verify-topology-on-startup` (default `true`, não sobrescrito em `docker-compose.ha.yml`). Nessa topologia local, `pgbouncer-ro` aponta pro mesmo Postgres single-node que `pgbouncer-rw` (`PJB_PGBOUNCER_RO_DB_HOST:-postgres`, mesmo host) — não existe réplica física de streaming configurada em `docker-compose.ha.yml`. `pg_is_in_recovery()` portanto sempre retorna `false`, e o verifier derruba a aplicação (`IllegalStateException`) poucos segundos depois de `Started BackendApplication`, entrando em loop de restart (`restart: on-failure:5`) até esgotar as tentativas.

**Risco:** alto pra rodar a topologia HA localmente de ponta a ponta (impede estabilidade indefinida do `backend`), mas não afeta produção real se lá houver uma réplica física de verdade — o verifier está fazendo exatamente o que deveria fazer dado o desenho atual do compose local.

**Cobertura de teste:** nenhuma — só descoberto rodando a topologia real, não há IT que suba `docker-compose.ha.yml` de ponta a ponta.

**Quando revisitar:** ao decidir como o dev local vai simular um read-replica de verdade (ex.: segundo Postgres com `pg_basebackup`/streaming replication, ou desabilitar o verifier via `PJB_DB_READ_VERIFY_TOPOLOGY_ON_STARTUP=false` explicitamente só em `docker-compose.ha.yml` como uma escolha deliberada e documentada, não um bug).

## D-ha-backend-b-elasticsearch-index-race

**Status:** aberta

**Contexto:** achada na mesma rodada de verificação, ao estabilizar `backend-b` o suficiente (depois de corrigir `PJB_LIVE_CLUSTER_ENABLED` ausente, ver abaixo) pra ele avançar além do bug anterior. Com `backend` e `backend-b` subindo ao mesmo tempo contra o mesmo Elasticsearch, `SimpleElasticsearchRepository` (Spring Data Elasticsearch, bean `processoQueryRepository`) chama `createIndexAndMappingIfNeeded()` no construtor sem tratar `resource_already_exists_exception` — quando os dois nós tentam criar o índice `pjb-processos` na mesma janela de boot, o segundo a chegar recebe a exceção do Elasticsearch (`[es/indices.create] failed: [resource_already_exists_exception] index [pjb-processos] already exists`) e falha a inicialização do Spring context inteiro. Race de boot concorrente, não determinístico (depende de qual nó chega primeiro).

**Risco:** médio — só se manifesta quando 2+ instâncias sobem ao mesmo tempo contra um Elasticsearch vazio (primeiro boot de um ambiente novo); uma vez o índice criado por qualquer nó, boots subsequentes não recriam.

**Cobertura de teste:** nenhuma — descoberto rodando a topologia HA real com 2 nós de aplicação simultâneos, cenário que nenhum teste automatizado exercita hoje.

**Quando revisitar:** se a topologia HA precisar bootar de forma confiável com Elasticsearch vazio (ex.: ambiente novo, CI que sobe a stack do zero) — tratar `resource_already_exists_exception` como sucesso idempotente, ou centralizar a criação do índice fora do path de inicialização do repository (migration/init job dedicado).

**Achado corrigido na mesma rodada (não é dívida, já fechado):** `backend-b` também falhava antes disso com `UnsatisfiedDependencyException` em `PjbLivePressureService`/`RedisLiveClusterStateStore` por falta de bean `LiveClusterStateStore` — `backend` (nó 1) herda `PJB_LIVE_CLUSTER_ENABLED: ${PJB_LIVE_CLUSTER_ENABLED:-false}` do `docker-compose.yml` base, mas `backend-b` só existe em `docker-compose.ha.yml` e não tinha essa env var, caindo no default `true` do `application-docker.yml` e tentando montar `RedisLiveClusterStateStore` sem `StringRedisTemplate` elegível. Corrigido adicionando a mesma env var (mesmo default) ao bloco `environment` de `backend-b`.

## D-ha-backend-b-java-opts-sem-hifen

**Status:** FECHADA — 2026-08-13

**Contexto:** achada na mesma verificação. `docker-compose.ha.yml`, serviço `backend-b`: faltava o
`-` na frente de `Dfile.encoding=UTF-8` no valor default de `JAVA_OPTS` (todas as outras flags do
mesmo valor têm `-`, essa não). Quando `PJB_JAVA_OPTS` não era definido no ambiente, o entrypoint
interpretava `Dfile.encoding=UTF-8` como o nome da classe principal a executar: `Error: Could not
find or load main class Dfile.encoding=UTF-8` — `backend-b` nunca chegava a inicializar a JVM,
crash-loop imediato (`Exited (1)`) até esgotar `restart: on-failure:5`.

**Correção:** `-Dfile.encoding=UTF-8`, igual ao padrão já usado em `backend`/`backend-a` no
`docker-compose.yml` base. Mudança de um caractere, sem efeito em nenhum outro serviço.

**Cobertura de teste:** nenhuma — nenhum guard de `docker-compose*.yml` valida sintaxe de JVM
flags dentro de valores de env var. Risco residual: uma futura edição manual pode reintroduzir o
mesmo erro sem detecção automática.

## D-equipe-switch-interceptor-noop-quatro-bugs-empilhados

**FECHADA — 2026-08-13.** O isolamento por equipe/usuário via Hibernate `@Filter`
(`filtroEquipe`/`filtroEquipeProcesso` em `Cliente`/`Processo`) estava confirmado inativo desde a
investigação anterior (`EquipeSwitchInterceptorHibernateFilterIT`, prova direta contra Postgres
real). Duas tentativas de correção anteriores (`TransactionSynchronizationManager` e
`TransactionExecutionListener`, ambas registradas e descartadas em rodadas prévias) falharam
porque nenhuma delas era a causa real — eram todas tentativas de consertar o *timing* de ativação
do filtro dentro de `EquipeSwitchInterceptor.preHandle()`, mas o interceptor nunca chegava a
executar. A causa raiz verdadeira só apareceu depois de instrumentar o `WebConfig` e descobrir que
o bean do interceptor era `null` no registro de interceptors — o que expôs, em cascata, quatro
bugs pré-existentes e independentes, cada um mascarando o próximo:

1. **`EquipeSwitchInterceptor` nunca era criado.** A classe é um `@Component` comum (não uma
   classe de auto-configuração) com `@ConditionalOnBean({MembroEquipeRepository.class,
   EntityManager.class, ...})` no nível da classe. `@ConditionalOnBean` sobre um `@Component`
   escaneado é uma armadilha conhecida do Spring Boot: a condição é avaliada durante a fase de
   component-scan, antes dos beans de infraestrutura JPA/Spring Data (repositórios, EntityManager)
   estarem registrados — a condição resolvia falso sempre, e `WebConfig` (usando
   `ObjectProvider.getIfAvailable()`) simplesmente pulava o registro do interceptor sem erro
   nenhum. Isolamento por equipe morto silenciosamente desde que a anotação foi escrita. Corrigido
   removendo `@ConditionalOnBean` da classe (mantido `@ConditionalOnWebApplication`).
2. **`AuditLedgerService.append`/`appendSafely` sem isolamento transacional.** Assim que o
   interceptor passou a rodar de verdade, qualquer chamador com `@Transactional(readOnly = true)`
   (ex.: `OfficeWorkspaceModeService.current()` → `buildView()`, chamado de dentro do próprio
   `preHandle`) tinha o `INSERT` do log de auditoria rejeitado pelo Postgres ("cannot execute
   INSERT in a read-only transaction"). `persistSafely` engolia a exceção, mas a transação
   ambiente já ficava marcada rollback-only — a chamada inteira falhava com
   `UnexpectedRollbackException` no commit, mesmo em requisições que nunca tocaram auditoria
   diretamente. Corrigido com `@Transactional(propagation = REQUIRES_NEW)` em `append()` e nos 4
   overloads de `appendSafely` (todos precisam da anotação — `appendSafely` chama `append` por
   self-invocation, que não passa pelo proxy do Spring).
3. **`Cliente.filtroEquipe` sem `@FilterDef`.** `Cliente.java` tinha `@Filter(name =
   "filtroEquipe", ...)` mas nenhum `@FilterDef(name = "filtroEquipe", ...)` em lugar nenhum do
   código — `session.enableFilter("filtroEquipe")` sempre lançava `UnknownFilterException`. O
   filtro nunca existiu de verdade na `SessionFactory`. Corrigido adicionando o `@FilterDef`
   correspondente (parâmetros `usuarioIdParam`/`equipeIdParam`, mesmo padrão já usado em
   `Processo.filtroEquipeProcesso`).
4. **`Processo.filtroEquipeProcesso` com parêntese desbalanceado.** A string de `condition` do
   `@Filter` em `Processo.java` tinha 14 parênteses de abertura e 13 de fechamento — um parêntese
   externo aberto em `(((` (linha 62) nunca era fechado no final da condição. Nunca fora exercitado
   porque o filtro nunca chegava a ser habilitado (bug #1). Assim que #1 e #3 foram corrigidos, a
   primeira consulta real via `ProcessoRepository` quebrou com `ERROR: syntax error at end of
   input` (SQLState 42601). Corrigido adicionando o parêntese de fechamento faltante.

**Verificação real:** `EquipeSwitchInterceptorHibernateFilterIT` — 2/2 verde contra Postgres real
(Testcontainers), confirmando `filtroEquipe`/`filtroEquipeProcesso` genuinamente ativos numa Session
vinculada a uma transação de negócio real, e que o `ThreadLocal` de contexto (`EquipeFiltroContexto`)
não vaza entre duas requisições sucessivas na mesma thread.

**Efeito colateral capturado e corrigido na mesma rodada:** com o interceptor genuinamente ativo em
toda rota `/api/v1/**`, `AdvogadoAuditoriaControllerIT.ledgerReturnsEvents` passou a falhar —
`OfficeWorkspaceModeService.current()` agora grava um evento `ADV_OFFICE_MODE_VIEW` real a cada
requisição autenticada, inclusive a chamada MockMvc que o próprio teste faz ao endpoint de ledger,
tornando a asserção posicional (`content.get(0)`) frágil. Teste corrigido para verificar presença
do evento esperado em vez de posição — o comportamento novo é correto (o interceptor deveria
mesmo rodar em toda rota `/api/v1/**`), a asserção antiga é que estava desatualizada.

**Cobertura de teste:** `EquipeSwitchInterceptorHibernateFilterIT` (prova direta, 2 testes),
`AdvogadoAuditoriaControllerIT` (regressão corrigida). Regressão ampla rodada:
`AdvogadoCockpitControllerIT`, `ProcessoCommandControllerIT`, `AuditLedgerServicePayloadHashNuloIT`
— todos verdes.
Não revisitar — os quatro pontos são estruturais, não workarounds.

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
