# Regras de dependencia para modulos do PJB

## Permitido

- `web -> application`
- `application -> domain`
- `application -> api/ports`
- `infrastructure -> application/api`
- `infrastructure -> domain` quando necessario para persistencia e mapeamento
- `outro modulo -> api/facade/event`

## Proibido

- `controller -> repository`
- `domain -> Spring`
- `domain -> JPA`
- `domain -> controller`
- `application -> web`
- `infrastructure -> web`
- `integration -> controller`
- `modulo -> repository interno de outro modulo`
- dependencia circular
- `findAll` em fluxo produtivo sem paginacao, limite ou read model
- entity exposta em controller

## Tabela operacional

| Modulo origem | Pode depender de | Nao pode depender de | Motivo | Como testar | Status |
|---|---|---|---|---|---|
| `modules.<modulo>.domain` | Java puro e tipos do proprio dominio | Spring, JPA, web, infrastructure, repository | Dominio deve ser testavel sem container | ArchUnit e guard | Bloqueado para novos modulos |
| `modules.<modulo>.application` | `domain`, `api`, ports, transacao Spring | `web`, controller, DTO HTTP, repository de outro modulo | Caso de uso nao conhece transporte | ArchUnit e guard | Bloqueado para novos modulos |
| `modules.<modulo>.infrastructure` | `application`, `api`, `domain`, JPA, adapters | `web`, controller | Infra implementa portas, nao chama HTTP local | ArchUnit e guard | Bloqueado para novos modulos |
| `modules.<modulo>.web` | `application`, DTOs, validation, security HTTP | repository, entity JPA, regra de negocio | Controller fino reduz vazamento de dominio | ArchUnit e guard | Bloqueado para novos modulos |
| Outro modulo | `api`, facade, event publico | `repository`, `entity`, `infrastructure` interna | Evita acoplamento e ciclos | Guard e review | Meta de migracao |
| Legado `controller/service/repository` | Contratos existentes | Novas dependencias cruzadas sem facade | Preservar compatibilidade enquanto migra | Guard como warning | Baseline tolerado |
| Jobs e schedulers | Application service ou facade | Repository direto quando houver caso de uso | Evita regra dispersa e queries totais | Guard e review | Reduzir por ondas |
| Queries pesadas | Query repository paginado ou read model | `findAll` sem limite em service | Performance e previsibilidade | Guard como warning/error contextual | Reduzir por ondas |

## Regra de importacao entre modulos

Um modulo nao deve importar diretamente `com.tcc.pjb.backend.modules.<outro>.repository`, `entity`, `infrastructure` ou `web`.

Quando a colaboracao for necessaria, criar um contrato em:

```text
com.tcc.pjb.backend.modules.<outro>.api
```

ou uma facade de aplicacao explicitamente documentada.

## Regra para repositorios legados

Repositories em `com.tcc.pjb.backend.model.repository` podem continuar sendo usados por adapters de infraestrutura e legado. Novos modulos devem preferir ports no application e adapters na infrastructure.

## Regra para controllers

Controllers devem:

- Validar entrada.
- Aplicar autorizacao HTTP quando necessario.
- Chamar application service.
- Retornar DTO.

Controllers nao devem:

- Injetar repository.
- Retornar entity JPA.
- Implementar state machine.
- Fazer auditoria diretamente quando isso pertence ao caso de uso.
