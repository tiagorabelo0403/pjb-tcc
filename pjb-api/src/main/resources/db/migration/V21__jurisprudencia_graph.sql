-- PJB v26 - Grafo de Jurisprudência (citações determinísticas)

CREATE TABLE IF NOT EXISTS tb_precedente_edge (
    id BIGSERIAL PRIMARY KEY,
    from_precedente_id BIGINT NOT NULL,
    relation VARCHAR(30) NOT NULL,
    target_type VARCHAR(40) NOT NULL,
    target_ref VARCHAR(220) NOT NULL,
    raw VARCHAR(260),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_edge_precedente'
    ) THEN
        ALTER TABLE tb_precedente_edge
            ADD CONSTRAINT fk_edge_precedente FOREIGN KEY (from_precedente_id)
                REFERENCES tb_precedente(id) ON DELETE CASCADE;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_edge_from ON tb_precedente_edge(from_precedente_id);
CREATE INDEX IF NOT EXISTS idx_edge_target ON tb_precedente_edge(target_type, target_ref);
CREATE UNIQUE INDEX IF NOT EXISTS ux_edge_unique ON tb_precedente_edge(from_precedente_id, relation, target_type, target_ref);
