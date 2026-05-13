ALTER TABLE adv_office_process_operation
    ADD COLUMN IF NOT EXISTS signature_payload_json TEXT,
    ADD COLUMN IF NOT EXISTS signature_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS signer_name_snapshot VARCHAR(255),
    ADD COLUMN IF NOT EXISTS signer_registration_snapshot VARCHAR(120);
