
## Round 105 — Legal AI Spine: anti-hallucination guard and grounded citations

Entrou o lote anti-alucinacao dentro da mesma IA juridica ja existente, sem abrir malha paralela:
- `LegalAiHallucinationGuardDescriptor` em `model/dto/ai/legal/spine`;
- `LegalHallucinationGuardRequest` e `LegalHallucinationGuardResponse` em `model/dto/ai/legal`;
- `JuridicaAntiHallucinationProfileService` e `JuridicaHallucinationGuardService` em `ai/juridica/spine`;
- ampliacao da `JuridicaLegalAiSpineService` e da `LegalAiSpineProfileResponse` para materializar politica anti-alucinacao;
- ampliacao da `LegalAiController` e da `LegalAiSurfaceFacadeService`;
- V1/V2/V3 agora propagam metadados explicitos de anti-alucinacao na mesma spine juridica.

O que passou a existir:
- endpoint juridico estruturado para checagem de grounding/citacao:
  - `POST /api/ai/legal/grounding/check`
- capability canonica de bloqueio de artigo/jurisprudencia inventados:
  - `LEGAL_HALLUCINATION_GUARD_V3`
- placeholder canonico para citacao nao confirmada:
  - `[NAO_CONFIRMADO]`

Politicas novas desta rodada:
- artigo exige verificacao grounded;
- precedente exige verificacao grounded;
- citacao livre sem fonte fica bloqueada;
- alegacao normativa sem base confirmada fica bloqueada;
- V1/V2/V3 passam a expor `juridica_hallucination_guard`, `juridica_citation_emission_mode` e `juridica_unresolved_citation_placeholder`.

Validacao honesta:
- compile dirigido do lote novo;
- guard de concorrencia;
- sem afirmar build Maven global verde;
- sem afirmar compile total do `pjb-api`;
- sem afirmar Docker estavel.
