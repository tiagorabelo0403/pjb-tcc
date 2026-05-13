DELETE FROM tb_processo_distribuicao_competencia t
USING tb_processo_distribuicao_competencia newer
WHERE t.request_hash IS NOT NULL
  AND newer.request_hash = t.request_hash
  AND newer.id > t.id;

CREATE UNIQUE INDEX IF NOT EXISTS uk_proc_dist_comp_request_hash
    ON tb_processo_distribuicao_competencia (request_hash)
    WHERE request_hash IS NOT NULL;
