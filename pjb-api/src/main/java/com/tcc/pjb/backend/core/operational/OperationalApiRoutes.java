package com.tcc.pjb.backend.core.operational;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class OperationalApiRoutes {

    public static final String API_V1 = "/api/v1";

    public static final String FORUM_BASE = API_V1 + "/forum";
    public static final String PATH_FORUM_DESKS_SELF = "/desks/self";
    public static final String PATH_FORUM_DESKS_RESOLVE = "/desks/resolve";
    public static final String PATH_FORUM_OFFICIAL_RETURNS = "/oficial-retornos";
    public static final String PATH_FORUM_OFFICIAL_RETURN_REACTIVATE = "/oficial-retornos/{deskWorkItemId}/reativar";
    public static final String FORUM_HABILITACOES_BASE = FORUM_BASE + "/habilitacoes";
    public static final String PATH_FORUM_HABILITACOES_PENDENTES = "/pendentes";
    public static final String PATH_FORUM_HABILITACOES_DEFERIR = "/{id}/deferir";
    public static final String PATH_FORUM_HABILITACOES_INDEFERIR = "/{id}/indeferir";

    public static final String SECRETARIAT_BASE = API_V1 + "/secretariat";
    public static final String OFICIAL_JUSTICA_BASE = API_V1 + "/oficial-justica";
    public static final String JUDGE_GABINETE_DECISOES_BASE = API_V1 + "/juiz/gabinete-decisoes";
    public static final String DESEMBARGADOR_COLEGIADO_BASE = API_V1 + "/desembargador/colegiado";
    public static final String DESEMBARGADOR_PLENARIO_BASE = API_V1 + "/desembargador/plenario";
    public static final String MINISTRO_PLENARIO_BASE = API_V1 + "/ministro/plenario";
    public static final String PATH_SECRETARIAT_QUEUE = "/queue";
    public static final String PATH_SECRETARIAT_QUEUE_SUMMARY = "/queue/summary";
    public static final String PATH_SECRETARIAT_QUEUE_PANEL = "/queue/panel";
    public static final String PATH_SECRETARIAT_QUEUE_AGENDA = "/queue/agenda";
    public static final String PATH_SECRETARIAT_QUEUE_GOVERNANCE = "/queue/governance";
    public static final String PATH_SECRETARIAT_QUEUE_EXCEPTIONS = "/queue/exceptions";
    public static final String PATH_SECRETARIAT_QUEUE_COVERAGE = "/queue/coverage";
    public static final String PATH_SECRETARIAT_QUEUE_FORMAL_CATALOG = "/queue/formal-catalog";
    public static final String PATH_SECRETARIAT_QUEUE_PRODUTIVIDADE = "/queue/produtividade";
    public static final String PATH_SECRETARIAT_QUEUE_VENUE_CONFIRMATION = "/queue/items/{workItemId}/venue-confirmation";
    public static final String PATH_SECRETARIAT_QUEUE_PARTICIPANT_NOTIFICATION = "/queue/items/{workItemId}/participant-notification";
    public static final String PATH_SECRETARIAT_QUEUE_PARTICIPANT_NOTIFICATION_CHALLENGE = "/queue/items/{workItemId}/participant-notification/challenge";
    public static final String PATH_SECRETARIAT_QUEUE_ATTENDANCE = "/queue/items/{workItemId}/attendance";
    public static final String PATH_SECRETARIAT_QUEUE_COMPLETION_EVENT = "/queue/items/{workItemId}/completion-event";
    public static final String PATH_SECRETARIAT_QUEUE_PROCESS_RETURN = "/queue/items/{workItemId}/process-return";
    public static final String SECRETARIAT_CREDENTIAL_SECURITY_BASE = SECRETARIAT_BASE + "/credential-security";
    public static final String PATH_SECRETARIAT_CREDENTIAL_SECURITY = "";
    public static final String PATH_SECRETARIAT_CREDENTIAL_CHALLENGE = "/functions/{functionCode}/challenge";
    public static final String PATH_SECRETARIAT_CREDENTIAL_PASSWORD = "/functions/{functionCode}/password";
    public static final String PATH_SECRETARIAT_CREDENTIAL_UNLOCK = "/functions/{functionCode}/unlock";

    public static final String OFICIAL_JUSTICA_CREDENTIAL_SECURITY_BASE = OFICIAL_JUSTICA_BASE + "/credential-security";
    public static final String PATH_OFICIAL_JUSTICA_CREDENTIAL_SECURITY = "";
    public static final String PATH_OFICIAL_JUSTICA_CREDENTIAL_CHALLENGE = "/functions/{functionCode}/challenge";
    public static final String PATH_OFICIAL_JUSTICA_CREDENTIAL_PASSWORD = "/functions/{functionCode}/password";
    public static final String PATH_OFICIAL_JUSTICA_CREDENTIAL_UNLOCK = "/functions/{functionCode}/unlock";

    public static final String INSTITUTIONAL_SUPPORT_BASE = API_V1 + "/institutional-support";
    public static final String PATH_INSTITUTIONAL_SUPPORT_BRANCH_SNAPSHOT = "/{branchCode}/snapshot";
    public static final String PATH_INSTITUTIONAL_SUPPORT_BRANCH_AGENDA = "/{branchCode}/agenda";
    public static final String PATH_INSTITUTIONAL_SUPPORT_BRANCH_CREDENTIAL_SECURITY = "/{branchCode}/credential-security";
    public static final String PATH_INSTITUTIONAL_SUPPORT_BRANCH_CREDENTIAL_CHALLENGE = "/{branchCode}/credential-security/functions/{functionCode}/challenge";
    public static final String PATH_INSTITUTIONAL_SUPPORT_BRANCH_CREDENTIAL_PASSWORD = "/{branchCode}/credential-security/functions/{functionCode}/password";
    public static final String PATH_INSTITUTIONAL_SUPPORT_BRANCH_CREDENTIAL_UNLOCK = "/{branchCode}/credential-security/functions/{functionCode}/unlock";
    public static final String PATH_INSTITUTIONAL_SUPPORT_BRANCH_COMPETENCE_MATRIX = "/{branchCode}/competence-matrix";
    public static final String PATH_INSTITUTIONAL_SUPPORT_BRANCH_COVERAGE = "/{branchCode}/coverage";
    public static final String PATH_INSTITUTIONAL_SUPPORT_BRANCH_PROCESS_PREPAUTA = "/{branchCode}/processos/{processoId}/pre-pauta";

    public static final String INSTITUTIONAL_CREDENTIAL_GOVERNANCE_BASE = API_V1 + "/institutional/credential-governance";
    public static final String PATH_INSTITUTIONAL_CREDENTIAL_GOVERNANCE = "/operational-functions";
    public static final String PATH_INSTITUTIONAL_CREDENTIAL_GOVERNANCE_TARGET = "/operational-functions/target/{targetUserId}";
    public static final String PATH_SECRETARIAT_STREAM = "/stream";
    public static final String PATH_SECRETARIAT_DOSSIE = "/dossie/{processoId}";
    public static final String PATH_SECRETARIAT_PROCESSO_MINUTA_JUNTADA_PDF = "/processos/{processoId}/minuta-juntada.pdf";
    public static final String PATH_SECRETARIAT_PROCESSO_JUNTADAS = "/processos/{processoId}/juntadas";

    public static final String SECRETARIAT_JULGAMENTOS_BASE = SECRETARIAT_BASE + "/julgamentos";
    public static final String PATH_SECRETARIAT_JULGAMENTO_PROCESSO = "/processos/{processoId}";
    public static final String PATH_SECRETARIAT_JULGAMENTO_STATUS = "/{julgamentoId}/status";
    public static final String PATH_SECRETARIAT_JULGAMENTO_VOTOS = "/{julgamentoId}/votos";
    public static final String PATH_SECRETARIAT_JULGAMENTO_ACORDAO = "/{julgamentoId}/acordao";

    public static final String SECRETARIAT_OPERATIONAL_BASE = SECRETARIAT_BASE + "/operacional";
    public static final String SECRETARIAT_ESPECIALIZADA_BASE = SECRETARIAT_BASE + "/especializada";
    public static final String PATH_SECRETARIAT_OPERATIONAL_SNAPSHOT = "/snapshot";
    public static final String PATH_SECRETARIAT_OPERATIONAL_PROCESS_JUNTADA = "/processos/{processoId}/juntada";
    public static final String PATH_SECRETARIAT_OPERATIONAL_PROCESS_INTIMACAO = "/processos/{processoId}/intimacao";
    public static final String PATH_SECRETARIAT_OPERATIONAL_PROCESS_MANDADO_CITACAO = "/processos/{processoId}/mandado-citacao";
    public static final String PATH_SECRETARIAT_OPERATIONAL_PROCESS_CONCLUSAO = "/processos/{processoId}/conclusao";
    public static final String PATH_SECRETARIAT_OPERATIONAL_QUEUE_SANEAMENTO = "/fila/saneamento";
    public static final String PATH_SECRETARIAT_OPERATIONAL_SERVIDOR_REATRIBUICAO = "/servidores/{servidorId}/reatribuir-carga";
    public static final String PATH_SECRETARIAT_OPERATIONAL_OFFICIAL_CLOSURES = "/oficial-cumprimentos";
    public static final String PATH_SECRETARIAT_OPERATIONAL_OFFICIAL_CLOSURE_RECLASSIFY = "/oficial-cumprimentos/{deskWorkItemId}/reclassificar";
    public static final String PATH_SECRETARIAT_OPERATIONAL_OFFICIAL_CLOSURE_NEXT_PROVIDENCE = "/oficial-cumprimentos/{deskWorkItemId}/proxima-providencia";
    public static final String PATH_SECRETARIAT_OPERATIONAL_OFFICIAL_CLOSURE_MATERIALIZE_ACT = "/oficial-cumprimentos/{deskWorkItemId}/materializar-ato";
    public static final String PATH_SECRETARIAT_OPERATIONAL_OFFICIAL_CLOSURE_DRAWERS = "/oficial-cumprimentos/gavetas";
    public static final String PATH_SECRETARIAT_OPERATIONAL_OFFICIAL_CLOSURE_DRAWER_DETAIL = "/oficial-cumprimentos/gavetas/detalhe";
    public static final String PATH_SECRETARIAT_OPERATIONAL_BREAK_GLASS = "/processos/{processoId}/break-glass";
    public static final String PATH_SECRETARIAT_OPERATIONAL_VISIBILIDADE_PESSOAL = "/processos/{processoId}/visibilidade-pessoal";
    public static final String PATH_SECRETARIAT_OPERATIONAL_COLLEGIATE_PAUTA = "/colegiado/processos/{processoId}/pauta";
    public static final String PATH_SECRETARIAT_OPERATIONAL_COLLEGIATE_PUBLICATION = "/colegiado/julgamentos/{julgamentoId}/publicacao-pauta";
    public static final String PATH_SECRETARIAT_OPERATIONAL_COLLEGIATE_SUSTENTACAO = "/colegiado/julgamentos/{julgamentoId}/sustentacao-oral";
    public static final String PATH_SECRETARIAT_OPERATIONAL_COLLEGIATE_ACORDAO = "/colegiado/julgamentos/{julgamentoId}/publicacao-acordao";
    public static final String PATH_SECRETARIAT_OPERATIONAL_COLLEGIATE_BAIXA = "/colegiado/julgamentos/{julgamentoId}/baixa-origem";
    public static final String PATH_SECRETARIAT_OPERATIONAL_ELECTORAL_CORREGEDORIA = "/eleitoral/processos/{processoId}/corregedoria/instaurar";
    public static final String PATH_SECRETARIAT_OPERATIONAL_ELECTORAL_INSPECAO = "/eleitoral/processos/{processoId}/corregedoria/inspecao";
    public static final String PATH_SECRETARIAT_OPERATIONAL_ELECTORAL_PESQUISA = "/eleitoral/processos/{processoId}/pesquisas/validacao";
    public static final String PATH_SECRETARIAT_OPERATIONAL_LABOUR_MIDIA_RECEBIMENTO = "/trabalhista/processos/{processoId}/midias/recebimento";
    public static final String PATH_SECRETARIAT_OPERATIONAL_LABOUR_MIDIA_DISPONIBILIZACAO = "/trabalhista/processos/{processoId}/midias/disponibilizacao";
    public static final String PATH_SECRETARIAT_OPERATIONAL_LABOUR_EXECUCAO = "/trabalhista/processos/{processoId}/execucao/impulsionamento";
    public static final String PATH_SECRETARIAT_OPERATIONAL_MILITARY_PLANTAO = "/militar/processos/{processoId}/plantao/recepcao";
    public static final String PATH_SECRETARIAT_OPERATIONAL_MILITARY_BALCAO = "/militar/processos/{processoId}/balcao-virtual/atendimento";

    public static final String PATH_OFICIAL_JUSTICA_AGENDA_OPERACIONAL = "/agenda-operacional";
    public static final String PATH_OFICIAL_JUSTICA_NAMED_PROCESS_WORKBENCH = "/processos-nomeados/{processoId}/workbench";
    public static final String PATH_OFICIAL_JUSTICA_BALCAO_VIRTUAL_ROOM = "/balcao-virtual/processos/{processoId}/sala";
    public static final String PATH_OFICIAL_JUSTICA_CIENTE_INTIMACAO = "/processos/{processoId}/ciente-intimacao";
    public static final String PATH_OFICIAL_JUSTICA_CIENTE_INTIMACAO_CHALLENGE = "/processos/{processoId}/ciente-intimacao/challenge";

    public static final String PATH_JUDGE_OFFICIAL_RETURN_SUGGESTION = "/oficial-retornos/{gabineteWorkItemId}/sugestao";
    public static final String PATH_JUDGE_OFFICIAL_RETURN_APPROVE_MINUTA = "/oficial-retornos/{gabineteWorkItemId}/aprovar-minuta";
    public static final String PATH_JUDGE_OFFICIAL_RETURN_APPROVE_REEXPEDICAO = "/oficial-retornos/{gabineteWorkItemId}/aprovar-reexpedicao";
    public static final String PATH_JUDGE_OFFICIAL_RETURN_REJECT = "/oficial-retornos/{gabineteWorkItemId}/rejeitar";

    public static final String BALCAO_VIRTUAL_BASE = API_V1 + "/balcao-virtual";
    public static final String PATH_BALCAO_ATENDIMENTOS       = "/atendimentos";
    public static final String PATH_BALCAO_ATENDIMENTO_ID     = "/atendimentos/{id}";
    public static final String PATH_BALCAO_PROTOCOLO          = "/atendimentos/protocolo/{protocolo}";
    public static final String PATH_BALCAO_FILA               = "/atendimentos/fila";
    public static final String PATH_BALCAO_CHAMAR             = "/atendimentos/{id}/chamar";
    public static final String PATH_BALCAO_INICIAR            = "/atendimentos/{id}/iniciar";
    public static final String PATH_BALCAO_CONCLUIR           = "/atendimentos/{id}/concluir";
    public static final String PATH_BALCAO_NAO_COMPARECEU     = "/atendimentos/{id}/nao-compareceu";
    public static final String PATH_BALCAO_CANCELAR           = "/atendimentos/{id}/cancelar";

    public static final String PAUTA_AUDIENCIA_BASE = API_V1 + "/audiencia/pauta";
    public static final String PATH_PAUTA_AGENDA              = "/agenda";
    public static final String PATH_PAUTA_POR_ID              = "/{audienciaId}";
    public static final String PATH_PAUTA_REDESIGNAR          = "/{audienciaId}/redesignar";
    public static final String PATH_PAUTA_CANCELAR            = "/{audienciaId}/cancelar";
    public static final String PATH_PAUTA_REALIZAR            = "/{audienciaId}/realizar";
    public static final String PATH_PAUTA_INTIMAR             = "/{audienciaId}/intimar";

    public static final String INTIMACAO_AUDIENCIA_BASE = API_V1 + "/audiencia/{audienciaId}/intimacoes";
    public static final String PATH_INTIMACAO_CIENCIA         = "/{intimacaoId}/ciencia";

    public static final String PAINEL_ORGANIZACAO_BASE = API_V1 + "/secretariat/painel-organizacao";

    public static final String PROCESSUAL_PENDENCIAS_BASE = API_V1 + "/processual/pendencias";
    public static final String PATH_PROCESSUAL_PENDENCIAS_PAINEL = "/painel";
    public static final String PROCESSUAL_PARTICIPACAO_ATIVA_BASE = API_V1 + "/processual/processos";
    public static final String PATH_PROCESSUAL_PARTICIPACAO_WORKSPACE = "/{processoId}/participacao-ativa/workspace";
    public static final String PATH_PROCESSUAL_PARTICIPACAO_PROTOCOLAR = "/{processoId}/participacao-ativa/protocolar";
    public static final String PATH_PROCESSUAL_PARTICIPACAO_SUBMISSOES = "/{processoId}/participacao-ativa/submissoes";

    private static final List<String> OPERATIONAL_BASES = List.of(
            FORUM_BASE,
            SECRETARIAT_BASE,
            OFICIAL_JUSTICA_BASE,
            JUDGE_GABINETE_DECISOES_BASE,
            PROCESSUAL_PENDENCIAS_BASE,
            PROCESSUAL_PARTICIPACAO_ATIVA_BASE
    );

    private OperationalApiRoutes() {
    }

    public static String forumDesksSelf() {
        return FORUM_BASE + PATH_FORUM_DESKS_SELF;
    }

    public static String forumDesksResolve() {
        return FORUM_BASE + PATH_FORUM_DESKS_RESOLVE;
    }

    public static String forumDesksResolve(Long processoId) {
        return withParams(forumDesksResolve(), orderedMap("processoId", processoId));
    }

    public static String forumOfficialReturns() {
        return FORUM_BASE + PATH_FORUM_OFFICIAL_RETURNS;
    }

    public static String forumOfficialReturns(String inboxKey) {
        return withParams(forumOfficialReturns(), orderedMap("inboxKey", inboxKey));
    }

    public static String forumOfficialReturnReactivate(Long deskWorkItemId) {
        return resolvePathVariable(FORUM_BASE + PATH_FORUM_OFFICIAL_RETURN_REACTIVATE, "deskWorkItemId", deskWorkItemId);
    }

    public static String forumHabilitacoesPendentes() {
        return FORUM_HABILITACOES_BASE + PATH_FORUM_HABILITACOES_PENDENTES;
    }

    public static String forumHabilitacaoDeferir(String id) {
        return resolvePathVariable(FORUM_HABILITACOES_BASE + PATH_FORUM_HABILITACOES_DEFERIR, "id", id);
    }

    public static String forumHabilitacaoIndeferir(String id) {
        return resolvePathVariable(FORUM_HABILITACOES_BASE + PATH_FORUM_HABILITACOES_INDEFERIR, "id", id);
    }

    public static String secretariatQueue() {
        return SECRETARIAT_BASE + PATH_SECRETARIAT_QUEUE;
    }

    public static String secretariatQueueSummary() {
        return SECRETARIAT_BASE + PATH_SECRETARIAT_QUEUE_SUMMARY;
    }

    public static String secretariatQueuePanel() {
        return SECRETARIAT_BASE + PATH_SECRETARIAT_QUEUE_PANEL;
    }

    public static String secretariatQueuePanel(String inboxKey) {
        return withParams(secretariatQueuePanel(), orderedMap("inboxKey", inboxKey));
    }

    public static String secretariatQueueAgenda() {
        return SECRETARIAT_BASE + PATH_SECRETARIAT_QUEUE_AGENDA;
    }

    public static String secretariatQueueAgenda(String inboxKey) {
        return withParams(secretariatQueueAgenda(), orderedMap("inboxKey", inboxKey));
    }

    public static String secretariatQueueGovernance() {
        return SECRETARIAT_BASE + PATH_SECRETARIAT_QUEUE_GOVERNANCE;
    }

    public static String secretariatQueueGovernance(String inboxKey) {
        return withParams(secretariatQueueGovernance(), orderedMap("inboxKey", inboxKey));
    }

    public static String secretariatQueueExceptions() {
        return SECRETARIAT_BASE + PATH_SECRETARIAT_QUEUE_EXCEPTIONS;
    }

    public static String secretariatQueueExceptions(String inboxKey) {
        return withParams(secretariatQueueExceptions(), orderedMap("inboxKey", inboxKey));
    }

    public static String secretariatQueueCoverage() {
        return SECRETARIAT_BASE + PATH_SECRETARIAT_QUEUE_COVERAGE;
    }

    public static String secretariatQueueCoverage(String inboxKey) {
        return withParams(secretariatQueueCoverage(), orderedMap("inboxKey", inboxKey));
    }

    public static String secretariatQueueFormalCatalog() {
        return SECRETARIAT_BASE + PATH_SECRETARIAT_QUEUE_FORMAL_CATALOG;
    }

    public static String secretariatQueueFormalCatalog(String inboxKey) {
        return withParams(secretariatQueueFormalCatalog(), orderedMap("inboxKey", inboxKey));
    }

    public static String secretariatQueueVenueConfirmation(Long workItemId) {
        return resolvePathVariable(SECRETARIAT_BASE + PATH_SECRETARIAT_QUEUE_VENUE_CONFIRMATION, "workItemId", workItemId);
    }

    public static String secretariatQueueParticipantNotification(Long workItemId) {
        return resolvePathVariable(SECRETARIAT_BASE + PATH_SECRETARIAT_QUEUE_PARTICIPANT_NOTIFICATION, "workItemId", workItemId);
    }

    public static String secretariatQueueParticipantNotificationChallenge(Long workItemId) {
        return resolvePathVariable(SECRETARIAT_BASE + PATH_SECRETARIAT_QUEUE_PARTICIPANT_NOTIFICATION_CHALLENGE, "workItemId", workItemId);
    }

    public static String secretariatQueueAttendance(Long workItemId) {
        return resolvePathVariable(SECRETARIAT_BASE + PATH_SECRETARIAT_QUEUE_ATTENDANCE, "workItemId", workItemId);
    }

    public static String secretariatQueueCompletionEvent(Long workItemId) {
        return resolvePathVariable(SECRETARIAT_BASE + PATH_SECRETARIAT_QUEUE_COMPLETION_EVENT, "workItemId", workItemId);
    }

    public static String secretariatQueueProcessReturn(Long workItemId) {
        return resolvePathVariable(SECRETARIAT_BASE + PATH_SECRETARIAT_QUEUE_PROCESS_RETURN, "workItemId", workItemId);
    }

    public static String secretariatCredentialSecurity() {
        return SECRETARIAT_CREDENTIAL_SECURITY_BASE + PATH_SECRETARIAT_CREDENTIAL_SECURITY;
    }

    public static String secretariatCredentialChallenge(String functionCode) {
        return resolvePathVariable(SECRETARIAT_CREDENTIAL_SECURITY_BASE + PATH_SECRETARIAT_CREDENTIAL_CHALLENGE, "functionCode", functionCode);
    }

    public static String secretariatCredentialPassword(String functionCode) {
        return resolvePathVariable(SECRETARIAT_CREDENTIAL_SECURITY_BASE + PATH_SECRETARIAT_CREDENTIAL_PASSWORD, "functionCode", functionCode);
    }

    public static String secretariatCredentialUnlock(String functionCode) {
        return resolvePathVariable(SECRETARIAT_CREDENTIAL_SECURITY_BASE + PATH_SECRETARIAT_CREDENTIAL_UNLOCK, "functionCode", functionCode);
    }

    public static String oficialCredentialSecurity() {
        return OFICIAL_JUSTICA_CREDENTIAL_SECURITY_BASE + PATH_OFICIAL_JUSTICA_CREDENTIAL_SECURITY;
    }

    public static String oficialCredentialChallenge(String functionCode) {
        return resolvePathVariable(OFICIAL_JUSTICA_CREDENTIAL_SECURITY_BASE + PATH_OFICIAL_JUSTICA_CREDENTIAL_CHALLENGE, "functionCode", functionCode);
    }

    public static String oficialCredentialPassword(String functionCode) {
        return resolvePathVariable(OFICIAL_JUSTICA_CREDENTIAL_SECURITY_BASE + PATH_OFICIAL_JUSTICA_CREDENTIAL_PASSWORD, "functionCode", functionCode);
    }

    public static String oficialCredentialUnlock(String functionCode) {
        return resolvePathVariable(OFICIAL_JUSTICA_CREDENTIAL_SECURITY_BASE + PATH_OFICIAL_JUSTICA_CREDENTIAL_UNLOCK, "functionCode", functionCode);
    }

    public static String institutionalSupportSnapshot(String branchCode) {
        return resolvePathVariable(INSTITUTIONAL_SUPPORT_BASE + PATH_INSTITUTIONAL_SUPPORT_BRANCH_SNAPSHOT, "branchCode", branchCode);
    }

    public static String institutionalSupportAgenda(String branchCode) {
        return resolvePathVariable(INSTITUTIONAL_SUPPORT_BASE + PATH_INSTITUTIONAL_SUPPORT_BRANCH_AGENDA, "branchCode", branchCode);
    }

    public static String institutionalSupportCredentialSecurity(String branchCode) {
        return resolvePathVariable(INSTITUTIONAL_SUPPORT_BASE + PATH_INSTITUTIONAL_SUPPORT_BRANCH_CREDENTIAL_SECURITY, "branchCode", branchCode);
    }

    public static String institutionalSupportCredentialChallenge(String branchCode, String functionCode) {
        return resolvePathVariable(resolvePathVariable(INSTITUTIONAL_SUPPORT_BASE + PATH_INSTITUTIONAL_SUPPORT_BRANCH_CREDENTIAL_CHALLENGE, "branchCode", branchCode), "functionCode", functionCode);
    }

    public static String institutionalSupportCredentialPassword(String branchCode, String functionCode) {
        return resolvePathVariable(resolvePathVariable(INSTITUTIONAL_SUPPORT_BASE + PATH_INSTITUTIONAL_SUPPORT_BRANCH_CREDENTIAL_PASSWORD, "branchCode", branchCode), "functionCode", functionCode);
    }

    public static String institutionalSupportCredentialUnlock(String branchCode, String functionCode) {
        return resolvePathVariable(resolvePathVariable(INSTITUTIONAL_SUPPORT_BASE + PATH_INSTITUTIONAL_SUPPORT_BRANCH_CREDENTIAL_UNLOCK, "branchCode", branchCode), "functionCode", functionCode);
    }

    public static String institutionalSupportCompetenceMatrix(String branchCode) {
        return resolvePathVariable(INSTITUTIONAL_SUPPORT_BASE + PATH_INSTITUTIONAL_SUPPORT_BRANCH_COMPETENCE_MATRIX, "branchCode", branchCode);
    }

    public static String institutionalSupportCoverage(String branchCode) {
        return resolvePathVariable(INSTITUTIONAL_SUPPORT_BASE + PATH_INSTITUTIONAL_SUPPORT_BRANCH_COVERAGE, "branchCode", branchCode);
    }

    public static String institutionalSupportProcessPrePauta(String branchCode, Long processoId) {
        return resolvePathVariable(
                resolvePathVariable(INSTITUTIONAL_SUPPORT_BASE + PATH_INSTITUTIONAL_SUPPORT_BRANCH_PROCESS_PREPAUTA, "branchCode", branchCode),
                "processoId",
                processoId
        );
    }

    public static String institutionalCredentialGovernance() {
        return INSTITUTIONAL_CREDENTIAL_GOVERNANCE_BASE + PATH_INSTITUTIONAL_CREDENTIAL_GOVERNANCE;
    }

    public static String institutionalCredentialGovernanceTarget(Long targetUserId) {
        return resolvePathVariable(INSTITUTIONAL_CREDENTIAL_GOVERNANCE_BASE + PATH_INSTITUTIONAL_CREDENTIAL_GOVERNANCE_TARGET, "targetUserId", targetUserId);
    }

    public static String secretariatStream() {
        return SECRETARIAT_BASE + PATH_SECRETARIAT_STREAM;
    }

    public static String secretariatDossie(Long processoId) {
        return resolvePathVariable(SECRETARIAT_BASE + PATH_SECRETARIAT_DOSSIE, "processoId", processoId);
    }

    public static String secretariatMinutaJuntadaPdf(Long processoId) {
        return resolvePathVariable(SECRETARIAT_BASE + PATH_SECRETARIAT_PROCESSO_MINUTA_JUNTADA_PDF, "processoId", processoId);
    }

    public static String secretariatJuntadas(Long processoId) {
        return resolvePathVariable(SECRETARIAT_BASE + PATH_SECRETARIAT_PROCESSO_JUNTADAS, "processoId", processoId);
    }

    public static String secretariatJulgamentoProcesso(Long processoId) {
        return resolvePathVariable(SECRETARIAT_JULGAMENTOS_BASE + PATH_SECRETARIAT_JULGAMENTO_PROCESSO, "processoId", processoId);
    }

    public static String secretariatJulgamentoStatus(Long julgamentoId) {
        return resolvePathVariable(SECRETARIAT_JULGAMENTOS_BASE + PATH_SECRETARIAT_JULGAMENTO_STATUS, "julgamentoId", julgamentoId);
    }

    public static String secretariatJulgamentoVotos(Long julgamentoId) {
        return resolvePathVariable(SECRETARIAT_JULGAMENTOS_BASE + PATH_SECRETARIAT_JULGAMENTO_VOTOS, "julgamentoId", julgamentoId);
    }

    public static String secretariatJulgamentoAcordao(Long julgamentoId) {
        return resolvePathVariable(SECRETARIAT_JULGAMENTOS_BASE + PATH_SECRETARIAT_JULGAMENTO_ACORDAO, "julgamentoId", julgamentoId);
    }

    public static String secretariatOperationalSnapshot() {
        return SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_SNAPSHOT;
    }

    public static String secretariatOperationalSnapshot(String unidadeCodigo) {
        return withParams(secretariatOperationalSnapshot(), orderedMap("unidadeCodigo", unidadeCodigo));
    }

    public static String secretariatOperationalJuntada(Long processoId) {
        return resolvePathVariable(SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_PROCESS_JUNTADA, "processoId", processoId);
    }

    public static String secretariatOperationalIntimacao(Long processoId) {
        return resolvePathVariable(SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_PROCESS_INTIMACAO, "processoId", processoId);
    }

    public static String secretariatOperationalConclusao(Long processoId) {
        return resolvePathVariable(SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_PROCESS_CONCLUSAO, "processoId", processoId);
    }

    public static String secretariatOperationalMandadoCitacao(Long processoId) {
        return resolvePathVariable(SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_PROCESS_MANDADO_CITACAO, "processoId", processoId);
    }

    public static String secretariatOperationalQueueSaneamento() {
        return SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_QUEUE_SANEAMENTO;
    }

    public static String secretariatOperationalOfficialClosures() {
        return SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_OFFICIAL_CLOSURES;
    }

    public static String secretariatOperationalOfficialClosures(String inboxKey) {
        return withParams(secretariatOperationalOfficialClosures(), orderedMap("inboxKey", inboxKey));
    }

    public static String secretariatOperationalOfficialClosureReclassify(Long deskWorkItemId) {
        return resolvePathVariable(SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_OFFICIAL_CLOSURE_RECLASSIFY, "deskWorkItemId", deskWorkItemId);
    }

    public static String secretariatOperationalOfficialClosureNextProvidence(Long deskWorkItemId) {
        return resolvePathVariable(SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_OFFICIAL_CLOSURE_NEXT_PROVIDENCE, "deskWorkItemId", deskWorkItemId);
    }

    public static String secretariatOperationalOfficialClosureMaterializeAct(Long deskWorkItemId) {
        return resolvePathVariable(SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_OFFICIAL_CLOSURE_MATERIALIZE_ACT, "deskWorkItemId", deskWorkItemId);
    }

    public static String secretariatOperationalOfficialClosureDrawers() {
        return SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_OFFICIAL_CLOSURE_DRAWERS;
    }

    public static String secretariatOperationalOfficialClosureDrawers(String inboxKey) {
        return withParams(secretariatOperationalOfficialClosureDrawers(), orderedMap("inboxKey", inboxKey));
    }

    public static String secretariatOperationalOfficialClosureDrawerDetail() {
        return SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_OFFICIAL_CLOSURE_DRAWER_DETAIL;
    }

    public static String secretariatOperationalOfficialClosureDrawerDetail(String inboxKey, String drawerKey) {
        return withParams(secretariatOperationalOfficialClosureDrawerDetail(), orderedMap("inboxKey", inboxKey, "drawerKey", drawerKey));
    }

    public static String secretariatOperationalBreakGlass(Long processoId) {
        return resolvePathVariable(SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_BREAK_GLASS, "processoId", processoId);
    }

    public static String secretariatOperationalVisibilidadePessoal(Long processoId) {
        return resolvePathVariable(SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_VISIBILIDADE_PESSOAL, "processoId", processoId);
    }

    public static String secretariatOperationalCollegiatePauta(Long processoId) {
        return resolvePathVariable(SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_COLLEGIATE_PAUTA, "processoId", processoId);
    }

    public static String secretariatOperationalCollegiatePublication(Long julgamentoId) {
        return resolvePathVariable(SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_COLLEGIATE_PUBLICATION, "julgamentoId", julgamentoId);
    }

    public static String secretariatOperationalCollegiateSustentacao(Long julgamentoId) {
        return resolvePathVariable(SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_COLLEGIATE_SUSTENTACAO, "julgamentoId", julgamentoId);
    }

    public static String secretariatOperationalCollegiateAcordao(Long julgamentoId) {
        return resolvePathVariable(SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_COLLEGIATE_ACORDAO, "julgamentoId", julgamentoId);
    }

    public static String secretariatOperationalCollegiateBaixa(Long julgamentoId) {
        return resolvePathVariable(SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_COLLEGIATE_BAIXA, "julgamentoId", julgamentoId);
    }


    public static String judgeGabineteDespacho(Long processoId) {
        return JUDGE_GABINETE_DECISOES_BASE + "/processos/" + valueOrPlaceholder(processoId, "processoId") + "/despacho";
    }

    public static String judgeGabineteDecisaoInterlocutoria(Long processoId) {
        return JUDGE_GABINETE_DECISOES_BASE + "/processos/" + valueOrPlaceholder(processoId, "processoId") + "/decisao-interlocutoria";
    }

    public static String judgeGabineteSentenca(Long processoId) {
        return JUDGE_GABINETE_DECISOES_BASE + "/processos/" + valueOrPlaceholder(processoId, "processoId") + "/sentenca";
    }

    public static String judgeGabineteAudiencia(Long processoId) {
        return JUDGE_GABINETE_DECISOES_BASE + "/processos/" + valueOrPlaceholder(processoId, "processoId") + "/audiencia";
    }

    public static String judgeGabineteOrdemCumprimentoOficial(Long processoId) {
        return JUDGE_GABINETE_DECISOES_BASE + "/processos/" + valueOrPlaceholder(processoId, "processoId") + "/ordem-cumprimento-oficial";
    }

    public static String judgeGabineteCertidaoTransitoJulgado(Long processoId) {
        return JUDGE_GABINETE_DECISOES_BASE + "/processos/" + valueOrPlaceholder(processoId, "processoId") + "/certidao-tj";
    }

    public static String desembargadorColegiadoMalhaProcesso(Long processoId) {
        return DESEMBARGADOR_COLEGIADO_BASE + "/processos/" + valueOrPlaceholder(processoId, "processoId") + "/malha";
    }

    public static String desembargadorColegiadoVoto(Long processoId) {
        return DESEMBARGADOR_COLEGIADO_BASE + "/processos/" + valueOrPlaceholder(processoId, "processoId") + "/voto";
    }

    public static String desembargadorColegiadoAcordao(Long processoId) {
        return DESEMBARGADOR_COLEGIADO_BASE + "/processos/" + valueOrPlaceholder(processoId, "processoId") + "/acordao";
    }

    public static String desembargadorColegiadoVista(Long processoId) {
        return DESEMBARGADOR_COLEGIADO_BASE + "/processos/" + valueOrPlaceholder(processoId, "processoId") + "/vista";
    }

    public static String desembargadorColegiadoDestaque(Long processoId) {
        return DESEMBARGADOR_COLEGIADO_BASE + "/processos/" + valueOrPlaceholder(processoId, "processoId") + "/destaque";
    }

    public static String desembargadorPlenarioRelator(Long sessaoId) {
        return DESEMBARGADOR_PLENARIO_BASE + "/sessoes/" + valueOrPlaceholder(sessaoId, "sessaoId") + "/relator";
    }

    public static String ministroPlenarioMalhaProcesso(Long processoId) {
        return MINISTRO_PLENARIO_BASE + "/processos/" + valueOrPlaceholder(processoId, "processoId") + "/malha";
    }

    public static String ministroPlenarioDecisaoMonocratica(Long processoId) {
        return MINISTRO_PLENARIO_BASE + "/processos/" + valueOrPlaceholder(processoId, "processoId") + "/decisao-monocratica";
    }

    public static String ministroPlenarioPauta(Long processoId) {
        return MINISTRO_PLENARIO_BASE + "/processos/" + valueOrPlaceholder(processoId, "processoId") + "/pauta";
    }

    public static String ministroPlenarioDecisaoPlenaria(Long processoId) {
        return MINISTRO_PLENARIO_BASE + "/processos/" + valueOrPlaceholder(processoId, "processoId") + "/decisao-plenaria";
    }

    public static String secretariatOperationalElectoralCorregedoria(Long processoId) {
        return resolvePathVariable(SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_ELECTORAL_CORREGEDORIA, "processoId", processoId);
    }

    public static String secretariatOperationalElectoralInspecao(Long processoId) {
        return resolvePathVariable(SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_ELECTORAL_INSPECAO, "processoId", processoId);
    }

    public static String secretariatOperationalElectoralPesquisa(Long processoId) {
        return resolvePathVariable(SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_ELECTORAL_PESQUISA, "processoId", processoId);
    }

    public static String secretariatOperationalLabourMidiaRecebimento(Long processoId) {
        return resolvePathVariable(SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_LABOUR_MIDIA_RECEBIMENTO, "processoId", processoId);
    }

    public static String secretariatOperationalLabourMidiaDisponibilizacao(Long processoId) {
        return resolvePathVariable(SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_LABOUR_MIDIA_DISPONIBILIZACAO, "processoId", processoId);
    }

    public static String secretariatOperationalLabourExecucao(Long processoId) {
        return resolvePathVariable(SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_LABOUR_EXECUCAO, "processoId", processoId);
    }

    public static String secretariatOperationalMilitaryPlantao(Long processoId) {
        return resolvePathVariable(SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_MILITARY_PLANTAO, "processoId", processoId);
    }

    public static String secretariatOperationalMilitaryBalcao(Long processoId) {
        return resolvePathVariable(SECRETARIAT_OPERATIONAL_BASE + PATH_SECRETARIAT_OPERATIONAL_MILITARY_BALCAO, "processoId", processoId);
    }

    public static String oficialJusticaAgendaOperacional() {
        return OFICIAL_JUSTICA_BASE + PATH_OFICIAL_JUSTICA_AGENDA_OPERACIONAL;
    }

    public static String oficialJusticaNamedProcessWorkbench(Long processoId) {
        return resolvePathVariable(OFICIAL_JUSTICA_BASE + PATH_OFICIAL_JUSTICA_NAMED_PROCESS_WORKBENCH, "processoId", processoId);
    }

    public static String oficialJusticaBalcaoVirtualRoom(Long processoId) {
        return resolvePathVariable(OFICIAL_JUSTICA_BASE + PATH_OFICIAL_JUSTICA_BALCAO_VIRTUAL_ROOM, "processoId", processoId);
    }

    public static String oficialJusticaCienteIntimacao(Long processoId) {
        return resolvePathVariable(OFICIAL_JUSTICA_BASE + PATH_OFICIAL_JUSTICA_CIENTE_INTIMACAO, "processoId", processoId);
    }

    public static String oficialJusticaCienteIntimacaoChallenge(Long processoId) {
        return resolvePathVariable(OFICIAL_JUSTICA_BASE + PATH_OFICIAL_JUSTICA_CIENTE_INTIMACAO_CHALLENGE, "processoId", processoId);
    }

    public static String processualParticipacaoWorkspace(Long processoId) {
        return resolvePathVariable(PROCESSUAL_PARTICIPACAO_ATIVA_BASE + PATH_PROCESSUAL_PARTICIPACAO_WORKSPACE, "processoId", processoId);
    }

    public static String processualParticipacaoProtocolar(Long processoId) {
        return resolvePathVariable(PROCESSUAL_PARTICIPACAO_ATIVA_BASE + PATH_PROCESSUAL_PARTICIPACAO_PROTOCOLAR, "processoId", processoId);
    }

    public static String processualParticipacaoSubmissoes(Long processoId) {
        return resolvePathVariable(PROCESSUAL_PARTICIPACAO_ATIVA_BASE + PATH_PROCESSUAL_PARTICIPACAO_SUBMISSOES, "processoId", processoId);
    }

    public static String judgeGabineteDecisoes() {
        return JUDGE_GABINETE_DECISOES_BASE;
    }

    public static String judgeGabinetePainel(String justiceAxis, String tribunalAxis) {
        return withParams(JUDGE_GABINETE_DECISOES_BASE, orderedMap(
                "justica", justiceAxis,
                "tribunal", tribunalAxis
        ));
    }

    public static String judgeGabinetePainel(String justiceAxis,
                                             String tribunalAxis,
                                             String unidadeCodigo,
                                             String caixaCodigo,
                                             String orgao) {
        return withParams(JUDGE_GABINETE_DECISOES_BASE, orderedMap(
                "justica", justiceAxis,
                "tribunal", tribunalAxis,
                "unidadeCodigo", unidadeCodigo,
                "caixaCodigo", caixaCodigo,
                "orgao", orgao
        ));
    }

    public static String desembargadorPainel(String justiceAxis, String tribunalAxis) {
        return withParams(DESEMBARGADOR_COLEGIADO_BASE, orderedMap(
                "justica", justiceAxis,
                "tribunal", tribunalAxis
        ));
    }

    public static String desembargadorPainel(String justiceAxis,
                                             String tribunalAxis,
                                             String unidadeCodigo,
                                             String caixaCodigo,
                                             String orgao) {
        return withParams(DESEMBARGADOR_COLEGIADO_BASE, orderedMap(
                "justica", justiceAxis,
                "tribunal", tribunalAxis,
                "unidadeCodigo", unidadeCodigo,
                "caixaCodigo", caixaCodigo,
                "orgao", orgao
        ));
    }

    public static String ministroPlenarioPainel(String tribunalAxis) {
        return withParams(MINISTRO_PLENARIO_BASE, orderedMap(
                "tribunal", tribunalAxis
        ));
    }

    public static String ministroPlenarioPainel(String tribunalAxis,
                                                String unidadeCodigo,
                                                String caixaCodigo,
                                                String orgao) {
        return withParams(MINISTRO_PLENARIO_BASE, orderedMap(
                "tribunal", tribunalAxis,
                "unidadeCodigo", unidadeCodigo,
                "caixaCodigo", caixaCodigo,
                "orgao", orgao
        ));
    }

    public static String judgeOfficialReturnSuggestion(Long gabineteWorkItemId) {
        return resolvePathVariable(JUDGE_GABINETE_DECISOES_BASE + PATH_JUDGE_OFFICIAL_RETURN_SUGGESTION, "gabineteWorkItemId", gabineteWorkItemId);
    }

    public static String judgeOfficialReturnApproveMinuta(Long gabineteWorkItemId) {
        return resolvePathVariable(JUDGE_GABINETE_DECISOES_BASE + PATH_JUDGE_OFFICIAL_RETURN_APPROVE_MINUTA, "gabineteWorkItemId", gabineteWorkItemId);
    }

    public static String judgeOfficialReturnApproveReexpedicao(Long gabineteWorkItemId) {
        return resolvePathVariable(JUDGE_GABINETE_DECISOES_BASE + PATH_JUDGE_OFFICIAL_RETURN_APPROVE_REEXPEDICAO, "gabineteWorkItemId", gabineteWorkItemId);
    }

    public static String judgeOfficialReturnReject(Long gabineteWorkItemId) {
        return resolvePathVariable(JUDGE_GABINETE_DECISOES_BASE + PATH_JUDGE_OFFICIAL_RETURN_REJECT, "gabineteWorkItemId", gabineteWorkItemId);
    }

    public static String processualPendenciasPainel() {
        return PROCESSUAL_PENDENCIAS_BASE + PATH_PROCESSUAL_PENDENCIAS_PAINEL;
    }

    public static List<String> operationalBases() {
        return OPERATIONAL_BASES;
    }

    public static String withOperationalContext(String basePath, Map<String, ?> params) {
        return withParams(basePath, params);
    }

    public static boolean isOperationalPath(String path) {
        String normalized = normalizePath(path);
        if (normalized == null) {
            return false;
        }
        return OPERATIONAL_BASES.stream()
                .map(OperationalApiRoutes::normalizePath)
                .filter(Objects::nonNull)
                .anyMatch(base -> normalized.equals(base) || normalized.startsWith(base + "/"));
    }

    private static String resolvePathVariable(String template, String variable, Object value) {
        Objects.requireNonNull(template, "template");
        String resolved = value == null ? "" : String.valueOf(value).trim();
        return normalizePath(template.replace("{" + variable + "}", encodeSegment(resolved)));
    }

    private static String withParams(String basePath, Map<String, ?> params) {
        String normalized = normalizePath(basePath);
        if (normalized == null || params == null || params.isEmpty()) {
            return normalized;
        }
        StringBuilder builder = new StringBuilder(normalized);
        boolean first = !normalized.contains("?");
        for (Map.Entry<String, ?> entry : params.entrySet()) {
            if (entry.getKey() == null || entry.getKey().isBlank() || entry.getValue() == null) {
                continue;
            }
            String value = String.valueOf(entry.getValue()).trim();
            if (value.isEmpty()) {
                continue;
            }
            builder.append(first ? '?' : '&');
            first = false;
            builder.append(encodeQuery(entry.getKey().trim()));
            builder.append('=');
            builder.append(encodeQuery(value));
        }
        return builder.toString();
    }

    private static String valueOrPlaceholder(Object value, String placeholder) {
        return value == null ? "{" + placeholder + "}" : String.valueOf(value);
    }

    private static LinkedHashMap<String, Object> orderedMap(Object... values) {
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();
        if (values == null || values.length == 0) {
            return map;
        }
        if (values.length % 2 != 0) {
            throw new IllegalArgumentException("orderedMap requer pares chave/valor");
        }
        for (int index = 0; index < values.length; index += 2) {
            Object key = values[index];
            if (key != null) {
                map.put(String.valueOf(key), values[index + 1]);
            }
        }
        return map;
    }

    private static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String normalized = path.trim().replaceAll("/+", "/");
        if (!normalized.startsWith("/")) {
            normalized = '/' + normalized;
        }
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String encodeSegment(String value) {
        return value.replace("/", "%2F");
    }

    private static String encodeQuery(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
