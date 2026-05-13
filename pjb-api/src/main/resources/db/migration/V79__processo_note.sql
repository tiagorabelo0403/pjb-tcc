CREATE TABLE tb_processo_note (
  id BIGSERIAL PRIMARY KEY,
  processo_id BIGINT NOT NULL,
  author_usuario_id BIGINT NOT NULL,
  author_tipo VARCHAR(24) NOT NULL,
  body TEXT NOT NULL,
  created_at TIMESTAMPTZ NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_pnote_proc_upd ON tb_processo_note (processo_id, updated_at DESC);
CREATE INDEX idx_pnote_author ON tb_processo_note (author_usuario_id);
