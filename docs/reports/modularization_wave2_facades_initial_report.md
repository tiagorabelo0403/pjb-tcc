# Relatorio inicial da Onda 2 - facades e ports

## 1. Estado inicial do git

- Branch: `master`
- `git status --short`: limpo
- HEAD inicial da Onda 2: `a7318df arch(modular): criar base cirurgica para evolucao do PJB em monolito modular`
- `HEAD..origin/master`: vazio
- `origin/master..HEAD`: vazio apos push do commit arquitetural anterior

## 2. Envio do commit `a7318df`

O commit `a7318df` foi enviado para `origin/master` antes do inicio das alteracoes da Onda 2. O remoto ficou alinhado com `HEAD`.

## 3. Ports, facades e adapters ja existentes

O modulo `acordo` ja possui:

- `ProcessoAcordoPort`
- `UsuarioAcordoPort`
- `MovimentacaoAcordoPort`
- `AuditoriaAcordoPort`
- `AcordoProcessualStorePort`
- `PjbProcessoAcordoAdapter`
- `PjbUsuarioAcordoAdapter`
- `PjbMovimentacaoAcordoAdapter`
- `JpaAuditoriaAcordoAdapter`
- `JpaAcordoProcessualStoreAdapter`
- `AcordoProcessualChatBridgeService`

Ha tambem varias facades legadas fora de `modules.*`, mas elas ainda nao formam uma fronteira uniforme para novos modulos.

## 4. Dependencias diretas encontradas no modulo acordo

Nao foram encontradas importacoes de repository legado em `acordo.application`, `acordo.domain` ou `acordo.api`.

As dependencias diretas com legado estao concentradas em `acordo.infrastructure`:

- `PjbProcessoAcordoAdapter` usa `ProcessoRepository`, `MovimentacaoProcessualRepository`, `Processo` e `MovimentacaoProcessual`.
- `PjbUsuarioAcordoAdapter` usa `ProcessoRepository`, `UsuarioRepository`, `Processo`, `Usuario` e `TipoUsuario`.
- `PjbMovimentacaoAcordoAdapter` usa `ProcessoRepository`, `UsuarioRepository`, `MovimentacaoProcessualRepository`, `Processo`, `Usuario` e `MovimentacaoProcessual`.
- `JpaAuditoriaAcordoAdapter` usa repository interno do proprio modulo.

## 5. Conexoes com legado que precisam ser isoladas

- Contexto processual deve sair como record do modulo, nunca entity legada.
- Usuario deve sair como contexto reduzido, sem email, CPF, senha ou dado sensivel desnecessario.
- Movimentacao deve receber comando explicito, nao parametros soltos espalhados.
- Auditoria deve receber comando/evento sensivel por contrato, sem regra no controller ou service legado.
- Documento nao sera conectado nesta onda porque o fluxo atual da sala nao consome documento.

## 6. Plano da Onda 2

- Complementar os ports existentes sem duplicar nomes.
- Enriquecer `ProcessoAcordoContexto` e criar records internos para usuario, movimentacao e auditoria.
- Fazer adapters converterem entities legadas para records internos.
- Trocar chamadas soltas de movimentacao/auditoria por comandos nos ports.
- Reforcar ArchUnit e guard para impedir retorno de entity por port e repository fora de infrastructure.
- Atualizar documentacao e testes focados no modulo `acordo`.

## 7. Riscos

- Falso positivo no guard se repositories internos do proprio modulo forem tratados como legado.
- Alterar contrato de port pode exigir ajuste em fakes de teste.
- Contexto processual ainda depende da qualidade dos campos legados disponiveis.
- Nao ha fonte segura atual para `magistradoId`; o valor deve permanecer nulo ate existir contrato legado confiavel.

## 8. O que nao sera mexido agora

- Migrations antigas.
- Controllers legados.
- Separacao global de `model`.
- Ciclos legados `advocacia/laiane/auditoria`.
- FindAll global.
- SecurityConfig, WebSocketConfig e idempotencia.
- Documento processual, salvo se a sala passar a consumir documento nesta rodada.
