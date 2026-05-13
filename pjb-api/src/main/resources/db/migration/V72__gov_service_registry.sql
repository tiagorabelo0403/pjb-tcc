CREATE TABLE IF NOT EXISTS tb_gov_service_registry (
  id UUID PRIMARY KEY,
  uf CHAR(2) NOT NULL,
  category VARCHAR(32) NOT NULL,
  name VARCHAR(120) NOT NULL,
  service_type VARCHAR(16) NOT NULL,
  url TEXT NOT NULL,
  requirements_json TEXT NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT true,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_govsvc_uf_cat_enabled ON tb_gov_service_registry (uf, category) WHERE enabled = true;

INSERT INTO tb_gov_service_registry (id, uf, category, name, service_type, url, requirements_json, enabled)
VALUES
  ('11111111-1111-1111-1111-111111111111', 'CE', 'BO', 'Delegacia Eletrônica (CE)', 'DEEPLINK', 'https://www.sspds.ce.gov.br/projeto/delegacia-eletronica/', '{"requiresGovBr":false,"minGovBrLevel":null,"stepUp":false,"checklist":["Documento com foto","Detalhes do fato","Endereço","Contato"]}', true),
  ('22222222-2222-2222-2222-222222222222', 'BR', 'INSS', 'Meu INSS', 'DEEPLINK', 'https://meu.inss.gov.br/', '{"requiresGovBr":true,"minGovBrLevel":"PRATA","stepUp":true,"checklist":["Conta gov.br","Dados cadastrais","Documentos do pedido"]}', true),
  ('33333333-3333-3333-3333-333333333333', 'BR', 'RECEITA', 'Portal e-CAC (Receita Federal)', 'DEEPLINK', 'https://cav.receita.fazenda.gov.br/', '{"requiresGovBr":true,"minGovBrLevel":"PRATA","stepUp":true,"checklist":["Conta gov.br","CPF","Comprovantes/declarações"]}', true),
  ('44444444-4444-4444-4444-444444444444', 'CE', 'FAZENDA', 'Portal de Serviços SEFAZ-CE', 'DEEPLINK', 'https://portalservicos.sefaz.ce.gov.br/', '{"requiresGovBr":false,"minGovBrLevel":null,"stepUp":false,"checklist":["CPF/CNPJ","Dados do serviço","Autenticação quando exigido"]}', true),
  ('55555555-5555-5555-5555-555555555555', 'CE', 'FAZENDA', 'Emissão de DAE (SEFAZ-CE)', 'DEEPLINK', 'https://servicos.sefaz.ce.gov.br/internet/dae/aplic/default.asp', '{"requiresGovBr":false,"minGovBrLevel":null,"stepUp":false,"checklist":["Identificação do contribuinte","Dados do tributo","Pagamento"]}', true),
  ('66666666-6666-6666-6666-666666666666', 'BR', 'OUTROS', 'Catálogo de Serviços (gov.br)', 'DEEPLINK', 'https://www.gov.br/pt-br/servicos', '{"requiresGovBr":false,"minGovBrLevel":null,"stepUp":false,"checklist":["Escolha o serviço","Siga o passo a passo"]}', true)
ON CONFLICT (id) DO NOTHING;
