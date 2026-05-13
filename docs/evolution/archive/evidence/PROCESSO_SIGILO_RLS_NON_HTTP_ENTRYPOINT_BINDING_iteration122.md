# PROCESSO_SIGILO_RLS_NON_HTTP_ENTRYPOINT_BINDING_ROUND122

## Objetivo
Fechar a lacuna entre o envelope sigiloso materializado no HTTP/async governado e os entry points internos que ainda podiam abrir trabalho sensível sem bind explícito do contexto antes do acesso a banco, read model ou workflow.

## Problema que continuava aberto
Mesmo após os rounds 119 a 121, ainda existia um delta importante: alguns consumers/listeners internos recebiam `processoId`, faziam leitura/materialização sensível ou disparavam job/workflow antes de existir um ponto reutilizável de bind/restore do `PjbProcessoSigiloRlsContext`.

O risco mais crítico era o `ProcessoMaterializadoConsumer`:
- o listener Kafka estava anotado com `@Transactional(readOnly = true)`
- em cenários reais, a transação pode obter conexão antes do corpo do método
- isso permitiria iniciar a sessão SQL com `app.pjb_*` ainda em contexto default
- o bind de sigilo, se feito só dentro do corpo, chegaria tarde demais para esse boundary

## Correção aplicada
Entrou `PjbProcessoSigiloRlsEntryPointSupport`, componente responsável por:
- materializar envelope via `ProcessoSigiloRlsEnvelopeService` a partir de `processoId` e tipo de interação
- fazer `bind`/`restore` do `PjbProcessoSigiloRlsContext` no mesmo thread
- permitir reutilização homogênea em listeners, consumers e bridges internas

## Entry points cobertos nesta rodada
- `ProcessoMaterializadoConsumer`
- `PjbProcessualReadModelProjector`
- `ProntuarioNacionalConsumer`
- `ProcessoAjuizadoWorkflowBridge`
- `CuradorEspecialAutomaticoService`
- `NotificacaoInteligentePJB`

## Ajuste crítico do consumer de materialização
O `ProcessoMaterializadoConsumer` deixou de abrir transação diretamente no método listener.

Agora o fluxo é:
1. resolver `processoId`
2. bind explícito do contexto via `PjbProcessoSigiloRlsEntryPointSupport`
3. abrir leitura transacional via `PjbTransactionalExecutionSupport.executeReadOnly(...)`
4. materializar query model já dentro da sessão SQL com contexto sigiloso correto

Esse ponto foi tratado como correção arquitetural real, não cosmética.

## Evidência executável adicionada
- `PjbProcessoSigiloRlsEntryPointSupportTest`
- `PjbSigiloNonHttpEntryPointArchitectureTest`
- ajuste do `PjbProcessualReadModelProjectorTest` para refletir a nova fronteira obrigatória

## Regressões que passam a ser mais difíceis
- listener/consumer sensível operar sem bind de sigilo
- consumer Kafka abrir boundary transacional antes do bind
- projeções/read models internos ignorarem a sessão sigilosa fora do HTTP
- workflow/event bridge sensível perder o envelope antes de acionar execução governada

## Limitação do ambiente
A rodada segue com a mesma limitação de ambiente:
- o Maven Wrapper não conseguiu baixar `apache-maven-3.9.6-bin.zip`
- portanto a comprovação local continua dependente de guards, inspeção estrutural, testes adicionados e checagem de diff/commit local
