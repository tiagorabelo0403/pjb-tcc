# Trio de autorização do cidadão — Design

## Contexto

Três dívidas técnicas abertas em `docs/quality/DEBT_LOG.md`, todas achadas na mesma investigação (Fatia 4 de `D-recursal-superficie-por-papel`):

- **`D-titularidade-cidadao-duplicada-dois-guards`** — `PjbAuthorizationService.requireReadProcessoAsCidadaoParte` e `PersonalProcessAccessGuardService.requireCurrentUserAsParty` reimplementam, byte a byte, a mesma comparação de CPF (`parteAutoraCpf` / `parteReuCpf` / `processo.getUsuario().getCpf()`).
- **`D-peticionamento-controller-domain-lacuna-cidadao`** — `PeticionamentoController.resolveDomain()` não tem branch para `CIDADAO` e cai por omissão em `CapabilityRateLimitDomain.LAWYER`.
- **`D-cidadao-parte-guard-sem-teste-rejeicao`** — nenhum teste prova a rejeição real por CPF divergente em nenhum dos 10 call sites reais de `requireReadProcessoAsCidadaoParte`.

Investigação de código (não só releitura do DEBT_LOG) confirmou 3 fatos que mudam o desenho:

1. **O bug de domínio de rate-limit está duplicado num terceiro lugar não documentado**: `ProcessualParticipacaoControllerRateLimitSupport` tem o mesmo `resolveDomain()` com o mesmo bug. Um quarto lugar, `UserCalendarController`, reimplementa a mesma ideia de um jeito diferente — e correto (tem branch `CIDADAO`→`CITIZEN`). Três reimplementações independentes da mesma regra, nenhuma fonte única de verdade.
2. **Dos 7 valores de `CapabilityRateLimitDomain`, só 3 (`CITIZEN`/`LAWYER`/`INSTITUCIONAL`) são resolvidos dinamicamente em algum lugar do projeto hoje.** Os outros 4 (`SERVIDOR`/`JURIDICA`/`FINANCEIRA`/`LEGAL_SKILLS`) são sempre constante fixa por controller — nunca derivados de `Authentication`. Um resolver de propósito geral que tentasse cobrir os 60 valores de `TipoUsuario` estaria inventando regra de negócio nova sem nenhuma convenção existente para se apoiar.
3. **A trilha de auditoria ABAC (`PjbAuthorizationTrailAssembler`/`PjbAuthorizationAuditFacade`) é `package-private`, não é bean Spring, e só é acessível dentro de `core.security.abac`.** `PersonalProcessAccessGuardService` mora em `service.security.access` — nunca teve acesso a essa máquina. `PjbAuthorizationAuditFacade.registerDecision` também só grava se a thread estiver dentro de um `RequestContext` (populado pelo filtro HTTP real via `RequestCorrelationFilter`); chamado fora disso (ex. um teste unitário puro) é um no-op silencioso, sem exceção.

## Arquitetura

Três correções independentes, sem migration nova, sem mudança de contrato público.

### 1. `CapabilityRateLimitDomainResolver` — fonte única para CITIZEN/LAWYER/INSTITUCIONAL

Novo `@Component` em `platform/security/ratelimit/` (mesmo pacote de `CapabilityRateLimitDomain`/`CapabilityRateLimiter`):

```java
CapabilityRateLimitDomain resolve(Authentication authentication)
```

Regra (mesma lógica de negócio que `RecursalPeticionamentoPerfilRouter.Perfil` já usa, generalizada a partir de `Authentication.getAuthorities()` em vez de um enum de perfil já resolvido): `ROLE_CIDADAO`/`ROLE_USER` → `CITIZEN`; `ROLE_ADVOGADO`/`ROLE_ADVOCACIA` → `LAWYER`; `ROLE_DEFENSOR*`/`ROLE_PROCURADOR*`/`ROLE_PROMOTOR*`/`ROLE_MINISTERIO_PUBLICO`/`ROLE_PROCURADORIA`/`ROLE_DEFENSORIA*` → `INSTITUCIONAL`; `authentication == null` → `CITIZEN` (mesmo fallback seguro de `UserCalendarController`, o único dos três que hoje acerta esse caso). `PjbGrantedAuthorityFactory` garante que todo `TipoUsuario` sempre recebe `ROLE_<NOME_DO_ENUM>` (linha incondicional, sem exceção), então a detecção de `ROLE_CIDADAO` é confiável para 100% dos usuários desse tipo.

`PeticionamentoController`, `ProcessualParticipacaoControllerRateLimitSupport` e `UserCalendarController` passam a injetar e chamar esse resolver em vez de manter `resolveDomain()` próprio — elimina os 3 métodos duplicados (2 com bug, 1 correto) numa penada, não só os 2 buggy. `SERVIDOR`/`JURIDICA`/`FINANCEIRA`/`LEGAL_SKILLS` continuam fora do escopo do resolver — nenhum controller que já usa constante fixa muda.

### 2. `ProcessoPartyCpfMatcher` — predicado único, resultado tipado

Novo em `core/security/access/` (pacote já existe, hoje só tem `PrivateResourceAccessGuardService`):

```java
sealed interface PartyMatchResult permits PartyMatchResult.Matched, PartyMatchResult.NotMatched {
    record Matched(PartyRole role) implements PartyMatchResult {}
    record NotMatched() implements PartyMatchResult {}
}
enum PartyRole { AUTOR, REU, USUARIO_VINCULADO }
```

com `PartyMatchResult match(String cpf, Processo processo)`. O `sealed interface` não é ornamento: o resultado já carrega o motivo (qual papel casou, ou nenhum), que os passos 3a/3b abaixo usam para compor a descrição de auditoria sem recalcular nada — e o compilador garante exaustividade em todo `switch` sobre o resultado (Java 21, mesmo padrão que `RitoProcessual`/`TipoJurisdicao` já usam no projeto).

`PjbAuthorizationService.requireReadProcessoAsCidadaoParte` e `PersonalProcessAccessGuardService.requireCurrentUserAsParty` passam a chamar `match(...)` em vez de reimplementar o OR de três CPFs. Nenhum dos 10 call sites muda de comportamento — mesma política de alto nível em cada um (o primeiro só age para `CIDADAO` e roda ABAC antes; o segundo age para qualquer autenticado, sem ABAC prévio).

### 3. Auditoria — duas soluções, cada uma dentro da convenção que já existe no seu próprio pacote

**3a — `PjbAuthorizationService.requireReadProcessoAsCidadaoParte`** (já dentro do pacote ABAC): novo método `assembleCidadaoParte(...)` em `PjbAuthorizationTrailAssembler`, seguindo o mesmo padrão dos 5 métodos `assembleXxx` já existentes (`assembleProcessRead`, `assembleDocumentRead`, etc.) — mesma forma, mesmo uso de `PjbAuthorizationDecisionContext`/`AuthzDecision`. A chamada a `auditFacade.registerDecision(evaluation)` passa a rodar tanto no caminho de permissão quanto no de negação do match de CPF, gerando `AUTHZ_CIDADAO_PARTE_ALLOW`/`AUTHZ_CIDADAO_PARTE_DENY` no ledger — a mesma convenção `AUTHZ_<ACAO>_ALLOW/DENY` que `requireReadProcesso` já usa, sem inventar convenção nova.

**3b — `PersonalProcessAccessGuardService.requireCurrentUserAsParty`** (fora do pacote ABAC, nunca teve acesso a essa máquina, e hoje não audita absolutamente nada): recebe `AuditLedgerService` por construtor — já é um `@Service` público, livre de injetar de qualquer pacote — e grava um par `PERSONAL_ACCESS_ALLOW`/`PERSONAL_ACCESS_DENY` via `appendSafely(...)`, mesmo padrão limpo de par nomeado já usado por `DevicePolicyFilter` (`DEVICE_POLICY_ALLOW`/`DEVICE_POLICY_DENY`). Não é forçado a entrar na máquina ABAC — seria cruzar fronteira de pacote de um jeito mais invasivo do que essa fatia pede.

## Testes

- **`ProcessoPartyCpfMatcherTest`** (unitário puro): CPF do autor casa → `Matched(AUTOR)`; CPF do réu casa → `Matched(REU)`; CPF do `processo.getUsuario()` casa → `Matched(USUARIO_VINCULADO)`; nenhum casa → `NotMatched`. Não testa gravação de auditoria aqui — `PjbAuthorizationAuditFacade.registerDecision` é no-op silencioso fora de um `RequestContext` real, então a prova de gravação fica só no IT.
- **`CapabilityRateLimitDomainResolverTest`** (unitário puro, `@MethodSource`/`Arguments.of` — convenção já usada em `InstitutionalCriticalActionHttpGuardFilterPolicyMappingTest`, não há precedente de `@EnumSource` neste projeto): uma entrada por família de authority já observada em produção (`ROLE_CIDADAO`, `ROLE_USER`, `ROLE_ADVOGADO`, `ROLE_ADVOCACIA`, `ROLE_DEFENSOR_PUBLICO`, `ROLE_MEMBRO_MINISTERIO_PUBLICO`, `ROLE_PROCURADOR`, authentication nulo).
- **1 IT real** (Testcontainers Postgres + JWT real, seguindo a montagem de `InstitutionalRecursalGateIT`/`InstitutionalMagistraturaGateIT` combinada com a asserção `status().isForbidden()` de `ClienteCrudIT`): CIDADAO autenticado com CPF que não bate com nenhuma parte do processo → 403, e a entrada `AUTHZ_CIDADAO_PARTE_DENY` de fato aparece no ledger (via `AuditLedgerService`/repositório, não só o retorno HTTP). Candidato a call site: `CidadaoInstanciasController` (`hasRole('CIDADAO')` puro, sem dependência de mock extra).

## Risco

Baixo. Extração + resultado tipado + 2 fixes mecânicos de rate-limit + auditoria nova em 2 métodos que hoje ou já auditam parcialmente ou não auditam nada. Nenhum comportamento de produção muda para os 10 call sites de `requireReadProcessoAsCidadaoParte` nem para o único call site de `requireCurrentUserAsParty` — mesma lógica de match, só sem duplicação; os 2 fixes de rate-limit só *adicionam* um branch que faltava.
