package com.tcc.pjb.backend.ai.legalai.memory.domain;

import java.util.Arrays;
import java.util.List;

public enum LegalAiJudicialRiteCatalog {

    CIVEL_COMUM(
            "CIV",
            "Procedimento Cível Comum",
            "CPC/2015 art. 318",
            "Petição inicial, citação, contestação, réplica, saneamento, instrução e julgamento"),

    SUMARIO_CIVEL(
            "SUM",
            "Procedimento Sumário / Simplificado",
            "CPC/2015 art. 1.049",
            "Rito simplificado para causas de menor complexidade"),

    JUIZADO_ESPECIAL_CIVEL(
            "JEC",
            "Juizado Especial Cível",
            "Lei 9.099/95",
            "Causas até 40 salários mínimos, conciliação obrigatória, gratuidade até 20 SM"),

    JUIZADO_ESPECIAL_FEDERAL(
            "JEF",
            "Juizado Especial Federal",
            "Lei 10.259/01",
            "Causas federais até 60 salários mínimos contra União, autarquias e fundações"),

    JUIZADO_ESPECIAL_FAZENDA(
            "JEFAZ",
            "Juizado Especial da Fazenda Pública Estadual/Municipal",
            "Lei 12.153/09",
            "Causas contra entes estaduais e municipais até 60 salários mínimos"),

    NUCLEO_JUSTICA_DIGITAL(
            "NJD",
            "Núcleo de Justiça Digital / Juizado 4.0",
            "Res. CNJ 271/2018 e portarias TJCE",
            "Juizado Especial Adjunto com protocolo digital, opção facultativa na inicial"),

    CRIMINAL_COMUM(
            "CRIM",
            "Processo Criminal Comum",
            "CPP art. 394, §1º, I",
            "Crimes com pena máxima superior a 4 anos: inquérito, denúncia, resposta, instrução, alegações e sentença"),

    CRIMINAL_SUMARIO(
            "CRIMS",
            "Processo Criminal Sumário",
            "CPP art. 394, §1º, II",
            "Crimes com pena mínima inferior a 2 anos e máxima entre 2 e 4 anos"),

    CRIMINAL_SUMARÍSSIMO(
            "CRIMIN",
            "Processo Criminal Sumaríssimo (JECRIM)",
            "Lei 9.099/95 arts. 60-92",
            "Infrações de menor potencial ofensivo, pena máxima até 2 anos, composição civil e transação penal"),

    TRIBUNAL_DO_JURI(
            "JURI",
            "Tribunal do Júri",
            "CPP art. 406-497 e CF/88 art. 5º, XXXVIII",
            "Crimes dolosos contra a vida: homicídio doloso, feminicídio, infanticídio, aborto provocado"),

    HABEAS_CORPUS(
            "HC",
            "Habeas Corpus",
            "CPP art. 647-667 e CF/88 art. 5º, LXVIII",
            "Tutela da liberdade de locomoção, liminar, informações, parecer ministerial e julgamento colegiado"),

    MANDADO_SEGURANCA(
            "MS",
            "Mandado de Segurança",
            "Lei 12.016/09",
            "Direito líquido e certo, ato de autoridade, prazo decadencial 120 dias"),

    ACAO_POPULAR(
            "AP",
            "Ação Popular",
            "Lei 4.717/65 e CF/88 art. 5º, LXXIII",
            "Controle cidadão de atos lesivos ao patrimônio público"),

    ACAO_CIVIL_PUBLICA(
            "ACP",
            "Ação Civil Pública",
            "Lei 7.347/85",
            "Tutela de direitos difusos e coletivos, legitimação extraordinária, liminar"),

    TRABALHISTA(
            "TRB",
            "Processo Trabalhista",
            "CLT art. 763-910 e Lei 13.467/17",
            "Reclamação trabalhista, audiência única, tentativa de conciliação, instrução e sentença"),

    TRABALHISTA_SUMARIO(
            "TRBS",
            "Rito Sumário Trabalhista",
            "CLT art. 852-A a 852-I",
            "Causas até 2 salários mínimos, audiência única, vedação de recurso de revista"),

    ELEITORAL(
            "ELET",
            "Processo Eleitoral",
            "Código Eleitoral Lei 4.737/65 e Lei 9.504/97",
            "AIJE, AIME, RCD, RCOP: ritos específicos com prazos reduzidos e recurso próprio"),

    RECURSAL_CIVIL(
            "REC",
            "Recursal Cível (2ª instância)",
            "CPC/2015 arts. 994-1.044",
            "Apelação, agravo, embargos de declaração, recurso ordinário e extraordinário"),

    RECURSAL_CRIMINAL(
            "RECC",
            "Recursal Criminal (2ª instância)",
            "CPP arts. 574-580",
            "Apelação, recurso em sentido estrito, carta testemunhável, embargos infringentes e de nulidade"),

    EXECUCAO_CIVIL(
            "EXEC",
            "Execução Civil",
            "CPC/2015 arts. 771-921",
            "Execução de título judicial e extrajudicial, penhora, expropriação e satisfação do crédito"),

    EXECUCAO_FISCAL(
            "EF",
            "Execução Fiscal",
            "Lei 6.830/80 (LEF)",
            "Cobrança de créditos da Fazenda Pública, CDA, citação, penhora, embargos e hasta"),

    FALIMENTAR(
            "FAL",
            "Falência e Recuperação Judicial/Extrajudicial",
            "Lei 11.101/03",
            "Falência, recuperação judicial, recuperação extrajudicial, administração judicial e verificação de créditos"),

    FAMILIA(
            "FAM",
            "Direito de Família",
            "CPC/2015 arts. 693-699 e ECA",
            "Divórcio, guarda, alimentos, adoção, interdição, investigação de paternidade e tutela"),

    INVENTARIO_PARTILHA(
            "INV",
            "Inventário e Partilha",
            "CPC/2015 arts. 610-673",
            "Inventário judicial e extrajudicial, arrolamento, sobrepartilha e habilitação de credores"),

    FAZENDA_PUBLICA(
            "FAZ",
            "Procedimento de Fazenda Pública",
            "CPC/2015 arts. 183-188 e 534-535",
            "Prerrogativas processuais, prazo em quádruplo (ritos anteriores), cumprimento de sentença contra ente público"),

    PRECATORIO(
            "PREC",
            "Regime de Precatórios",
            "CF/88 art. 100 e Res. CNJ 303/2019",
            "Expedição, processamento, sequestro, fracionamento e pagamento de precatórios"),

    HABEAS_DATA(
            "HD",
            "Habeas Data",
            "Lei 9.507/97 e CF/88 art. 5º, LXXII",
            "Acesso e retificação de informações em registros públicos ou banco de dados governamentais"),

    MANDADO_INJUNCAO(
            "MI",
            "Mandado de Injunção",
            "CF/88 art. 5º, LXXI e Lei 13.300/16",
            "Tutela de direito constitucional inviabilizado por omissão legislativa"),

    ACAO_RESCISORIA(
            "AR",
            "Ação Rescisória",
            "CPC/2015 arts. 966-975",
            "Desconstituição de coisa julgada, prazo bienal, competência do tribunal que proferiu a decisão"),

    ADI_ADC_ADPF(
            "ADIP",
            "Controle Concentrado de Constitucionalidade",
            "Lei 9.868/99, Lei 9.882/99 e CF/88 art. 102",
            "ADI, ADC, ADPF: legitimidade, amicus curiae, efeito vinculante e erga omnes"),

    CAUTELAR_ANTECIPACAO(
            "CAUT",
            "Tutelas de Urgência e Evidência",
            "CPC/2015 arts. 294-311",
            "Tutela cautelar, tutela antecipada, estabilização, fungibilidade e reversibilidade"),

    CUMPRIMENTO_SENTENCA(
            "CPS",
            "Cumprimento de Sentença",
            "CPC/2015 arts. 523-538",
            "Obrigação de pagar, fazer, não fazer e entregar coisa; multa, execução de alimentos e insolvência");

    private final String codigo;
    private final String nome;
    private final String baseLegal;
    private final String contextoAprendizado;

    LegalAiJudicialRiteCatalog(String codigo, String nome, String baseLegal, String contextoAprendizado) {
        this.codigo = codigo;
        this.nome = nome;
        this.baseLegal = baseLegal;
        this.contextoAprendizado = contextoAprendizado;
    }

    public String codigo() {
        return codigo;
    }

    public String nome() {
        return nome;
    }

    public String baseLegal() {
        return baseLegal;
    }

    public String contextoAprendizado() {
        return contextoAprendizado;
    }

    public static List<LegalAiJudicialRiteCatalog> ritos() {
        return Arrays.asList(values());
    }

    public static LegalAiJudicialRiteCatalog porCodigo(String codigo) {
        return Arrays.stream(values())
                .filter(r -> r.codigo.equalsIgnoreCase(codigo))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Rito não encontrado: " + codigo));
    }
}
