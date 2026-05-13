CREATE TABLE IF NOT EXISTS tb_doc_provider_registry (
  id UUID PRIMARY KEY,
  uf CHAR(2) NOT NULL,
  category VARCHAR(40) NOT NULL,
  doc_key VARCHAR(80) NOT NULL,
  title VARCHAR(160) NOT NULL,
  provider_type VARCHAR(16) NOT NULL,
  url TEXT NOT NULL,
  requirements_json TEXT NOT NULL,
  enabled BOOLEAN NOT NULL DEFAULT true,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS ix_docprov_uf_cat_enabled ON tb_doc_provider_registry (uf, category) WHERE enabled = true;
CREATE INDEX IF NOT EXISTS ix_docprov_uf_dockey_enabled ON tb_doc_provider_registry (uf, doc_key) WHERE enabled = true;

INSERT INTO tb_doc_provider_registry (id, uf, category, doc_key, title, provider_type, url, requirements_json, enabled)
VALUES
  ('aaaaaaaa-0000-0000-0000-000000000001', 'BR', 'IDENTIDADE', 'CPF_CARTAO', 'Cartão de CPF (comprovante de inscrição)', 'WEB', 'https://www.gov.br/pt-br/servicos/obter-cartao-de-cpf', '{"requiresGovBr":false,"minGovBrLevel":null,"stepUp":false,"checklist":["CPF","Dados pessoais"]}', true),
  ('aaaaaaaa-0000-0000-0000-00000000000f', 'BR', 'IDENTIDADE', 'CIN', 'Carteira de Identidade Nacional (CIN)', 'WEB', 'https://www.gov.br/governodigital/pt-br/identidade/identificacao-do-cidadao-e-carteira-de-identidade-nacional', '{"requiresGovBr":false,"minGovBrLevel":null,"stepUp":false,"checklist":["Emissão pelo estado","Versão digital no gov.br"]}', true),
  ('aaaaaaaa-0000-0000-0000-000000000010', 'BR', 'IDENTIDADE', 'CIN_QR_VALIDAR', 'Verificar validade do QR Code da CIN', 'WEB', 'https://www.gov.br/pt-br/servicos/verificar-validade-de-qr-code-da-carteira-de-identidade-nacional', '{"requiresGovBr":true,"minGovBrLevel":"PRATA","stepUp":true,"checklist":["App Carteira de Identidade Nacional","Conta gov.br"]}', true),
  ('aaaaaaaa-0000-0000-0000-000000000002', 'BR', 'TRANSITO', 'CDT_CNH_CRLV', 'Carteira Digital de Trânsito (CNH e CRLV)', 'WEB', 'https://www.gov.br/pt-br/apps/carteira-digital-de-transito-1', '{"requiresGovBr":true,"minGovBrLevel":"PRATA","stepUp":true,"checklist":["Conta gov.br","CNH/CRLV vinculados"]}', true),
  ('aaaaaaaa-0000-0000-0000-000000000003', 'BR', 'TRABALHO', 'CTPS_DIGITAL', 'Carteira de Trabalho Digital (CTPS Digital)', 'WEB', 'https://www.gov.br/pt-br/temas/carteira-de-trabalho-digital', '{"requiresGovBr":true,"minGovBrLevel":"PRATA","stepUp":true,"checklist":["Conta gov.br","Dados pessoais"]}', true),
  ('aaaaaaaa-0000-0000-0000-000000000004', 'BR', 'ELEITORAL', 'E_TITULO', 'e-Título (título eleitoral digital)', 'WEB', 'https://www.tse.jus.br/eleitor/servicos/aplicativo-e-titulo', '{"requiresGovBr":false,"minGovBrLevel":null,"stepUp":false,"checklist":["Dados do eleitor","Biometria quando disponível"]}', true),
  ('aaaaaaaa-0000-0000-0000-000000000005', 'BR', 'ELEITORAL', 'CERTIDOES_ELEITORAIS', 'Certidões eleitorais (quitação, crimes eleitorais, etc.)', 'WEB', 'https://www.tse.jus.br/eleitor/certidoes/certidoes', '{"requiresGovBr":false,"minGovBrLevel":null,"stepUp":false,"checklist":["Dados do eleitor"]}', true),
  ('aaaaaaaa-0000-0000-0000-000000000006', 'BR', 'PREVIDENCIA', 'MEU_INSS', 'Meu INSS', 'WEB', 'https://meu.inss.gov.br/', '{"requiresGovBr":true,"minGovBrLevel":"PRATA","stepUp":true,"checklist":["Conta gov.br","Dados do pedido"]}', true),
  ('aaaaaaaa-0000-0000-0000-000000000007', 'BR', 'SAUDE', 'MEU_SUS_DIGITAL', 'Meu SUS Digital', 'WEB', 'https://meususdigital.saude.gov.br/', '{"requiresGovBr":true,"minGovBrLevel":"PRATA","stepUp":true,"checklist":["Conta gov.br"]}', true),
  ('aaaaaaaa-0000-0000-0000-000000000008', 'BR', 'FISCAL', 'ECAC', 'Portal e-CAC (Receita Federal)', 'WEB', 'https://www.gov.br/receitafederal/pt-br/canais_atendimento/atendimento-virtual', '{"requiresGovBr":true,"minGovBrLevel":"PRATA","stepUp":true,"checklist":["Conta gov.br","CPF","Procuração eletrônica quando necessário"]}', true),
  ('aaaaaaaa-0000-0000-0000-000000000009', 'BR', 'FISCAL', 'CND_REGULARIDADE_FISCAL', 'Certidão de regularidade fiscal (CND/Conjunta)', 'WEB', 'https://www.gov.br/pt-br/servicos/emitir-certidao-de-regularidade-fiscal', '{"requiresGovBr":false,"minGovBrLevel":null,"stepUp":false,"checklist":["CPF/CNPJ","Dados do contribuinte"]}', true),
  ('aaaaaaaa-0000-0000-0000-00000000000a', 'BR', 'SEGURANCA', 'ANTECEDENTES_PF', 'Certidão de antecedentes criminais (Polícia Federal)', 'WEB', 'https://www.gov.br/pt-br/servicos/emitir-certidao-de-antecedentes-criminais', '{"requiresGovBr":false,"minGovBrLevel":null,"stepUp":false,"checklist":["CPF","Dados pessoais"]}', true),
  ('aaaaaaaa-0000-0000-0000-00000000000b', 'BR', 'SOCIAL', 'CADUNICO', 'Cadastro Único (consulta e comprovante)', 'WEB', 'https://www.gov.br/mds/pt-br/acoes-e-programas/cadastro-unico/paginas/app-cadunico', '{"requiresGovBr":true,"minGovBrLevel":"PRATA","stepUp":true,"checklist":["Conta gov.br","Dados do cadastro"]}', true),
  ('aaaaaaaa-0000-0000-0000-00000000000c', 'BR', 'AGRARIO', 'CCIR', 'CCIR (Certificado de Cadastro de Imóvel Rural)', 'WEB', 'https://www.gov.br/pt-br/servicos/emitir-o-certificado-de-cadastro-de-imovel-rural-ccir', '{"requiresGovBr":true,"minGovBrLevel":"PRATA","stepUp":true,"checklist":["Conta gov.br","Dados do imóvel (SNCR)"]}', true),
  ('aaaaaaaa-0000-0000-0000-00000000000d', 'BR', 'AGRARIO', 'CAR_DOCUMENTOS', 'Baixar documentos do CAR (Meu Imóvel Rural)', 'WEB', 'https://www.gov.br/pt-br/servicos/baixar-documentos-do-car-cadastro-ambiental-rural-pela-plataforma-meu-imovel-rural', '{"requiresGovBr":true,"minGovBrLevel":"PRATA","stepUp":true,"checklist":["Conta gov.br","Imóvel cadastrado no CAR"]}', true),
  ('aaaaaaaa-0000-0000-0000-00000000000e', 'BR', 'CARTORIO', 'REGISTRO_CIVIL_2VIA', '2ª via de certidões (Registro Civil)', 'WEB', 'https://www.registrocivil.org.br/', '{"requiresGovBr":false,"minGovBrLevel":null,"stepUp":false,"checklist":["Pode haver custos","Dados do cartório","Dados do registro"]}', true),
  ('bbbbbbbb-0000-0000-0000-000000000001', 'CE', 'SEGURANCA', 'BO_CE', 'Boletim de Ocorrência (Delegacia Eletrônica CE)', 'WEB', 'https://www.sspds.ce.gov.br/projeto/delegacia-eletronica/', '{"requiresGovBr":false,"minGovBrLevel":null,"stepUp":false,"checklist":["Documento com foto","Detalhes do fato","Endereço","Contato"]}', true),
  ('bbbbbbbb-0000-0000-0000-000000000002', 'CE', 'SEGURANCA', 'ANTECEDENTES_CE', 'Atestado de antecedentes criminais (CE)', 'WEB', 'https://www.sspds.ce.gov.br/atestado-de-antecedentes-criminais/', '{"requiresGovBr":false,"minGovBrLevel":null,"stepUp":false,"checklist":["CPF","Dados pessoais"]}', true),
  ('bbbbbbbb-0000-0000-0000-000000000003', 'CE', 'JUSTICA', 'CERTIDOES_TJCE', 'Certidões (TJCE)', 'WEB', 'https://www.tjce.jus.br/certidoes/', '{"requiresGovBr":false,"minGovBrLevel":null,"stepUp":false,"checklist":["CPF","Dados para emissão","Pode haver prazo"]}', true),
  ('bbbbbbbb-0000-0000-0000-000000000004', 'CE', 'OUTROS', 'CEARA_DIGITAL', 'Ceará Digital (catálogo de serviços)', 'WEB', 'https://cearadigital.ce.gov.br/servicos/219', '{"requiresGovBr":false,"minGovBrLevel":null,"stepUp":false,"checklist":["Escolha o serviço","Siga o passo a passo"]}', true)
ON CONFLICT (id) DO NOTHING;
