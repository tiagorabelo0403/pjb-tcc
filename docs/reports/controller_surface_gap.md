# Controller Surface Gap Report

**Data de geração:** 2026-05-15  
**Gerado por:** governance audit round 28AR

## Sumário executivo

| Métrica | Valor |
|---|---|
| Total `@RestController` | 394 |
| Com teste direto (arquivo `*Controller*Test*.java`) | 55 |
| Cobertura direta | **14%** |
| Gap (sem teste direto) | **339 (86%)** |

Testes de contrato Pact (`contracts/provider`) cobrem 9 provedores adicionais.  
Smoke test (`ControllerSurfaceSmokeTest`) valida carregamento de contexto Spring, não comportamento de endpoint.

---

## Distribuição por domínio

### Controladores (produção)

| Domínio | Controllers | Testes diretos | Cobertura |
|---|---|---|---|
| `controller/admin` | 70 | 26 | 37% |
| `controller` (raiz) | 31 | 0 | 0% |
| `controller/cidadao` | 18 | 0 | 0% |
| `controller/intelligence` | 12 | 1 | 8% |
| `controller/security` | 12 | 0 | 0% |
| `controller/publico` | 10 | 0 | 0% |
| `controller/ui` | 9 | 2 | 22% |
| `controller/processo` | 9 | 0 | 0% |
| `modules/laiane/api` | 8 | 3 | 37% |
| `modules/atendimento/controller` | 8 | 0 | 0% |
| `controller/advogado` | 7 | 0 | 0% |
| `controller/ministro` | 6 | 0 | 0% |
| `controller/oficial_justica` | 6 | 0 | 0% |
| `ai/juridica/api` | 6 | 0 | 0% |
| `controller/secretariat/operational` | 5 | 0 | 0% |
| `controller/processual/recursal/surface` | 5 | 1 | 20% |
| `configs/api` | 5 | 0 | 0% |
| Outros (< 5 por domínio) | 161 | 22 | ~14% |

---

## Domínios críticos sem cobertura

Os domínios abaixo têm impacto direto em usuários externos e zero testes de controller:

### 1. `controller/cidadao` — 18 controllers, 0 testes
Expõe endpoints de acesso público para partes processuais: dashboard, processos, julgamentos, juntadas, audiências, documentos, perfil.  
**Risco:** regressão em fluxo de consulta pública e integração Gov.br.

### 2. `controller/security` — 12 controllers, 0 testes
Autenticação, passkey, step-up, ABAC.  
**Risco:** falha silenciosa de autorização.

### 3. `controller/intelligence` — 12 controllers, 1 teste
Dossiê processual, radar de padrões, BATNA, inteligência recursal.  
**Risco:** outputs de IA sem contrato de response validado.

### 4. `controller/publico` — 10 controllers, 0 testes
Acesso não autenticado a consultas públicas processuais.  
**Risco:** quebra de contrato com portais externos (DATAJUD, tribunais).

### 5. `controller/processo` — 9 controllers, 0 testes
Fluxo central de gestão processual.  
**Risco:** mais crítico do ponto de vista de negócio.

---

## Priorização de cobertura recomendada

| Prioridade | Domínio | Motivo |
|---|---|---|
| P1 | `controller/security` | Falha de authn/authz tem impacto legal e de segurança |
| P1 | `controller/cidadao` | Acesso externo, integração Gov.br, SLA público |
| P1 | `controller/processo` | Core de negócio, sem qualquer teste |
| P2 | `controller/publico` | Contrato externo com portais e integrações |
| P2 | `controller/intelligence` | Contratos de response de IA precisam ser fixados |
| P3 | `controller/advogado` | Fluxo de patrono, alto volume de uso |
| P3 | `modules/atendimento/controller` | Atendimento e triagem |
| P4 | Demais domínios | Cobertura oportunística |

---

## Estratégia recomendada

1. **Pact contracts** para endpoints públicos e integração externa (cidadao, publico) — testa contrato sem subir servidor completo.
2. **`@WebMvcTest` slice** para security e processo — isola controller, mocka service, valida status HTTP e serialização de response.
3. **Smoke test por domínio** expandindo `ControllerSurfaceSmokeTest` — ao menos garante que o contexto Spring carrega todos os beans do domínio.
4. Não criar testes apenas para atingir métrica — focar em endpoints com lógica de status HTTP, validação de input e mapeamento de erro.

---

## Backlog de itens (rastreamento)

| ID | Domínio | Tipo de teste sugerido | Estimativa |
|---|---|---|---|
| GAP-01 | `controller/security` | `@WebMvcTest` — passkey, step-up, 401/403 paths | 2 dias |
| GAP-02 | `controller/cidadao` | Pact consumer + `@WebMvcTest` dashboard/processos | 3 dias |
| GAP-03 | `controller/processo` | `@WebMvcTest` CRUD core | 2 dias |
| GAP-04 | `controller/publico` | Pact provider — consulta pública DATAJUD | 1 dia |
| GAP-05 | `controller/intelligence` | `@WebMvcTest` — fixar response schema IA | 2 dias |
