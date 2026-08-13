-- tb_unidade_institucional so existe a partir de V289 (criada depois de V275, que ja converteu
-- em bloco TODAS as colunas CHAR(n) do repositorio ate entao para VARCHAR/varchar — inclusive
-- tb_gov_service_registry.uf, que por isso NAO precisa de fix aqui, ja chegou varchar(2) direto
-- de V275). tb_unidade_institucional ficou de fora da varredura de V275 por ordem cronologica —
-- nasceu CHAR(2) em V289 e nunca foi corrigida, unico mismatch real remanescente entre migration
-- e a entidade JPA (UnidadeInstituicao.uf, @Column(length = 2) private String uf).
ALTER TABLE tb_unidade_institucional ALTER COLUMN uf TYPE VARCHAR(2);
