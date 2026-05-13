# Architecture Hygiene Round 106

## Objetivo
Fechar uma rodada mais ampla de higiene estrutural sem reescrever contratos HTTP nem abrir trilha paralela no monólito modular.

## O que entrou

### Extração de tipos internos enterrados em classes grandes
Foram removidos tipos internos privados e package-private de arquivos grandes para arquivos próprios no mesmo pacote, reduzindo acoplamento estrutural e facilitando navegação da base:

- `core/distribuicao/DistribuicaoProcessualNacionalEngine`
  - `DistributionAssessment`
- `core/identidade/grafo/application/IdentidadeJuridicaGraphApplicationService`
  - `GraphProjection`
  - `PathCandidate`
  - `IdentidadeJuridicaVerticeAccumulator`
  - `IdentidadeJuridicaArestaAccumulator`
- `service/triagem/TriagemNacionalIAEngine`
  - `PrazoLegal`
  - `NaturezaPrazo`
- `inovacao/radar/RadarPadroesService`
  - `TetoDiagnostico`
- `core/comunicacao/institucional/CatalogoInstitucionalUnificadoService`
  - `ScoredUnit`
- `platform/jusos/v2/conciliacao/CejuscEngine`
  - `CachedResultadoRegistro`
- `service/secretariat/SecretariaOficialCumprimentoRoutingService`
  - `Classification`
  - `MaterializationAct`
- `platform/jusos/v2/colegiado/NationalColegiadoEngine`
  - `ContagemVotos`

### Extração de mapper de chat
Foi criado `AtendimentoChatMessageMapper` para retirar da `AtendimentoChatService` a montagem repetitiva de:

- preview de resposta
- ocultação de conteúdo bloqueado/quarentenado
- display do remetente
- DTO final de mensagem e anexo

### Extração de suporte de expedição judicial
Foi criado `CitacaoIntimacaoExpedicaoSupport` para retirar da `CitacaoIntimacaoEngine` o bloco repetitivo de:

- resolução do tipo do destinatário
- extração de documento e nome
- montagem de fundamentação
- rebuild de request
- montagem de vias de interceptação
- hashing e mascaramento
- resposta de expedição
- alvo jurídico para HSM

## Ganho estrutural observado

### Redução de tamanho dos arquivos principais

- `CitacaoIntimacaoEngine`: **1593 -> 1052 linhas**
- `IdentidadeJuridicaGraphApplicationService`: **1187 -> 1083 linhas**
- `AtendimentoChatService`: **1419 -> 1251 linhas**
- `SecretariaOficialCumprimentoRoutingService`: **1261 -> 1107 linhas**
- `DistribuicaoProcessualNacionalEngine`: **1234 -> 1214 linhas**
- `TriagemNacionalIAEngine`: **1329 -> 1321 linhas**
- `RadarPadroesService`: **1043 -> 1030 linhas**
- `CatalogoInstitucionalUnificadoService`: **1019 -> 1016 linhas**
- `CejuscEngine`: **1057 -> 1047 linhas**
- `NationalColegiadoEngine`: **1403 -> 1401 linhas**

### Nova guarda estrutural
Entrou `scripts/internal_type_hygiene_guard.py` com relatórios:

- `docs/reports/internal_type_hygiene_guard.json`
- `docs/reports/internal_type_hygiene_guard.md`

Essa guarda mostra quais arquivos ainda concentram enums/records/classes internas demais acima de um threshold configurado.

## Resultado arquitetural
A base ficou melhor em quatro pontos:

- menos arquivo gigante carregando tipos auxiliares internos
- menos mapper/DTO builder enterrado em service principal
- menor atrito para próximas extrações por bounded context
- mais capacidade de detectar regressão estrutural cedo

## Próximo uso prático
Essa rodada deixa pronto o caminho para uma futura extração mais segura dos grandes blocos que ainda restam, sem misturar regras operacionais, DTOs internos e tipos auxiliares dentro do mesmo arquivo.
