# Round 139 - Triagem Nacional IA Engine Refinement

## Objetivo
Reduzir a concentração semântica da `TriagemNacionalIAEngine` e preservar a engine principal como borda curta de orquestração, sem regressão em persistência, outbox ou cálculo de veredito.

## O que foi extraído
- `TriagemNacionalValidationSupport`
  - normalização do pedido
  - validação documental das partes
  - validação de representação
  - documentos obrigatórios por contexto
  - completude narrativa
  - teto processual e coerência das partes
- `TriagemNacionalInferenceSupport`
  - classificação TPU heurística
  - prescrição/decadência heurística
  - competência inicial
  - detecção de conexidade/litispendência/coisa julgada suspeita
  - confiança geral
  - resumo da triagem

## Resultado estrutural
- `TriagemNacionalIAEngine`: 1321 -> 443 linhas
- construtor da engine passou a depender de suportes dedicados, não dos detalhes internos de validação/inferência

## Evidência adicionada
- `TriagemNacionalValidationSupportTest`
- `TriagemNacionalInferenceSupportTest`
- `TriagemNacionalIAEngineRefinementArchitectureTest`

## Observação honesta
A validação final desta rodada continua baseada em guards Python, inspeção estrutural, `git diff --check` e commit local, porque o Maven Wrapper permanece limitado pelo ambiente.
