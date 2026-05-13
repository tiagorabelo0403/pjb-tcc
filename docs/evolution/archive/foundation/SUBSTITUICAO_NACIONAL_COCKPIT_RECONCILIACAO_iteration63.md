# Substituição nacional - round 63

## O que entrou

- detalhe operacional da execução nacional
- cockpit administrativo de onda/cutover/rollback
- reconciliação por tribunal com evidência exportável
- novas rotas canônicas sem alias literal solto
- blindagem contra truncamento silencioso no cockpit e na reconciliação

## Endpoints novos

- `GET /api/v1/processual/plataforma/substituicao-nacional/execucoes/{execucaoId}/operacional`
- `GET /api/v1/processual/plataforma/substituicao-nacional/cockpit`
- `GET /api/v1/processual/plataforma/substituicao-nacional/reconciliacao/tribunal/{tribunalCodigo}`
- `GET /api/v1/processual/plataforma/substituicao-nacional/reconciliacao/tribunal/{tribunalCodigo}/evidencia-exportavel`

## Estruturas novas

- `PjbSubstituicaoNacionalOperationalCockpitApplicationService`
- DTOs operacionais de probe, lote, cursor, item, cockpit e reconciliação
- repositórios ampliados para leitura por tribunal

## Endurecimento aplicado

- cockpit e reconciliação deixaram de depender da listagem limitada a 100 execuções
- payloads dos DTOs novos não usam `Map.copyOf` para evitar falha com valores nulos vindos de JSON real
- leitura operacional reutiliza a persistência já materializada pelos rounds anteriores, sem pipeline paralelo

## Testes

- `PjbSubstituicaoNacionalRouteHardeningTest`
- `PjbSubstituicaoNacionalExecutionFacadeServiceTest`
