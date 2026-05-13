# Round 134 — signed attestation executável na borda das superfícies jurídicas

Nesta rodada eu continuei do round 133 sem abrir trilha paralela e fechei a próxima prova executável do boundary HTTP das três rotas jurídicas governadas sob chamadas server-to-server com atestação assinada de origem:
- novo suporte de teste compartilhado em `pjb-api/src/test/java/com/tcc/pjb/backend/ai/juridica/api/LegalAiSignedOriginTestSupport.java`, reaproveitando a malha real com:
  - `RequestBodyHashFilter`
  - `ApiRouteGovernanceFilter`
  - `ApiRequestOriginGovernanceFilter`
  - origem confiável `edge-app`
  - CIDR permitido e faixas permitidas em `/api/ai/legal/**`
- novo IT HTTP em `pjb-api/src/test/java/com/tcc/pjb/backend/ai/juridica/api/LegalAiSignedOriginGovernanceIT.java` provando:
  - aceite de atestação assinada válida em:
    - `POST /api/ai/legal/minuta`
    - `POST /api/ai/legal/grounding/check`
    - `POST /api/ai/legal/conversation`
  - rejeição por `X-PJB-Origin-Id` ausente em `conversation`
  - rejeição por assinatura inválida em `grounding/check`
  - rejeição por `X-PJB-Timestamp` fora da janela soberana em `minuta`
  - rejeição real por `BODY_HASH_MISMATCH` quando `X-PJB-Body-Hash` diverge do corpo canônico no boundary
- novo provider contract test em `pjb-api/src/test/java/com/tcc/pjb/backend/contracts/provider/LegalAiSignedOriginProviderContractTest.java`
- novo pact em `pjb-api/src/test/resources/pacts/provider/PjbLegalAiSignedOriginConsumer-PjbLegalAiSignedOriginProvider.json`
- novos guards arquiteturais:
  - `JuridicaLegalAiSignedOriginRound134ArchitectureTest`
  - `PjbLegalAiSignedOriginContractCoverageArchitectureTest`

Correção material descoberta nesta rodada:
- no boundary real, `RequestBodyHashFilter` executa antes da governança de origem e, por isso, `X-PJB-Body-Hash` divergente não deve ser “fingido” como rejeição da origem soberana;
- a rejeição correta ponta a ponta é `409 BODY_HASH_MISMATCH`, preservando a ordem real da cadeia de filtros já travada em `SecurityConfig`.

Ajustes de borda adicionais:
- `application.yml` passou a permitir explicitamente nos headers de CORS:
  - `X-PJB-Origin-Id`
  - `X-PJB-Signature-Alg`
- `application.yml` passou a expor na resposta:
  - `X-PJB-Origin-Mode`
  - `X-PJB-Origin-Subject`

O que isso materializou:
- as três superfícies jurídicas passaram a ter prova executável de aceite por origem assinada soberana na borda;
- a malha agora prova separadamente origem assinada válida, assinatura inválida, `origin id` ausente, timestamp inválido e divergência de body hash;
- a prova de `BODY_HASH_MISMATCH` respeita a ordem real do pipeline de segurança já recuperado e não cria comportamento fictício para agradar teste;
- tudo isso foi feito sem gateway paralelo, sem fila, sem executor novo e sem reabrir a IA jurídica.
