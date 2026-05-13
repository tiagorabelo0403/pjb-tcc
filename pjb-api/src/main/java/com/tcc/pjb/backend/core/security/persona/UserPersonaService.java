package com.tcc.pjb.backend.core.security.persona;

import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.magistratura.delegation.DelegationCredential;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.EsferaJurisdicao;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.jurisdicao.GrauJurisdicao;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;

@Service
public class UserPersonaService {

    private final CurrentUserService currentUserService;
    private final UsuarioRepository usuarioRepository;

    public UserPersonaService(CurrentUserService currentUserService, UsuarioRepository usuarioRepository) {
        this.currentUserService = Objects.requireNonNull(currentUserService);
        this.usuarioRepository = Objects.requireNonNull(usuarioRepository);
    }

    public UserPersona getRequiredPersona() {
        Usuario usuario = currentUserService.getRequired();
        TipoUsuario tipo = usuario.getTipoUsuario();
        DelegationCredential credential = RequestContext.getDelegationCredential().orElse(null);
        Long delegateId = credential == null ? null : credential.delegateId();
        boolean delegacaoAtiva = java.util.Objects.equals(delegateId, usuario.getId());

        if (delegacaoAtiva && tipo != null && tipo.isAssessor()) {
            return new UserPersona(tipo, toPersonaKey(tipo), "Assessor (delegação ativa)", "Assessor",
                    inferGrau(tipo), inferEsferaDefault(usuario), true);
        }

        if (delegacaoAtiva && tipo == TipoUsuario.JUIZ && credential != null) {
            Usuario titular = credential.magistrateId() != null
                    ? usuarioRepository.findById(credential.magistrateId()).orElse(null)
                    : null;
            if (titular != null && titular.getTipoUsuario() == TipoUsuario.MINISTRO) {
                return new UserPersona(tipo, PersonaKey.JUIZ_AUXILIAR,
                        "Juiz Auxiliar (Gabinete de Ministro)", "Juiz Auxiliar",
                        GrauJurisdicao.SUPERIOR, inferEsferaDefault(titular), true);
            }
        }

        if (tipo != null && tipo.isMagistratura()) {
            GrauJurisdicao grau = inferGrau(tipo);
            EsferaJurisdicao esfera = inferEsferaDefault(usuario);
            return new UserPersona(tipo, toPersonaKey(tipo), buildMagistrateDisplay(tipo, grau, esfera), buildTratamento(tipo), grau, esfera, delegacaoAtiva);
        }

        if (tipo == null) {
            return new UserPersona(null, PersonaKey.OUTRO, "Usuário", "Usuário", null, null, delegacaoAtiva);
        }

        return switch (tipo) {
            case ASSESSOR_JUDICIAL, ASSESSOR_DESEMBARGADOR, ASSESSOR_MINISTRO ->
                    new UserPersona(tipo, toPersonaKey(tipo), buildTratamento(tipo), buildTratamento(tipo), inferGrau(tipo), inferEsferaDefault(usuario), delegacaoAtiva);
            case MEMBRO_MINISTERIO_PUBLICO, PROMOTOR_ELEITORAL, PROMOTOR_TRABALHISTA, PROCURADOR_GERAL_REPUBLICA,
                    DEFENSOR_PUBLICO, DEFENSOR_PUBLICO_FEDERAL, PROCURADOR, PROCURADORIA_MUNICIPAL,
                    PROCURADORIA_ESTADUAL, PROCURADORIA_FEDERAL,
                    DELEGADO_POLICIA, DELEGADO_POLICIA_FEDERAL, AGENTE_POLICIAL, ESCRIVAO_POLICIAL,
                    OFICIAL_JUSTICA, OFICIAL_JUSTICA_AVALIADOR,
                    PERITO, PERITO_CRIMINAL, PERITO_AMBIENTAL, PERITO_CONTABIL, PERITO_ENGENHARIA,
                    PERITO_DIGITAL, PERITO_INSS, PERITO_MEDICO, PSICOLOGO_JUDICIAL, ASSISTENTE_SOCIAL_JUDICIAL,
                    ASSISTENTE_TECNICO,
                    CONCILIADOR_CEJUSC, MEDIADOR, ARBITRO,
                    CONTADOR_JUDICIAL, ADMINISTRADOR_JUDICIAL, LEILOEIRO_JUDICIAL, CURADOR_ESPECIAL,
                    CURADOR_AUSENTES, INVENTARIANTE,
                    TABELIAO, REGISTRADOR_IMOVEIS, ESCREVENTE_CARTORIO,
                    ADVOGADO, OAB_PRESIDENTE_SECCIONAL, CIDADAO,
                    SERVIDOR, SERVIDOR_FORUM, ADMINISTRADOR,
                    PROFESSOR, JURISTA, MEDICO, HOSPITAL, UPA, CLINICA ->
                    new UserPersona(tipo, toPersonaKey(tipo), buildDisplay(tipo), buildTratamento(tipo), inferGrau(tipo), inferEsferaDefault(usuario), delegacaoAtiva);
            default -> new UserPersona(tipo, PersonaKey.OUTRO, buildDisplay(tipo), buildTratamento(tipo), inferGrau(tipo), inferEsferaDefault(usuario), delegacaoAtiva);
        };
    }

    private static PersonaKey toPersonaKey(TipoUsuario tipo) {
        if (tipo == null) {
            return PersonaKey.OUTRO;
        }
        return switch (tipo) {
            case MAGISTRADO, JUIZ -> PersonaKey.JUIZ;
            case JUIZ_ESTADUAL -> PersonaKey.JUIZ_ESTADUAL;
            case JUIZ_FEDERAL -> PersonaKey.JUIZ_FEDERAL;
            case JUIZ_ESPECIAL -> PersonaKey.JUIZ_ESPECIAL;
            case JUIZ_ELEITORAL -> PersonaKey.JUIZ_ELEITORAL;
            case JUIZ_TRABALHISTA -> PersonaKey.JUIZ_TRABALHISTA;
            case JUIZ_MILITAR -> PersonaKey.JUIZ_MILITAR;
            case DESEMBARGADOR -> PersonaKey.DESEMBARGADOR;
            case DESEMBARGADOR_FEDERAL -> PersonaKey.DESEMBARGADOR_FEDERAL;
            case MINISTRO -> PersonaKey.MINISTRO;
            case ASSESSOR_JUDICIAL -> PersonaKey.ASSESSOR_JUDICIAL;
            case ASSESSOR_DESEMBARGADOR -> PersonaKey.ASSESSOR_DESEMBARGADOR;
            case ASSESSOR_MINISTRO -> PersonaKey.ASSESSOR_MINISTRO;
            case SERVIDOR, SERVIDOR_FORUM -> PersonaKey.SERVIDOR;
            case ADMINISTRADOR -> PersonaKey.ADMINISTRADOR;
            case MEMBRO_MINISTERIO_PUBLICO -> PersonaKey.PROMOTOR;
            case PROMOTOR_ELEITORAL -> PersonaKey.PROMOTOR_ELEITORAL;
            case PROMOTOR_TRABALHISTA -> PersonaKey.PROMOTOR_TRABALHISTA;
            case PROCURADOR_GERAL_REPUBLICA -> PersonaKey.PROCURADOR_GERAL_REPUBLICA;
            case DEFENSOR_PUBLICO -> PersonaKey.DEFENSOR_ESTADUAL;
            case DEFENSOR_PUBLICO_FEDERAL -> PersonaKey.DEFENSOR_FEDERAL;
            case PROCURADOR, PROCURADORIA_ESTADUAL -> PersonaKey.PROCURADOR_ESTADUAL;
            case PROCURADORIA_MUNICIPAL -> PersonaKey.PROCURADOR_MUNICIPAL;
            case PROCURADORIA_FEDERAL -> PersonaKey.PROCURADOR_FEDERAL;
            case DELEGADO_POLICIA -> PersonaKey.DELEGADO_ESTADUAL;
            case DELEGADO_POLICIA_FEDERAL -> PersonaKey.DELEGADO_FEDERAL;
            case AGENTE_POLICIAL -> PersonaKey.AGENTE_POLICIAL;
            case ESCRIVAO_POLICIAL -> PersonaKey.ESCRIVAO_POLICIAL;
            case OFICIAL_JUSTICA -> PersonaKey.OFICIAL_JUSTICA;
            case OFICIAL_JUSTICA_AVALIADOR -> PersonaKey.OFICIAL_JUSTICA_AVALIADOR;
            case PERITO -> PersonaKey.PERITO_GERAL;
            case PERITO_CRIMINAL -> PersonaKey.PERITO_CRIMINAL;
            case PERITO_AMBIENTAL -> PersonaKey.PERITO_AMBIENTAL;
            case PERITO_CONTABIL -> PersonaKey.PERITO_CONTABIL;
            case PERITO_ENGENHARIA -> PersonaKey.PERITO_ENGENHARIA;
            case PERITO_DIGITAL -> PersonaKey.PERITO_DIGITAL;
            case PERITO_MEDICO -> PersonaKey.PERITO_MEDICO;
            case PERITO_INSS -> PersonaKey.PERITO_INSS;
            case PSICOLOGO_JUDICIAL -> PersonaKey.PSICOLOGO_JUDICIAL;
            case ASSISTENTE_SOCIAL_JUDICIAL -> PersonaKey.ASSISTENTE_SOCIAL_JUDICIAL;
            case ASSISTENTE_TECNICO -> PersonaKey.ASSISTENTE_TECNICO;
            case CONCILIADOR_CEJUSC -> PersonaKey.CONCILIADOR;
            case MEDIADOR -> PersonaKey.MEDIADOR;
            case ARBITRO -> PersonaKey.ARBITRO;
            case CONTADOR_JUDICIAL -> PersonaKey.CONTADOR_JUDICIAL;
            case ADMINISTRADOR_JUDICIAL -> PersonaKey.ADMINISTRADOR_JUDICIAL;
            case LEILOEIRO_JUDICIAL -> PersonaKey.LEILOEIRO_JUDICIAL;
            case CURADOR_ESPECIAL -> PersonaKey.CURADOR_ESPECIAL;
            case CURADOR_AUSENTES -> PersonaKey.CURADOR_AUSENTES;
            case INVENTARIANTE -> PersonaKey.INVENTARIANTE;
            case TABELIAO -> PersonaKey.TABELIAO;
            case REGISTRADOR_IMOVEIS -> PersonaKey.REGISTRADOR_IMOVEIS;
            case ESCREVENTE_CARTORIO -> PersonaKey.ESCREVENTE_CARTORIO;
            case ADVOGADO -> PersonaKey.ADVOGADO;
            case OAB_PRESIDENTE_SECCIONAL -> PersonaKey.OAB_PRESIDENTE_SECCIONAL;
            case CIDADAO -> PersonaKey.CIDADAO;
            case PROFESSOR -> PersonaKey.PROFESSOR;
            case JURISTA -> PersonaKey.JURISTA;
            case MEDICO -> PersonaKey.MEDICO;
            case HOSPITAL -> PersonaKey.HOSPITAL;
            case UPA -> PersonaKey.UPA;
            case CLINICA -> PersonaKey.CLINICA;
            default -> PersonaKey.OUTRO;
        };
    }

    private static EsferaJurisdicao inferEsferaDefault(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        if (usuario.getEnteFederativo() != null) {
            return switch (usuario.getEnteFederativo()) {
                case UNIAO -> EsferaJurisdicao.JUSTICA_FEDERAL;
                case ESTADO, MUNICIPIO -> EsferaJurisdicao.JUSTICA_ESTADUAL;
                default -> EsferaJurisdicao.JUSTICA_ESTADUAL;
            };
        }
        TipoUsuario tipo = usuario.getTipoUsuario();
        if (tipo == TipoUsuario.DESEMBARGADOR_FEDERAL || tipo == TipoUsuario.JUIZ_FEDERAL || tipo == TipoUsuario.MINISTRO
                || tipo == TipoUsuario.DELEGADO_POLICIA_FEDERAL || tipo == TipoUsuario.DEFENSOR_PUBLICO_FEDERAL
                || tipo == TipoUsuario.PROCURADORIA_FEDERAL || tipo == TipoUsuario.PROCURADOR_GERAL_REPUBLICA) {
            return EsferaJurisdicao.JUSTICA_FEDERAL;
        }
        return EsferaJurisdicao.JUSTICA_ESTADUAL;
    }

    private static GrauJurisdicao inferGrau(TipoUsuario tipo) {
        if (tipo == null) {
            return null;
        }
        return switch (tipo) {
            case MINISTRO, ASSESSOR_MINISTRO -> GrauJurisdicao.SUPERIOR;
            case DESEMBARGADOR, DESEMBARGADOR_FEDERAL, ASSESSOR_DESEMBARGADOR -> GrauJurisdicao.SEGUNDO_GRAU;
            case MAGISTRADO, JUIZ, JUIZ_ESTADUAL, JUIZ_FEDERAL, JUIZ_ESPECIAL, JUIZ_ELEITORAL,
                    JUIZ_TRABALHISTA, JUIZ_MILITAR, ASSESSOR_JUDICIAL -> GrauJurisdicao.PRIMEIRO_GRAU;
            default -> null;
        };
    }

    private static String buildMagistrateDisplay(TipoUsuario tipo, GrauJurisdicao grau, EsferaJurisdicao esfera) {
        String base = switch (tipo) {
            case JUIZ_FEDERAL -> "Juiz Federal";
            case JUIZ_ELEITORAL -> "Juiz Eleitoral";
            case JUIZ_TRABALHISTA -> "Juiz do Trabalho";
            case JUIZ_MILITAR -> "Juiz Militar";
            case DESEMBARGADOR_FEDERAL -> "Desembargador Federal";
            case DESEMBARGADOR -> "Desembargador";
            case MINISTRO -> "Ministro";
            default -> esfera == EsferaJurisdicao.JUSTICA_FEDERAL ? "Magistrado Federal" : "Magistrado";
        };
        if (grau == null) {
            return base;
        }
        String grauTxt = switch (grau) {
            case PRIMEIRO_GRAU -> "1º Grau";
            case SEGUNDO_GRAU -> "2º Grau";
            case SUPERIOR -> "Tribunal Superior";
            case CONSTITUCIONAL -> "Máximo Constitucional";
        };
        return base + " (" + grauTxt + ")";
    }

    private static String buildDisplay(TipoUsuario tipo) {
        return switch (tipo) {
            case OAB_PRESIDENTE_SECCIONAL -> "Presidência Seccional OAB";
            case PERITO_INSS -> "Perito do INSS";
            case CONCILIADOR_CEJUSC -> "Conciliador CEJUSC";
            case CURADOR_AUSENTES -> "Curador de Ausentes";
            case REGISTRADOR_IMOVEIS -> "Registrador de Imóveis";
            case ESCREVENTE_CARTORIO -> "Escrevente de Cartório";
            case LEILOEIRO_JUDICIAL -> "Leiloeiro Judicial";
            case PSICOLOGO_JUDICIAL -> "Psicólogo Judicial";
            case ASSISTENTE_SOCIAL_JUDICIAL -> "Assistente Social Judicial";
            default -> humanize(tipo.name());
        };
    }

    private static String buildTratamento(TipoUsuario tipo) {
        return switch (tipo) {
            case MAGISTRADO, JUIZ, JUIZ_ESTADUAL -> "Magistrado";
            case JUIZ_FEDERAL -> "Magistrado Federal";
            case JUIZ_ESPECIAL -> "Juiz";
            case JUIZ_ELEITORAL -> "Juiz Eleitoral";
            case JUIZ_TRABALHISTA -> "Juiz do Trabalho";
            case JUIZ_MILITAR -> "Juiz Auditor Militar";
            case DESEMBARGADOR -> "Desembargador";
            case DESEMBARGADOR_FEDERAL -> "Desembargador Federal";
            case MINISTRO -> "Ministro";
            case MEMBRO_MINISTERIO_PUBLICO -> "Promotor";
            case PROMOTOR_ELEITORAL -> "Promotor Eleitoral";
            case PROMOTOR_TRABALHISTA -> "Promotor do Trabalho";
            case PROCURADOR_GERAL_REPUBLICA -> "Procurador-Geral da República";
            case DEFENSOR_PUBLICO -> "Defensor Público";
            case DEFENSOR_PUBLICO_FEDERAL -> "Defensor Público Federal";
            case PROCURADOR -> "Procurador";
            case PROCURADORIA_MUNICIPAL -> "Procurador Municipal";
            case PROCURADORIA_ESTADUAL -> "Procurador Estadual";
            case PROCURADORIA_FEDERAL -> "Procurador Federal";
            case DELEGADO_POLICIA -> "Delegado de Polícia";
            case DELEGADO_POLICIA_FEDERAL -> "Delegado Federal";
            case AGENTE_POLICIAL -> "Agente Policial";
            case ESCRIVAO_POLICIAL -> "Escrivão Policial";
            case OFICIAL_JUSTICA -> "Oficial de Justiça";
            case OFICIAL_JUSTICA_AVALIADOR -> "Oficial de Justiça Avaliador";
            case PERITO -> "Perito Judicial";
            case PERITO_CRIMINAL -> "Perito Criminal";
            case PERITO_AMBIENTAL -> "Perito Ambiental";
            case PERITO_CONTABIL -> "Perito Contábil";
            case PERITO_ENGENHARIA -> "Perito de Engenharia";
            case PERITO_DIGITAL -> "Perito Forense Digital";
            case PERITO_INSS -> "Perito do INSS";
            case PERITO_MEDICO -> "Perito Médico";
            case PSICOLOGO_JUDICIAL -> "Psicólogo Judicial";
            case ASSISTENTE_SOCIAL_JUDICIAL -> "Assistente Social Judicial";
            case ASSISTENTE_TECNICO -> "Assistente Técnico";
            case CONCILIADOR_CEJUSC -> "Conciliador CEJUSC";
            case MEDIADOR -> "Mediador";
            case ARBITRO -> "Árbitro";
            case CONTADOR_JUDICIAL -> "Contador Judicial";
            case ADMINISTRADOR_JUDICIAL -> "Administrador Judicial";
            case LEILOEIRO_JUDICIAL -> "Leiloeiro Judicial";
            case CURADOR_ESPECIAL -> "Curador Especial";
            case CURADOR_AUSENTES -> "Curador de Ausentes";
            case INVENTARIANTE -> "Inventariante";
            case TABELIAO -> "Tabelião";
            case REGISTRADOR_IMOVEIS -> "Registrador de Imóveis";
            case ESCREVENTE_CARTORIO -> "Escrevente de Cartório";
            case SERVIDOR, SERVIDOR_FORUM -> "Servidor";
            case ADMINISTRADOR -> "Administrador";
            case ADVOGADO -> "Advogado";
            case OAB_PRESIDENTE_SECCIONAL -> "Presidente Seccional OAB";
            case CIDADAO -> "Cidadão";
            default -> buildDisplay(tipo);
        };
    }

    private static String humanize(String raw) {
        if (raw == null || raw.isBlank()) {
            return "Usuário";
        }
        String s = raw.trim().toLowerCase(Locale.ROOT).replace('_', ' ');
        String[] parts = s.split("\\s+");
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            if (part.isBlank()) {
                continue;
            }
            builder.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return builder.toString().trim();
    }
}
