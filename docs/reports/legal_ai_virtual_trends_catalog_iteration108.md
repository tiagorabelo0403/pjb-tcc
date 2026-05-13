Round 108 corrigiu a canonicalização dos virtual trends para o formato certo da malha conversacional jurídica: uma classe exclusiva e dedicada ao catálogo dos trends.

Entrou `JuridicaVirtualTrendsCatalog`, que agora concentra os cinco trends, seus papéis e suas ações canônicas:
- `RELATOR_ESTRUTURAL`
- `LEGISLADOR_NORMATIVO`
- `PRECEDENTES_ESTRATEGICOS`
- `AUDITOR_SIMBOLICO`
- `REDATOR_CONVERSACIONAL`

O `JuridicaVirtualTrendsCouncilService` passou a consumir apenas `JuridicaVirtualTrendsCatalog.profiles()`.

Com isso:
- a `JuridicaSpineLabels` voltou a ficar limpa, restrita aos labels estruturantes da espinha;
- o catálogo dos virtual trends deixou de ficar espelhado na spine;
- a conversa jurídica continua no mesmo eixo, mas agora com a organização correta do catálogo.
