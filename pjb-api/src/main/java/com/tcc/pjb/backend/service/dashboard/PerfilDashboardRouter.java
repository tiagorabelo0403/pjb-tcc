package com.tcc.pjb.backend.service.dashboard;

import com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes;
import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;

@Service
public final class PerfilDashboardRouter {

    public record PerfilRoute(
            String dashboardEndpoint,
            String navigationGroup,
            boolean multiTabSupport,
            boolean requiresJurisdictionContext,
            String[] allowedModules
    ) {
    }

    public PerfilRoute route(TipoUsuario tipo) {
        if (tipo == null) {
            return routeCidadao();
        }
        return switch (tipo) {
            case CIDADAO -> routeCidadao();
            case ADVOGADO -> routeAdvogado();
            case OAB_PRESIDENTE_SECCIONAL -> routeOabSeccional();
            case JUIZ, MAGISTRADO, JUIZ_ESTADUAL -> routeJuizEstadual();
            case JUIZ_FEDERAL -> routeJuizFederal();
            case JUIZ_ESPECIAL -> routeJuizEspecial();
            case JUIZ_ELEITORAL -> routeJuizEleitoral();
            case JUIZ_TRABALHISTA -> routeJuizTrabalhista();
            case JUIZ_MILITAR -> routeJuizMilitar();
            case DESEMBARGADOR -> routeDesembargador();
            case DESEMBARGADOR_FEDERAL -> routeDesembargadorFederal();
            case MINISTRO -> routeMinistro();
            case ASSESSOR_JUDICIAL, ASSESSOR_DESEMBARGADOR, ASSESSOR_MINISTRO -> routeAssessor();
            case MEMBRO_MINISTERIO_PUBLICO, PROMOTOR_ELEITORAL, PROMOTOR_TRABALHISTA, PROCURADOR_GERAL_REPUBLICA -> routeMinisterioPublico();
            case DEFENSOR_PUBLICO, DEFENSOR_PUBLICO_FEDERAL -> routeDefensoria();
            case PROCURADOR, PROCURADORIA_MUNICIPAL, PROCURADORIA_ESTADUAL, PROCURADORIA_FEDERAL -> routeProcuradoria();
            case DELEGADO_POLICIA, DELEGADO_POLICIA_FEDERAL, AGENTE_POLICIAL, ESCRIVAO_POLICIAL -> routeDelegacia();
            case OFICIAL_JUSTICA, OFICIAL_JUSTICA_AVALIADOR -> routeOficialJustica();
            case PERITO, PERITO_CRIMINAL, PERITO_AMBIENTAL, PERITO_CONTABIL, PERITO_ENGENHARIA, PERITO_DIGITAL,
                    PERITO_INSS, PERITO_MEDICO, PSICOLOGO_JUDICIAL, ASSISTENTE_SOCIAL_JUDICIAL, ASSISTENTE_TECNICO -> routePericia();
            case CONCILIADOR_CEJUSC, MEDIADOR, ARBITRO -> routeConciliacao();
            case CONTADOR_JUDICIAL, ADMINISTRADOR_JUDICIAL, CURADOR_ESPECIAL, INVENTARIANTE -> routeAuxiliar();
            case LEILOEIRO_JUDICIAL -> routeLeiloeiro();
            case CURADOR_AUSENTES -> routeCuradorAusentes();
            case TABELIAO, REGISTRADOR_IMOVEIS, ESCREVENTE_CARTORIO -> routeCartorio();
            case SERVIDOR, SERVIDOR_FORUM -> routeServidor();
            case ADMINISTRADOR -> routeAdmin();
            case PROFESSOR, JURISTA -> routeAcademico();
            case MEDICO, HOSPITAL, UPA, CLINICA -> routeApoioTecnico();
            default -> routeCidadao();
        };
    }

    private static PerfilRoute routeCidadao() { return new PerfilRoute("/api/v1/cidadao/painel", "CIDADAO", false, false, new String[]{"MEU_PROCESSO", "AUDIENCIAS", "DOCUMENTOS", "GOV_HUB"}); }
    private static PerfilRoute routeAdvogado() { return new PerfilRoute("/api/v1/advogado/dashboard", "ADVOCACIA", true, false, new String[]{"PROCESSOS", "CLIENTES", "ESCRITORIO", "IA_JURIDICA", "PRAZOS", "AUDITORIA"}); }
    private static PerfilRoute routeOabSeccional() { return new PerfilRoute("/api/v1/advogado/dashboard", "ADVOCACIA", true, false, new String[]{"PROCESSOS", "CLIENTES", "ESCRITORIO", "IA_JURIDICA", "PRAZOS", "OAB_SECCIONAL"}); }
    private static PerfilRoute routeJuizEstadual() { return new PerfilRoute("/api/v1/judge/docket", "MAGISTRATURA", true, true, new String[]{"PAUTAS", "SENTENCAS", "DESPACHOS", "MINUTAS", "IA_JUIZ", "DELEGACAO", "SIGILO"}); }
    private static PerfilRoute routeJuizFederal() { return new PerfilRoute("/api/v1/judge/docket", "MAGISTRATURA", true, true, new String[]{"PAUTAS", "SENTENCAS", "DESPACHOS", "MINUTAS", "INFOJUD", "SIGILO"}); }
    private static PerfilRoute routeJuizEspecial() { return new PerfilRoute("/api/v1/judge/docket", "MAGISTRATURA", true, true, new String[]{"PAUTAS", "ACORDOS", "SENTENCAS", "IA_JUIZ"}); }
    private static PerfilRoute routeJuizEleitoral() { return new PerfilRoute("/api/v1/judge/docket", "MAGISTRATURA", true, true, new String[]{"PAUTAS", "DIPLOMACAO", "REPRESENTACOES", "IA_JUIZ"}); }
    private static PerfilRoute routeJuizTrabalhista() { return new PerfilRoute("/api/v1/judge/docket", "MAGISTRATURA", true, true, new String[]{"PAUTAS", "AUDIENCIAS", "SENTENCAS", "EXECUCAO", "IA_JUIZ"}); }
    private static PerfilRoute routeJuizMilitar() { return new PerfilRoute("/api/v1/judge/docket", "MAGISTRATURA", true, true, new String[]{"PAUTAS", "IPM", "SENTENCAS", "IA_JUIZ"}); }
    private static PerfilRoute routeDesembargador() { return new PerfilRoute("/api/v1/judge/docket", "COLEGIADO", true, true, new String[]{"PAUTA_COLEGIADA", "ACORDAOS", "GABINETE", "PRECEDENTES"}); }
    private static PerfilRoute routeDesembargadorFederal() { return new PerfilRoute("/api/v1/judge/docket", "COLEGIADO", true, true, new String[]{"PAUTA_COLEGIADA", "ACORDAOS", "PRECEDENTES", "INFOJUD"}); }
    private static PerfilRoute routeMinistro() { return new PerfilRoute("/api/v1/judge/docket", "TRIBUNAL_SUPERIOR", true, true, new String[]{"GABINETE", "PRECEDENTES", "PAUTA", "ASSINATURAS", "SIGILO"}); }
    private static PerfilRoute routeAssessor() { return new PerfilRoute("/api/v1/assessor/painel", "GABINETE", true, true, new String[]{"MINUTAS", "PAUTA", "PUBLICACAO", "DELEGACAO"}); }
    private static PerfilRoute routeMinisterioPublico() { return new PerfilRoute("/api/v1/mp/painel", "MP", true, true, new String[]{"MANIFESTACOES", "INQUERITOS", "RECURSOS", "DILIGENCIAS"}); }
    private static PerfilRoute routeDefensoria() { return new PerfilRoute("/api/v1/defensor/painel", "DEFENSORIA", true, true, new String[]{"ASSISTIDOS", "PRAZOS", "AUDIENCIAS", "GRATUIDADE"}); }
    private static PerfilRoute routeProcuradoria() { return new PerfilRoute("/api/v1/advogado/dashboard", "PROCURADORIA", true, true, new String[]{"CONTENCIOSO", "EXECUCAO", "PRAZOS", "TESES"}); }
    private static PerfilRoute routeDelegacia() { return new PerfilRoute("/api/v1/delegado/painel", "SEGURANCA_PUBLICA", true, true, new String[]{"BNMP", "RENAJUD", "INQUERITOS", "MANDADOS", "PLANTAO"}); }
    private static PerfilRoute routeOficialJustica() { return new PerfilRoute("/api/v1/oficial-justica/painel", "CUMPRIMENTO", true, true, new String[]{"MANDADOS", "ROTA_DIA", "CNIB", "PENHORAS"}); }
    private static PerfilRoute routePericia() { return new PerfilRoute("/api/v1/perito/painel", "PERICIA", true, true, new String[]{"NOMEACOES", "LAUDOS", "PRAZOS", "ONBOARDING"}); }
    private static PerfilRoute routeConciliacao() { return new PerfilRoute("/api/v1/conciliacao/painel", "AUTOCOMPOSICAO", true, true, new String[]{"SESSOES", "ACORDOS", "HOMOLOGACOES", "METRICAS"}); }
    private static PerfilRoute routeAuxiliar() { return new PerfilRoute("/api/v1/perito/painel", "AUXILIAR_JUSTICA", true, true, new String[]{"TASKS", "EXPEDIENTES", "PRAZOS"}); }
    private static PerfilRoute routeLeiloeiro() { return new PerfilRoute("/api/v1/leilao/painel", "EXECUCAO", true, true, new String[]{"EDITAIS", "LEILOES", "PRESTACAO_CONTAS"}); }
    private static PerfilRoute routeCuradorAusentes() { return new PerfilRoute("/api/v1/curadoria/painel", "CURADORIA", true, true, new String[]{"BENS", "PRESTACAO_CONTAS", "MEDIDAS_URGENTES"}); }
    private static PerfilRoute routeCartorio() { return new PerfilRoute("/api/v1/extrajudicial/painel", "EXTRAJUDICIAL", true, true, new String[]{"CERTIDOES", "AVERBACOES", "CNIB", "PENHORAS"}); }
    private static PerfilRoute routeServidor() { return new PerfilRoute(OperationalApiRoutes.secretariatOperationalSnapshot(), "SECRETARIA", true, true, new String[]{"FILAS", "CUMPRIMENTO", "PAUTA", "PUBLICACAO"}); }
    private static PerfilRoute routeAdmin() { return new PerfilRoute("/api/v1/admin/dashboard", "ADMIN", true, false, new String[]{"USUARIOS", "SEGURANCA", "TELEMETRIA", "AUDITORIA"}); }
    private static PerfilRoute routeAcademico() { return new PerfilRoute("/api/v1/admin/dashboard", "ACADEMICO", false, false, new String[]{"ESTUDOS", "ANALISES", "READ_ONLY"}); }
    private static PerfilRoute routeApoioTecnico() { return new PerfilRoute(InstitutionalApiRoutes.painelExecutivo("APOIO_TECNICO"), "APOIO_TECNICO", true, true, new String[]{"PARECERES", "LAUDOS", "REQUISICOES", "COMPLEMENTACOES"}); }
}
