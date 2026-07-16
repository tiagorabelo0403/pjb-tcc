# Débitos Técnicos — Registro Aberto

Registro de dívidas técnicas conhecidas e ainda não fechadas. Diferente da seção de Testes do
`README.md` (que narra dívidas já resolvidas), este arquivo documenta lacunas abertas — sem bloquear
nenhuma entrega em andamento — para que não fiquem só na memória de quem investigou.

Cada entrada sai daqui quando a dívida é fechada; o fechamento é então narrado no `README.md`, seguindo
o padrão já em uso (ex.: D-routing-preprotocolo, D-d25-testes-anexo).

## D-classificacao-contextual-default-permissivo

**Status:** aberta

**Contexto:** `classificacaoContextualCoerente` (em `QualifiedDocumentSignatureEnvelopeService.resolveClassificacaoContextualCoerente`)
retorna `true` por padrão, sem verificação, para 2 dos 14 chamadores de assinatura qualificada:

- `OfficialDocumentTemplateService` (ramo `TERMO_ACORDO`/`SEM_INTERESSE_MANIFESTACAO`) — gate é ABAC de
  leitura de processo (`requireReadProcesso`), não de papel. Pode ser intencional: esses 2 templates são
  tipicamente assinados por parte/advogado, não magistratura — mas não foi verificado se o valor `true`
  cego é a decisão de negócio certa ou só um ponto cego.
- `OperationalNotificationProofService` — gate é ABAC de capacidade institucional
  (`ASSINAR_MANIFESTACAO`), cujo motor de afiliação institucional (`InstitutionalAffiliationApplicationService`
  + `CapacidadeCaixaInstitucional`) não foi mapeado. Não se sabe quais `TipoUsuario` efetivamente recebem
  essa capacidade em produção.

**Risco:** baixo — os dois já têm gate próprio (ABAC), então não é ausência de controle de acesso. É
ausência de comparação contra `segmentoInstitucional()`, como os outros 12 chamadores já têm.

**Cobertura de teste:** o comportamento `default -> true` do switch é testado isoladamente (com um
`papelAssinante` sintético, não um dos 2 chamadores reais). Nenhum teste exercita
`OfficialDocumentTemplateService` ou `OperationalNotificationProofService` end-to-end pra confirmar que o
`true` realmente se propaga desses fluxos específicos.

**Quando revisitar:** se o motor de afiliação institucional for mapeado por outro motivo, ou se a política
de assinatura de TERMO_ACORDO precisar de auditoria mais rígida.

## D-domicilio-parte-tres-canais-nao-populam

**Status:** aberta

**Contexto:** `Processo.ufAutor`/`comarcaAutor`/`ufReu`/`comarcaReu` só são populados pelo canal REST
(via `ProcessoMapper`). Marketplace (`ApiMarketplaceService`), MNI (`MniXmlToProcessoAdapter`) e Laiane
(`LaianePeticaoInicialDraftService`) deixam os 4 campos nulos — cada um seta apenas `uf`/`comarca`
(competência), não domicílio de parte. `PoloCompositionPolicy` deriva `ufDomicilio`/`comarcaDomicilio`
diretamente desses 4 campos sem fallback, então o domicílio de parte fica nulo em `PoloProcessual`
nesses 3 canais também.

**Risco:** três correções de tamanho e natureza diferentes, não uma correção uniforme:
- Marketplace exige mudança de contrato público (`MarketplaceProtocoloRequest` não expõe esses campos
  hoje — afeta integradores externos já conectados).
- Laiane precisa de investigação prévia: não confirmado se a informação de domicílio da parte ré existe
  em algum lugar acessível no fluxo de draft.
- MNI exige parsing de endereço por parte no XML (`resolvePartes()` hoje só extrai nome e documento) —
  é extensão de parsing de formato externo, não ajuste pontual.

**Quando revisitar:** ao decidir prioridade de cada um dos três separadamente — não tratar como um único
item de trabalho.

## D-advisory-modos-nao-implementados

**Status:** aberta (não bloqueia nada — documentação corrigida para refletir o comportamento real)

**Contexto:** `LaianeJudicialDecisionAdvisoryService` sempre bloqueia publicação e exige revisão humana
(`publicationLocked`/`reviewRequired` sempre `true`, por política de segurança deliberada — não é bug) e
sempre opera em modo único (`advisoryMode = "ADVISORY_DRAFT_ONLY"`). Os 3 modos originalmente
documentados (`SUGESTIVO`/`RESTRITIVO`/`BLOQUEADOR`) nunca foram implementados.

**Quando revisitar:** se o produto decidir que a Laiane deve diferenciar níveis de consultoria (ex.:
permitir publicação sem revisão em casos de baixíssimo risco) — isso exigiria definir critério jurídico
de classificação por template, trabalho substantivo, não uma correção pontual.
