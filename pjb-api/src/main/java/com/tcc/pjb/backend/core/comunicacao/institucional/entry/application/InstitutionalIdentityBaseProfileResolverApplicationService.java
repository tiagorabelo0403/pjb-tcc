package com.tcc.pjb.backend.core.comunicacao.institucional.entry.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalEntryLandingPanel;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalIdentityBaseProfile;
import com.tcc.pjb.backend.core.comunicacao.institucional.entry.domain.InstitutionalProcessProfile;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalEntryMode;
import com.tcc.pjb.backend.model.entity.enums.InstitutionalTrustLevel;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalIdentityBaseProfileResolverApplicationService {

    public InstitutionalIdentityBaseProfile resolve(Usuario usuario) {
        TipoUsuario tipo = usuario == null ? null : usuario.getTipoUsuario();
        if (tipo == null) {
            return new InstitutionalIdentityBaseProfile(
                    "IDENTIDADE_BASE_GENERICA",
                    null,
                    false,
                    InstitutionalEntryMode.INSTITUCIONAL_AFILIADO,
                    InstitutionalProcessProfile.PERFIL_HIBRIDO,
                    InstitutionalEntryLandingPanel.PAINEL_UNIDADE,
                    InstitutionalTrustLevel.NIVEL_1_IDENTIDADE_FEDERADA,
                    true,
                    List.of(
                            "identidade_pessoal_base_sem_tipo_especializado",
                            "atuacao_sensivel_depende_de_vinculo_ou_nomeacao_homologada"
                    )
            );
        }
        if (tipo == TipoUsuario.CIDADAO) {
            return profile("CIDADAO_DIRETO", tipo, true, InstitutionalEntryMode.DIRETO_PESSOA,
                    InstitutionalProcessProfile.PERFIL_HIBRIDO,
                    InstitutionalEntryLandingPanel.PAINEL_UNIDADE,
                    InstitutionalTrustLevel.NIVEL_1_IDENTIDADE_FEDERADA,
                    false,
                    "tipo_usuario_base_preservado_como_identidade_raiz",
                    "entrada_pessoal_direta_sem_contexto_institucional_por_padrao");
        }
        if (tipo.isAdvocacia()) {
            return profile(tipo == TipoUsuario.OAB_PRESIDENTE_SECCIONAL ? "OAB_SECCIONAL_DIRETA" : "ADVOGACIA_DIRETA",
                    tipo,
                    true,
                    InstitutionalEntryMode.DIRETO_PESSOA,
                    InstitutionalProcessProfile.PERFIL_HIBRIDO,
                    tipo == TipoUsuario.OAB_PRESIDENTE_SECCIONAL ? InstitutionalEntryLandingPanel.PAINEL_ADMINISTRATIVO : InstitutionalEntryLandingPanel.PAINEL_UNIDADE,
                    tipo == TipoUsuario.OAB_PRESIDENTE_SECCIONAL ? InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO : InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                    false,
                    "identidade_pessoal_ativa_com_responsabilidade_individual",
                    "atuacao_em_nome_de_orgao_depende_de_nomeacao_homologada_distinta");
        }
        if (tipo.isMagistratura()) {
            return profile("MAGISTRATURA_DIRETA", tipo, true, InstitutionalEntryMode.DIRETO_PESSOA,
                    InstitutionalProcessProfile.MAGISTRADO_COOPERANTE,
                    InstitutionalEntryLandingPanel.PAINEL_TITULAR,
                    InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO,
                    false,
                    "magistratura_permanece_com_fluxo_pessoal_proprio",
                    "estrutura_institucional_do_forum_ou_secretaria_nao_substitui_o_perfil_do_magistrado");
        }
        if (tipo.isPerito() || tipo.isAuxiliarJustica()) {
            return profile("AUXILIAR_DIRETO", tipo, true, InstitutionalEntryMode.DIRETO_PESSOA,
                    resolveProfile(tipo),
                    InstitutionalEntryLandingPanel.PAINEL_APOIO_TECNICO,
                    InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                    false,
                    "perfil_pessoal_mantido_para_atuacao_tecnica_ou_auxiliar",
                    "quando_ingressar_em_orgao_ou_unidade_publica_o_contexto_passa_a_exigir_nomeacao_homologada");
        }
        if (tipo.isSaude()) {
            return profile("SAUDE_CONVENIADA_INSTITUCIONAL", tipo, false, InstitutionalEntryMode.INSTITUCIONAL_AFILIADO,
                    InstitutionalProcessProfile.ORGAO_TECNICO_CONVENIADO,
                    InstitutionalEntryLandingPanel.PAINEL_APOIO_TECNICO,
                    InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                    true,
                    "atores_de_saude_atuam_como_apoio_tecnico_ou_rede_conveniada",
                    "ingresso_processual_depende_de_vinculo_institucional_homologado_ou_convocacao_formal");
        }
        if (tipo.isConciliacaoMediacao()) {
            return profile("CONCILIACAO_INSTITUCIONAL", tipo, false, InstitutionalEntryMode.INSTITUCIONAL_AFILIADO,
                    resolveProfile(tipo),
                    InstitutionalEntryLandingPanel.PAINEL_AUDIENCIAS_CONCILIACAO,
                    InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA,
                    true,
                    "atuacao_conciliatoria_e_mediadora_depende_de_vinculo_ou_credenciamento_institucional",
                    "painel_operacional_de_conciliacao_e_mediacao_respeita_governanca_do_cejusc_ou_orgao_equivalente");
        }
        return profile("IDENTIDADE_BASE_INSTITUCIONAL", tipo, false, InstitutionalEntryMode.INSTITUCIONAL_AFILIADO,
                resolveProfile(tipo),
                resolvePanel(tipo),
                resolveTrust(tipo),
                true,
                "tipo_usuario_base_preservado_como_raiz_da_autenticacao",
                "atuacao_operacional_passa_a_ser_resolvida_por_orgao_unidade_caixa_nomeacao_e_contexto_ativo");
    }

    private InstitutionalIdentityBaseProfile profile(String code,
                                                     TipoUsuario tipo,
                                                     boolean fluxoDireto,
                                                     InstitutionalEntryMode mode,
                                                     InstitutionalProcessProfile profile,
                                                     InstitutionalEntryLandingPanel panel,
                                                     InstitutionalTrustLevel trust,
                                                     boolean exigeNomeacao,
                                                     String... fundamentos) {
        ArrayList<String> out = new ArrayList<>();
        out.add("identity_code=" + code);
        out.add("entry_mode=" + mode.name());
        out.add("fluxo_direto=" + fluxoDireto);
        if (fundamentos != null) {
            for (String fundamento : fundamentos) {
                if (fundamento != null && !fundamento.isBlank()) {
                    out.add(fundamento);
                }
            }
        }
        return new InstitutionalIdentityBaseProfile(code, tipo, fluxoDireto, mode, profile, panel, trust, exigeNomeacao, List.copyOf(out));
    }

    private InstitutionalTrustLevel resolveTrust(TipoUsuario tipo) {
        if (tipo == null) {
            return InstitutionalTrustLevel.NIVEL_1_IDENTIDADE_FEDERADA;
        }
        if (tipo.isMagistratura() || tipo == TipoUsuario.OAB_PRESIDENTE_SECCIONAL || tipo == TipoUsuario.DELEGADO_POLICIA_FEDERAL) {
            return InstitutionalTrustLevel.NIVEL_3_CERTIFICADO_QUALIFICADO;
        }
        if (tipo.isAdvocacia() || tipo.isPerito() || tipo.isAuxiliarJustica()) {
            return InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA;
        }
        return InstitutionalTrustLevel.NIVEL_2_NOMEACAO_ATIVA;
    }

    private InstitutionalEntryLandingPanel resolvePanel(TipoUsuario tipo) {
        if (tipo == null) {
            return InstitutionalEntryLandingPanel.PAINEL_UNIDADE;
        }
        if (tipo.isMagistratura()) {
            return InstitutionalEntryLandingPanel.PAINEL_TITULAR;
        }
        if (tipo.isPerito() || tipo.isAuxiliarJustica()) {
            return InstitutionalEntryLandingPanel.PAINEL_APOIO_TECNICO;
        }
        if (tipo.isSaude()) {
            return InstitutionalEntryLandingPanel.PAINEL_APOIO_TECNICO;
        }
        if (tipo.isConciliacaoMediacao()) {
            return InstitutionalEntryLandingPanel.PAINEL_AUDIENCIAS_CONCILIACAO;
        }
        if (tipo.isMinisterioPublico() || tipo.isDefensoriaPublica() || tipo.isProcuradoria()) {
            return InstitutionalEntryLandingPanel.PAINEL_TITULAR;
        }
        if (tipo.isServidorJudiciario()) {
            return InstitutionalEntryLandingPanel.PAINEL_SECRETARIA_FORUM;
        }
        if (tipo.isSegurancaPublica()) {
            return InstitutionalEntryLandingPanel.PAINEL_DELEGACIA;
        }
        return InstitutionalEntryLandingPanel.PAINEL_UNIDADE;
    }

    private InstitutionalProcessProfile resolveProfile(TipoUsuario tipo) {
        if (tipo == null) {
            return InstitutionalProcessProfile.PERFIL_HIBRIDO;
        }
        return switch (tipo) {
            case MEMBRO_MINISTERIO_PUBLICO, PROMOTOR_ELEITORAL, PROMOTOR_TRABALHISTA, PROCURADOR_GERAL_REPUBLICA -> InstitutionalProcessProfile.PROMOTOR;
            case DEFENSOR_PUBLICO, DEFENSOR_PUBLICO_FEDERAL -> InstitutionalProcessProfile.DEFENSOR;
            case PROCURADOR, PROCURADORIA_MUNICIPAL, PROCURADORIA_ESTADUAL, PROCURADORIA_FEDERAL -> InstitutionalProcessProfile.PROCURADOR;
            case DELEGADO_POLICIA, DELEGADO_POLICIA_FEDERAL -> InstitutionalProcessProfile.DELEGADO;
            case AGENTE_POLICIAL, ESCRIVAO_POLICIAL -> InstitutionalProcessProfile.GESTOR_DELEGACIA;
            case JUIZ, JUIZ_ESTADUAL, JUIZ_FEDERAL, JUIZ_ESPECIAL, JUIZ_ELEITORAL, JUIZ_TRABALHISTA, JUIZ_MILITAR, MAGISTRADO, DESEMBARGADOR, DESEMBARGADOR_FEDERAL, MINISTRO -> InstitutionalProcessProfile.MAGISTRADO_COOPERANTE;
            case PERITO, PERITO_CRIMINAL, PERITO_AMBIENTAL, PERITO_CONTABIL, PERITO_ENGENHARIA, PERITO_DIGITAL, PERITO_INSS, PERITO_MEDICO -> InstitutionalProcessProfile.PERITO_JUDICIAL;
            case MEDICO, HOSPITAL, UPA, CLINICA -> InstitutionalProcessProfile.ORGAO_TECNICO_CONVENIADO;
            case PSICOLOGO_JUDICIAL -> InstitutionalProcessProfile.PSICOLOGO_JUDICIAL;
            case ASSISTENTE_SOCIAL_JUDICIAL -> InstitutionalProcessProfile.ASSISTENTE_SOCIAL_JUDICIAL;
            case CONTADOR_JUDICIAL -> InstitutionalProcessProfile.CONTADOR_JUDICIAL;
            case OFICIAL_JUSTICA, OFICIAL_JUSTICA_AVALIADOR, ASSISTENTE_TECNICO, ADMINISTRADOR_JUDICIAL, LEILOEIRO_JUDICIAL, CURADOR_ESPECIAL, CURADOR_AUSENTES, INVENTARIANTE -> InstitutionalProcessProfile.TECNICO_INSTITUCIONAL;
            case CONCILIADOR_CEJUSC -> InstitutionalProcessProfile.CONCILIADOR;
            case MEDIADOR, ARBITRO -> InstitutionalProcessProfile.MEDIADOR;
            case ASSESSOR_JUDICIAL, ASSESSOR_DESEMBARGADOR, ASSESSOR_MINISTRO -> InstitutionalProcessProfile.ASSESSOR_INSTITUCIONAL;
            case TABELIAO, REGISTRADOR_IMOVEIS, ESCREVENTE_CARTORIO -> InstitutionalProcessProfile.CARTORIO_EXTRAJUDICIAL;
            case SERVIDOR, SERVIDOR_FORUM -> InstitutionalProcessProfile.SECRETARIA_FORUM;
            case OAB_PRESIDENTE_SECCIONAL, ADVOGADO, CIDADAO -> InstitutionalProcessProfile.PERFIL_HIBRIDO;
            case ADMINISTRADOR -> InstitutionalProcessProfile.ADMINISTRADOR_INSTITUCIONAL;
            default -> InstitutionalProcessProfile.PERFIL_HIBRIDO;
        };
    }
}
