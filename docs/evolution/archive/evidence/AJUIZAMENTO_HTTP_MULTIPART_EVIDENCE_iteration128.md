# Round 128 - Evidência executável do boundary HTTP multipart de ajuizamento

## Objetivo
Fechar uma lacuna que ainda permanecia aberta após o phase split do command path: o boundary HTTP multipart do ajuizamento ainda não estava provado com Testcontainers/MockMvc real e ainda carregava um bug silencioso de propagação do `juizo100Digital`.

## O que entrou
- `ProcessoCommandSurfaceFacadeService`
- `ProcessoCommandController` reduzido para boundary HTTP fino, delegando integralmente para a surface facade
- `ProcessoRequest` passou a carregar `juizo100Digital`
- o boundary HTTP passou a aceitar ausência de `anexos` sem regressão funcional
- filtro explícito de arquivos vazios antes do `SmartFileSplitter`
- `ProcessoCommandSurfaceFacadeServiceTest`
- `ProcessoCommandControllerIT`
- `PjbAjuizamentoHttpBoundaryArchitectureTest`
- `docs/openapi/public-api.yaml` ampliado com requestBody multipart mais honesto para `/api/v1/processos/ajuizar`

## O que esta rodada passa a provar
- o ajuizamento via `multipart/form-data` persiste processo real em banco PostgreSQL/Testcontainers
- o usuário autenticado no boundary HTTP é resolvido e ligado ao processo persistido
- `juizo100Digital=true` deixa de morrer no controller e passa a atravessar o fluxo até o pós-commit governado
- a ausência de `anexos` não quebra o ajuizamento HTTP
- anexo não PDF é rejeitado no boundary multipart sem persistência indevida

## Resultado estrutural
- o `ProcessoCommandController` deixa de acumular montagem do comando, split de anexos, resolução do usuário e montagem de resposta
- o bug do `juizo100Digital` hardcoded em `false` é removido da surface HTTP
- entra trava arquitetural para impedir regressão do boundary de volta ao acoplamento anterior

## Limitações honestas
- a validação local continua baseada em guards, inspeção estrutural, testes adicionados no código, `git diff --check` e commit local
- o Maven Wrapper segue sem comprovação completa neste ambiente por falha externa no download de `apache-maven-3.9.6-bin.zip`
