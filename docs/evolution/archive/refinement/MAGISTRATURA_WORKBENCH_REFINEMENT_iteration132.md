# Round 132 - Decomposição do MagistraturaJudicialActWorkbenchService

## Objetivo
Reduzir o hotspot remanescente do eixo `/api/v1/magistratura/atos` sem alterar a surface pública endurecida na rodada 131, preservando budgets explícitos, provider contracts, IT HTTP real e leitura quente protegida por `EntityGraph`.

## O que entrou
- extração de `MagistraturaJudicialActProjectionSupport`
- extração de `MagistraturaJudicialActExecutionSupport`
- `MagistraturaJudicialActWorkbenchService` reduzido a orchestration service curto
- novos testes:
  - `MagistraturaJudicialActWorkbenchRefinementArchitectureTest`
  - `MagistraturaJudicialActExecutionSupportTest`
- `MagistraturaJudicialActWorkbenchServiceTest` reescrito para provar preservação da API pública do service já decomposto

## Ganho arquitetural
A leitura/catálogo/preview do workbench da magistratura deixou de conviver no mesmo arquivo com a execução material dos atos, a formalização de despacho/decisão relatorial e a serialização de payloads operacionais.

O `MagistraturaJudicialActWorkbenchService` passou a concentrar apenas:
- identidade/persona do magistrado
- carregamento/autorização do processo
- validação da action solicitada
- consulta do guard rail material
- composição final da resposta pública do workbench

A projeção/catálogo/template/fields/rotas nativas ficou em `MagistraturaJudicialActProjectionSupport`, enquanto a execução dos atos e a formalização relatorial ficaram em `MagistraturaJudicialActExecutionSupport`.

## Evidência adicionada
- trava de hotspot por tamanho abaixo de 420 linhas para o service principal
- trava explícita garantindo que métodos de catálogo/template/nativeRoute/relatoria permaneçam fora do `MagistraturaJudicialActWorkbenchService`
- testes unitários cobrindo:
  - delegação do service para o support de execução
  - preview com métricas/razões preservadas
  - execução singular e colegiada no support de execução
  - persistência/documento formal no despacho relatorial

## Resultado prático
A surface de magistratura segue compatível com os contratos e ITs já existentes, mas o núcleo de serviço deixou de concentrar leitura quente, catálogo de atos, template routing, formalização relatorial e dispatch material no mesmo arquivo.
