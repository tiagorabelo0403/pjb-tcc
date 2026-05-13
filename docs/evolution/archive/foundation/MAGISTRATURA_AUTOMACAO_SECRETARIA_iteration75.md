# Round 75 — automação viva entre magistratura e secretaria

Entrou uma malha única para transformar o ato jurisdicional em pacote operacional de secretaria sem engessar a marcha processual do magistrado.

## O que entrou
- `MagistraturaJudicialProvidenceAutomationService`
- `MagistraturaJudicialProvidenceCode`
- `MagistraturaJudicialProvidenceResponse`
- `MagistraturaJudicialProvidenceDispatchResponse`
- `POST /api/v1/magistratura/processos/{processoId}/atos/automation-preview`

## O que mudou
- preview e execução dos atos da magistratura agora devolvem providências derivadas
- o despacho/decisão continua livre, mas o sistema detecta audiência, intimação, publicação, perícia, ordem de cumprimento e conclusão
- quando o ato é executado, as providências podem ser projetadas automaticamente na malha correta da secretaria
- quando o ato nativo já criou `workItem`, a automação reaproveita esse item em vez de duplicar a fila

## Efeitos práticos
- juiz pode despachar de acordo com o próprio entendimento
- sistema sugere e distribui providências ao cartório/secretaria competente
- audiência cai na fila e no painel corretos, com data, hora e motivo
- secretaria recebe contexto operacional pronto para cumprir, anotar e impulsionar o processo
