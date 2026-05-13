# PROCESSO_SIGILO_RLS_ASYNC_PROPAGATION_ROUND121

## Objetivo
Fechar a lacuna entre o envelope HTTP de sigilo e os fluxos não HTTP/assíncronos governados pela plataforma.

## O que entrou
- `PjbExecutionContextTaskDecorator` para propagar `SecurityContext` e `PjbProcessoSigiloRlsContext`
- `PjbRuntimeAccelerationConfiguration` passou a aplicar o decorator em `taskExecutor`, `pjbIoExecutor`, `pjbBurstExecutor`, `pjbExternalIoExecutor`, `pjbLiveExecutor` e `pjbJobExecutor`
- `JudicialConnectorSecurityConfiguration` passou a aplicar o mesmo decorator
- `PjbExecutionOrchestrator` agora captura `SessionSettings` no submit e restaura dentro da thread executora antes de rodar o supplier

## Risco tratado
Sem essa propagação, um fluxo iniciado em request sigilosa poderia perder o escopo de sigilo ao cruzar uma fronteira assíncrona governada, consultando read models seguros com session settings default.

## Evidência executável
- `PjbExecutionContextTaskDecoratorTest`
- `PjbExecutionOrchestratorTest#devePropagarContextoDeSigiloParaExecucaoAssincrona`

## Estado honesto
A rodada materializa propagação de contexto em runtime governado e reduz drift entre HTTP, executor e DataSource. Ainda é valioso adicionar bindings explícitos em jobs/batches que iniciam fora do ciclo web.
