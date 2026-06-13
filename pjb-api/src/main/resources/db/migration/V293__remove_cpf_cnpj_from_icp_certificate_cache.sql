DROP INDEX IF EXISTS idx_icp_cert_cpf;
DROP INDEX IF EXISTS idx_icp_cert_cnpj;
ALTER TABLE pjb_icp_certificate_cache DROP COLUMN IF EXISTS cpf_titular;
ALTER TABLE pjb_icp_certificate_cache DROP COLUMN IF EXISTS cnpj_titular;
