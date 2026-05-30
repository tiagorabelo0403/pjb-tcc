# ADR-0060 — Matriz de Identidade e Autenticação (NIST SP 800-63-4)

**Status:** Aceito
**Data:** 2026-05-30

## Contexto

O PJB serve atores com níveis de confiança radicalmente distintos: do cidadão em consulta pública até o Ministro do STF assinando acórdão. Cada papel exige uma combinação diferente de IAL (Identity Assurance Level), AAL (Authenticator Assurance Level) e FAL (Federation Assurance Level) conforme NIST SP 800-63-4.

## Decisão

### Matriz AAL/IAL/FAL por Papel

| Papel | IAL | AAL | FAL | Step-up obrigatório |
|-------|-----|-----|-----|---------------------|
| Cidadão (consulta pública) | 1 | 1 | — | Não |
| Advogado | 2 | 2 | 1 | Peticionamento: sim |
| Membro MP (Promotor) | 2 | 2 | 1 | Cancelar ofício: AAL3 (GovBr Ouro) |
| Oficial de Justiça | 2 | 2 | 1 | Certidão: sim |
| Delegado | 2 | 2 | 1 | Inquérito: sim |
| Juiz | 2 | 3 | 2 | Sentença/despacho: AAL3 obrigatório |
| Desembargador | 2 | 3 | 2 | Acórdão: AAL3 obrigatório |
| Ministro (STF/STJ) | 3 | 3 | 2 | Todo ato: AAL3 obrigatório |
| Assessor (delegado) | 2 | 2 | 1 | Ato em nome de: trilha obrigatória |

### Implementação atual no PJB

- `LaianeOficioAccessGuard.requireHighAssuranceForCancellation()` → AAL3 para cancelamento de ofício MP
- `CurrentAuthenticationContextService.current().acr()` → nível GovBr corrente da sessão
- `GovBrAssuranceLevel.meetsMinimum()` → verificação de patamar mínimo por ato
- `CapabilityRateLimiter` → proteção de taxa por domínio institucional (INSTITUCIONAL, JURIDICA, LAWYER…)
- `QualifiedDocumentSignatureEnvelopeService` → assinatura com identidade completa e envelope soberano

## Pendências documentadas

As pendências abaixo exigem infraestrutura externa não disponível no ambiente atual.

### FAPI 2.0 / RFC 9700 (quando houver integrações externas)

- Sender-constrained tokens (DPoP ou mTLS) por conector sensível
- Token replay detection com janela temporal configurável
- Audience e issuer estritos por ambiente (prod ≠ staging)
- JWK com `kid` versionado e rotação automatizada
- Proibição de bearer token fraco em conector que acesse dados pessoais (LGPD art. 46)

### Observabilidade Forense (quando houver Jaeger/Tempo)

- `traceId` obrigatório em todo ato judicial como span attribute
- `correlationId` por processo, auditável por número CNJ
- `actorId`, `delegatedActorId` em todo span de ato processual
- SLO declarado para autenticação, peticionamento, assinatura e intimação
- Alerta automático para tentativa de acesso BOLA (Broken Object Level Authorization)
- Alerta para step-up negado acima de threshold por unidade judiciária

### Crypto Agility (quando ICP-Brasil publicar roadmap PQC)

- Inventário criptográfico completo por algoritmo em uso
- Campo `algorithmVersion` obrigatório nos envelopes de assinatura
- Plano de migração para FIPS 203 (ML-KEM), 204 (ML-DSA), 205 (SLH-DSA)
- Período de dupla assinatura (clássica + pós-quântica) durante transição

## Consequências

- Implementação atual cobre os pontos críticos sem dependência de infraestrutura externa
- Pendências FAPI, observabilidade e PQC ficam documentadas como próximos patamares evolutivos
- Qualquer nova integração externa deve referenciar este ADR antes de escolher mecanismo de autenticação
