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
| Membro MP (Promotor) | 2 | 3 | 1 | Todo ato: AAL3 obrigatório (certificado A3 + passkey TPM) |
| Defensor Público | 2 | 3 | 1 | Todo ato: AAL3 obrigatório (certificado A3 + passkey TPM) |
| Oficial de Justiça | 2 | 2 | 1 | Certidão: sim |
| Delegado | 2 | 2 | 1 | Inquérito: sim |
| Juiz | 2 | 3 | 2 | Sentença/despacho: AAL3 obrigatório |
| Desembargador | 2 | 3 | 2 | Acórdão: AAL3 obrigatório |
| Ministro (STF/STJ) | 3 | 3 | 2 | Todo ato: AAL3 obrigatório |
| Assessor (delegado) | 2 | 2 | 1 | Ato em nome de: trilha obrigatória |

### Implementação atual no PJB

- `LaianeOficioAccessGuard.requireHighAssuranceForCancellation()` → AAL3 para cancelamento de ofício MP (controle específico anterior à matriz revisada; hoje o MP já exige AAL3 para todo ato, não só este)
- `CurrentAuthenticationContextService.current().acr()` → nível GovBr corrente da sessão
- `GovBrAssuranceLevel.meetsMinimum()` → verificação de patamar mínimo por ato
- `CapabilityRateLimiter` → proteção de taxa por domínio institucional (INSTITUCIONAL, JURIDICA, LAWYER…)
- `QualifiedDocumentSignatureEnvelopeService` → assinatura com identidade completa e envelope soberano

### Hardware-based authenticator para magistratura (2026-08-08)

A matriz já exigia AAL3 para Juiz/Desembargador/Ministro desde a criação deste ADR, mas o requisito de "hardware-based authenticator" (NIST SP 800-63-4 §4.3) não tinha implementação real — o único step-up adicional (`MinisterStepUpFilter`, token facial) emitia credencial sem verificar biometria nenhuma. Esse gap foi fechado:

- **Certificado ICP-Brasil A3/A4 obrigatório**: `CertificadoAuthFacadeService` rejeita certificado A1 (chave em arquivo) para `isMagistratura()`, exigindo A3 (token/smartcard) ou A4 (HSM) via `IcpBrasilCertProfile.certType()`. Descrição do estado original desta etapa — só magistratura; a extensão a MP e Defensoria está documentada na subseção seguinte.
- **Passkey platform + TPM/Apple + UV obrigatória**: `WebAuthnService` força `authenticatorAttachment(PLATFORM)` e `userVerification(REQUIRED)` no cadastro e em todo login/step-up de magistratura; `finishEnrollment` rejeita attestation fora de `{tpm, apple}`. `PasskeyRequirementEnforcer.exigirParaMagistratura` passou a exigir essa combinação (attachment=platform + attestation confiável + fmt tpm/apple), não mais "qualquer passkey cadastrada alguma vez". (extensão a MP e Defensoria documentada na subseção seguinte)
- **`MinisterStepUpFilter` removido**: era redundante com a passkey endurecida acima e emitia credencial sem verificação real — mecanismo de segurança dormente/falso substituído, não apenas desligado.
- **Trava por inatividade de 10 min** (`MagistraturaIdleLockFilter`) e **geo-bloqueio de país/UF + detecção de VPN** (`MagistraturaGeofencePolicyService`, MaxMind GeoLite2) — controles complementares de defesa em profundidade, fora da matriz IAL/AAL/FAL formal mas reforçando o mesmo objetivo de AAL3 real para magistratura.
- **Primeiro cadastro guiado** (`MagistradoAtivacaoService`): ativação da conta via código de e-mail (OTP) emite a primeira sessão, pré-requisito para o próprio cadastro de certificado A3/passkey acontecer.

### Extensão do hardware-based authenticator para MP e Defensoria (2026-08-08)

A matriz revisada acima eleva Promotor e Defensor Público de AAL2 para AAL3, equiparando-os à magistratura — decisão de domínio apoiada nas garantias constitucionais análogas dessas carreiras (CF art. 127 e 134, funções essenciais à Justiça). Implementação: `TipoUsuario.requiresHardwareAuthAssurance()` substitui `isMagistratura()` nos 5 pontos que aplicam o endurecimento (`CertificadoAuthFacadeService`, `WebAuthnService`, `PasskeyRequirementEnforcer`, `MagistraturaIdleLockFilter`, `MagistraturaGeofenceFilter`) — certificado A3/A4 obrigatório, passkey platform+TPM/Apple com verificação de usuário obrigatória, trava por inatividade de 10 min e geo-bloqueio de país/UF/VPN agora valem também para promotor e defensor público. A exceção de viagem (`SupportTicketCategoria`) foi renomeada de `EXCECAO_VIAGEM_MAGISTRATURA` para `EXCECAO_VIAGEM_CARREIRA_JURIDICA` para refletir o escopo ampliado. Procuradoria (PGM/PGE/AGU) permanece fora desta extensão — decisão de escopo explícita, não descuido.

Na mesma revisão, `MagistraturaGeofencePolicyService.avaliar()` passou a bloquear (`BLOQUEADO_UF`) qualquer usuário sujeito ao geo-bloqueio (magistratura, MP ou Defensoria) que não tenha UF de lotação cadastrada — antes, `Usuario.uf == null` liberava o acesso silenciosamente, validando só o país. É uma mudança fail-closed deliberada: cadastro incompleto agora bloqueia em vez de degradar a proteção prometida pela matriz.

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
