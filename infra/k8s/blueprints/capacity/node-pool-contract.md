# Contrato soberano de node pools do PJB

Rótulos sugeridos:
- `pjb.io/node-plane=frontdoor`
- `pjb.io/node-plane=processual`
- `pjb.io/node-plane=bulk`
- `pjb.io/node-plane=control-support`

Taints sugeridos:
- `pjb.io/frontdoor=critical:NoSchedule`
- `pjb.io/processual=critical:NoSchedule`
- `pjb.io/bulk=preferred:NoSchedule`

Uso sugerido:
- `frontdoor` para `pjb-api`, ingress e borda crítica.
- `processual` para `pjb-worker`, recomposição, protocolo e cargas judiciais quentes.
- `bulk` para indexação, recomputação pesada e rotinas de baixa prioridade.
- `control-support` para observabilidade, operadores e componentes auxiliares.

Diretriz operacional:
- manter pools separados por zona;
- reservar capacidade mínima para `frontdoor` e `processual`;
- permitir que `bulk` seja a primeira classe a sofrer redução em cenário de contenção.
