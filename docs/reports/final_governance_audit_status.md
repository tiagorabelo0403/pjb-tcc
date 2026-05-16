# PJB — Relatório de Auditoria Final de Governança

Data: 2026-05-16 | Branch: `main` | Commit HEAD: ver `git log --oneline -1`

---

## Tabela de Status

| ID | Item | Comando executado | Resultado | Status |
|----|------|-------------------|-----------|--------|
| 01 | kafka-clients 3.9.2 | `./mvnw -pl pjb-api dependency:tree -Dincludes=org.apache.kafka:kafka-clients` | `org.apache.kafka:kafka-clients:jar:3.9.2:compile` | ✅ OK |
| 02 | Trivy HIGH/CRITICAL (vuln+secret+misconfig) | `docker run aquasec/trivy:latest fs --scanners vuln,secret,misconfig --severity HIGH,CRITICAL --timeout 30m --skip-dirs /src/.claude,...` | 4 CVEs HIGH (todos fixados — ver seção III) + 0 CRITICAL + misconfigs K8s (falsos positivos documentados) | ✅ Corrigido |
| 03 | DataJud key no código atual | `grep -r "cDZHYzlZ" src/main/resources` | Sem ocorrência — variável de ambiente `${PJB_INSTITUTIONAL_CNJ_DATAJUD_API_KEY:}` | ✅ OK |
| 04 | DataJud key no histórico Git | `git log --all -S "cDZHYzlZ" --oneline` | Presente em `921fc0f` — purge aguarda rotação da chave | ⚠️ Ação externa necessária |
| 05 | Suite pjb-api (sem profile) | `./mvnw test -pl pjb-api` | 2941 testes, 0 falhas, 0 erros | ✅ OK |
| 06 | Suite pjb-api (profile=test) | `./mvnw -B test -pl pjb-api -Dspring.profiles.active=test` | 2941 testes, 0 falhas, 0 erros | ✅ OK |
| 07 | Build clean verify | `./mvnw -B verify -Dspring.profiles.active=test -DskipTests` | BUILD SUCCESS | ✅ OK |
| 08 | docker compose config | `docker compose config --quiet` | Sem erros | ✅ OK |
| 09 | docker compose build | `docker compose --profile app build --no-cache` | `pjb-backend:local` construída | ✅ OK |
| 10 | ZIP de entrega limpa | `pwsh ./scripts/make-clean-delivery.ps1` | 12.29 MB, 12214 entradas, 0 violações | ✅ OK |
| 11 | git status final | `git status --short` | 8 arquivos modificados (todos staged no commit final) | ✅ OK |

---

## I — Repositório

Arquivos modificados antes do commit final de auditoria:
- `docs/reports/architecture_hygiene_guard.{json,md}` — atualização de contadores (7718→7722 Java files)
- `docs/reports/transactional_hotspot_guard.{json,md}` — atualização de contadores
- `pjb-api/src/test/**/controller/*.java` e `**/laiane/api/*.java` — fix profile=test

---

## II — kafka-clients

```
[INFO] \- org.apache.kafka:kafka-clients:jar:3.9.2:compile
```

Confirmado. CVE-2025-27817 mitigada.

---

## III — Trivy

Scan executado com:
```
docker run --rm \
  -v "C:/PJB:/src" \
  -v "C:/PJB/.trivy-cache:/root/.cache/trivy" \
  aquasec/trivy:latest \
  fs /src \
  --scanners vuln,secret,misconfig \
  --severity HIGH,CRITICAL \
  --exit-code 1 \
  --timeout 30m \
  --skip-dirs /src/.git \
  --skip-dirs /src/.claude \
  --skip-dirs /src/target \
  --skip-dirs /src/pjb-api/target \
  --skip-dirs /src/pjb-core/target \
  --skip-dirs /src/node_modules \
  --skip-dirs /src/tools/ai-agents/node_modules \
  --skip-dirs /src/.trivy-cache
```

**Nota sobre tentativas anteriores**: O scan sem `--timeout 30m` falhava com
`semaphore acquire: context deadline exceeded` ao analisar `pjb-api/pom.xml` (muitas
dependências Maven). Com `--timeout 30m` o scan completou em ~22 min.

### IIIa — Vulnerabilidades encontradas e CORRIGIDAS

**NENHUM CRITICAL. 4 HIGH únicos, todos com fix disponível:**

| CVE | Biblioteca | Versão anterior | Fix | Ação tomada |
|-----|-----------|-----------------|-----|-------------|
| CVE-2026-5598 | `org.bouncycastle:bcprov-jdk18on` | 1.80 | 1.84 | `bouncycastle.version` → 1.84 ✅ |
| CVE-2026-42198 | `org.postgresql:postgresql` | 42.7.10 | 42.7.11 | `postgresql.version` → 42.7.11 ✅ |
| CVE-2026-22731 | `spring-boot-starter-actuator` | 3.5.11 | 3.5.12 | `spring-boot.version` → 3.5.12 ✅ |
| CVE-2026-22733 | `spring-boot-starter-actuator` | 3.5.11 | 3.5.12 | `spring-boot.version` → 3.5.12 ✅ |

Todos os três bumps feitos em `pom.xml` (parent + propriedades).

### IIIb — Misconfigurations Kubernetes (falsos positivos documentados)

**KSV-0109** — `PJB_SECRETARIAT_SSE_HEARTBEAT_MS: "15000"` no ConfigMap flagado como "secret".
Falso positivo: Trivy detectou o padrão "SECRET" no nome da chave, mas o valor é um
intervalo de heartbeat SSE em milissegundos (15s). Nenhuma credencial armazenada.

**KSV-0014 / KSV-0118** — Patches de overlay Kustomize flagados por ausência de
`securityContext`. Falso positivo: Trivy avalia os patches em isolamento, sem fundir
com o base deployment. O arquivo `infra/k8s/base/api-deployment.yaml` já define:
```yaml
securityContext:
  allowPrivilegeEscalation: false
  capabilities:
    drop: [ALL]
  runAsNonRoot: true
  runAsUser: 10001
  readOnlyRootFilesystem: true
```
Os overlays apenas sobrescrevem recursos — a securityContext do base é preservada pelo
Kustomize em runtime. Não há exposição real.

---

## IV — Chave DataJud no Histórico Git

**Diagnóstico**: Credencial privada de serviço (formato `api_key:api_secret` Basic Auth).

- Removida do código em `ca51411`.
- Presente no histórico em `921fc0f`.
- **Cenário B**: exige rotação no portal DataJud/CNJ antes do purge do histórico.

Plano de limpeza documentado em: `docs/reports/datajud_key_history_note.md`

**Próxima ação obrigatória do titular**:
1. Revogar/regenerar a chave em https://datajud-wiki.cnj.jus.br/
2. Confirmar "chave rotacionada"
3. Executar `git filter-repo --replace-text replacements.txt` (requer instalação: `pip install git-filter-repo`)
4. Force push somente após autorização explícita

---

## V — Suite de Testes

**Correção adicional identificada nesta auditoria**:

`application-test.yml` contém `spring.main.web-application-type: none`, que impede
`MockMvcAutoConfiguration` (`@ConditionalOnWebApplication(type=SERVLET)`) de se ativar.
Adicionado `@TestPropertySource(properties = "spring.main.web-application-type=servlet")`
nas 4 classes `@WebMvcTest` para que o profile `test` não quebre o slice web.

Resultado: 2941/0/0 com e sem profile=test.

---

## VI — Build Completo

```
./mvnw -B verify -Dspring.profiles.active=test -DskipTests → BUILD SUCCESS
```

---

## VII — Docker Compose

```
docker compose config --quiet                    → OK (sem erros)
docker compose --profile app build --no-cache   → pjb-backend:local built
```

---

## VIII — ZIP de Entrega

```
pwsh ./scripts/make-clean-delivery.ps1
→ ZIP: dist/PJB-clean.zip
→ Tamanho: 12.29 MB | Entradas: 12214 | Violações: nenhuma
```

Script usa `git archive HEAD` — exclui automaticamente `.git/`, `target/`,
`.claude/`, `.idea/`, `node_modules/`, `__pycache__/`, `application-local.*`, `.env`.

---

## Pendências que exigem ação externa

| # | Item | Responsável | Detalhe |
|---|------|-------------|---------|
| P1 | Rotação da chave DataJud | Titular da chave CNJ | Portal: https://datajud-wiki.cnj.jus.br/ |
| P2 | Purge do histórico Git | Engenheiro (após P1) | `git filter-repo --replace-text replacements.txt` + force push autorizado |
| P3 | `git filter-repo` instalado | Engenheiro | `pip install git-filter-repo` |
