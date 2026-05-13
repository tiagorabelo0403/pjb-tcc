package com.tcc.pjb.backend.core.comunicacao.judicial;

public enum ModalidadeExpedicaoJudicial {
    DIGITAL_GOVBR_PUSH(
            1,
            "Digital Gov.br Push Autenticado",
            "Entrega direta na conta Gov.br do destinatário via push autenticado. Presunção de entrega em 24h.",
            true,
            false,
            false,
            24
    ),
    DIGITAL_DOMICILIO_ELETRONICO_MNI(
            2,
            "Domicílio Judicial Eletrônico / MNI 3.0",
            "Envio via MNI para sistema judicial de origem. Cobre advogados cadastrados no PJe/ESAJ/eProc/Projudi.",
            true,
            false,
            false,
            72
    ),
    DIGITAL_SEFAZ_NF_EMAIL(
            3,
            "E-mail operacional NF-e / SEFAZ",
            "Envio para e-mail operacional identificado em cadastro fiscal estadual de emissão NF-e, com trilha ICP-Brasil e enriquecimento de endereço para fallback físico.",
            true,
            false,
            false,
            96
    ),
    DIGITAL_EMAIL_CERTIFICADO_ICP(
            4,
            "E-mail com certificação ICP-Brasil",
            "Envio para endereço de e-mail com hash SHA-256 e carimbo de tempo ICP-Brasil.",
            true,
            false,
            false,
            120
    ),
    PORTAL_EMPRESA_CNPJ(
            5,
            "Portal Gov.br Empresa / Receita Federal",
            "Intimação via portal de serviços do CNPJ. Válida para PJ com CNPJ ativo. CPC art. 246 §1.",
            true,
            false,
            false,
            72
    ),
    DIGITAL_WHATSAPP_GOV(
            6,
            "WhatsApp Gov.br Autenticado",
            "Canal WhatsApp vinculado à conta Gov.br com confirmação de leitura e autenticação 2FA.",
            true,
            false,
            false,
            48
    ),
    DIGITAL_SMS_AUTENTICADO(
            7,
            "SMS autenticado com código de confirmação",
            "SMS com link tokenizado de acesso único. Confirmação registrada com timestamp e IP.",
            true,
            false,
            false,
            72
    ),
    OFICIAL_JUSTICA_ROTA_OTIMIZADA(
            8,
            "Oficial de Justiça com rota GPS otimizada",
            "Mandado físico com rota otimizada por IA. Registro biométrico de entrega. GPS tracking.",
            false,
            true,
            false,
            0
    ),
    CORREIO_AR_DIGITAL(
            9,
            "Correios AR Digital com rastreamento blockchain",
            "Carta física com Aviso de Recebimento digital. Rastreamento via hash de envelope.",
            false,
            false,
            true,
            120
    ),
    CARTA_PRECATORIA_DIGITAL(
            10,
            "Carta Precatória Digital via PJBR",
            "Expedição eletrônica para comarca de destino via malha PJB. CPC art. 260-268.",
            true,
            false,
            false,
            240
    ),
    EDITAL_DOU_DJE(
            11,
            "Edital no DOU / DJE eletrônico",
            "Publicação em Diário Oficial da União ou DJE eletrônico. Prazo de leitura presumida definido pelo juízo.",
            false,
            false,
            false,
            480
    ),
    COOPERACAO_JUDICIAL_NACIONAL(
            12,
            "Cooperação Judicial Nacional",
            "Requisição ao tribunal competente da comarca de destino. Res. CNJ 350/2020.",
            true,
            false,
            false,
            480
    );

    private final int prioridade;
    private final String label;
    private final String descricao;
    private final boolean digital;
    private final boolean exigeOficial;
    private final boolean exigeCorreio;
    private final int horasPresuncaoEntrega;

    ModalidadeExpedicaoJudicial(int prioridade,
                                String label,
                                String descricao,
                                boolean digital,
                                boolean exigeOficial,
                                boolean exigeCorreio,
                                int horasPresuncaoEntrega) {
        this.prioridade = prioridade;
        this.label = label;
        this.descricao = descricao;
        this.digital = digital;
        this.exigeOficial = exigeOficial;
        this.exigeCorreio = exigeCorreio;
        this.horasPresuncaoEntrega = horasPresuncaoEntrega;
    }

    public static final ModalidadeExpedicaoJudicial GOVBR_PUSH = DIGITAL_GOVBR_PUSH;
    public static final ModalidadeExpedicaoJudicial WHATSAPP_GOV = DIGITAL_WHATSAPP_GOV;
    public static final ModalidadeExpedicaoJudicial PORTAL_CNPJ = PORTAL_EMPRESA_CNPJ;
    public static final ModalidadeExpedicaoJudicial MNI = DIGITAL_DOMICILIO_ELETRONICO_MNI;

    public int getPrioridade() {
        return prioridade;
    }

    public String getLabel() {
        return label;
    }

    public String getDescricao() {
        return descricao;
    }

    public boolean isDigital() {
        return digital;
    }

    public boolean isExigeOficial() {
        return exigeOficial;
    }

    public boolean isExigeCorreio() {
        return exigeCorreio;
    }

    public int getHorasPresuncaoEntrega() {
        return horasPresuncaoEntrega;
    }

    public boolean isFisicoOuCorreio() {
        return exigeOficial || exigeCorreio;
    }

    public boolean temPresuncaoAutomatica() {
        return horasPresuncaoEntrega > 0;
    }

    public boolean isUltimoRecurso() {
        return this == EDITAL_DOU_DJE;
    }
}
