# Relatorio inicial da Onda 3 de modularizacao

## 1. Estado inicial do git

- Branch: `master`.
- Working tree antes da Onda 3: limpo.
- Commit local ainda nao enviado: `8e53215 arch(modular): isolar modulo acordo do legado com ports e facades oficiais`.
- `HEAD..origin/master`: vazio.
- Push da nova rodada: nao realizado.

## 2. Objetivo da onda

Bloquear crescimento de violacoes arquiteturais sem tentar corrigir todo o legado. A divida antiga continua catalogada, mas deixa de poder aumentar silenciosamente.

## 3. Diagnostico aceito

O PJB continua sendo tratado como monolito modular parcial com nucleo ainda megamonolitico. O guard modular registrou 0 errors e 419 warnings legados.

## 4. Problema antes desta onda

O guard ja separava `ERROR` de `WARNING`, mas os warnings legados ainda eram apenas informativos. Um novo warning poderia entrar junto com os antigos sem evidenciar aumento de divida.

## 5. Plano da Onda 3

- Versionar um baseline numerico do guard.
- Fazer o guard comparar warnings atuais contra o baseline.
- Gerar relatorio JSON alem do markdown.
- Manter errors bloqueantes.
- Manter warnings legados tolerados apenas dentro do orcamento.
- Atualizar documentacao de baseline, regras e plano de ondas.

## 6. O que nao sera mexido agora

- Pacotes legados em massa.
- Controllers legados.
- Repositories legados.
- FindAll global.
- Ciclos historicos entre modulos antigos.
- Auditoria global, conforme decisao da rodada.

## 7. Riscos

- Falso positivo se o guard for agressivo demais.
- Baseline envelhecer se reducoes futuras nao forem registradas.
- Aumento de budget sem justificativa pode virar permissao informal.

## 8. Controle

O controle desta onda e simples: baseline atual como teto, falha se crescer, reducao progressiva por ondas documentadas.
