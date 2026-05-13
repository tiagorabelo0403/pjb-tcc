# Consulta pública portal unificado 2026

## Objetivo

Fechar a trilha de frontend da consulta pública do PJB com separação explícita entre:

- processos próprios em contexto autenticado
- busca pública por número de processo
- busca pública por pessoa com desambiguação territorial
- busca pública direta por CPF apenas para autos públicos
- leitura pública textual limitada a despacho, decisão, sentença e acórdão efetivamente públicos

## Contrato novo para frontend

### Workspace

`GET /api/v1/public/consultas-publicas/workspace`

Agora o workspace expõe:

- `journeys`
  - `PROCESS_NUMBER`
  - `PERSON_NAME`
  - `PERSON_CPF`
- `publicActs`
  - `DESPACHO_PUBLICO`
  - `DECISAO_PUBLICA`
  - `SENTENCA_PUBLICA`
  - `ACORDAO_PUBLICO`
- `routes.personCandidates`
- `routes.personCandidateProcesses`
- `routes.cpfPublicProcesses`
- `datasets.regionalDisambiguationEnabled`
- `datasets.cpfDirectLookupEnabled`
- `datasets.publicActResolveEnabled`

### Busca por nome

`GET /api/v1/public/processos-pessoas/candidatos?nome=...`

A resposta agora devolve:

- `matchMode=NAME_DISAMBIGUATION`
- `regioes` com bucket por UF, comarca e foro
- `candidatos` ordenados por movimentação recente e volume público

### Busca por CPF

`GET /api/v1/public/processos-pessoas/cpf/{cpf}/processos`

A resposta devolve apenas processos públicos vinculados ao CPF consultado:

- `matchMode=CPF_DIRECT`
- `queryMasked`
- `processos`

## Regras de exposição pública

### Permitido ao cidadão sem vínculo processual

- resumo processual
- movimentações públicas
- identificação territorial do processo
- leitura textual de despacho, decisão, sentença e acórdão públicos

### Vedado na trilha pública comum

- autos sigilosos
- documentos pessoais
- anexos não jurisdicionais
- páginas públicas de petições, laudos ou peças que não sejam atos judiciais liberados
- qualquer documento cuja política de sigilo exija credencial

## Diretriz de frontend

### Tela inicial

Três cards principais:

1. Meus processos
2. Consulta por número do processo
3. Consulta por pessoa

### Busca por pessoa

Quando houver homônimos:

- primeiro renderizar buckets regionais
- depois listar candidatos dentro do bucket escolhido
- só então abrir a lista de processos públicos

### Detalhe público do processo

- cabeçalho institucional com tribunal, comarca, foro e sigilo
- resumo público
- últimas movimentações públicas
- bloco separado de atos públicos disponíveis
- CTA para visão autenticada quando o usuário for o titular

## Backend

Arquivos centrais desta rodada:

- `ConsultasPublicasController`
- `ConsultaPublicaWorkspaceService`
- `ConsultaPublicaSearchService`
- `PublicProcessoPessoaController`
- `ProcessoPesquisaIdentidadePublicaService`
- `ConsultaPublicaDocumentPolicy`
