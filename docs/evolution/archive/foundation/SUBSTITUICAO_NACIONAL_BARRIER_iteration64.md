# Round 64 - barreira completa de runtime, transação e consistência

## Entradas desta rodada
- barreira central de execução agendada por domínio com feature flags em `pjb.runtime.barrier.*`
- endurecimento condicional das configurações sensíveis de Gov.br, MNI, DataJud, DJe, digitalização, integração judicial financeira, segurança judicial, HSM e ICP
- segmentação transacional da execução nacional por coordenador próprio, removendo a transação longa do orquestrador
- persistência da trilha de homologação, migração e comunicação usando referência gerenciada da execução, evitando drift de entidade destacada
- testes estruturais para versões Flyway, contratos de repositório e barreira de scheduler

## Arquivos centrais
- `configs/runtime/PjbRuntimeBarrierProperties`
- `configs/runtime/PjbScheduledExecutionBarrierAspect`
- `core/plataforma/substituicao/application/PjbSubstituicaoNacionalExecutionTransactionCoordinator`
- `core/plataforma/substituicao/application/PjbSubstituicaoPayloadSupport`
- ajustes em `PjbSubstituicaoNacionalExecutionOrchestrator`
- ajustes em `PjbSubstituicaoTribunalHomologacaoProbeService`
- ajustes em `PjbSubstituicaoMigracaoIndustrialBatchService`
- ajustes em `PjbSubstituicaoComunicacaoNacionalSyncService`
