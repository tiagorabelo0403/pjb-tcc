-- Reduz o limiar de ANALYZE automatico do autovacuum nas tabelas de maior volume.
-- Achado real: apos uma carga em massa (ex.: migracao de processos), o planejador do
-- Postgres usa estatistica desatualizada ate o autovacuum rodar ANALYZE — e o limiar
-- padrao (10% da tabela) so dispara tarde demais em tabelas de milhoes de linhas,
-- deixando consultas usando o indice errado por um bom tempo (observado: 266ms com
-- estatistica velha vs 0.18ms apos ANALYZE, na mesma consulta).
alter table tb_processo
    set (autovacuum_analyze_scale_factor = 0.02, autovacuum_analyze_threshold = 200);

alter table tb_movimentacao_processual
    set (autovacuum_analyze_scale_factor = 0.02, autovacuum_analyze_threshold = 200);

alter table tb_documento_processual
    set (autovacuum_analyze_scale_factor = 0.02, autovacuum_analyze_threshold = 200);
