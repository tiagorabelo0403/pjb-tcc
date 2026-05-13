# PJB — Innovation Layer II

A segunda camada de inovação complementa a base já existente sem criar bounded contexts paralelos. Ela transforma capacidades transversais em componentes pequenos, verificáveis e compatíveis com a governança do monólito modular Java 21.

## Audiência digital governada

A audiência digital passa a ter plano operacional antes da emissão de convites e intimações. A orquestração verifica sala segura, gravação, transcrição, identidade, acessibilidade e perfis sensíveis. O objetivo é impedir audiência digital improvisada e manter revisão humana quando houver sigilo, incapazes ou lacunas técnicas.

Artefatos:

- `service.audiencia.digital.PjbDigitalHearingOrchestrator`
- `service.audiencia.digital.PjbDigitalHearingPlan`
- `service.audiencia.digital.PjbDigitalHearingInput`

## Núcleo de Justiça Digital

O núcleo digital passa a ser avaliado por matéria, território, capacidade mensal, carga ativa, equipe especializada e audiência digital. A alocação de novos processos depende de readiness e saturação operacional.

Artefatos:

- `core.plataforma.sustentacao.digitaljustice.PjbDigitalJusticeUnitPlanner`
- `core.plataforma.sustentacao.digitaljustice.PjbDigitalJusticeUnitProfile`

## Continuidade offline controlada

O modo offline não autoriza atos críticos sem revalidação. Consulta, minuta e juntada podem ser capturadas quando houver cofre local selado, snapshot atual e dispositivo vinculado. Assinatura, decisão e protocolo exigem revalidação online.

Artefatos:

- `service.offline.continuity.PjbOfflineContinuityPolicy`
- `service.offline.continuity.PjbOfflineContinuityDecision`

## Atermação assistida

A atermação recebe triagem estruturada para narrativa, pedido, valor, documentos, urgência, vulnerabilidade e presença de ente público. A saída é minuta revisável por servidor ou defensor, nunca protocolo autônomo sem validação humana.

Artefatos:

- `core.procedural.atermacao.PjbAtermacaoGuidedIntakeService`
- `core.procedural.atermacao.PjbAtermacaoGuidedIntakePlan`

## Precedentes vivos

A camada de precedentes qualificados identifica sinais de repercussão geral, repetitivos, IRDR, IAC, súmula, distinguishing e risco de superação. O resultado orienta sobrestamento, aplicação, afastamento ou revisão humana.

Artefatos:

- `service.jurisprudencia.awareness.PjbPrecedentAwarenessEngine`
- `service.jurisprudencia.awareness.PjbPrecedentAwarenessReport`

## Acordos inteligentes com salvaguardas

A inteligência de acordo não impõe composição. Ela mede janela de oportunidade com base em litigância repetitiva, prova documental, jurisprudência estável, valor, vulnerabilidade e restrições de interesse público.

Artefatos:

- `core.kernel.advisory.PjbSettlementGovernanceLens`
- `core.kernel.advisory.PjbSettlementOpportunityReport`

## Acesso à Justiça mensurável

Cada jornada pode ser avaliada por linguagem simples, mobile, leitor de tela, baixo consumo de dados, pessoa idosa, deficiência, parte sem advogado e orientação multicanal.

Artefatos:

- `core.frontend.accessibility.PjbAccessToJusticeScoreService`
- `core.frontend.accessibility.PjbAccessToJusticeAssessment`

## Observabilidade processual

A observabilidade deixa de ser apenas técnica. O PJB passa a representar processo parado, pico de fila, falha de intimação, latência de assinatura, protocolo represado, pressão de prazo e sobrecarga de secretaria como sinais operacionais.

Artefatos:

- `core.observability.procedural.PjbProceduralObservabilityService`
- `core.observability.procedural.PjbProceduralObservabilitySnapshot`

## Marketplace judicial governado

O marketplace existente recebe uma lente específica para serviços judiciais homologados. Perícia, mediação, conciliação, tradução, leilão, cálculo, OCR, transcrição e oficialato digital dependem de homologação, auditoria, LGPD e cobertura por tribunal.

Artefatos:

- `service.api.PjbJudicialServiceMarketplaceGovernance`
- `service.api.PjbJudicialServiceOffering`

## Paridade com sistemas legados

O kit de paridade prova cobertura por capacidade, sem copiar vícios dos sistemas substituídos. Consulta pública, peticionamento, comunicação, assinatura, migração, audiência digital, Juizado, indisponibilidade e MNI são avaliados como compromissos explícitos.

Artefatos:

- `core.plataforma.substituicao.parity.PjbLegacyParityTestKit`
- `core.plataforma.substituicao.parity.PjbLegacyParityReport`

## Critério de evolução

A camada é deliberadamente conservadora. Ela não cria novo motor processual, não duplica secretaria, não duplica marketplace e não substitui serviços existentes. Cada componente entrega uma política ou projeção de governança que pode ser integrada progressivamente às superfícies atuais.
