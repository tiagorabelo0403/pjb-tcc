package com.tcc.pjb.backend.core.comunicacao.judicial;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import com.tcc.pjb.backend.model.entity.enums.RamoDireito;

public enum TipoComunicacaoJudicial {
    CITACAO_INICIAL(
            "Citação inicial",
            "CPC art. 238-259 / CPP art. 351-372 / CLT art. 841",
            NaturezaAto.CITACAO,
            Set.of(
                    RamoDireito.CIVIL,
                    RamoDireito.CONSUMIDOR,
                    RamoDireito.FAMILIA,
                    RamoDireito.TRIBUTARIO,
                    RamoDireito.PREVIDENCIARIO,
                    RamoDireito.AMBIENTAL,
                    RamoDireito.EMPRESARIAL
            ),
            false,
            true,
            15,
            false
    ),
    CITACAO_PESSOAL_REU(
            "Citação pessoal do réu",
            "CPP art. 351-352 / ECA art. 184 / LCP / LIA",
            NaturezaAto.CITACAO,
            Set.of(
                    RamoDireito.PENAL,
                    RamoDireito.INFANCIA_JUVENTUDE,
                    RamoDireito.ELEITORAL,
                    RamoDireito.MILITAR
            ),
            true,
            false,
            0,
            true
    ),
    CITACAO_PESSOAL_EXECUTADO(
            "Citação pessoal do executado",
            "CPC art. 829 / CTN art. 174 / LEF art. 8º",
            NaturezaAto.CITACAO,
            Set.of(RamoDireito.TRIBUTARIO, RamoDireito.CIVIL, RamoDireito.EMPRESARIAL),
            true,
            false,
            3,
            false
    ),
    INTIMACAO_ADVOGADO(
            "Intimação de advogado",
            "CPC art. 272 / Lei 11.419/2006 art. 5º / NCPC art. 1.050",
            NaturezaAto.INTIMACAO,
            todosRamos(),
            false,
            true,
            3,
            false
    ),
    INTIMACAO_PESSOAL_DEFENSOR(
            "Intimação pessoal de defensor público / MP",
            "LC 80/1994 art. 44, IV / LONMP art. 41, IV / CPC art. 183, 186",
            NaturezaAto.INTIMACAO,
            todosRamos(),
            true,
            false,
            0,
            false
    ),
    INTIMACAO_PESSOAL_REU(
            "Intimação pessoal do réu / parte sem advogado",
            "CPC art. 273 / CPP art. 370 / CLT art. 841",
            NaturezaAto.INTIMACAO,
            todosRamos(),
            true,
            false,
            0,
            true
    ),
    INTIMACAO_PUBLICA_DJE(
            "Intimação por Diário de Justiça Eletrônico",
            "Lei 11.419/2006 art. 4º / CPC art. 272 §1",
            NaturezaAto.INTIMACAO,
            todosRamos(),
            false,
            true,
            5,
            false
    ),
    INTIMACAO_EMPRESA_CNPJ(
            "Intimação de pessoa jurídica via CNPJ/Gov.br Empresas",
            "CPC art. 246 §1-2 / Res. CNJ 455/2022 / Decreto 10.278/2020",
            NaturezaAto.INTIMACAO,
            todosRamos(),
            false,
            true,
            3,
            false
    ),
    INTIMACAO_DIGITAL_MNI(
            "Intimação via Modelo Nacional de Interoperabilidade",
            "Res. CNJ 335/2020 / MNI 3.0",
            NaturezaAto.INTIMACAO,
            todosRamos(),
            false,
            true,
            3,
            false
    ),
    NOTIFICACAO_JUDICIAL(
            "Notificação judicial",
            "CPC art. 726-729 / NCPC art. 726",
            NaturezaAto.NOTIFICACAO,
            todosRamos(),
            false,
            true,
            5,
            false
    ),
    MANDADO_PENHORA_INTIMACAO(
            "Mandado de penhora com intimação",
            "CPC art. 829-854",
            NaturezaAto.MANDADO,
            Set.of(
                    RamoDireito.CIVIL,
                    RamoDireito.TRIBUTARIO,
                    RamoDireito.EMPRESARIAL,
                    RamoDireito.TRABALHISTA,
                    RamoDireito.PREVIDENCIARIO
            ),
            true,
            false,
            3,
            false
    ),
    MANDADO_ARRESTO_SEQUESTRO(
            "Mandado de arresto/sequestro com intimação",
            "CPC art. 830 / 301",
            NaturezaAto.MANDADO,
            Set.of(RamoDireito.CIVIL, RamoDireito.TRIBUTARIO),
            true,
            false,
            0,
            false
    ),
    CARTA_PRECATORIA_EXPEDIDA(
            "Carta precatória expedida",
            "CPC art. 260-268 / CPP art. 353-360",
            NaturezaAto.CARTA_PRECATORIA,
            todosRamos(),
            false,
            true,
            30,
            false
    ),
    CARTA_ROGATORIA_EXPEDIDA(
            "Carta rogatória expedida para o exterior",
            "CPC art. 960-965 / LINDB / Tratados bilaterais",
            NaturezaAto.CARTA_ROGATORIA,
            todosRamos(),
            false,
            true,
            90,
            false
    ),
    EDITAL_CITACAO(
            "Citação por edital (réu em lugar incerto)",
            "CPC art. 256-259 / CPP art. 361-363",
            NaturezaAto.EDITAL,
            todosRamos(),
            false,
            true,
            20,
            false
    ),
    EDITAL_INTIMACAO(
            "Intimação por edital",
            "CPC art. 256 II / CPP art. 370 §1",
            NaturezaAto.EDITAL,
            todosRamos(),
            false,
            true,
            15,
            false
    ),
    INTIMACAO_TRABALHISTA_VIA_POSTAL(
            "Intimação trabalhista via postal com AR",
            "CLT art. 841 §1 / RITST art. 36",
            NaturezaAto.INTIMACAO,
            Set.of(RamoDireito.TRABALHISTA),
            false,
            true,
            5,
            false
    ),
    INTIMACAO_ECA_RESPONSAVEL(
            "Intimação de responsável por adolescente infrator",
            "ECA art. 184-186 / Art. 226 CF",
            NaturezaAto.INTIMACAO,
            Set.of(RamoDireito.INFANCIA_JUVENTUDE),
            true,
            false,
            0,
            true
    ),
    CITACAO_ELEITORAL(
            "Citação em ação eleitoral",
            "CE art. 22 / RIJE / Res. TSE 23.611",
            NaturezaAto.CITACAO,
            Set.of(RamoDireito.ELEITORAL),
            true,
            false,
            0,
            false
    ),
    INTIMACAO_FAZENDA_PUBLICA(
            "Intimação de Fazenda Pública",
            "CPC art. 183 / LC 73/1993 / CPC art. 272",
            NaturezaAto.INTIMACAO,
            todosRamos(),
            true,
            false,
            0,
            false
    ),
    INTIMACAO_PREVIDENCIARIA(
            "Intimação em matéria previdenciária",
            "Lei 9.099/1995 / IN INSS / CPC subsidiário",
            NaturezaAto.INTIMACAO,
            Set.of(RamoDireito.PREVIDENCIARIO),
            false,
            true,
            5,
            false
    ),
    VISTA_MP_FISCAL_ORDEM_JURIDICA(
            "Vista ao Ministério Público como fiscal da ordem jurídica",
            "CPC art. 178-179 / LONMP art. 25 / Lei 11.419/2006 art. 5º",
            NaturezaAto.INTIMACAO,
            todosRamos(),
            true,
            true,
            0,
            false
    ),
    INTIMACAO_INSTITUCIONAL_DEFENSORIA(
            "Intimação institucional da Defensoria Pública",
            "LC 80/1994 art. 44 §1º / CPC art. 186 / Lei 11.419/2006 art. 5º",
            NaturezaAto.INTIMACAO,
            todosRamos(),
            true,
            true,
            0,
            false
    ),
    COMUNICACAO_CONSELHO_TUTELAR(
            "Comunicação ao Conselho Tutelar",
            "ECA art. 13, 56, 87 e 136",
            NaturezaAto.NOTIFICACAO,
            Set.of(RamoDireito.FAMILIA, RamoDireito.INFANCIA_JUVENTUDE),
            false,
            true,
            3,
            false
    ),
    ENCAMINHAMENTO_CEJUSC(
            "Encaminhamento institucional ao CEJUSC",
            "Res. CNJ 125/2010 / CPC art. 334",
            NaturezaAto.NOTIFICACAO,
            Set.of(RamoDireito.CIVIL, RamoDireito.FAMILIA, RamoDireito.CONSUMIDOR),
            false,
            true,
            3,
            false
    ),
    REQUISICAO_ORGAO_TECNICO(
            "Requisição a órgão técnico ou auxiliar da justiça",
            "CPC art. 460-480 / CPP art. 159 / ECA art. 151",
            NaturezaAto.NOTIFICACAO,
            todosRamos(),
            false,
            true,
            3,
            false
    ),
    OFICIO_UNIDADE_PRISIONAL(
            "Ofício ou requisição à unidade prisional/polícia penal",
            "LEP art. 66 / CPP art. 360",
            NaturezaAto.NOTIFICACAO,
            Set.of(RamoDireito.PENAL, RamoDireito.MILITAR, RamoDireito.INFANCIA_JUVENTUDE),
            false,
            true,
            1,
            true
    ),
    COMUNICACAO_COOPERACAO_NACIONAL(
            "Comunicação via cooperação judicial nacional",
            "CPC art. 67-69 / Res. CNJ 350/2020",
            NaturezaAto.COOPERACAO,
            todosRamos(),
            false,
            true,
            15,
            false
    );

    public enum NaturezaAto {
        CITACAO,
        INTIMACAO,
        NOTIFICACAO,
        MANDADO,
        CARTA_PRECATORIA,
        CARTA_ROGATORIA,
        EDITAL,
        COOPERACAO
    }

    public static final TipoComunicacaoJudicial INTIMACAO_PESSOAL_MP = INTIMACAO_PESSOAL_DEFENSOR;
    public static final TipoComunicacaoJudicial INTIMACAO_PESSOAL_PORTAL = INTIMACAO_DIGITAL_MNI;

    private final String descricao;
    private final String fundamentoLegal;
    private final NaturezaAto natureza;
    private final Set<RamoDireito> ramosAplicaveis;
    private final boolean exigePessoalidade;
    private final boolean admiteDigital;
    private final int diasPresuncaoEntrega;
    private final boolean bloqueiaPresuncaoRebeldia;

    TipoComunicacaoJudicial(String descricao,
                            String fundamentoLegal,
                            NaturezaAto natureza,
                            Set<RamoDireito> ramosAplicaveis,
                            boolean exigePessoalidade,
                            boolean admiteDigital,
                            int diasPresuncaoEntrega,
                            boolean bloqueiaPresuncaoRebeldia) {
        this.descricao = Objects.requireNonNull(descricao, "descricao");
        this.fundamentoLegal = Objects.requireNonNull(fundamentoLegal, "fundamentoLegal");
        this.natureza = Objects.requireNonNull(natureza, "natureza");
        this.ramosAplicaveis = normalizeRamosAplicaveis(ramosAplicaveis);
        this.exigePessoalidade = exigePessoalidade;
        this.admiteDigital = admiteDigital;
        this.diasPresuncaoEntrega = diasPresuncaoEntrega;
        this.bloqueiaPresuncaoRebeldia = bloqueiaPresuncaoRebeldia;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getFundamentoLegal() {
        return fundamentoLegal;
    }

    public NaturezaAto getNatureza() {
        return natureza;
    }

    public Set<RamoDireito> getRamosAplicaveis() {
        return ramosAplicaveis;
    }

    public boolean isExigePessoalidade() {
        return exigePessoalidade;
    }

    public boolean isAdmiteDigital() {
        return admiteDigital;
    }

    public int getDiasPresuncaoEntrega() {
        return diasPresuncaoEntrega;
    }

    public boolean isBloqueiaPresuncaoRebeldia() {
        return bloqueiaPresuncaoRebeldia;
    }

    public boolean isCitacao() {
        return natureza == NaturezaAto.CITACAO;
    }

    public boolean isIntimacao() {
        return natureza == NaturezaAto.INTIMACAO;
    }

    public boolean isMandado() {
        return natureza == NaturezaAto.MANDADO;
    }

    public boolean isEdital() {
        return natureza == NaturezaAto.EDITAL;
    }

    public boolean isUrgentissimo() {
        return bloqueiaPresuncaoRebeldia && exigePessoalidade;
    }

    public boolean admitePresuncaoEntrega() {
        return diasPresuncaoEntrega > 0;
    }

    public boolean isAplicavelAo(RamoDireito ramo) {
        return ramo != null && ramosAplicaveis.contains(ramo);
    }

    private static Set<RamoDireito> normalizeRamosAplicaveis(Set<RamoDireito> ramosAplicaveis) {
        if (ramosAplicaveis == null || ramosAplicaveis.isEmpty()) {
            throw new IllegalArgumentException("ramosAplicaveis deve conter ao menos um ramo");
        }
        EnumSet<RamoDireito> normalized = EnumSet.noneOf(RamoDireito.class);
        for (RamoDireito ramo : ramosAplicaveis) {
            normalized.add(Objects.requireNonNull(ramo, "ramoDireito"));
        }
        return Collections.unmodifiableSet(normalized);
    }

    private static Set<RamoDireito> todosRamos() {
        return Collections.unmodifiableSet(EnumSet.allOf(RamoDireito.class));
    }
}
