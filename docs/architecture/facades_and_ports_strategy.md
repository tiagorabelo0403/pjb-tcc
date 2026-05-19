# Estrategia de facades e ports

## 1. Por que os ports e facades sao necessarios

O PJB possui nucleo legado grande, com repositories, entities, services e controllers historicos. Novos modulos nao devem conhecer esse detalhe. Ports e facades criam contratos pequenos, testaveis e orientados a caso de uso.

## 2. Problema atual do megamonolito

O risco atual e cada novo modulo importar `ProcessoRepository`, `UsuarioRepository`, entities JPA ou controller legado diretamente. Isso aumenta acoplamento, dificulta teste, amplia superficie de sigilo e faz regra judicial ficar espalhada.

## 3. Como os modulos novos devem falar com o legado

O fluxo correto e:

`modules.<modulo>.application -> modules.<modulo>.api port -> modules.<modulo>.infrastructure adapter -> legado`

O modulo consome records internos. O adapter converte entities legadas para esses records e concentra a dependencia tecnica.

## 4. Diferenca entre port, facade e adapter

- Port: contrato que a aplicacao do modulo chama.
- Facade: superficie publicada quando outro modulo precisa consumir uma capacidade estavel.
- Adapter: implementacao tecnica que conversa com repository, service externo, banco ou legado.

## 5. Regras de uso

- `modules.*` nao acessa repository legado diretamente fora de `infrastructure`.
- `modules.*` nao acessa controller legado.
- `modules.*` nao depende de web legado.
- `application` chama port ou facade, nao repository.
- `domain` nao depende de Spring, JPA, repository, web ou infrastructure.
- Adapter fica em `infrastructure`.
- Contratos ficam em `api` ou `application`, conforme a fronteira.
- Retorno de port deve ser record/DTO interno, nunca entity JPA do legado.

## 6. Exemplo com modulo acordo

O acordo consulta processo por `ProcessoAcordoPort`, usuario por `UsuarioAcordoPort`, movimentacao por `MovimentacaoAcordoPort` e auditoria por `AuditoriaAcordoPort`.

Os adapters `PjbProcessoAcordoAdapter`, `PjbUsuarioAcordoAdapter`, `PjbMovimentacaoAcordoAdapter` e `JpaAuditoriaAcordoAdapter` concentram a conversa com repositories e entities. A aplicacao da sala recebe apenas contextos e comandos.

## 7. Exemplo futuro com ledger

Um modulo `ledger` deve publicar `LedgerPort` ou `LedgerFacade` com comandos como `registrarEventoImutavel`. Modulos consumidores nao devem acessar tabela, repository ou service interno do ledger.

## 8. Exemplo com prazos e notificacoes

Prazos agora possuem `PrazoProcessualPort`, `PrazoProcessualApplicationService` e `LegacyPrazoProcessualAdapter`. O modulo chamador envia comando interno com tipo de prazo, ramo, grau, tribunal, UF, comarca e data inicial. O adapter conversa com `PrazoProcessualNacionalService` e devolve record modular, sem DTO HTTP e sem entity JPA.

Notificacoes continuam como proxima fronteira: devem expor port de envio/agendamento e consumir resultado de prazo sem conhecer o service legado.

## 9. O que e proibido

- Controller chamando repository.
- Application chamando repository legado.
- Domain importando Spring ou JPA.
- Port retornando entity JPA.
- Adapter fora de `infrastructure`.
- Importar repository interno de outro modulo.
- Criar facade generica sem caso de uso.
- Duplicar regra de negocio do legado sem teste.

## 10. Como testar

- ArchUnit para camada e retorno de ports.
- Guard estatico para imports proibidos.
- Teste unitario de application usando fakes de ports.
- Teste de adapter verificando conversao para record sem expor entity.
- Teste negativo de autorizacao e sigilo.
