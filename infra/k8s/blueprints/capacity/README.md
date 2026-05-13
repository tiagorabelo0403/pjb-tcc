# Blueprints de capacidade soberana do PJB

Este diretório guarda blueprints opcionais para o plano de capacidade do cluster.

Nada aqui entra nas sobreposições principais por padrão. A ideia é preparar a expansão por nó sem acoplar o projeto a um provedor ou a CRDs opcionais no caminho de renderização base.

Arquivos:
- `kubelet-reserved-node-profile.yaml` — reserva de CPU, memória e armazenamento efêmero para kubelet e daemons do sistema, além de limiares de eviction.
- `node-pool-contract.md` — contrato de rótulos, taints e perfis de node pool sugeridos para frontdoor, processual crítico e carga bulk.

Aplicação sugerida:
1. Ajustar os valores ao perfil real de nó do cluster.
2. Aplicar a configuração de kubelet via mecanismo do provedor ou bootstrap do cluster.
3. Refletir os rótulos e taints do contrato nos grupos de nós ou node pools do ambiente.
