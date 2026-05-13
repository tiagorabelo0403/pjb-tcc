# Round 115 - Procedural catalog decomposition

## Objetivo
Reduzir o acoplamento e a concentração de regra em `ProceduralCatalogSupport`, que acumulava ao mesmo tempo:

- resolução pública e contrato externo do catálogo procedural
- definição de snapshots por família de rito
- montagem de stages/work templates
- serialização auxiliar para descrição/catalog

Esse desenho elevava risco de regressão transversal em rito, competência, parties, documentos e workflow procedural.

## O que entrou

### 1. Fachada curta do catálogo procedural
`ProceduralCatalogSupport` passou a concentrar apenas a API pública estável do catálogo:

- `snapshot`
- `enrichDefinition`
- `requiredDocuments`
- `requiredParties`
- `describe`
- `catalog`
- `catalogDrivenRitos`
- `resolveRito` / `tryResolveRito`
- aliases de roles e resolução via TPU

### 2. Definições de rito extraídas
Foi criado `ProceduralCatalogDefinitionSupport` para concentrar:

- switch de famílias procedurais
- snapshots civis, constitucionais, penais, trabalhistas, previdenciários, tributários, administrativos, eleitorais, militares, empresariais, infância, agrário, ambiental, internacional e autocompositivos

### 3. Montagem de stages extraída
Foi criado `ProceduralCatalogStageSupport` para concentrar:

- macro stages
- stages especializados de writ e habeas corpus
- merge de stages/work items
- serialização de stage para mapa
- factories de `PartyRoleSpec`, `DocumentSpec`, `RitoStage` e `WorkTemplate`

### 4. Trava de regressão
Foi adicionado `ProceduralCatalogSupportRefinementArchitectureTest` para garantir:

- `ProceduralCatalogSupport` abaixo do limiar de hotspot
- `ProceduralCatalogDefinitionSupport` abaixo do limiar de hotspot
- `ProceduralCatalogStageSupport` abaixo do limiar local definido para helper estrutural
- permanência da delegação da fachada para os supports

### 5. Pipeline
O `quality-gates.yml` passou a incluir o teste de refinamento do catálogo procedural.

## Resultado objetivo

- `ProceduralCatalogSupport`: 1694 -> 482 linhas
- `ProceduralCatalogDefinitionSupport`: 893 linhas
- `ProceduralCatalogStageSupport`: 374 linhas

## Relação com o diagnóstico sênior
Esta rodada ataca diretamente o item de god classes e ajuda no item de modelo de domínio anêmico/espalhado, porque o catálogo procedural deixa de ser um monólito estático que mistura contrato público, definição material e montagem operacional.

Também ajuda a tornar mais viável a próxima etapa do diagnóstico:

- specification tests por regra jurídica/procedural
- isolamento modular por bounded context procedural
- extração progressiva de `process.lifecycle` / `communication` / `identity.security`

## Próximo alvo sugerido
- `TribunalRuleEngine`
- Pact provider verification real para superfícies sensíveis
- catálogo explícito de classificação LGPD
