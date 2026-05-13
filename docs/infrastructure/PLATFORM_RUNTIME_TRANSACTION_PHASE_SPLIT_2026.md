# Runtime Transaction Phase Split 2026

Esta rodada endurece três serviços com maior risco de retenção silenciosa de conexão e transação longa:

- `FacilitadorBatnaService`
- `RadarPadroesService`
- `AtlasAcessoJusticaService`

## Diretriz adotada

1. leitura curta governada
2. cálculo pesado fora da transação
3. persistência curta em nova transação com budget explícito
4. publicação/auditoria fora da transação sempre que possível

## Ganho arquitetural

- menor retenção de conexão em análise BATNA e Radar
- sincronização IBGE sem uma transação monolítica única
- menor risco de saturar pool por laço longo com `save` sequencial
- base preparada para batch governance por operação
