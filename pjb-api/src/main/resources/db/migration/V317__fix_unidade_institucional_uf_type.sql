ALTER TABLE tb_unidade_institucional ALTER COLUMN uf TYPE VARCHAR(2);

-- Mesmo mismatch CHAR(2) x VARCHAR(2) em tb_gov_service_registry.uf (V72), entre a única outra
-- migration com CHAR(n) que tem entidade JPA mapeada (GovServiceRegistry.uf, @Column(length = 2)
-- private String uf) — Hibernate valida bpchar contra varchar e aborta o boot na primeira
-- divergência. Varredura sistemática confirmou que as demais colunas CHAR(n) do repositório
-- (pjb_instituicao_judicial.uf, pjb_processo_externo_carga.payload_hash,
-- pjb_assinatura_pendente.sha256_esperado, pjb_prazo_processual.uf, pjb_perito.cpf,
-- tb_doc_provider_registry.uf) não têm nenhuma entidade @Entity mapeada, então não entram na
-- validação de schema do Hibernate.
ALTER TABLE tb_gov_service_registry ALTER COLUMN uf TYPE VARCHAR(2);
