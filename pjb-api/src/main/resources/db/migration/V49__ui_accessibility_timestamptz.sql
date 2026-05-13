alter table tb_usuario_accessibility_pref
  alter column accepted_at type timestamptz using accepted_at at time zone 'UTC',
  alter column updated_at type timestamptz using updated_at at time zone 'UTC',
  alter column last_evaluated_at type timestamptz using last_evaluated_at at time zone 'UTC',
  alter column next_eligible_at type timestamptz using next_eligible_at at time zone 'UTC';

alter table tb_accessibility_usage_snapshot
  alter column observed_at type timestamptz using observed_at at time zone 'UTC';
