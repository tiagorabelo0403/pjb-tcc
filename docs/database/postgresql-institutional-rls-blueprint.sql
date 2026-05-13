ALTER TABLE tb_authz_trail_read_model ENABLE ROW LEVEL SECURITY;
ALTER TABLE tb_authz_trail_read_model FORCE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS pjb_authz_trail_institutional_scope_read ON tb_authz_trail_read_model;
CREATE POLICY pjb_authz_trail_institutional_scope_read
ON tb_authz_trail_read_model
FOR SELECT
USING (
    current_setting('app.pjb_affiliation_id', true) IS NOT NULL
    AND (
        institutional_unit_code IS NULL
        OR institutional_unit_code = current_setting('app.pjb_unit_code', true)
    )
    AND (
        institutional_box_code IS NULL
        OR current_setting('app.pjb_box_code', true) IS NULL
        OR institutional_box_code = current_setting('app.pjb_box_code', true)
    )
);
