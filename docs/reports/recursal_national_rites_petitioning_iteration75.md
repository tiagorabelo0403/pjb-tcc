# Round 75 — matriz nacional de peticionamento recursal por rito e espécie

## Objetivo

Trazer a inteligência do peticionamento da primeira instância para o universo recursal e dos embargos sem duplicar studio, wizard, jornada, rascunhos e revisão governada, mas diferenciando a peça conforme ramo, espécie recursal e ator jurídico habilitado.

## Entradas principais

- `RecursalNationalRitesPetitioningBlueprint`
- `RecursalNationalRitesPetitioningTrackFactory`
- passo novo no playbook: `DIFERENCIAR_PETICIONAMENTO_POR_RITO_E_ESPECIE`
- trilha nova no workspace: `MATRIZ_NACIONAL_PETICIONAMENTO_RECURSAL`

## Critério estrutural adotado

A rodada não abriu novo motor de peticionamento. Ela reaproveitou a espinha já existente do studio e classificou a saída pela matriz nacional de rito, espécie e ator jurídico:

- cível, execução e juizados especiais
- penal e execução penal
- trabalhista
- eleitoral
- militar
- peças institucionais e contraditório recursal

## Efeito operacional

O sistema passa a distinguir:

- peça inaugural de 1º grau
- petição recursal
- petição de embargos
- contrarrazões e contraminuta
- parecer, cota, promoção e manifestação institucional
- memoriais, resposta a laudo, quesitos complementares e petição intercorrente recursal

## Integração com o que já existia

A classificação foi conectada ao que já existia em:

- session/studio/workspace
- minuta rápida
- revisão governada
- diff de minuta
- wizard de protocolo
- jornada inteligente
- rascunhos e protocolo
- playbook recursal
- workspace recursal

## Validação honesta

- guards Python passaram
- compilação dirigida com `javac` dos arquivos centrais alterados passou com stub mínimo de `@Service`
- não houve validação Maven global
- não houve validação Git real porque o ZIP continua sem `.git`
