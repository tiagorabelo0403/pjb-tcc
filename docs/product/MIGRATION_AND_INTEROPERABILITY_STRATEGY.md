# Migration and Interoperability Strategy

A substituição nacional exige convivência controlada com PJe, PJe 2.x, e-SAJ, eproc, Creta e Projudi antes do corte definitivo.

## Princípios

- Migração sem perda de protocolo histórico.
- Reconciliação de partes, advogados, documentos, movimentos, classes, assuntos, sigilo e anexos.
- Dry-run obrigatório antes de importação produtiva.
- Divergência classificada por severidade e evidência.
- Rollback planejado por tribunal, unidade, competência e lote.
- Interoperabilidade por MNI como contrato nacional.
- Conectores legados evoluem em `integration.judicial` ou `integration.mni`, nunca em pacote paralelo.

## Fases

| Fase | Entrega |
|---|---|
| Inventário | Capabilities, conectores, unidades, classes, assuntos e volumes por tribunal. |
| Dry-run | Importação simulada com relatório de divergência. |
| Reconciliação | Saneamento de identidade, documento, movimento, sigilo e protocolo. |
| Homologação | Testes por tribunal, MNI, DataJud, certidão, assinatura e secretaria. |
| Piloto | Escopo reduzido com operação supervisionada. |
| Corte | Substituição por onda, com rollback e prova de integridade. |
| Pós-corte | Replay, observabilidade, suporte, métricas e aprendizado operacional. |

## Matriz MNI

A compatibilidade MNI é representada em:

```text
pjb-api/src/main/java/com/tcc/pjb/backend/integration/mni/compatibility
```

Cada operação por tribunal deve ter status `VERIFIED`, `DEGRADED`, `BLOCKED` ou `NOT_DECLARED`.

## Próximas evoluções

- Relatório de divergência por lote.
- Certidão técnica de migração.
- Painel de cutover por tribunal.
- Plano de rollback materializado por unidade.
- Replay governado de eventos externos.
