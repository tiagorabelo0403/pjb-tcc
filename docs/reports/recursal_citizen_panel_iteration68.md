# Round 68 — painel recursal próprio do cidadão

## Objetivo

Reforçar a visibilidade recursal do cidadão sem misturar processos alheios, reutilizando as superfícies já existentes de processos pessoais, overview autenticado, timeline visual, event mirror e legenda de cor processual.

## O que entrou

- `RecursalCitizenPanelBlueprint`
- `RecursalCitizenPanelTrackFactory`
- trilha `PAINEL_CIDADAO_RECURSAL_PROPRIO`
- passo `PUBLICAR_PAINEL_CIDADAO_RECURSAL_PROPRIO`
- novas entradas em `RecursalWorkbenchSurfaceCatalog` para cidadão/processos pessoais/timeline/legend
- novas labels formais para filtro de envolvimento do cidadão, últimas movimentações e cores processuais
- reforço do painel recursal profissional para exigir últimas movimentações e reaproveitamento das cores

## Regra operacional

- cidadão vê apenas processos próprios
- filtro recursal já nasce por ramo, rito, classe e espécie
- overview autenticado preserva instâncias e vínculo recursal
- últimas movimentações e timeline visual entram como parte da leitura do degrau recursal
- cor processual continua vindo das superfícies já existentes do PJB
