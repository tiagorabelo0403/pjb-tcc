CREATE TABLE IF NOT EXISTS tb_cidadao_dashboard_snapshot (
  cidadao_user_id BIGINT PRIMARY KEY,
  cpf_hash VARCHAR(64) NOT NULL,
  version BIGINT NOT NULL DEFAULT 0,
  updated_at TIMESTAMPTZ NOT NULL,
  badges_json TEXT NOT NULL,
  widgets_json TEXT NOT NULL,
  pendencias_json TEXT NOT NULL,
  proximos_eventos_json TEXT NOT NULL,
  recentes_json TEXT NOT NULL,
  gov_hub_json TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS ix_cid_dash_snapshot_cpfhash ON tb_cidadao_dashboard_snapshot (cpf_hash);

CREATE TABLE IF NOT EXISTS tb_cidadao_dashboard_item (
  cidadao_user_id BIGINT NOT NULL,
  processo_id BIGINT NOT NULL,
  last_update_at TIMESTAMPTZ NOT NULL,
  sort_key BIGINT NOT NULL,
  card_json TEXT NOT NULL,
  flags_json TEXT NOT NULL,
  PRIMARY KEY (cidadao_user_id, processo_id)
);

CREATE INDEX IF NOT EXISTS ix_cid_dash_item_sort ON tb_cidadao_dashboard_item (cidadao_user_id, sort_key DESC);
CREATE INDEX IF NOT EXISTS ix_cid_dash_item_updated ON tb_cidadao_dashboard_item (cidadao_user_id, last_update_at DESC);
