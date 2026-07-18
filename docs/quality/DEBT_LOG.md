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

**Status:** aberta (parcialmente fechada — Laiane resolvido)

**Contexto:** `Processo.ufAutor`/`comarcaAutor`/`ufReu`/`comarcaReu` eram populados só pelo canal REST
(via `ProcessoMapper`). **Laiane já foi corrigido**: `EstruturarRequest` captura os 4 campos +
`enderecoReuDesconhecido`, a sessão (`LaianePeticaoInicialDraftSession`, migration V301) os carrega até
`protocolar()`, que os aplica ao `Processo` (flag vence os valores quando o réu é desconhecido).
Marketplace (`ApiMarketplaceService`) e MNI (`MniXmlToProcessoAdapter`) continuam deixando os 4 campos
nulos — cada um seta apenas `uf`/`comarca` (competência), não domicílio de parte. `PoloCompositionPolicy`
deriva `ufDomicilio`/`comarcaDomicilio` diretamente desses 4 campos sem fallback, então o domicílio de
parte fica nulo em `PoloProcessual` nesses 2 canais restantes também.

**Risco:** duas correções de tamanho e natureza diferentes, não uma correção uniforme:
- Marketplace exige mudança de contrato público (`MarketplaceProtocoloRequest` não expõe esses campos
  hoje — afeta integradores externos já conectados).
- MNI exige parsing de endereço por parte no XML (`resolvePartes()` hoje só extrai nome e documento) —
  é extensão de parsing de formato externo, não ajuste pontual.

**Quando revisitar:** ao decidir prioridade de cada um dos dois separadamente — não tratar como um único
item de trabalho.

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

**Contexto:** `vigencia_inicio` das 37 unidades do TRT7/CE e das 155 unidades do TRT3/MG (e, por padrão,
das demais 22 regiões quando carregadas) usa uma data conservadora presumida (CF/88), não a data real
de criação de cada vara.

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

**Contexto:** o documento-fonte do TRT3/MG (`End03.pdf`) atribui o mesmo "Código atribuído pelo TRT" a
3 pares de varas fisicamente distintas: `0031` (3ª e 5ª VT de Contagem), `0070` (2ª VT de Ouro Preto e
1ª VT de Passos) e `0142` (5ª VT de Betim e 2ª VT de Uberaba). Confirmado por leitura direta do texto
extraído — os 6 registros são completos e bem formados, com endereço, e-mail e data de instalação
distintos entre si; não é artefato de parsing. Carregado como está, decisão consciente do usuário.

**Risco:** `TRT3-0070` (por exemplo) aponta simultaneamente para Ouro Preto e para Passos —
identificador de vara ambíguo nesses 3 casos específicos. Para Contagem (0031) o efeito é mais sutil:
como as duas varas com código duplicado atendem exatamente o mesmo conjunto de municípios, o
`Set<String> unidadesElegiveis` colapsa as duas em uma entrada só — a carga não perde competência
territorial nenhuma, mas perde a informação de que existiam originalmente 2 varas ali com códigos que
deveriam ser distintos.

**Quando revisitar:** se o TST/TRT3 publicar uma revisão do documento-fonte corrigindo a duplicidade,
ou se algum fluxo precisar citar univocamente uma dessas 6 varas (ex.: intimação, mandado) — nesse caso
a resolução exige fonte primária adicional (ex.: consulta direta ao TRT3), não inferência.

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
