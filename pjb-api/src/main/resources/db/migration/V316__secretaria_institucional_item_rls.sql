ALTER TABLE secretaria_institucional_item ENABLE ROW LEVEL SECURITY;
ALTER TABLE secretaria_institucional_item FORCE ROW LEVEL SECURITY;

CREATE POLICY secretaria_institucional_item_unidade_scope ON secretaria_institucional_item
    FOR ALL
    USING (
        current_setting('app.pjb_secretaria_unidade_id', true) IS NULL
        OR current_setting('app.pjb_secretaria_unidade_id', true) = ''
        OR unidade_institucional_id IS NULL
        OR unidade_institucional_id::text = current_setting('app.pjb_secretaria_unidade_id', true)
    )
    WITH CHECK (true);
