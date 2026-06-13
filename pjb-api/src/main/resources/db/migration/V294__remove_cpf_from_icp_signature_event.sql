UPDATE pjb_icp_signature_event SET cpf_signatario = NULL;
DROP INDEX IF EXISTS idx_icp_sig_evento_cpf;
ALTER TABLE pjb_icp_signature_event DROP COLUMN IF EXISTS cpf_signatario;
