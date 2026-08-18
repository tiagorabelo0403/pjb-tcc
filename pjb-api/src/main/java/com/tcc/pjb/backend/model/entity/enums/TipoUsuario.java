package com.tcc.pjb.backend.model.entity.enums;

import com.tcc.pjb.backend.core.util.EnumText;

public enum TipoUsuario {
    CIDADAO,

    ADVOGADO,
    OAB_PRESIDENTE_SECCIONAL,

    MAGISTRADO,
    JUIZ,
    JUIZ_ESTADUAL,
    JUIZ_FEDERAL,
    JUIZ_ESPECIAL,
    JUIZ_ELEITORAL,
    JUIZ_TRABALHISTA,
    JUIZ_MILITAR,
    DESEMBARGADOR,
    DESEMBARGADOR_FEDERAL,
    MINISTRO,

    ASSESSOR_JUDICIAL,
    ASSESSOR_DESEMBARGADOR,
    ASSESSOR_MINISTRO,

    MEMBRO_MINISTERIO_PUBLICO,
    PROMOTOR_ELEITORAL,
    PROMOTOR_TRABALHISTA,
    PROCURADOR_GERAL_REPUBLICA,

    DEFENSOR_PUBLICO,
    DEFENSOR_PUBLICO_FEDERAL,

    PROCURADOR,
    PROCURADORIA_MUNICIPAL,
    PROCURADORIA_ESTADUAL,
    PROCURADORIA_FEDERAL,

    DELEGADO_POLICIA,
    DELEGADO_POLICIA_FEDERAL,
    AGENTE_POLICIAL,
    ESCRIVAO_POLICIAL,

    OFICIAL_JUSTICA,
    OFICIAL_JUSTICA_AVALIADOR,

    PERITO,
    PERITO_CRIMINAL,
    PERITO_AMBIENTAL,
    PERITO_CONTABIL,
    PERITO_ENGENHARIA,
    PERITO_DIGITAL,
    PERITO_INSS,
    PERITO_MEDICO,
    PSICOLOGO_JUDICIAL,
    ASSISTENTE_SOCIAL_JUDICIAL,
    ASSISTENTE_TECNICO,

    CONCILIADOR_CEJUSC,
    MEDIADOR,
    ARBITRO,

    CONTADOR_JUDICIAL,
    ADMINISTRADOR_JUDICIAL,
    LEILOEIRO_JUDICIAL,
    CURADOR_ESPECIAL,
    CURADOR_AUSENTES,
    INVENTARIANTE,

    TABELIAO,
    REGISTRADOR_IMOVEIS,
    ESCREVENTE_CARTORIO,

    MEDICO,
    HOSPITAL,
    UPA,
    CLINICA,

    SERVIDOR,
    SERVIDOR_FORUM,
    ADMINISTRADOR,
    SUPORTE_TECNICO,

    PROFESSOR,
    JURISTA;

    public static final TipoUsuario SERVIDOR_JUDICIARIO = SERVIDOR_FORUM;
    public static final TipoUsuario DEFENSOR = DEFENSOR_PUBLICO;
    public static final TipoUsuario PROMOTOR = MEMBRO_MINISTERIO_PUBLICO;

    public boolean isMagistratura() {
        return switch (this) {
            case MAGISTRADO,
                    JUIZ,
                    JUIZ_ESTADUAL,
                    JUIZ_FEDERAL,
                    JUIZ_ESPECIAL,
                    JUIZ_ELEITORAL,
                    JUIZ_TRABALHISTA,
                    JUIZ_MILITAR,
                    DESEMBARGADOR,
                    DESEMBARGADOR_FEDERAL,
                    MINISTRO -> true;
            default -> false;
        };
    }

    public boolean isMinisterioPublico() {
        return switch (this) {
            case MEMBRO_MINISTERIO_PUBLICO,
                    PROMOTOR_ELEITORAL,
                    PROMOTOR_TRABALHISTA,
                    PROCURADOR_GERAL_REPUBLICA -> true;
            default -> false;
        };
    }

    public boolean isDefensoriaPublica() {
        return this == DEFENSOR_PUBLICO || this == DEFENSOR_PUBLICO_FEDERAL;
    }

    public boolean requiresHardwareAuthAssurance() {
        return isMagistratura() || isMinisterioPublico() || isDefensoriaPublica();
    }

    public boolean isProcuradoria() {
        return switch (this) {
            case PROCURADOR,
                    PROCURADORIA_MUNICIPAL,
                    PROCURADORIA_ESTADUAL,
                    PROCURADORIA_FEDERAL -> true;
            default -> false;
        };
    }

    public boolean isAdvocacia() {
        return this == ADVOGADO || this == OAB_PRESIDENTE_SECCIONAL;
    }

    public boolean isPerito() {
        return switch (this) {
            case PERITO,
                    PERITO_CRIMINAL,
                    PERITO_AMBIENTAL,
                    PERITO_CONTABIL,
                    PERITO_ENGENHARIA,
                    PERITO_DIGITAL,
                    PERITO_INSS,
                    PERITO_MEDICO,
                    PSICOLOGO_JUDICIAL,
                    ASSISTENTE_SOCIAL_JUDICIAL,
                    ASSISTENTE_TECNICO -> true;
            default -> false;
        };
    }

    public boolean isAuxiliarJustica() {
        return isPerito()
                || this == OFICIAL_JUSTICA
                || this == OFICIAL_JUSTICA_AVALIADOR
                || this == CONTADOR_JUDICIAL
                || this == ADMINISTRADOR_JUDICIAL
                || this == LEILOEIRO_JUDICIAL
                || this == CURADOR_ESPECIAL
                || this == CURADOR_AUSENTES
                || this == INVENTARIANTE
                || isCartorioExtrajudicial();
    }

    public boolean isSegurancaPublica() {
        return this == DELEGADO_POLICIA
                || this == DELEGADO_POLICIA_FEDERAL
                || this == AGENTE_POLICIAL
                || this == ESCRIVAO_POLICIAL;
    }

    public boolean isAssessor() {
        return this == ASSESSOR_JUDICIAL
                || this == ASSESSOR_DESEMBARGADOR
                || this == ASSESSOR_MINISTRO;
    }

    public boolean isConciliacaoMediacao() {
        return this == CONCILIADOR_CEJUSC
                || this == MEDIADOR
                || this == ARBITRO;
    }

    public boolean isAdministradorSistema() {
        return this == ADMINISTRADOR;
    }

    public boolean isAdmin() {
        return isAdministradorSistema();
    }

    public boolean isSaude() {
        return this == MEDICO
                || this == HOSPITAL
                || this == UPA
                || this == CLINICA
                || this == PERITO_INSS
                || this == PERITO_MEDICO;
    }

    public boolean isServidorJudiciario() {
        return this == SERVIDOR || this == SERVIDOR_FORUM;
    }

    public boolean isAcademico() {
        return this == PROFESSOR || this == JURISTA;
    }

    public boolean isCartorioExtrajudicial() {
        return this == TABELIAO || this == REGISTRADOR_IMOVEIS || this == ESCREVENTE_CARTORIO;
    }

    public boolean isDelegadoOuAgente() {
        return this == DELEGADO_POLICIA || this == DELEGADO_POLICIA_FEDERAL || this == AGENTE_POLICIAL;
    }

    public boolean isPerfilCritico() {
        return isMagistratura()
                || isAssessor()
                || this == DELEGADO_POLICIA_FEDERAL
                || this == MINISTRO
                || this == PROCURADOR_GERAL_REPUBLICA;
    }

    public static TipoUsuario fromString(String raw) {
        return fromPerfil(raw);
    }

    public String papelArquitetural() {
        if (isMagistratura()) return "MAGISTRATURA";
        if (isAssessor()) return "ASSESSORIA";
        if (isAdvocacia()) return "ADVOCACIA";
        if (isMinisterioPublico()) return "MINISTERIO_PUBLICO";
        if (isDefensoriaPublica()) return "DEFENSORIA";
        if (isProcuradoria()) return "PROCURADORIA";
        if (isAuxiliarJustica()) return "AUXILIAR";
        if (isServidorJudiciario()) return "SERVIDOR";
        if (isAdministradorSistema()) return "ADMIN";
        return "CIDADAO";
    }

    public boolean isInstitucional() {
        return !isCidadaniaExterna();
    }

    public boolean isCidadaniaExterna() {
        return this == CIDADAO || this == PROFESSOR || this == JURISTA;
    }

    public boolean isPeticionantePessoal() {
        return this == CIDADAO;
    }

    public static TipoUsuario fromPerfil(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String v = EnumText.normalizeToken(raw);
        if (v.isBlank()) {
            return null;
        }
        v = switch (v) {
            case "MAGISTRADO" -> "MAGISTRADO";
            case "JUIZ_DE_DIREITO", "JUIZADO", "JUIZA", "JUIZ" -> "JUIZ";
            case "JUIZ_ESTADUAL", "JUIZ_DE_DIREITO_ESTADUAL" -> "JUIZ_ESTADUAL";
            case "JUIZ_FEDERAL", "JUIZ_DE_DIREITO_FEDERAL" -> "JUIZ_FEDERAL";
            case "JUIZ_ESPECIAL", "JUIZADO_ESPECIAL" -> "JUIZ_ESPECIAL";
            case "JUIZ_ELEITORAL" -> "JUIZ_ELEITORAL";
            case "JUIZ_TRABALHISTA", "JUIZ_DO_TRABALHO" -> "JUIZ_TRABALHISTA";
            case "JUIZ_MILITAR", "JUIZ_AUDITOR" -> "JUIZ_MILITAR";
            case "DESEMBARGADOR", "DESEMBARGADORA" -> "DESEMBARGADOR";
            case "DESEMBARGADOR_FEDERAL" -> "DESEMBARGADOR_FEDERAL";
            case "MINISTRO", "MINISTRA" -> "MINISTRO";
            case "ASSESSOR_JUDICIAL", "ASSESSOR" -> "ASSESSOR_JUDICIAL";
            case "ASSESSOR_DESEMBARGADOR" -> "ASSESSOR_DESEMBARGADOR";
            case "ASSESSOR_MINISTRO" -> "ASSESSOR_MINISTRO";
            case "PGM", "PROCURADORIA_MUNICIPAL", "PROCURADOR_MUNICIPAL", "PROCURADORA_MUNICIPAL" -> "PROCURADORIA_MUNICIPAL";
            case "PGE", "PROCURADORIA_ESTADUAL", "PROCURADOR_ESTADUAL", "PROCURADORA_ESTADUAL" -> "PROCURADORIA_ESTADUAL";
            case "AGU", "PGF", "PROCURADORIA_FEDERAL", "PROCURADOR_FEDERAL", "PROCURADORA_FEDERAL" -> "PROCURADORIA_FEDERAL";
            case "PROCURADOR" -> "PROCURADOR";
            case "PGR", "PROCURADOR_GERAL_REPUBLICA" -> "PROCURADOR_GERAL_REPUBLICA";
            case "PROMOTOR", "PROMOTORA", "MEMBRO_MP", "MEMBRO_MINISTERIO_PUBLICO", "MINISTERIO_PUBLICO", "MP" -> "MEMBRO_MINISTERIO_PUBLICO";
            case "PROMOTOR_ELEITORAL" -> "PROMOTOR_ELEITORAL";
            case "PROMOTOR_TRABALHISTA" -> "PROMOTOR_TRABALHISTA";
            case "DEFENSOR", "DEFENSORA", "DEFENSOR_PUBLICO", "DEFENSORIA", "DEFENSORIA_PUBLICA" -> "DEFENSOR_PUBLICO";
            case "DEFENSOR_PUBLICO_FEDERAL", "DEFENSOR_FEDERAL" -> "DEFENSOR_PUBLICO_FEDERAL";
            case "ADV", "ADVOGADO", "ADVOGADA" -> "ADVOGADO";
            case "CIDADAO", "USUARIO", "CITIZEN" -> "CIDADAO";
            case "DELEGADO", "DELEGADA", "DELEGADO_POLICIA", "DELEGADO_DE_POLICIA" -> "DELEGADO_POLICIA";
            case "DELEGADO_FEDERAL", "DELEGADO_POLICIA_FEDERAL", "DELEGADO_PF" -> "DELEGADO_POLICIA_FEDERAL";
            case "POLICIAL", "AGENTE", "AGENTE_POLICIAL", "OFICIAL_POLICIA" -> "AGENTE_POLICIAL";
            case "ESCRIVAO", "ESCRIVAO_POLICIAL" -> "ESCRIVAO_POLICIAL";
            case "OFICIAL_DE_JUSTICA", "OFICIAL_JUSTICA", "OFICIAL" -> "OFICIAL_JUSTICA";
            case "OFICIAL_AVALIADOR", "OFICIAL_JUSTICA_AVALIADOR" -> "OFICIAL_JUSTICA_AVALIADOR";
            case "SECRETARIA", "SERVIDOR_FORUM" -> "SERVIDOR_FORUM";
            case "OAB", "PRESIDENTE_OAB", "PRESIDENTE_CONSELHO_SECCIONAL", "OAB_PRESIDENTE_SECCIONAL" -> "OAB_PRESIDENTE_SECCIONAL";
            case "PERITO", "PERICIA" -> "PERITO";
            case "PERITO_CRIMINAL" -> "PERITO_CRIMINAL";
            case "PERITO_AMBIENTAL" -> "PERITO_AMBIENTAL";
            case "PERITO_CONTABIL" -> "PERITO_CONTABIL";
            case "PERITO_ENGENHARIA" -> "PERITO_ENGENHARIA";
            case "PERITO_DIGITAL" -> "PERITO_DIGITAL";
            case "PERITO_MEDICO" -> "PERITO_MEDICO";
            case "PERITO_INSS" -> "PERITO_INSS";
            case "ASSISTENTE", "ASSISTENTE_TECNICO", "ASSISTENTE_TECNICA" -> "ASSISTENTE_TECNICO";
            case "PSICOLOGO", "PSICOLOGO_JUDICIAL" -> "PSICOLOGO_JUDICIAL";
            case "ASSISTENTE_SOCIAL", "ASSISTENTE_SOCIAL_JUDICIAL" -> "ASSISTENTE_SOCIAL_JUDICIAL";
            case "CONCILIADOR", "CONCILIADOR_CEJUSC" -> "CONCILIADOR_CEJUSC";
            case "MEDIADOR" -> "MEDIADOR";
            case "ARBITRO" -> "ARBITRO";
            case "CONTADOR_JUDICIAL" -> "CONTADOR_JUDICIAL";
            case "ADMINISTRADOR_JUDICIAL" -> "ADMINISTRADOR_JUDICIAL";
            case "LEILOEIRO", "LEILOEIRO_JUDICIAL" -> "LEILOEIRO_JUDICIAL";
            case "CURADOR_ESPECIAL" -> "CURADOR_ESPECIAL";
            case "CURADOR_DE_AUSENTES", "CURADOR_AUSENTES" -> "CURADOR_AUSENTES";
            case "INVENTARIANTE" -> "INVENTARIANTE";
            case "TABELIAO", "TABELIA" -> "TABELIAO";
            case "REGISTRADOR_IMOVEIS", "REGISTRADOR_DE_IMOVEIS" -> "REGISTRADOR_IMOVEIS";
            case "ESCREVENTE", "ESCREVENTE_CARTORIO" -> "ESCREVENTE_CARTORIO";
            case "SERVIDOR", "SERVIDOR_JUDICIARIO" -> "SERVIDOR";
            case "ADMIN", "ADMINISTRADOR", "ADMINISTRADOR_SISTEMA" -> "ADMINISTRADOR";
            case "PROF", "PROFESSOR", "PROFESSORA" -> "PROFESSOR";
            case "JURISTA" -> "JURISTA";
            default -> v;
        };
        try {
            return TipoUsuario.valueOf(v);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
