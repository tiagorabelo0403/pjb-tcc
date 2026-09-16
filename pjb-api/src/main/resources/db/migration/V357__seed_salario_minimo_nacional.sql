-- Fecha D-scheduler-salario-minimo-nunca-ativado: semear por migration, decisao explicita do
-- Tiago em 2026-09-16 (nao ativar sync contra o Banco Central agora).
--
-- Investigando antes de escrever este INSERT: a dividia dizia "nenhuma migration semeia
-- salario_minimo_nacional", mas V115__teto_processual_salario_minimo.sql ja semeia 2024, 2025
-- e 2026 (mesmos decretos que uma pesquisa independente confirmou: 11.864/2023, 12.342/2024,
-- 12.797/2025) -- a dividia estava desatualizada, faltava so 2023. Idempotente com WHERE NOT
-- EXISTS, mesmo padrao da V115, para nao duplicar se alguma base ja tiver a linha por outro
-- caminho.
INSERT INTO salario_minimo_nacional (
    ano_referencia, valor_mensal, valor_diario, valor_hora, vigente_desde, vigente_ate,
    norma_referencia, fonte_oficial, ativo, atualizado_em, versao
)
SELECT 2023, 1320.00, 44.00, 6.00, DATE '2023-01-01', NULL, 'Lei 14.663/2023', 'Planalto', TRUE, NOW(), 0
WHERE NOT EXISTS (SELECT 1 FROM salario_minimo_nacional WHERE ano_referencia = 2023);

-- Achado ao conferir a aritmetica da V115 (ja commitada, nao pode ter o INSERT alterado):
-- valor_diario de 2026 saiu 54.04, mas 1621.00 / 30 = 54.0333..., que arredonda (HALF_UP, 2
-- casas) para 54.03 -- e o mesmo criterio que SalarioMinimoNacionalService.salvarOuAtualizar usa
-- em codigo (mensal.divide(new BigDecimal("30"), 2, RoundingMode.HALF_UP)). Erro de 1 centavo,
-- baixo impacto real, mas incorreto -- corrigido aqui em vez de deixar passar por ter sido achado
-- de raspao.
UPDATE salario_minimo_nacional
SET valor_diario = 54.03
WHERE ano_referencia = 2026 AND valor_diario = 54.04;
