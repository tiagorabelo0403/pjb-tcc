package com.tcc.pjb.backend.core.comunicacao.judicial.hsm;

public sealed interface ViaInterceptacao permits
        ViaInterceptacao.GovBrAutenticado,
        ViaInterceptacao.MalhaFinanceiraBacen,
        ViaInterceptacao.SefazNfeEmissor,
        ViaInterceptacao.ReceitaFederalCnpjCpf,
        ViaInterceptacao.AnatelOperadora,
        ViaInterceptacao.DetranRegistroVeiculo,
        ViaInterceptacao.Serpro,
        ViaInterceptacao.OabSistemaJudicial,
        ViaInterceptacao.CooperacaoCnjMalha,
        ViaInterceptacao.PortalGovBrEmpresa,
        ViaInterceptacao.CartorioRegistroCivil,
        ViaInterceptacao.GovBrPush,
        ViaInterceptacao.WhatsappGov,
        ViaInterceptacao.SmsAutenticado,
        ViaInterceptacao.PortalCnpj,
        ViaInterceptacao.EmailCertificado {

    String identificadorCanal();

    int prioridadeOrdem();

    record GovBrAutenticado(
            String tokenSessaoGov,
            String ipOrigem,
            String cpfAlvo,
            String nivelConta
    ) implements ViaInterceptacao {
        @Override
        public String identificadorCanal() {
            return "GOV_BR_PUSH:" + (ipOrigem != null ? ipOrigem : "S/D");
        }

        @Override
        public int prioridadeOrdem() {
            return 1;
        }
    }

    record MalhaFinanceiraBacen(
            String chaveDictCentral,
            String ispbBanco,
            String documentoAlvo,
            boolean chevePixAtiva
    ) implements ViaInterceptacao {
        @Override
        public String identificadorCanal() {
            return "BACEN_DICT:" + (ispbBanco != null ? ispbBanco : "S/ISPB");
        }

        @Override
        public int prioridadeOrdem() {
            return 2;
        }
    }

    record SefazNfeEmissor(
            String cnpj,
            String uf,
            String emailOperacionalSnapshot,
            String telefoneOperacionalSnapshot,
            String enderecoEstabelecimentoSnapshot,
            boolean emissorNfeAtivo
    ) implements ViaInterceptacao {
        @Override
        public String identificadorCanal() {
            return "SEFAZ_NFE:" + (uf != null ? uf : "NA") + ":" + (cnpj != null && cnpj.length() >= 4 ? cnpj.substring(cnpj.length() - 4) : "DOC");
        }

        @Override
        public int prioridadeOrdem() {
            return 3;
        }
    }

    record ReceitaFederalCnpjCpf(
            String documento,
            boolean isPessoaJuridica,
            String emailReceita,
            String enderecoFiscal,
            boolean cnpjAtivo
    ) implements ViaInterceptacao {
        @Override
        public String identificadorCanal() {
            return "RF_CADASTRO:" + (isPessoaJuridica ? "CNPJ" : "CPF");
        }

        @Override
        public int prioridadeOrdem() {
            return 4;
        }
    }

    record AnatelOperadora(
            String documento,
            String codigoOperadora,
            String numeroVinculado,
            boolean numeroAtivo
    ) implements ViaInterceptacao {
        @Override
        public String identificadorCanal() {
            return "ANATEL:" + (codigoOperadora != null ? codigoOperadora : "NA");
        }

        @Override
        public int prioridadeOrdem() {
            return 5;
        }
    }

    record DetranRegistroVeiculo(
            String documento,
            String uf,
            String enderecoRegistro,
            String renavam
    ) implements ViaInterceptacao {
        @Override
        public String identificadorCanal() {
            return "DETRAN:" + (uf != null ? uf : "NA");
        }

        @Override
        public int prioridadeOrdem() {
            return 6;
        }
    }

    record Serpro(
            String documento,
            String sistemaOrigem,
            String dadosVinculo,
            String enderecoAtualizado
    ) implements ViaInterceptacao {
        @Override
        public String identificadorCanal() {
            return "SERPRO:" + (sistemaOrigem != null ? sistemaOrigem : "NA");
        }

        @Override
        public int prioridadeOrdem() {
            return 7;
        }
    }

    record OabSistemaJudicial(
            String cpf,
            String oabNumero,
            String uf,
            String sistemaPrincipal,
            String emailInstitucional,
            boolean mniAtivo
    ) implements ViaInterceptacao {
        @Override
        public String identificadorCanal() {
            return "OAB_MNI:" + (sistemaPrincipal != null ? sistemaPrincipal : "PJB") + ":" + (uf != null ? uf : "NA");
        }

        @Override
        public int prioridadeOrdem() {
            return 8;
        }
    }

    record CooperacaoCnjMalha(
            String codigoTribunalDestino,
            String comarcaDestino,
            String uf,
            String emailCartaDestino
    ) implements ViaInterceptacao {
        @Override
        public String identificadorCanal() {
            return "CNJ_COOPERACAO:" + (codigoTribunalDestino != null ? codigoTribunalDestino : "NA") + ":" + (uf != null ? uf : "NA");
        }

        @Override
        public int prioridadeOrdem() {
            return 9;
        }
    }

    record PortalGovBrEmpresa(
            String cnpj,
            String razaoSocial,
            boolean portalAtivo,
            String emailPortal
    ) implements ViaInterceptacao {
        @Override
        public String identificadorCanal() {
            return "PORTAL_GOV_BR_EMPRESA:CNPJ";
        }

        @Override
        public int prioridadeOrdem() {
            return 11;
        }
    }

    record CartorioRegistroCivil(
            String cpf,
            String nomeCompleto,
            String municipioNascimento,
            String ufNascimento
    ) implements ViaInterceptacao {
        @Override
        public String identificadorCanal() {
            return "CRC_NACIONAL:" + (municipioNascimento != null ? municipioNascimento : "NA") + "/" + (ufNascimento != null ? ufNascimento : "NA");
        }

        @Override
        public int prioridadeOrdem() {
            return 11;
        }
    }


    record GovBrPush(String tokenSessaoGov, String ipOrigem, String cpfAlvo, String nivelConta) implements ViaInterceptacao {
        @Override public String identificadorCanal() { return "GOV_BR_PUSH:" + (ipOrigem != null ? ipOrigem : "S/D"); }
        @Override public int prioridadeOrdem() { return 1; }
    }

    record WhatsappGov(String documento, String numero, boolean ativo) implements ViaInterceptacao {
        @Override public String identificadorCanal() { return "WHATSAPP_GOV:" + (numero != null ? numero : "NA"); }
        @Override public int prioridadeOrdem() { return 6; }
    }

    record SmsAutenticado(String documento, String numero, boolean ativo) implements ViaInterceptacao {
        @Override public String identificadorCanal() { return "SMS_AUTENTICADO:" + (numero != null ? numero : "NA"); }
        @Override public int prioridadeOrdem() { return 7; }
    }

    record PortalCnpj(String cnpj, String razaoSocial, boolean portalAtivo, String emailPortal) implements ViaInterceptacao {
        @Override public String identificadorCanal() { return "PORTAL_GOV_BR_EMPRESA:CNPJ"; }
        @Override public int prioridadeOrdem() { return 11; }
    }

    record EmailCertificado(String documento, String email, boolean ativo) implements ViaInterceptacao {
        @Override public String identificadorCanal() { return "EMAIL_CERTIFICADO:" + (email != null ? email : "NA"); }
        @Override public int prioridadeOrdem() { return 4; }
    }

}
