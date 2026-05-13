create index if not exists idx_prof_access_grant_status_actor_window
    on tb_professional_access_grant (approval_status, actor_class, ativo, inicio_vigencia, fim_vigencia);

create index if not exists idx_prof_access_grant_gov_anchor
    on tb_professional_access_grant (tribunal, unidade_judiciaria_codigo, orgao_colegiado_codigo, ente_code);

create index if not exists idx_prof_access_grant_requested_approved
    on tb_professional_access_grant (requested_by_user_id, approved_by_user_id, revoked_by_user_id);
