# ADR-0042 — trilha explicável e persistível de autorização

## Status
Aceito

## Contexto

Após a modularização da fronteira ABAC, o serviço principal de autorização ficou mais curto, mas a decisão ainda era devolvida apenas como `AuthzDecision`, sem uma trilha formal e persistível contendo:

- recurso efetivamente avaliado
- nível de sigilo considerado
- exigência de step-up por recurso
- nível de risco da decisão
- vínculo da decisão com request, ator e versão de política

Isso dificultava auditoria forte, endurecimento zero trust e futura exposição controlada de diagnósticos de autorização para superfícies administrativas ou forenses.

## Decisão

Foi introduzida uma trilha formal de autorização com os seguintes elementos:

- `PjbAuthorizationEvaluation` como envelope da decisão final
- `PjbAuthorizationDecisionTrail` como trilha explicável persistível
- `PjbAuthorizationTrailAssembler` para montagem da trilha por recurso
- `PjbAuthorizationStepUpAssessment` para formalizar exigência, satisfação e canal de autenticação reforçada
- `PjbAuthorizationRiskLevel` para classificar o risco da decisão

A `PjbAuthorizationPolicyFacade` passa a produzir avaliações completas para:

- leitura de processo
- leitura de votos colegiados
- leitura de documento
- escrita processual

A auditoria agora persiste a decisão em ledger com:

- `eventCode` específico por ação e resultado
- `payloadHash` derivado da trilha
- descrição resumida do motivo, risco, sigilo e estado de step-up

## Consequências

### Positivas

- a autorização passa a ter trilha persistível e auditável por recurso
- decisões negadas por falta de step-up deixam de ser apenas efeito colateral e viram evento formalizado
- o monólito ganha base concreta para futura exposição de diagnósticos de segurança sem reabrir o serviço principal
- a política de sigilo alto fica mais coerente entre leitura processual, documental, votos e escrita

### Negativas

- a malha ABAC fica mais rica e exige coordenação maior entre política, sigilo, trilha e auditoria
- o aumento de rigidez em step-up pode revelar fluxos antigos que dependiam de leitura de sigilo alto sem autenticação reforçada
