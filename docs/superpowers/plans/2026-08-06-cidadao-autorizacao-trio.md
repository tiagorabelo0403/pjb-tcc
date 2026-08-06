# Trio de autorização do cidadão — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fechar 3 dívidas relacionadas (`D-titularidade-cidadao-duplicada-dois-guards`, `D-peticionamento-controller-domain-lacuna-cidadao`, `D-cidadao-parte-guard-sem-teste-rejeicao`) elevando o padrão em vez de só remendar: um resolver único de domínio de rate-limit, um predicado de match de CPF com resultado tipado, e auditoria real via o ledger ABAC existente onde hoje há denial silencioso.

**Architecture:** ver `docs/superpowers/specs/2026-08-06-cidadao-autorizacao-trio-design.md` para o desenho completo e o porquê de cada decisão. Resumo: `CapabilityRateLimitDomainResolver` (novo `@Component`) substitui 3 reimplementações de `Authentication`→domínio (2 com bug, 1 correta); `ProcessoPartyCpfMatcher` (novo, `sealed interface PartyMatchResult`) substitui a comparação de CPF duplicada entre `PjbAuthorizationService` e `PersonalProcessAccessGuardService`; cada um dos dois métodos de alto nível passa a auditar sua decisão na convenção já existente no seu próprio pacote (`AUTHZ_CIDADAO_PARTE_ALLOW/DENY` via `PjbAuthorizationTrailAssembler`/`PjbAuthorizationAuditFacade` para o primeiro; `PERSONAL_ACCESS_ALLOW/DENY` via `AuditLedgerService` direto para o segundo, que nunca teve acesso à máquina ABAC).

**Tech Stack:** Java 21 (sealed interfaces, records, pattern matching), Spring Boot 3, JUnit 5 (`@ParameterizedTest`/`@MethodSource`), Mockito, Testcontainers (Postgres real via `PjbIntegrationTestBase`), Spring Security Test (`SecurityMockMvcRequestPostProcessors.jwt()`).

## Global Constraints

- DI exclusivamente por construtor — zero `@Autowired` em campos (CLAUDE.md).
- Nenhum dos 10 call sites de `PjbAuthorizationService.requireReadProcessoAsCidadaoParte` nem o único call site de `PersonalProcessAccessGuardService.requireCurrentUserAsParty` muda de comportamento — mesma lógica de match, mesmas mensagens de exceção, só sem duplicação de código e com auditoria nova.
- O novo `CapabilityRateLimitDomainResolver` adota a lógica já comprovada correta de `UserCalendarController.resolveDomain` (match exato de `ROLE_ADVOGADO`/`ROLE_ADVOCACIA`→`LAWYER`, `ROLE_CIDADAO`/`ROLE_USER`→`CITIZEN`, `Authentication` nulo→`CITIZEN`, qualquer outra coisa→`INSTITUCIONAL`) — não a lógica de `.contains()` por substring dos outros 2 lugares. Isso é uma mudança de comportamento **intencional e dentro do escopo** desta fatia para `PeticionamentoController` e `ProcessualParticipacaoControllerRateLimitSupport`: qualquer papel que hoje caía em `LAWYER` por não bater nenhum substring da lista antiga (ex.: perito acessando `PeticionamentoController`, que nunca teve o substring `PERITO` que `ProcessualParticipacaoControllerRateLimitSupport` já tinha) passa a cair corretamente em `INSTITUCIONAL`. Não é regressão — é a mesma classe de bug que esta fatia existe para fechar.
- `SERVIDOR`/`JURIDICA`/`FINANCEIRA`/`LEGAL_SKILLS` (os outros 4 valores de `CapabilityRateLimitDomain`) ficam fora do escopo do resolver — continuam constante fixa por controller onde já estão hoje. Não adicionar lógica dinâmica para eles nesta fatia.
- `PjbAuthorizationAuditFacade.registerDecision` é no-op silencioso fora de uma thread com `RequestContext` vinculado (populado só pelo filtro HTTP real). Nenhum teste unitário deve fazer assert sobre gravação no ledger através desse caminho — só o IT (Task 8), que roda a cadeia real do Spring Security.

---

### Task 1: `ProcessoPartyCpfMatcher` — predicado de CPF compartilhado

**Files:**
- Create: `pjb-api/src/main/java/com/tcc/pjb/backend/core/security/access/PartyRole.java`
- Create: `pjb-api/src/main/java/com/tcc/pjb/backend/core/security/access/PartyMatchResult.java`
- Create: `pjb-api/src/main/java/com/tcc/pjb/backend/core/security/access/ProcessoPartyCpfMatcher.java`
- Test: `pjb-api/src/test/java/com/tcc/pjb/backend/core/security/access/ProcessoPartyCpfMatcherTest.java`

**Interfaces:**
- Produces: `PartyRole` (enum: `AUTOR`, `REU`, `USUARIO_VINCULADO`), `PartyMatchResult` (sealed interface, variantes `Matched(PartyRole role)` e `NotMatched`), `ProcessoPartyCpfMatcher.match(String cpf, Processo processo)` retornando `PartyMatchResult` — Tasks 3 e 4 consomem esses 3 tipos.

- [ ] **Step 1: Criar `PartyRole`**

```java
package com.tcc.pjb.backend.core.security.access;

public enum PartyRole {
    AUTOR,
    REU,
    USUARIO_VINCULADO
}
```

- [ ] **Step 2: Criar `PartyMatchResult`**

```java
package com.tcc.pjb.backend.core.security.access;

public sealed interface PartyMatchResult permits PartyMatchResult.Matched, PartyMatchResult.NotMatched {

    record Matched(PartyRole role) implements PartyMatchResult {
    }

    record NotMatched() implements PartyMatchResult {
    }
}
```

- [ ] **Step 3: Escrever os testes de `ProcessoPartyCpfMatcherTest` primeiro**

```java
package com.tcc.pjb.backend.core.security.access;

import static org.assertj.core.api.Assertions.assertThat;

import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import org.junit.jupiter.api.Test;

class ProcessoPartyCpfMatcherTest {

    private final ProcessoPartyCpfMatcher matcher = new ProcessoPartyCpfMatcher();

    @Test
    void casaComParteAutora() {
        Processo processo = Processo.builder().parteAutoraCpf("11111111111").parteReuCpf("22222222222").build();

        PartyMatchResult result = matcher.match("11111111111", processo);

        assertThat(result).isEqualTo(new PartyMatchResult.Matched(PartyRole.AUTOR));
    }

    @Test
    void casaComParteRe() {
        Processo processo = Processo.builder().parteAutoraCpf("11111111111").parteReuCpf("22222222222").build();

        PartyMatchResult result = matcher.match("22222222222", processo);

        assertThat(result).isEqualTo(new PartyMatchResult.Matched(PartyRole.REU));
    }

    @Test
    void casaComUsuarioVinculadoQuandoAutorEReuNaoBatem() {
        Usuario usuarioVinculado = new Usuario();
        usuarioVinculado.setCpf("33333333333");
        Processo processo = Processo.builder()
                .parteAutoraCpf("11111111111")
                .parteReuCpf("22222222222")
                .usuario(usuarioVinculado)
                .build();

        PartyMatchResult result = matcher.match("33333333333", processo);

        assertThat(result).isEqualTo(new PartyMatchResult.Matched(PartyRole.USUARIO_VINCULADO));
    }

    @Test
    void naoCasaQuandoNenhumCpfBate() {
        Processo processo = Processo.builder().parteAutoraCpf("11111111111").parteReuCpf("22222222222").build();

        PartyMatchResult result = matcher.match("99999999999", processo);

        assertThat(result).isEqualTo(new PartyMatchResult.NotMatched());
    }

    @Test
    void naoCasaQuandoCpfNulo() {
        Processo processo = Processo.builder().parteAutoraCpf("11111111111").build();

        PartyMatchResult result = matcher.match(null, processo);

        assertThat(result).isEqualTo(new PartyMatchResult.NotMatched());
    }

    @Test
    void naoCasaQuandoCpfEmBranco() {
        Processo processo = Processo.builder().parteAutoraCpf("11111111111").build();

        PartyMatchResult result = matcher.match("   ", processo);

        assertThat(result).isEqualTo(new PartyMatchResult.NotMatched());
    }

    @Test
    void naoCasaQuandoProcessoNulo() {
        PartyMatchResult result = matcher.match("11111111111", null);

        assertThat(result).isEqualTo(new PartyMatchResult.NotMatched());
    }
}
```

- [ ] **Step 4: Rodar os testes e confirmar que falham por `ProcessoPartyCpfMatcher` não existir**

Run: `./mvnw test -pl pjb-api -Dtest=ProcessoPartyCpfMatcherTest`
Expected: FAIL (compile error — classe não existe)

- [ ] **Step 5: Criar `ProcessoPartyCpfMatcher`**

```java
package com.tcc.pjb.backend.core.security.access;

import com.tcc.pjb.backend.model.entity.Processo;
import org.springframework.stereotype.Component;

@Component
public class ProcessoPartyCpfMatcher {

    public PartyMatchResult match(String cpf, Processo processo) {
        if (cpf == null || cpf.isBlank() || processo == null) {
            return new PartyMatchResult.NotMatched();
        }
        if (cpf.equals(processo.getParteAutoraCpf())) {
            return new PartyMatchResult.Matched(PartyRole.AUTOR);
        }
        if (cpf.equals(processo.getParteReuCpf())) {
            return new PartyMatchResult.Matched(PartyRole.REU);
        }
        if (processo.getUsuario() != null && cpf.equals(processo.getUsuario().getCpf())) {
            return new PartyMatchResult.Matched(PartyRole.USUARIO_VINCULADO);
        }
        return new PartyMatchResult.NotMatched();
    }
}
```

- [ ] **Step 6: Rodar os testes e confirmar que passam**

Run: `./mvnw test -pl pjb-api -Dtest=ProcessoPartyCpfMatcherTest`
Expected: PASS (7/7)

- [ ] **Step 7: Commit**

```bash
git add pjb-api/src/main/java/com/tcc/pjb/backend/core/security/access/PartyRole.java \
        pjb-api/src/main/java/com/tcc/pjb/backend/core/security/access/PartyMatchResult.java \
        pjb-api/src/main/java/com/tcc/pjb/backend/core/security/access/ProcessoPartyCpfMatcher.java \
        pjb-api/src/test/java/com/tcc/pjb/backend/core/security/access/ProcessoPartyCpfMatcherTest.java
git commit -m "feat(security): ProcessoPartyCpfMatcher — predicado unico de match de CPF por parte"
```

---

### Task 2: `CapabilityRateLimitDomainResolver` — resolver único de domínio

**Files:**
- Create: `pjb-api/src/main/java/com/tcc/pjb/backend/platform/security/ratelimit/CapabilityRateLimitDomainResolver.java`
- Test: `pjb-api/src/test/java/com/tcc/pjb/backend/platform/security/ratelimit/CapabilityRateLimitDomainResolverTest.java`

**Interfaces:**
- Produces: `CapabilityRateLimitDomainResolver.resolve(Authentication authentication)` retornando `CapabilityRateLimitDomain` — Tasks 5, 6 e 7 consomem.

- [ ] **Step 1: Escrever os testes primeiro**

```java
package com.tcc.pjb.backend.platform.security.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class CapabilityRateLimitDomainResolverTest {

    private final CapabilityRateLimitDomainResolver resolver = new CapabilityRateLimitDomainResolver();

    static Stream<Arguments> casos() {
        return Stream.of(
                Arguments.of("ROLE_ADVOGADO", CapabilityRateLimitDomain.LAWYER),
                Arguments.of("ROLE_ADVOCACIA", CapabilityRateLimitDomain.LAWYER),
                Arguments.of("ROLE_CIDADAO", CapabilityRateLimitDomain.CITIZEN),
                Arguments.of("ROLE_USER", CapabilityRateLimitDomain.CITIZEN),
                Arguments.of("ROLE_DEFENSOR_PUBLICO", CapabilityRateLimitDomain.INSTITUCIONAL),
                Arguments.of("ROLE_MEMBRO_MINISTERIO_PUBLICO", CapabilityRateLimitDomain.INSTITUCIONAL),
                Arguments.of("ROLE_PROCURADOR", CapabilityRateLimitDomain.INSTITUCIONAL),
                Arguments.of("ROLE_PERITO", CapabilityRateLimitDomain.INSTITUCIONAL),
                Arguments.of("ROLE_JUIZ_ESTADUAL", CapabilityRateLimitDomain.INSTITUCIONAL),
                Arguments.of("ROLE_SERVIDOR_FORUM", CapabilityRateLimitDomain.INSTITUCIONAL)
        );
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("casos")
    void resolveDominioPorAuthority(String authority, CapabilityRateLimitDomain esperado) {
        var authentication = new TestingAuthenticationToken("user", "pwd", List.of(new SimpleGrantedAuthority(authority)));

        assertThat(resolver.resolve(authentication)).isEqualTo(esperado);
    }

    @Test
    void resolveCitizenQuandoAuthenticationNula() {
        assertThat(resolver.resolve(null)).isEqualTo(CapabilityRateLimitDomain.CITIZEN);
    }

    @Test
    void resolveInstitucionalQuandoAuthenticationSemNenhumaAuthority() {
        var authentication = new TestingAuthenticationToken("user", "pwd", List.of());

        assertThat(resolver.resolve(authentication)).isEqualTo(CapabilityRateLimitDomain.INSTITUCIONAL);
    }
}
```

- [ ] **Step 2: Rodar e confirmar falha por classe ausente**

Run: `./mvnw test -pl pjb-api -Dtest=CapabilityRateLimitDomainResolverTest`
Expected: FAIL (compile error)

- [ ] **Step 3: Criar `CapabilityRateLimitDomainResolver`**

```java
package com.tcc.pjb.backend.platform.security.ratelimit;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

@Component
public class CapabilityRateLimitDomainResolver {

    public CapabilityRateLimitDomain resolve(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return CapabilityRateLimitDomain.CITIZEN;
        }
        Set<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .collect(Collectors.toSet());
        if (authorities.contains("ROLE_ADVOGADO") || authorities.contains("ROLE_ADVOCACIA")) {
            return CapabilityRateLimitDomain.LAWYER;
        }
        if (authorities.contains("ROLE_CIDADAO") || authorities.contains("ROLE_USER")) {
            return CapabilityRateLimitDomain.CITIZEN;
        }
        return CapabilityRateLimitDomain.INSTITUCIONAL;
    }
}
```

O guard inicial checa só `authentication == null || authentication.getAuthorities() == null` (não `.isEmpty()`) — réplica exata do guard de `UserCalendarController.resolveDomain`. `Authentication` com lista de authorities vazia (não nula) passa pelo guard, não bate nenhum dos dois `if` seguintes, e cai no `return CapabilityRateLimitDomain.INSTITUCIONAL` final — mesmo comportamento do código correto que está sendo generalizado, sem mudança.

- [ ] **Step 4: Rodar e confirmar que passam todos**

Run: `./mvnw test -pl pjb-api -Dtest=CapabilityRateLimitDomainResolverTest`
Expected: PASS (12/12)

- [ ] **Step 5: Commit**

```bash
git add pjb-api/src/main/java/com/tcc/pjb/backend/platform/security/ratelimit/CapabilityRateLimitDomainResolver.java \
        pjb-api/src/test/java/com/tcc/pjb/backend/platform/security/ratelimit/CapabilityRateLimitDomainResolverTest.java
git commit -m "feat(ratelimit): CapabilityRateLimitDomainResolver — fonte unica pra CITIZEN/LAWYER/INSTITUCIONAL"
```

---

### Task 3: Auditoria real em `PjbAuthorizationService.requireReadProcessoAsCidadaoParte`

**Files:**
- Modify: `pjb-api/src/main/java/com/tcc/pjb/backend/core/security/abac/PjbAuthorizationTrailAssembler.java`
- Modify: `pjb-api/src/main/java/com/tcc/pjb/backend/core/security/abac/PjbAuthorizationService.java`

**Interfaces:**
- Consumes: `ProcessoPartyCpfMatcher.match(String, Processo)` → `PartyMatchResult` (Task 1).
- Produces: `PjbAuthorizationTrailAssembler.assembleCidadaoParte(Processo, PjbAuthorizationDecisionContext, AuthzDecision)` → `PjbAuthorizationEvaluation`.

- [ ] **Step 1: Adicionar `assembleCidadaoParte` em `PjbAuthorizationTrailAssembler`**

Adicionar este método logo após `assembleProcessWrite` (linha 104 do arquivo atual), mesma classe, mesmo estilo dos outros `assembleXxx`:

```java
    PjbAuthorizationEvaluation assembleCidadaoParte(Processo processo,
                                                    PjbAuthorizationDecisionContext context,
                                                    AuthzDecision decision) {
        return new PjbAuthorizationEvaluation(
                decision,
                assemble(
                        "CIDADAO_PARTE",
                        "PROCESSO",
                        resolveProcessId(processo),
                        context,
                        null,
                        decision,
                        NivelSigilo.PUBLICO,
                        PjbAuthorizationStepUpAssessment.notRequired("NONE", "cidadao_parte"),
                        PjbAuthorizationGovernanceAssessment.notRequired("NONE", "cidadao_parte", "processo")
                )
        );
    }
```

`activePolicy = null` é seguro — o `assemble(...)` privado já trata `activePolicy == null` (linha 208 do arquivo atual: `activePolicy == null ? null : activePolicy.descriptorSha256()`). `NivelSigilo.PUBLICO` é uma escolha deliberada: `requireReadProcesso(processo)` já rodou e já aplicou o gate de sigilo real antes deste ponto (linha 138 de `PjbAuthorizationService`); esta trilha é sobre uma checagem adicional (posse de CPF), não uma reavaliação de sigilo.

- [ ] **Step 2: Rodar test-compile pra confirmar que o assembler ainda compila**

Run: `./mvnw test-compile -pl pjb-api`
Expected: sucesso (nenhum teste ainda exercita o método novo diretamente — ele é `package-private`, exercitado via `PjbAuthorizationService`)

- [ ] **Step 3: Guardar `trailAssembler` e `contextResolver` como campos de `PjbAuthorizationService`**

Em `PjbAuthorizationService.java`, adicionar 2 campos privados e populá-los no construtor (hoje `trailAssembler`/`contextResolver` são variáveis locais do construtor, usadas só pra construir outros colaboradores — precisam ficar acessíveis ao método `requireReadProcessoAsCidadaoParte`):

```java
    private final PjbAuthorizationAuditFacade auditFacade;
    private final PjbAuthorizationTrailAssembler trailAssembler;
    private final PjbAuthorizationDecisionContextResolver contextResolver;
    private final ProcessoPartyCpfMatcher partyCpfMatcher;
```

(adicionar `trailAssembler`, `contextResolver` e `partyCpfMatcher` logo após o campo `auditFacade` já existente na linha 27). No construtor, adicionar o parâmetro novo `ProcessoPartyCpfMatcher partyCpfMatcher` (import `com.tcc.pjb.backend.core.security.access.ProcessoPartyCpfMatcher`) na lista de parâmetros (após `ProfessionalDocumentScopePolicyService professionalDocumentScopePolicyService`), e trocar:

```java
        PjbAuthorizationDecisionContextResolver contextResolver = new PjbAuthorizationDecisionContextResolver(currentUserService, govBrAssuranceExtractor);
        PjbAuthorizationTrailAssembler trailAssembler = new PjbAuthorizationTrailAssembler();
```

por:

```java
        this.contextResolver = new PjbAuthorizationDecisionContextResolver(currentUserService, govBrAssuranceExtractor);
        this.trailAssembler = new PjbAuthorizationTrailAssembler();
        this.partyCpfMatcher = partyCpfMatcher;
```

e trocar todas as 3 referências subsequentes a `contextResolver`/`trailAssembler` dentro do mesmo construtor (nas construções de `policyFacade`, `institutionalCapabilityFacade`, `sensitiveIntegrationFacade`) de `contextResolver`/`trailAssembler` para `this.contextResolver`/`this.trailAssembler` (ou deixar como está — variável local `this.contextResolver` já resolve por escopo em Java; mais simples: manter os nomes de parâmetro do construtor como estão e só adicionar as 3 atribuições `this.x = x` acima, sem precisar prefixar as outras 3 referências, já que `this.contextResolver` e a variável recém-atribuída referenciam o mesmo objeto).

- [ ] **Step 4: Reescrever `requireReadProcessoAsCidadaoParte`**

```java
    public void requireReadProcessoAsCidadaoParte(Processo processo) {
        requireReadProcesso(processo);
        Usuario usuario = currentUserService.getOrNull();
        if (usuario == null || usuario.getTipoUsuario() != TipoUsuario.CIDADAO) {
            return;
        }
        String cpf = usuario.getCpf();
        if (cpf == null || cpf.isBlank()) {
            throw new AccessDeniedPjbException("CPF não encontrado no perfil");
        }
        PartyMatchResult match = partyCpfMatcher.match(cpf, processo);
        if (match instanceof PartyMatchResult.NotMatched) {
            registerCidadaoParteDecision(processo, usuario, AuthzDecision.deny("cidadao_nao_e_parte_do_processo", "cidadao-parte-v1"));
            throw new AccessDeniedPjbException("Cidadão não é parte do processo.");
        }
        registerCidadaoParteDecision(processo, usuario, AuthzDecision.allow("cidadao_e_parte_do_processo", "cidadao-parte-v1"));
    }

    private void registerCidadaoParteDecision(Processo processo, Usuario usuario, AuthzDecision decision) {
        PjbAuthorizationDecisionContext context = contextResolver.resolve(usuario);
        auditFacade.registerDecision(trailAssembler.assembleCidadaoParte(processo, context, decision));
    }
```

Import novo necessário: `com.tcc.pjb.backend.core.security.access.PartyMatchResult`. O branch de CPF ausente/em branco continua sem auditoria — comportamento idêntico ao original, fora do escopo explícito desta fatia (a dívida fala de CPF divergente, não CPF ausente).

- [ ] **Step 5: Compilar**

Run: `./mvnw test-compile -pl pjb-api`
Expected: sucesso

- [ ] **Step 6: Commit**

```bash
git add pjb-api/src/main/java/com/tcc/pjb/backend/core/security/abac/PjbAuthorizationTrailAssembler.java \
        pjb-api/src/main/java/com/tcc/pjb/backend/core/security/abac/PjbAuthorizationService.java
git commit -m "feat(security): requireReadProcessoAsCidadaoParte usa ProcessoPartyCpfMatcher e audita AUTHZ_CIDADAO_PARTE_ALLOW/DENY"
```

---

### Task 4: Auditoria real em `PersonalProcessAccessGuardService.requireCurrentUserAsParty`

**Files:**
- Modify: `pjb-api/src/main/java/com/tcc/pjb/backend/service/security/access/PersonalProcessAccessGuardService.java`
- Modify: `pjb-api/src/test/java/com/tcc/pjb/backend/service/security/access/PersonalProcessAccessGuardServiceTest.java`

**Interfaces:**
- Consumes: `ProcessoPartyCpfMatcher.match(String, Processo)` (Task 1), `AuditLedgerService.appendSafely(String, String, String)` (já existe, `core/audit/ledger/AuditLedgerService`).

- [ ] **Step 1: Adicionar as duas dependências ao construtor**

```java
    private final CurrentUserService currentUserService;
    private final UserSecurityProfileRepository securityProfileRepository;
    private final ProcessoPartyCpfMatcher partyCpfMatcher;
    private final AuditLedgerService auditLedgerService;

    public PersonalProcessAccessGuardService(CurrentUserService currentUserService,
                                             UserSecurityProfileRepository securityProfileRepository,
                                             ProcessoPartyCpfMatcher partyCpfMatcher,
                                             AuditLedgerService auditLedgerService) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.securityProfileRepository = Objects.requireNonNull(securityProfileRepository);
        this.partyCpfMatcher = Objects.requireNonNull(partyCpfMatcher);
        this.auditLedgerService = Objects.requireNonNull(auditLedgerService);
    }
```

Imports novos: `com.tcc.pjb.backend.core.security.access.ProcessoPartyCpfMatcher`, `com.tcc.pjb.backend.core.security.access.PartyMatchResult`, `com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService`.

- [ ] **Step 2: Reescrever `requireCurrentUserAsParty`**

```java
    public void requireCurrentUserAsParty(com.tcc.pjb.backend.model.entity.Processo processo) {
        Usuario usuario = currentUserService.getRequired();
        String cpf = usuario.getCpf();
        String resourceId = processo == null || processo.getId() == null ? "UNKNOWN" : String.valueOf(processo.getId());
        if (cpf == null || cpf.isBlank() || processo == null) {
            auditLedgerService.appendSafely("PERSONAL_ACCESS_DENY", "PROCESSO", resourceId);
            throw new AccessDeniedPjbException("Acesso pessoal ao processo bloqueado: usuário sem CPF civil válido ou processo ausente.");
        }
        PartyMatchResult match = partyCpfMatcher.match(cpf, processo);
        if (match instanceof PartyMatchResult.NotMatched) {
            auditLedgerService.appendSafely("PERSONAL_ACCESS_DENY", "PROCESSO", resourceId);
            throw new AccessDeniedPjbException("Acesso pessoal ao processo bloqueado: a identidade civil autenticada não está vinculada ao processo informado.");
        }
        auditLedgerService.appendSafely("PERSONAL_ACCESS_ALLOW", "PROCESSO", resourceId);
    }
```

- [ ] **Step 3: Atualizar `PersonalProcessAccessGuardServiceTest`**

As 2 chamadas de construtor (linhas 40 e 61 do arquivo atual) passam a precisar de 2 argumentos a mais. Trocar:

```java
        PersonalProcessAccessGuardService service = new PersonalProcessAccessGuardService(currentUserService, repository);
```

(em ambos os testes) por:

```java
        PersonalProcessAccessGuardService service = new PersonalProcessAccessGuardService(
                currentUserService, repository, new com.tcc.pjb.backend.core.security.access.ProcessoPartyCpfMatcher(),
                mock(com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService.class));
```

`ProcessoPartyCpfMatcher` não tem dependências — instanciar direto é mais simples que mockar. `AuditLedgerService` mockado porque os 2 testes existentes não exercitam `requireCurrentUserAsParty` (só `resolveOwnProcessAccess`), então o mock nunca precisa de stub.

- [ ] **Step 4: Compilar e rodar os testes existentes**

Run: `./mvnw test -pl pjb-api -Dtest=PersonalProcessAccessGuardServiceTest`
Expected: PASS (2/2, sem regressão)

- [ ] **Step 5: Commit**

```bash
git add pjb-api/src/main/java/com/tcc/pjb/backend/service/security/access/PersonalProcessAccessGuardService.java \
        pjb-api/src/test/java/com/tcc/pjb/backend/service/security/access/PersonalProcessAccessGuardServiceTest.java
git commit -m "feat(security): requireCurrentUserAsParty usa ProcessoPartyCpfMatcher e audita PERSONAL_ACCESS_ALLOW/DENY"
```

---

### Task 5: Migrar `PeticionamentoController` pro resolver único

**Files:**
- Modify: `pjb-api/src/main/java/com/tcc/pjb/backend/controller/processual/peticionamento/PeticionamentoController.java`

**Interfaces:**
- Consumes: `CapabilityRateLimitDomainResolver.resolve(Authentication)` (Task 2).

- [ ] **Step 1: Injetar o resolver e remover o `resolveDomain` privado**

Adicionar campo e parâmetro de construtor:

```java
    private final CapabilityRateLimiter rateLimiter;
    private final CapabilityRateLimitDomainResolver domainResolver;

    public PeticionamentoController(PeticionamentoSessaoFacadeService facadeService,
                                    LaianePeticaoInicialDraftService draftService,
                                    PeticionamentoStudioWorkspaceService studioWorkspaceService,
                                    PeticionamentoSimpleProtocolWizardService simpleProtocolWizardService,
                                    PeticionamentoJourneyIntelligenceService journeyIntelligenceService,
                                    CapabilityRateLimiter rateLimiter,
                                    CapabilityRateLimitDomainResolver domainResolver) {
        this.facadeService = Objects.requireNonNull(facadeService, "facadeService");
        this.draftService = Objects.requireNonNull(draftService, "draftService");
        this.studioWorkspaceService = Objects.requireNonNull(studioWorkspaceService, "studioWorkspaceService");
        this.simpleProtocolWizardService = Objects.requireNonNull(simpleProtocolWizardService, "simpleProtocolWizardService");
        this.journeyIntelligenceService = Objects.requireNonNull(journeyIntelligenceService, "journeyIntelligenceService");
        this.rateLimiter = Objects.requireNonNull(rateLimiter, "rateLimiter");
        this.domainResolver = Objects.requireNonNull(domainResolver, "domainResolver");
    }
```

Import novo: `com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomainResolver`.

Trocar:

```java
    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(resolveDomain(authentication), authentication, capability, ApiVersion.V1);
    }

    private CapabilityRateLimitDomain resolveDomain(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return CapabilityRateLimitDomain.LAWYER;
        }
        boolean institutional = authentication.getAuthorities().stream()
                .map(authority -> authority == null ? null : authority.getAuthority())
                .filter(Objects::nonNull)
                .map(String::toUpperCase)
                .anyMatch(authority -> authority.contains("DEFENSOR")
                        || authority.contains("PROCURADOR")
                        || authority.contains("PROMOTOR")
                        || authority.contains("MINISTERIO_PUBLICO")
                        || authority.contains("PROCURADORIA")
                        || authority.contains("DEFENSORIA"));
        return institutional ? CapabilityRateLimitDomain.INSTITUCIONAL : CapabilityRateLimitDomain.LAWYER;
    }
```

por:

```java
    private void enforce(Authentication authentication, String capability) {
        rateLimiter.enforce(domainResolver.resolve(authentication), authentication, capability, ApiVersion.V1);
    }
```

`CapabilityRateLimitDomain` deixa de ser usado diretamente neste arquivo — remover o import `com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomain` se nenhuma outra linha do arquivo o referenciar (conferir com grep antes de remover).

- [ ] **Step 2: Compilar**

Run: `./mvnw test-compile -pl pjb-api`
Expected: sucesso

- [ ] **Step 3: Commit**

```bash
git add pjb-api/src/main/java/com/tcc/pjb/backend/controller/processual/peticionamento/PeticionamentoController.java
git commit -m "fix(peticionamento): usa CapabilityRateLimitDomainResolver, corrige CIDADAO caindo em LAWYER"
```

---

### Task 6: Migrar `ProcessualParticipacaoControllerRateLimitSupport` + 2 controllers

**Files:**
- Modify: `pjb-api/src/main/java/com/tcc/pjb/backend/controller/processual/participacao/support/ProcessualParticipacaoControllerRateLimitSupport.java`
- Modify: `pjb-api/src/main/java/com/tcc/pjb/backend/controller/processual/participacao/submission/ProcessualParticipacaoSubmissionController.java`
- Modify: `pjb-api/src/main/java/com/tcc/pjb/backend/controller/processual/participacao/workspace/ProcessualParticipacaoWorkspaceController.java`

**Interfaces:**
- Consumes: `CapabilityRateLimitDomainResolver.resolve(Authentication)` (Task 2).
- Produces: `ProcessualParticipacaoControllerRateLimitSupport.enforce(CapabilityRateLimiter, CapabilityRateLimitDomainResolver, Authentication, String)` — assinatura muda, 4 args em vez de 3.

- [ ] **Step 1: Reescrever `ProcessualParticipacaoControllerRateLimitSupport`**

```java
package com.tcc.pjb.backend.controller.processual.participacao.support;

import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomainResolver;
import com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimiter;
import com.tcc.pjb.backend.platform.versioning.ApiVersion;
import org.springframework.security.core.Authentication;

public final class ProcessualParticipacaoControllerRateLimitSupport {

    private ProcessualParticipacaoControllerRateLimitSupport() {
    }

    public static void enforce(CapabilityRateLimiter rateLimiter,
                               CapabilityRateLimitDomainResolver domainResolver,
                               Authentication authentication,
                               String capability) {
        rateLimiter.enforce(domainResolver.resolve(authentication), authentication, capability, ApiVersion.V1);
    }
}
```

- [ ] **Step 2: Atualizar `ProcessualParticipacaoSubmissionController`**

Adicionar campo `domainResolver` e parâmetro de construtor (mesmo padrão do Task 5), e nas 2 chamadas existentes a `ProcessualParticipacaoControllerRateLimitSupport.enforce(rateLimiter, ...)`, inserir `domainResolver` como segundo argumento: `ProcessualParticipacaoControllerRateLimitSupport.enforce(rateLimiter, domainResolver, ...)`.

- [ ] **Step 3: Atualizar `ProcessualParticipacaoWorkspaceController`**

Mesmo tratamento do Step 2, aplicado a este controller (1 call site).

- [ ] **Step 4: Compilar**

Run: `./mvnw test-compile -pl pjb-api`
Expected: sucesso

- [ ] **Step 5: Commit**

```bash
git add pjb-api/src/main/java/com/tcc/pjb/backend/controller/processual/participacao/support/ProcessualParticipacaoControllerRateLimitSupport.java \
        pjb-api/src/main/java/com/tcc/pjb/backend/controller/processual/participacao/submission/ProcessualParticipacaoSubmissionController.java \
        pjb-api/src/main/java/com/tcc/pjb/backend/controller/processual/participacao/workspace/ProcessualParticipacaoWorkspaceController.java
git commit -m "fix(participacao): usa CapabilityRateLimitDomainResolver, corrige CIDADAO caindo em LAWYER"
```

---

### Task 7: Migrar `UserCalendarController` pro resolver único

**Files:**
- Modify: `pjb-api/src/main/java/com/tcc/pjb/backend/controller/calendar/UserCalendarController.java`

**Interfaces:**
- Consumes: `CapabilityRateLimitDomainResolver.resolve(Authentication)` (Task 2).

- [ ] **Step 1: Injetar o resolver**

Este arquivo usa indentação de 2 espaços (diferente do resto do projeto) — preservar o estilo do arquivo. Adicionar campo e parâmetro de construtor:

```java
  private final CapabilityRateLimiter rateLimiter;
  private final CapabilityRateLimitDomainResolver domainResolver;
  ...

  public UserCalendarController(UserCalendarService service,
                                CapabilityRateLimiter rateLimiter,
                                CapabilityRateLimitDomainResolver domainResolver,
                                UserCalendarWorkspaceService workspaceService,
                                UserCalendarPanelService panelService,
                                UserCalendarProcessMirrorService processMirrorService,
                                UserCalendarPreferenceService preferenceService,
                                UserCalendarNotificationPreviewService notificationPreviewService,
                                CalendarInstitutionalBridgeService institutionalBridgeService) {
    this.service = service;
    this.rateLimiter = rateLimiter;
    this.domainResolver = domainResolver;
    this.workspaceService = workspaceService;
    this.panelService = panelService;
    this.processMirrorService = processMirrorService;
    this.preferenceService = preferenceService;
    this.notificationPreviewService = notificationPreviewService;
    this.institutionalBridgeService = institutionalBridgeService;
  }
```

Import novo: `com.tcc.pjb.backend.platform.security.ratelimit.CapabilityRateLimitDomainResolver`.

- [ ] **Step 2: Trocar as 16 chamadas de `resolveDomain(authentication)` por `domainResolver.resolve(authentication)`**

Toda ocorrência do texto literal `resolveDomain(authentication)` no arquivo vira `domainResolver.resolve(authentication)` (substituição textual idêntica nas 16 linhas — confirmado via `grep -c "resolveDomain(authentication)"`: 16 hoje, deve ser 0 depois da troca).

- [ ] **Step 3: Remover o método privado `resolveDomain`**

```java
  private CapabilityRateLimitDomain resolveDomain(Authentication authentication) {
    if (authentication == null || authentication.getAuthorities() == null) {
      return CapabilityRateLimitDomain.CITIZEN;
    }
    boolean lawyer = authentication.getAuthorities().stream().anyMatch(item -> "ROLE_ADVOGADO".equalsIgnoreCase(item.getAuthority()) || "ROLE_ADVOCACIA".equalsIgnoreCase(item.getAuthority()));
    if (lawyer) {
      return CapabilityRateLimitDomain.LAWYER;
    }
    boolean citizen = authentication.getAuthorities().stream().anyMatch(item -> "ROLE_CIDADAO".equalsIgnoreCase(item.getAuthority()) || "ROLE_USER".equalsIgnoreCase(item.getAuthority()));
    if (citizen) {
      return CapabilityRateLimitDomain.CITIZEN;
    }
    return CapabilityRateLimitDomain.INSTITUCIONAL;
  }
```

Deletar o método inteiro. `CapabilityRateLimitDomain` deixa de ser usado neste arquivo — remover o import se nenhuma outra linha o referenciar (conferir com grep antes de remover).

- [ ] **Step 4: Compilar**

Run: `./mvnw test-compile -pl pjb-api`
Expected: sucesso

- [ ] **Step 5: Commit**

```bash
git add pjb-api/src/main/java/com/tcc/pjb/backend/controller/calendar/UserCalendarController.java
git commit -m "refactor(calendar): usa CapabilityRateLimitDomainResolver compartilhado em vez de resolveDomain proprio"
```

---

### Task 8: IT provando 403 real + entrada no ledger pra CIDADAO com CPF divergente

**Files:**
- Create: `pjb-api/src/test/java/com/tcc/pjb/backend/controller/cidadao/CidadaoInstanciasControllerCpfMismatchIT.java`

**Interfaces:**
- Consumes: `PjbAuthorizationService.requireReadProcessoAsCidadaoParte` (via `CidadaoInstanciasController` → `CidadaoInstanciasService.instancias`, endpoint `GET /api/v1/cidadao/processos/{processoId}/instancias`, Task 3), `AuditLedgerService.entries()` (já existe).

Endpoint escolhido: `CidadaoInstanciasController` (`hasRole('CIDADAO')` puro, sem `@MockitoBean` extra necessário — `CidadaoInstanciasService.instancias` chama `authz.requireReadProcessoAsCidadaoParte(p)` na primeira linha do método, antes de qualquer outra lógica).

- [ ] **Step 1: Escrever o IT**

```java
package com.tcc.pjb.backend.controller.cidadao;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.tcc.pjb.backend.PjbIntegrationTestBase;
import com.tcc.pjb.backend.core.audit.ledger.AuditLedgerService;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.repository.ProcessoRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@AutoConfigureMockMvc
class CidadaoInstanciasControllerCpfMismatchIT extends PjbIntegrationTestBase {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ProcessoRepository processoRepository;

    @Autowired
    private AuditLedgerService auditLedgerService;

    @Test
    void cidadaoComCpfDivergenteRecebe403EGeraEntradaNoLedger() throws Exception {
        Usuario cidadao = new Usuario();
        cidadao.setNome("Cidadao CPF Divergente");
        cidadao.setEmail("cidadao.divergente@pjb.local");
        cidadao.setCpf("11111111111");
        cidadao.setAtivo(true);
        cidadao.setTipoUsuario(TipoUsuario.CIDADAO);
        cidadao.setPerfil(TipoUsuario.CIDADAO.name());
        cidadao = usuarioRepository.save(cidadao);
        long cidadaoId = cidadao.getId();

        Processo processo = Processo.builder()
                .numeroUnificado("0009999-40.2026.8.06.0001")
                .numeroProcesso("0009999-40.2026.8.06.0001")
                .rito(RitoProcessual.COMUM_ORDINARIO)
                .parteAutoraCpf("22222222222")
                .parteReuCpf("33333333333")
                .dataCriacao(LocalDateTime.now())
                .build();
        processo = processoRepository.save(processo);

        MockHttpServletResponse response = mockMvc.perform(get("/api/v1/cidadao/processos/{processoId}/instancias", processo.getId())
                        .with(jwt().jwt(j -> j.claim("uid", String.valueOf(cidadaoId)))
                                .authorities(new SimpleGrantedAuthority("ROLE_CIDADAO"))))
                .andReturn()
                .getResponse();

        assertThat(response.getStatus())
                .as("CPF do cidadao (11111111111) nao bate com autor (22222222222) nem reu (33333333333) do processo")
                .isEqualTo(403);

        boolean denyRegistrado = auditLedgerService.entries().stream()
                .anyMatch(entry -> "AUTHZ_CIDADAO_PARTE_DENY".equals(entry.getAction())
                        && String.valueOf(processo.getId()).equals(entry.getResourceId()));
        assertThat(denyRegistrado)
                .as("Decisao de negacao por CPF divergente deve gerar entrada AUTHZ_CIDADAO_PARTE_DENY no ledger, nao so a excecao HTTP")
                .isTrue();
    }
}
```

Se `AuditLedgerEntry` não tiver exatamente os métodos `getAction()`/`getResourceId()` (nomes podem divergir — conferir contra `pjb-api/src/main/java/com/tcc/pjb/backend/core/audit/ledger/AuditLedgerEntry.java` antes de rodar), ajustar os nomes de getter usados na asserção pros reais da classe, mantendo a mesma lógica de filtro (eventCode == "AUTHZ_CIDADAO_PARTE_DENY" e resourceId == id do processo).

Se `requireReadProcesso(processo)` (chamado antes do match de CPF, dentro de `requireReadProcessoAsCidadaoParte`) bloquear o cidadão por algum motivo de ABAC não relacionado a posse (ex.: sigilo do processo recém-criado não é `PUBLICO` por padrão), investigar `PjbAuthorizationPolicyFacade.evaluateReadProcesso` e ajustar a fixture do `Processo` (ex.: setar `nivelSigilo(NivelSigilo.PUBLICO)` explicitamente) até o teste alcançar de fato o branch de CPF divergente — o objetivo do teste é provar especificamente esse branch, não o ABAC de leitura geral (que já tem cobertura própria).

- [ ] **Step 2: Rodar o IT isolado**

Run: `./mvnw verify -pl pjb-api -Dsurefire.skip=true -Dit.test=CidadaoInstanciasControllerCpfMismatchIT -DfailIfNoTests=true`
Expected: BUILD SUCCESS, 1/1

- [ ] **Step 3: Commit**

```bash
git add pjb-api/src/test/java/com/tcc/pjb/backend/controller/cidadao/CidadaoInstanciasControllerCpfMismatchIT.java
git commit -m "test(cidadao): prova 403 real + entrada AUTHZ_CIDADAO_PARTE_DENY no ledger pra CPF divergente"
```

---

## Fechamento

Após a Task 8 verde: rodar suite unitária completa (`./mvnw test -pl pjb-api`) e os ITs afetados (`CidadaoInstanciasControllerCpfMismatchIT` + qualquer IT existente que já cubra `PeticionamentoController`/`ProcessualParticipacaoSubmissionController`/`ProcessualParticipacaoWorkspaceController`/`UserCalendarController`/`CidadaoInstanciasController`, buscando por `grep -rl "PeticionamentoController\|ProcessualParticipacaoSubmissionController\|ProcessualParticipacaoWorkspaceController\|UserCalendarController" pjb-api/src/test --include=*IT.java`), atualizar `docs/quality/DEBT_LOG.md` marcando as 3 dívidas como fechadas com o resumo do que foi feito, e seguir para `superpowers:finishing-a-development-branch`.
