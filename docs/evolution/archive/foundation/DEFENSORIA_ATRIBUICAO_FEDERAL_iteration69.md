# Round 69 — trava institucional da Defensoria por atribuição e competência

Entrou uma malha nova para evitar protocolo indevido da defensoria estadual em fluxo federal.

## O que faz

- detecta sinais de Justiça Federal, tribunal federal e polo passivo federal
- diferencia defensoria estadual de defensoria federal
- bloqueia protocolo da defensoria estadual quando o caso aponta para União, autarquia federal, empresa pública federal ou Justiça Federal
- devolve redirecionamento para DPU/unidade federal competente
- sinaliza revisão assistida quando a DPU encontrar fluxo estadual

## Onde passou a operar

- wizard simples de protocolo
- protocolo de rascunho convertido em processo real
- protocolo ativo dentro do processo
- painel da defensoria para petição, recurso e gratuidade

## Núcleo novo

- `DefensoriaInstitutionalCompetenceGuardService`

## Efeito prático

- defensoria estadual não sobe processo federal por engano
- frontend recebe o guard institucional no wizard antes do protocolo final
- pontos de protocolo reais passaram a ter bloqueio duro
