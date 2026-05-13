package com.tcc.pjb.backend.core.security.login;

import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

@Service
public final class PerfilLoginStrategyResolver {

    private static final PerfilLoginStrategy GOVBR = new PerfilLoginStrategy.GovBrOidc(new TipoUsuario[]{TipoUsuario.CIDADAO});
    private static final PerfilLoginStrategy OAB = new PerfilLoginStrategy.OabCredential(new TipoUsuario[]{TipoUsuario.ADVOGADO, TipoUsuario.OAB_PRESIDENTE_SECCIONAL});
    private static final PerfilLoginStrategy TRIBUNAL_INST = new PerfilLoginStrategy.TribunalInstitucional(new TipoUsuario[]{
            TipoUsuario.JUIZ, TipoUsuario.JUIZ_ESTADUAL, TipoUsuario.JUIZ_FEDERAL, TipoUsuario.JUIZ_ESPECIAL,
            TipoUsuario.JUIZ_ELEITORAL, TipoUsuario.JUIZ_TRABALHISTA, TipoUsuario.JUIZ_MILITAR,
            TipoUsuario.MAGISTRADO, TipoUsuario.DESEMBARGADOR, TipoUsuario.DESEMBARGADOR_FEDERAL, TipoUsuario.MINISTRO,
            TipoUsuario.ASSESSOR_JUDICIAL, TipoUsuario.ASSESSOR_DESEMBARGADOR, TipoUsuario.ASSESSOR_MINISTRO,
            TipoUsuario.SERVIDOR, TipoUsuario.SERVIDOR_FORUM, TipoUsuario.OFICIAL_JUSTICA, TipoUsuario.OFICIAL_JUSTICA_AVALIADOR,
            TipoUsuario.LEILOEIRO_JUDICIAL
    });
    private static final PerfilLoginStrategy SIAPE_MATRICULA = new PerfilLoginStrategy.SiapeComMatricula(new TipoUsuario[]{
            TipoUsuario.ASSESSOR_JUDICIAL, TipoUsuario.ASSESSOR_DESEMBARGADOR, TipoUsuario.ASSESSOR_MINISTRO,
            TipoUsuario.SERVIDOR, TipoUsuario.SERVIDOR_FORUM, TipoUsuario.OFICIAL_JUSTICA, TipoUsuario.OFICIAL_JUSTICA_AVALIADOR
    });
    private static final PerfilLoginStrategy SIAPE_MP = new PerfilLoginStrategy.SiapeCredential(new TipoUsuario[]{
            TipoUsuario.MEMBRO_MINISTERIO_PUBLICO, TipoUsuario.PROMOTOR_ELEITORAL, TipoUsuario.PROMOTOR_TRABALHISTA,
            TipoUsuario.PROCURADOR_GERAL_REPUBLICA, TipoUsuario.DEFENSOR_PUBLICO, TipoUsuario.DEFENSOR_PUBLICO_FEDERAL,
            TipoUsuario.PROCURADOR, TipoUsuario.PROCURADORIA_MUNICIPAL, TipoUsuario.PROCURADORIA_ESTADUAL, TipoUsuario.PROCURADORIA_FEDERAL
    });
    private static final PerfilLoginStrategy SESSP = new PerfilLoginStrategy.SesspCredential(new TipoUsuario[]{
            TipoUsuario.DELEGADO_POLICIA, TipoUsuario.DELEGADO_POLICIA_FEDERAL, TipoUsuario.AGENTE_POLICIAL, TipoUsuario.ESCRIVAO_POLICIAL
    });
    private static final PerfilLoginStrategy PERITO_REG = new PerfilLoginStrategy.PeritoCadastro(new TipoUsuario[]{
            TipoUsuario.PERITO, TipoUsuario.PERITO_CRIMINAL, TipoUsuario.PERITO_AMBIENTAL, TipoUsuario.PERITO_CONTABIL,
            TipoUsuario.PERITO_ENGENHARIA, TipoUsuario.PERITO_DIGITAL, TipoUsuario.PERITO_INSS, TipoUsuario.PERITO_MEDICO,
            TipoUsuario.PSICOLOGO_JUDICIAL, TipoUsuario.ASSISTENTE_SOCIAL_JUDICIAL, TipoUsuario.ASSISTENTE_TECNICO,
            TipoUsuario.CONTADOR_JUDICIAL, TipoUsuario.ADMINISTRADOR_JUDICIAL, TipoUsuario.CURADOR_ESPECIAL, TipoUsuario.CURADOR_AUSENTES,
            TipoUsuario.INVENTARIANTE, TipoUsuario.LEILOEIRO_JUDICIAL
    });
    private static final PerfilLoginStrategy CEJUSC = new PerfilLoginStrategy.CejuscPortal(new TipoUsuario[]{
            TipoUsuario.CONCILIADOR_CEJUSC, TipoUsuario.MEDIADOR, TipoUsuario.ARBITRO
    });
    private static final PerfilLoginStrategy CARTORIO = new PerfilLoginStrategy.CartorioInstitucional(new TipoUsuario[]{
            TipoUsuario.TABELIAO, TipoUsuario.REGISTRADOR_IMOVEIS, TipoUsuario.ESCREVENTE_CARTORIO
    });
    private static final PerfilLoginStrategy ADMIN_INT = new PerfilLoginStrategy.AdminInternal(new TipoUsuario[]{
            TipoUsuario.ADMINISTRADOR, TipoUsuario.PROFESSOR, TipoUsuario.JURISTA
    });

    public PerfilLoginStrategy resolve(TipoUsuario tipo) {
        if (tipo == null) {
            return GOVBR;
        }
        return switch (tipo) {
            case CIDADAO -> GOVBR;
            case ADVOGADO, OAB_PRESIDENTE_SECCIONAL -> OAB;
            case JUIZ, JUIZ_ESTADUAL, JUIZ_FEDERAL, JUIZ_ESPECIAL, JUIZ_ELEITORAL, JUIZ_TRABALHISTA,
                    JUIZ_MILITAR, MAGISTRADO, DESEMBARGADOR, DESEMBARGADOR_FEDERAL, MINISTRO -> TRIBUNAL_INST;
            case ASSESSOR_JUDICIAL, ASSESSOR_DESEMBARGADOR, ASSESSOR_MINISTRO,
                    SERVIDOR, SERVIDOR_FORUM, OFICIAL_JUSTICA, OFICIAL_JUSTICA_AVALIADOR -> SIAPE_MATRICULA;
            case MEMBRO_MINISTERIO_PUBLICO, PROMOTOR_ELEITORAL, PROMOTOR_TRABALHISTA,
                    PROCURADOR_GERAL_REPUBLICA, DEFENSOR_PUBLICO, DEFENSOR_PUBLICO_FEDERAL,
                    PROCURADOR, PROCURADORIA_MUNICIPAL, PROCURADORIA_ESTADUAL, PROCURADORIA_FEDERAL -> SIAPE_MP;
            case DELEGADO_POLICIA, DELEGADO_POLICIA_FEDERAL, AGENTE_POLICIAL, ESCRIVAO_POLICIAL -> SESSP;
            case PERITO, PERITO_CRIMINAL, PERITO_AMBIENTAL, PERITO_CONTABIL, PERITO_ENGENHARIA,
                    PERITO_DIGITAL, PERITO_INSS, PERITO_MEDICO, PSICOLOGO_JUDICIAL, ASSISTENTE_SOCIAL_JUDICIAL,
                    ASSISTENTE_TECNICO, CONTADOR_JUDICIAL, ADMINISTRADOR_JUDICIAL,
                    LEILOEIRO_JUDICIAL, CURADOR_ESPECIAL, CURADOR_AUSENTES, INVENTARIANTE -> PERITO_REG;
            case CONCILIADOR_CEJUSC, MEDIADOR, ARBITRO -> CEJUSC;
            case TABELIAO, REGISTRADOR_IMOVEIS, ESCREVENTE_CARTORIO -> CARTORIO;
            case ADMINISTRADOR, PROFESSOR, JURISTA -> ADMIN_INT;
            default -> TRIBUNAL_INST;
        };
    }
}
