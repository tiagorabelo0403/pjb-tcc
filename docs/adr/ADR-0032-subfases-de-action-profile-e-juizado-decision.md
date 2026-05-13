# ADR-0032 — subfases de action profile e juizado decision

## Status
Aceito

## Contexto

Após a blindagem da fronteira de payload procedural e da fronteira de `forum allocation`, os principais mini-monólitos residuais do eixo procedural passaram a ser:

- `NationalProceduralActionProfileResolver`, que ainda misturava classificação material de direito público, direito privado e aplicação de alertas/checklists específicos em um único corpo
- `NationalProceduralJuizadoDecisionResolver`, que ainda misturava exclusões estruturais do regime dos juizados, roteamento por trilha material e fechamento de confiança/revisão em um único corpo

Isso mantinha dois pontos relevantes de decisão material com alta densidade interna, pouca separação por trust boundary e menor previsibilidade para endurecimento e evolução futura.

## Decisão

Foram introduzidas subfases explícitas nesses dois eixos.

### Action profile

- `NationalProceduralActionProfileContext`
- `NationalProceduralActionProfilePublicLawResolver`
- `NationalProceduralActionProfilePrivateRightsResolver`
- `NationalProceduralActionProfileSupport`

`NationalProceduralActionProfileResolver` passa a atuar apenas como orquestrador entre a subfase de direito público/especial e a subfase de direitos privados/civis.

A subfase pública concentra:

- constitucional
- eleitoral
- trabalhista
- militar/penal
- improbidade
- ação civil pública
- desapropriação
- previdenciário
- fazenda pública

A subfase privada concentra:

- família
- sucessões
- imobiliário/possessório
- monitória/consignação
- empresarial
- consumo
- fallback civil geral

### Juizado decision

- `NationalProceduralJuizadoDecisionContext`
- `NationalProceduralJuizadoExclusionResolver`
- `NationalProceduralJuizadoTrackResolver`
- `NationalProceduralJuizadoDecisionSupport`

`NationalProceduralJuizadoDecisionResolver` passa a atuar apenas como orquestrador entre:

- exclusões estruturais do regime dos juizados
- fechamento por trilha material/econômica do juizado aplicável

A subfase de exclusão concentra:

- exclusões por natureza especial
- exclusões por famílias incompatíveis com juizado

A subfase de trilha concentra:

- JEF
- Juizado da Fazenda Pública
- JEC
- JECRIM
- fallback final

## Consequências

### Positivas

- reduz-se a concentração material residual em dois pontos críticos do eixo procedural
- melhora-se a previsibilidade das decisões de classificação material e de aderência ao sistema dos juizados
- o padrão arquitetural do eixo fica mais uniforme, com contexto próprio e subfases explícitas
- aumenta a governança sobre exclusões, fallback e alertas/checklists operacionais sem alterar o contrato externo do `ProceduralRoutingReport`

### Custos

- cresce o número de colaboradores e contratos internos do eixo procedural
- exige disciplina para evitar recontaminação do orquestrador por novas regras materiais nas próximas rodadas

## Relações

- ADR-0029 — subfases do core analyzer do NationalProceduralRouting
- ADR-0030 — subfases de judicial placement e review synthesis
- ADR-0031 — blindagem do payload procedural e da fronteira de forum allocation
