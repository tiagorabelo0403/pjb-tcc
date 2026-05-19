# Guia oficial de modulos do PJB

## 1. Como criar novo modulo

Todo novo modulo deve nascer em:

```text
pjb-api/src/main/java/com/tcc/pjb/backend/modules/<modulo>
```

com a estrutura:

```text
domain
application
infrastructure
web
api
```

## 2. Estrutura obrigatoria

```text
com.tcc.pjb.backend.modules.<modulo>.domain
com.tcc.pjb.backend.modules.<modulo>.application
com.tcc.pjb.backend.modules.<modulo>.infrastructure
com.tcc.pjb.backend.modules.<modulo>.web
com.tcc.pjb.backend.modules.<modulo>.api
```

## 3. O que fica em domain

- Regras puras.
- Policies.
- State machines.
- Value objects.
- Enums de dominio.
- Exceptions de dominio.

Nao fica em domain:

- Spring.
- JPA.
- Controller.
- Repository.
- DTO HTTP.

## 4. O que fica em application

- Casos de uso.
- Application services transacionais.
- Autorizacao de caso de uso.
- Auditoria de ato sensivel.
- Ports de persistencia.
- Ports de integracao.
- Publicacao de eventos.

Application nao deve conhecer HTTP.

## 5. O que fica em infrastructure

- Entities JPA.
- Spring Data repositories.
- Adapters de ports.
- Integracao com repositories legados.
- Mapeamento entre entity e snapshot.

Infrastructure nao deve depender de web.

## 6. O que fica em web

- Controllers.
- DTOs de request/response.
- Bean Validation.
- Mapeamento HTTP.
- Rate limit e guardas de superficie.

Controller deve chamar application service e nao repository.

## 7. O que fica em api

- Ports consumidos por outros contextos.
- Facades publicas.
- Eventos publicos.
- Contracts de integracao interna.

## 8. Como criar ports

Ports devem descrever a necessidade do modulo, nao a tecnologia usada.

Exemplo:

```text
ProcessoAcordoPort.obterContextoProcessual(processoId)
```

em vez de expor `ProcessoRepository`.

## 9. Como criar facades

Facades devem ser pequenas e orientadas a caso de uso. Elas podem ser consumidas por outros modulos quando uma dependencia por evento ainda nao for adequada.

## 10. Como publicar eventos

Eventos devem conter somente dados necessarios para o consumidor. Nao publicar entity JPA. Quando a operacao for critica, registrar auditoria antes ou junto da publicacao.

## 11. Como evitar dependencia circular

- Consumir `api` do outro modulo, nao `infrastructure`.
- Nao importar repository de outro modulo.
- Evitar que dois application services chamem um ao outro.
- Preferir evento ou facade unidirecional.
- Criar contrato compartilhado pequeno quando necessario.

## 12. Como testar

Todo modulo novo deve ter:

- Teste de dominio para policy/state machine.
- Teste de application service com ports fake ou mocks.
- Teste de adapter quando houver regra de persistencia relevante.
- Teste ArchUnit para fronteira.
- Teste negativo para autorizacao e estado invalido.

## 13. Como documentar

Criar ou atualizar:

```text
docs/architecture/modules/<modulo>.md
```

com responsabilidade, fronteira, tabelas, services, eventos, testes, riscos e proxima fase.

## 14. Como nao quebrar legado

- Nao mover classes legadas em massa.
- Nao trocar endpoint sem adaptador.
- Nao alterar migration antiga.
- Criar adapter ou facade antes de substituir dependencia.
- Manter contrato antigo ate a UI e os consumidores migrarem.

## 15. Exemplo usando o modulo acordo

O modulo `acordo` e a referencia atual:

- `domain`: `AcordoProcessualWindowPolicy`, `AcordoProcessualStateMachine`.
- `application`: `AcordoProcessualApplicationService`, store port e ponte de chat.
- `api`: ports de processo, usuario, auditoria, movimentacao e contexto de chat.
- `infrastructure`: JPA entities, repositories e adapters.
- `web`: reservado para endpoints especificos quando houver superficie segura dedicada.

A integracao com o chat legado foi feita por application bridge e DTOs de compatibilidade, sem expor repositories da sala ao controller.

## 16. Exemplo usando o modulo prazos

O modulo `prazos` e a referencia para encapsular um nucleo legado existente sem mover classes em massa:

- `domain`: `PrazoProcessualBoundaryPolicy`, parametros e exception de dominio.
- `application`: `PrazoProcessualApplicationService`.
- `api`: `PrazoProcessualPort`, comandos e resultados internos.
- `infrastructure`: `LegacyPrazoProcessualAdapter`.
- `web`: nao criado porque os endpoints legados permanecem em funcionamento.

Esse padrao deve ser usado quando o legado ja tem regra robusta e o objetivo da onda e impedir acesso direto por novos modulos.
