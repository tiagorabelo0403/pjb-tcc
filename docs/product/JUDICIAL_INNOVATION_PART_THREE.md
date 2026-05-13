# Inovação judicial assistida III: Núcleo 4.0 para Juizados Especiais Adjuntos

A terceira camada de inovação incorpora ao PJB a governança operacional do Núcleo de Justiça 4.0 aplicado aos Juizados Especiais Cíveis Adjuntos, com atenção especial ao cenário TJCE, comarca de Morada Nova, funcionamento em 18 de maio de 2026 e opção facultativa no momento do protocolo.

## Informação operacional absorvida

O PJB passa a representar o fluxo em que o Núcleo 4.0 é unidade 100% digital, utiliza PJe como sistema de tramitação, busca maior agilidade, eficiência e modernização, e exige que a opção seja feita no cadastro da ação no momento do protocolo.

A orientação operacional codificada preserva as seguintes regras:

- a opção pelo Núcleo 4.0 é facultativa;
- a opção deve ocorrer no cadastro da ação no PJe;
- não basta mencionar o Núcleo 4.0 na petição inicial;
- a escolha não pode ser alterada após a distribuição;
- se não houver opção no cadastro, o processo segue na vara comum;
- não há redistribuição automática para o Núcleo 4.0;
- a escolha da parte autora deve ser respeitada integralmente;
- a comarca de Morada Nova entra na etapa com início em 18 de maio de 2026;
- a base legal operacional inclui Portaria TJCE nº 73/2026, Resolução do Tribunal Pleno TJCE nº 13/2024 e Orientação Normativa CGJE nº 05/2025.

## Integração sem duplicidade

A implementação foi conectada ao eixo já existente de Justiça Digital:

```text
pjb-api/src/main/java/com/tcc/pjb/backend/core/plataforma/sustentacao/digitaljustice
```

Não foi criado módulo paralelo de juizado, distribuição, secretaria ou PJe. O novo código complementa o planner digital já existente e conversa com o fluxo nacional de Juizados por meio de decisão de roteamento, guia pública, catálogo de etapas e bridge de protocolo.

## Artefatos executáveis

```text
PjbJuizadoAdjuntoNucleoStage
PjbJuizadoAdjuntoNucleoStageCatalog
PjbJuizadoAdjuntoNucleoOptionRequest
PjbJuizadoAdjuntoNucleoOptionDecision
PjbJuizadoAdjuntoNucleoOptionService
PjbJuizadoAdjuntoPublicGuidance
PjbJuizadoAdjuntoOperationalProfile
PjbJuizadoAdjuntoPjeProtocolBridge
PjbDigitalJusticeUnitPlanner.assessJuizadoAdjunto
```

## Critério de produto

O fluxo só direciona o processo ao Núcleo 4.0 quando há unidade contemplada no cronograma, matéria de Juizado Especial Cível, ausência de unidade autônoma impeditiva, protocolo dentro da etapa aplicável e opção expressa no cadastro da ação. Em todos os demais cenários, a decisão conserva a tramitação na origem ou bloqueia o roteamento com motivos auditáveis.
