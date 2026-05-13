# Round 130 — governança soberana de origem na borda

## Objetivo
Endurecer a borda HTTP do PJB para que rotas mutáveis governadas não aceitem payload sem origem conhecida, sem abrir gateway paralelo, executor novo ou fila nova.

## O que entrou
- `ApiRequestOriginGovernanceProperties`
- `ApiRequestOriginGovernanceMessages`
- `ApiRequestOriginSignatureService`
- `ApiRequestOriginGovernanceFilter`
- wiring novo em `SecurityConfig`
- bean condicional novo em `PerimeterConfig`
- profiles endurecidos em `application.yml`, `application-prod.yml`, `application-docker.yml`, `application-k8s.yml` e `application-frontend-dev.yml`
- testes:
  - `ApiRequestOriginGovernanceFilterTest`
  - `PjbOriginGovernanceWiringGuardTest`
  - `PjbZeroTrustDataPlaneInfraGuardTest`

## Regras materiais novas
- requisição mutável governada precisa de `Origin/Referer` confiável ou de atestação assinada;
- payload JSON assinado pode exigir `X-PJB-Body-Hash` coerente com o hash canônico da borda;
- origem assinada pode ser limitada por CIDR, método e prefixo de rota;
- CORS deixa de partir de wildcard implícito no baseline.

## Validação honesta
- guards Python: OK
- compilação dirigida do lote novo de perímetro/origem com `javac` + stubs locais: OK
- compilação dirigida dos testes novos de wiring/infra/filtro com `javac` + stubs locais: OK
- probe local: `SIGNED_ATTESTATION|edge-app|ok`
- sem build Maven global verde
- sem compile total do `pjb-api`
- sem Docker estável
