-- Governança: propostas de regra para calibrar ritos sem redeploy
-- (não aplica automaticamente; serve como workflow auditável)

CREATE TABLE IF NOT EXISTS tb_rito_rule_proposal (
  id UUID PRIMARY KEY,
  rito_resolved VARCHAR(64) NOT NULL,
  rito_chosen   VARCHAR(64) NOT NULL,
  occurrences   INTEGER     NOT NULL DEFAULT 0,
  sample_reasons_json TEXT,
  status        VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
  notes         TEXT,
  created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
  created_by_user_id BIGINT,
  reviewed_at   TIMESTAMPTZ,
  reviewed_by_user_id BIGINT,
  decision_notes TEXT
);

CREATE INDEX IF NOT EXISTS idx_rito_rule_proposal_status_created
  ON tb_rito_rule_proposal(status, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_rito_rule_proposal_pair
  ON tb_rito_rule_proposal(rito_resolved, rito_chosen);

-- Evita múltiplos rascunhos concorrentes para o mesmo par (resolved->chosen)
CREATE UNIQUE INDEX IF NOT EXISTS uq_rito_rule_proposal_pair_draft
  ON tb_rito_rule_proposal(rito_resolved, rito_chosen)
  WHERE status = 'DRAFT';
