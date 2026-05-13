# Round 148 - refinamento do atendimento chat

## Objetivo
Reduzir a concentração estrutural de `AtendimentoChatService` sem quebrar a borda pública do módulo de atendimento.

## Mudança material
A classe `AtendimentoChatService` deixou de concentrar, no mesmo arquivo, acesso processual do chat, projeção de threads/checklists e malha de mensageria/attachments/notificações.

Entraram três suportes explícitos:
- `AtendimentoChatAccessSupport`
- `AtendimentoChatThreadViewSupport`
- `AtendimentoChatMessagingSupport`

Entrou também:
- `AtendimentoChatSupportUtils`

## Efeito estrutural
- `AtendimentoChatService` caiu de 1251 linhas para 585 linhas
- o construtor passou a depender de suportes dedicados em vez de reter diretamente toda a heurística do bounded context

## O que saiu da service principal
### AccessSupport
- validação de acesso de cidadão/advogado ao thread
- validação de leitura processual
- resolução de cidadão por id/CPF para criação do thread

### ThreadViewSupport
- paginação governada de threads
- hidratação de processo/usuários
- agregação de checklist por thread
- cálculo de unread por participante
- montagem de `AtendimentoThreadDto`
- filtragem de advogados válidos para o cidadão no processo

### MessagingSupport
- validação de anexos por limite/tamanho/status
- montagem de `AtendimentoMessageDto` com attachments e reply preview
- publicação de evento live inbox
- notificação de inbox para UI
- notificação de delivered/read/typing
- resolução de mute/quiet hours para tokens de inbox

### SupportUtils
- normalização de body
- normalização de CPF/hash
- utilidades de mute/quiet hours
- normalização de pageable
- título seguro do processo
- topic por usuário
- cálculo de hash da mensagem

## O que ficou na service principal
- borda pública do módulo (`listThreads`, `createThread`, `listMessages`, `sendMessage`, `markRead`, `markDelivered`, `typing`)
- orquestração transacional curta
- integração com TOS/moderação/outbox/receipts

## Evidência executável adicionada
- `AtendimentoChatThreadViewSupportTest`
- `AtendimentoChatMessagingSupportTest`
- `AtendimentoChatServiceRefinementArchitectureTest`

## O que os testes travam
- DTO de thread com unread, mute e checklist agregado
- filtro de advogados sem vínculo real de cliente
- bloqueio de anexos quando desabilitados ou acima do limite
- montagem de DTO de mensagem com preview de resposta, receipt e attachment
- trava arquitetural contra reabsorção das heurísticas removidas

## Observação honesta
Não houve comprovação de build Maven completo neste ambiente. A validação local permaneceu baseada em guards Python, inspeção estrutural, testes adicionados no código e `git diff --check`.
