# Round 136 - Tribunal plugin manifest refinement

## Objetivo
Reduzir o hotspot de `PluginResolucaoTribunalService` sem cosmética, preservando a borda de aplicação/publicação e extraindo a semântica de manifesto, calendário/prazo e perfil institucional para um suporte dedicado.

## O que entrou
- `PluginResolucaoTribunalManifestSupport`
- delegação do parsing JSON do manifesto
- delegação da fusão legado↔novo de regras e calendário
- delegação da derivação de `pluginId` e resolução de `tipoPlugin`
- delegação do merge de feriados de prazo
- delegação do merge de `PerfilInstanciaTribunalService.PerfilInstancia`
- `PluginResolucaoTribunalManifestSupportTest`
- `PluginResolucaoTribunalServiceRefinementArchitectureTest`

## Resultado estrutural
- `PluginResolucaoTribunalService`: 1413 -> 1040 linhas
- `PluginResolucaoTribunalManifestSupport`: 448 linhas

## Evidência adicionada
- manifesto legado com `regras` e `feriados` continua sendo absorvido no pipeline atual
- derivação de `tipoPlugin` permanece coerente para manifesto completo
- merge de perfil respeita fallback institucional com overrides explícitos do plugin
- combinação de feriados de prazo evita duplicação silenciosa entre prazoConfig, calendário e recesso

## Limitação honesta
O Maven Wrapper continua sem comprovação completa neste ambiente por falha externa no download do `apache-maven-3.9.6-bin.zip`. A validação local desta rodada ficou ancorada em guards Python, inspeção estrutural, testes adicionados no código, `git diff --check` e commit local temporário.
