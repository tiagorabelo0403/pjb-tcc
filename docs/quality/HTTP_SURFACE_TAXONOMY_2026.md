# HTTP Surface Taxonomy 2026

## Diretriz canônica

- Controllers HTTP devem viver sob `com.tcc.pjb.backend.controller`.
- A raiz `api` deixa de ser superfície Java ativa do runtime e não deve receber novos controllers.
- Contratos, DTOs e payloads continuam fora dos controllers, preferencialmente em `model.dto` ou em contratos do bounded context correspondente.

## Rodada aplicada

Nesta rodada foram consolidados dois movimentos:

- remoção dos últimos controllers Java remanescentes sob `com.tcc.pjb.backend.api`
- decomposição do controller gigante do oficial de justiça em três superfícies menores, mantendo as mesmas rotas HTTP

## Controller decomposition do oficial de justiça

A antiga classe única foi repartida em:

- `OficialJusticaPainelController`
  - bootstrap do painel
  - workbench resumido
  - agenda, calendário, notificações e balcão virtual
  - experiência compartilhada
- `OficialJusticaMandadoController`
  - mandados, cumprimento, frustração e avaliação
  - ciência/intimação
  - ofícios e malha externa do ofício
- `OficialJusticaCampoController`
  - rota, telemetria, rastreio e localizador
  - checkpoints, certidões, formalização, juntada e encerramento
  - malha institucional e custódia

## Objetivo arquitetural

- reduzir classes HTTP soberbas
- diminuir construtores inflados por responsabilidades misturadas
- facilitar evolução por eixo funcional sem duplicar rotas
- manter a malha do monólito modular organizada por responsabilidade real
