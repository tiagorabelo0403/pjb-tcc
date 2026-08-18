-- Converte todas as colunas CHAR(n) para VARCHAR(n) para alinhar com mapeamentos JPA
ALTER TABLE pjb_assinatura_pendente     ALTER COLUMN sha256_esperado TYPE varchar(64);
ALTER TABLE pjb_instituicao_judicial    ALTER COLUMN uf              TYPE varchar(2);
ALTER TABLE pjb_prazo_processual        ALTER COLUMN uf              TYPE varchar(2);
ALTER TABLE pjb_processo_externo_carga  ALTER COLUMN payload_hash    TYPE varchar(64);
ALTER TABLE tb_doc_provider_registry    ALTER COLUMN uf              TYPE varchar(2);
ALTER TABLE tb_gov_service_registry     ALTER COLUMN uf              TYPE varchar(2);

-- pjb_perito.cpf — tabela sem entidade JPA, mas corrige para consistência
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name='pjb_perito') THEN
        ALTER TABLE pjb_perito ALTER COLUMN cpf TYPE varchar(11);
    END IF;
END$$;
