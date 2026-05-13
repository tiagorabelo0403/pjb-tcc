-- Governança: dupla checagem (4 olhos) para propostas sensíveis

ALTER TABLE tb_rito_rule_proposal
  ADD COLUMN IF NOT EXISTS requires_dual_approval BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE tb_rito_rule_proposal
  ADD COLUMN IF NOT EXISTS first_reviewed_at TIMESTAMPTZ;

ALTER TABLE tb_rito_rule_proposal
  ADD COLUMN IF NOT EXISTS first_reviewed_by_user_id BIGINT;

ALTER TABLE tb_rito_rule_proposal
  ADD COLUMN IF NOT EXISTS first_decision_notes TEXT;

ALTER TABLE tb_rito_rule_proposal
  ADD COLUMN IF NOT EXISTS second_decision_notes TEXT;

CREATE INDEX IF NOT EXISTS idx_rito_rule_proposal_pending_second
  ON tb_rito_rule_proposal(status, first_reviewed_at DESC)
  WHERE status = 'PENDING_SECOND_APPROVAL';
