INSERT INTO tb_tribunal (sigla, nome, tipo_justica, grau, uf_sede)
VALUES
    ('STF', 'Supremo Tribunal Federal', 'SUPERIOR', 'CONSTITUCIONAL', 'DF'),
    ('STJ', 'Superior Tribunal de Justiça', 'SUPERIOR', 'SUPERIOR', 'DF'),
    ('TST', 'Tribunal Superior do Trabalho', 'SUPERIOR', 'SUPERIOR', 'DF'),
    ('TSE', 'Tribunal Superior Eleitoral', 'SUPERIOR', 'SUPERIOR', 'DF'),
    ('STM', 'Superior Tribunal Militar', 'SUPERIOR', 'SUPERIOR', 'DF')
ON CONFLICT (sigla) DO NOTHING;
