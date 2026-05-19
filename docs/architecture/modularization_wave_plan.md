# Plano de migracao por ondas para monolito modular

## ONDA 1: novos modulos nascem em `modules.*`

- Objetivo: impedir que novos contextos nascam no pacote legado.
- Risco: excesso de formalismo para fluxos pequenos.
- Arquivos provaveis: novos pacotes em `modules.<modulo>`.
- Teste obrigatorio: ArchUnit de camada e teste de application service.
- Evidencia: doc do modulo e relatorio de guard.
- O que nao fazer: mover classes antigas em massa.

## ONDA 2: facades para processo, usuario, documento, movimentacao e auditoria

- Status: iniciada no modulo `acordo`.
- Objetivo: reduzir dependencia direta em repositories compartilhados.
- Risco: criar facades anemicas ou duplicar regra.
- Arquivos provaveis: `modules.<modulo>.api`, adapters em `infrastructure`.
- Teste obrigatorio: contrato de facade e teste de autorizacao.
- Evidencia: dependencias removidas do modulo consumidor e repositories legados restritos a adapters.
- O que nao fazer: substituir todos os repositories do sistema de uma vez.

Aplicacao inicial:

- `acordo.application` consome `ProcessoAcordoPort`, `UsuarioAcordoPort`, `MovimentacaoAcordoPort` e `AuditoriaAcordoPort`.
- `acordo.api` publica records internos de contexto/comando, sem entity JPA legada.
- `acordo.infrastructure` concentra `ProcessoRepository`, `UsuarioRepository` e `MovimentacaoProcessualRepository`.
- Documento ficou fora desta onda porque a sala ainda nao consome documento processual.

Proxima aplicacao da Onda 2:

- Repetir o padrao em ledger.
- Depois aplicar em prazos/notificacoes.
- Depois documento.
- Depois auditoria global.

## ONDA 3: bloquear novas violacoes

- Status: iniciada com baseline numerico do guard modular.
- Objetivo: transformar regras em guardas automatizados.
- Risco: falso positivo agressivo travar entrega legitima.
- Arquivos provaveis: scripts, ArchUnit e docs de baseline.
- Teste obrigatorio: guard e suite de arquitetura.
- Evidencia: relatorio com warnings legados e zero erros novos.
- O que nao fazer: quebrar build por divida antiga catalogada.

Aplicacao inicial:

- `scripts/modular_monolith_guard.py` compara warnings atuais contra `docs/architecture/modular_monolith_guard_baseline.json`.
- O baseline inicial ficou em 419 warnings e 0 errors.
- Warnings catalogados podem reduzir; crescimento acima do orcamento falha o guard.
- O relatorio JSON `docs/reports/modular_monolith_guard_report.json` permite comparar regras por tipo sem depender apenas do markdown.

Proxima aplicacao da Onda 3:

- Ligar o guard no fluxo de CI quando o pipeline estiver estavel.
- Rebaixar o baseline por onda concluida.
- Criar politica de aprovacao para qualquer aumento temporario de budget.

## ONDA 4: migrar fluxos pequenos e isolados

- Status: iniciada com fronteira modular para prazos processuais.
- Objetivo: mover casos de uso com baixa dependencia cruzada.
- Risco: quebrar endpoint ou comportamento historico.
- Arquivos provaveis: services pequenos, controllers finos, adapters.
- Teste obrigatorio: teste de regressao do fluxo e teste de fronteira.
- Evidencia: antes/depois do grafo de imports.
- O que nao fazer: migrar fluxo critico sem teste negativo.

Aplicacao inicial:

- `modules.prazos.api` publica comandos e resultados internos.
- `modules.prazos.domain` valida fronteira e conferencia manual.
- `modules.prazos.application` oferece caso de uso de calculo e dia forense.
- `modules.prazos.infrastructure` concentra o adapter para `PrazoProcessualNacionalService`.
- Nenhum endpoint legado foi alterado.

Proxima aplicacao da Onda 4:

- Criar fronteira de notificacoes consumindo resultado de prazo.
- Migrar um fluxo pequeno de alerta de prazo para a fronteira modular.
- Manter auditoria global fora ate rodada propria.

Aplicacao complementar:

- `modules.notificacoes.api` publica contrato de alerta de prazo.
- `modules.notificacoes.domain` normaliza prioridade, texto, origem e chave.
- `modules.notificacoes.application` consome `PrazoProcessualCalculoResult`.
- `modules.notificacoes.infrastructure` adapta para `CalendarNotificationEventPublisher`.
- Nenhum canal, repository ou controller legado foi reescrito.

Proxima aplicacao complementar:

- Conectar um fluxo pequeno existente de alerta de prazo ao novo application service.
- Depois avaliar port geral de notificacao sem escalar a mudanca para todos os canais.

## ONDA 5: criar read models para consultas pesadas

- Objetivo: remover `findAll` e consultas amplas de services.
- Risco: inconsistencias entre projection e entity.
- Arquivos provaveis: query repositories, projections, materialized views quando aplicavel.
- Teste obrigatorio: teste de consulta paginada e carga minima.
- Evidencia: remocao de consulta total do fluxo produtivo.
- O que nao fazer: esconder consulta pesada atras de facade sem resolver performance.

## ONDA 6: mover dominio puro para `pjb-core` ou `modules` quando seguro

- Objetivo: separar regra pura de infraestrutura Spring.
- Risco: quebrar serializacao, JPA ou mapeamento legado.
- Arquivos provaveis: policies, state machines, value objects.
- Teste obrigatorio: teste unitario sem Spring.
- Evidencia: dominio sem dependencias de framework.
- O que nao fazer: mover entity JPA para core como se fosse dominio puro.

## ONDA 7: avaliar Spring Modulith apenas se fizer sentido

- Objetivo: decidir se a ferramenta agrega verificacao e eventos sem custo excessivo.
- Risco: adicionar framework sem resolver fronteiras reais.
- Arquivos provaveis: pom, anotacoes de modulo, testes de ApplicationModule.
- Teste obrigatorio: prova de conceito em um modulo ja estabilizado.
- Evidencia: regra automatizada adicional e ganho claro.
- O que nao fazer: instalar Spring Modulith para mascarar ausencia de bounded contexts.

## Sequencia recomendada

1. Consolidar `acordo` como modulo referencia.
2. Criar facades de processo, usuario, documento, movimentacao e auditoria.
3. Aplicar guard em todo novo modulo.
4. Migrar um fluxo pequeno por vez.
5. Reduzir consultas totais.
6. Separar dominio puro.
7. Reavaliar ferramenta externa de modularidade.
