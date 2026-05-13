package com.tcc.pjb.backend.service.processual.comunicacao.flow;

public final class NationalCommunicationFlowRoutes {

    public static final String CANONICAL_BASE = "/api/v1/processual/comunicacoes";
    public static final String PATH_DISPATCH = "/expedir";
    public static final String PATH_ACKNOWLEDGE = "/acuse";
    public static final String PATH_RESOLVE_CANONICAL_ACT = "/resolver-ato-canonico";
    public static final String PATH_RESOLVE_PROCESSUAL_RECIPIENT = "/resolver-destinatario-processual";
    public static final String PATH_RESOLVE_INSTITUTIONAL = "/resolver-institucional";
    public static final String PATH_RESOLVE_INSTITUTIONAL_ROUTING = "/resolver-roteamento-institucional";
    public static final String PATH_FALLBACK = "/fallback";
    public static final String PATH_DASHBOARD = "/painel";
    public static final String PATH_INSTITUTIONAL_BOXES = "/institucional/minhas-caixas";
    public static final String PATH_INSTITUTIONAL_AUTHORIZE_BOX = "/institucional/autorizar-caixa";
    public static final String PATH_INSTITUTIONAL_INBOX = "/institucional/inbox";
    public static final String PATH_INSTITUTIONAL_RECEIVE = "/institucional/receber";
    public static final String PATH_INSTITUTIONAL_REDISTRIBUTE = "/institucional/redistribuir";
    public static final String PATH_INSTITUTIONAL_CERTIFY_SCIENCE = "/institucional/certificar-ciencia";
    public static final String PATH_INSTITUTIONAL_FULFILL = "/institucional/cumprir";
    public static final String PATH_INSTITUTIONAL_TIMELINE = "/institucional/timeline";
    public static final String PATH_INSTITUTIONAL_SEMANTIC_TIMELINE = "/institucional/timeline-semantica";
    public static final String PATH_INSTITUTIONAL_PROOFS = "/institucional/provas";
    public static final String PATH_INSTITUTIONAL_GATES = "/institucional/gates";
    public static final String PATH_INSTITUTIONAL_DELIVERIES = "/institucional/entregas";
    public static final String PATH_INSTITUTIONAL_DLQ = "/institucional/dlq";
    public static final String PATH_INSTITUTIONAL_REPROCESS_DELIVERY = "/institucional/reprocessar-entrega";
    public static final String PATH_INSTITUTIONAL_EXTERNAL_INTEGRATIONS = "/institucional/integracoes-externas";
    public static final String PATH_INSTITUTIONAL_OBSERVABILITY = "/institucional/observabilidade";
    public static final String PATH_INSTITUTIONAL_ANALYTICS = "/institucional/analytics";
    public static final String PATH_INSTITUTIONAL_HARDENING = "/institucional/hardening";
    public static final String PATH_INSTITUTIONAL_DELEGATE = "/institucional/delegar";
    public static final String PATH_INSTITUTIONAL_SUBSTITUTE = "/institucional/substituir";
    public static final String PATH_INSTITUTIONAL_DELEGATIONS = "/institucional/delegacoes";
    public static final String PATH_INSTITUTIONAL_DRAFT_CREATE = "/institucional/minutas/criar";
    public static final String PATH_INSTITUTIONAL_DRAFT_SUBMIT = "/institucional/minutas/submeter";
    public static final String PATH_INSTITUTIONAL_DRAFT_APPROVE = "/institucional/minutas/aprovar";
    public static final String PATH_INSTITUTIONAL_DRAFT_REJECT = "/institucional/minutas/rejeitar";
    public static final String PATH_INSTITUTIONAL_DRAFTS = "/institucional/minutas";

    private NationalCommunicationFlowRoutes() {
    }

    public static String dispatch() { return CANONICAL_BASE + PATH_DISPATCH; }
    public static String acknowledge() { return CANONICAL_BASE + PATH_ACKNOWLEDGE; }
    public static String resolveCanonicalAct() { return CANONICAL_BASE + PATH_RESOLVE_CANONICAL_ACT; }
    public static String resolveProcessualRecipient() { return CANONICAL_BASE + PATH_RESOLVE_PROCESSUAL_RECIPIENT; }
    public static String resolveInstitutional() { return CANONICAL_BASE + PATH_RESOLVE_INSTITUTIONAL; }
    public static String resolveInstitutionalRouting() { return CANONICAL_BASE + PATH_RESOLVE_INSTITUTIONAL_ROUTING; }
    public static String fallback() { return CANONICAL_BASE + PATH_FALLBACK; }
    public static String dashboard() { return CANONICAL_BASE + PATH_DASHBOARD; }
    public static String institutionalBoxes() { return CANONICAL_BASE + PATH_INSTITUTIONAL_BOXES; }
    public static String institutionalAuthorizeBox() { return CANONICAL_BASE + PATH_INSTITUTIONAL_AUTHORIZE_BOX; }
    public static String institutionalInbox() { return CANONICAL_BASE + PATH_INSTITUTIONAL_INBOX; }
    public static String institutionalReceive() { return CANONICAL_BASE + PATH_INSTITUTIONAL_RECEIVE; }
    public static String institutionalRedistribute() { return CANONICAL_BASE + PATH_INSTITUTIONAL_REDISTRIBUTE; }
    public static String institutionalCertifyScience() { return CANONICAL_BASE + PATH_INSTITUTIONAL_CERTIFY_SCIENCE; }
    public static String institutionalFulfill() { return CANONICAL_BASE + PATH_INSTITUTIONAL_FULFILL; }
    public static String institutionalTimeline() { return CANONICAL_BASE + PATH_INSTITUTIONAL_TIMELINE; }
    public static String institutionalSemanticTimeline() { return CANONICAL_BASE + PATH_INSTITUTIONAL_SEMANTIC_TIMELINE; }
    public static String institutionalProofs() { return CANONICAL_BASE + PATH_INSTITUTIONAL_PROOFS; }
    public static String institutionalGates() { return CANONICAL_BASE + PATH_INSTITUTIONAL_GATES; }
    public static String institutionalDeliveries() { return CANONICAL_BASE + PATH_INSTITUTIONAL_DELIVERIES; }
    public static String institutionalDlq() { return CANONICAL_BASE + PATH_INSTITUTIONAL_DLQ; }
    public static String institutionalReprocessDelivery() { return CANONICAL_BASE + PATH_INSTITUTIONAL_REPROCESS_DELIVERY; }
    public static String institutionalExternalIntegrations() { return CANONICAL_BASE + PATH_INSTITUTIONAL_EXTERNAL_INTEGRATIONS; }
    public static String institutionalObservability() { return CANONICAL_BASE + PATH_INSTITUTIONAL_OBSERVABILITY; }
    public static String institutionalAnalytics() { return CANONICAL_BASE + PATH_INSTITUTIONAL_ANALYTICS; }
    public static String institutionalHardening() { return CANONICAL_BASE + PATH_INSTITUTIONAL_HARDENING; }
    public static String delegarInstitucional() { return CANONICAL_BASE + PATH_INSTITUTIONAL_DELEGATE; }
    public static String substituirInstitucional() { return CANONICAL_BASE + PATH_INSTITUTIONAL_SUBSTITUTE; }
    public static String delegacoesInstitucionais() { return CANONICAL_BASE + PATH_INSTITUTIONAL_DELEGATIONS; }
    public static String criarMinutaInstitucional() { return CANONICAL_BASE + PATH_INSTITUTIONAL_DRAFT_CREATE; }
    public static String submeterMinutaInstitucional() { return CANONICAL_BASE + PATH_INSTITUTIONAL_DRAFT_SUBMIT; }
    public static String aprovarMinutaInstitucional() { return CANONICAL_BASE + PATH_INSTITUTIONAL_DRAFT_APPROVE; }
    public static String rejeitarMinutaInstitucional() { return CANONICAL_BASE + PATH_INSTITUTIONAL_DRAFT_REJECT; }
    public static String listarMinutasInstitucionais() { return CANONICAL_BASE + PATH_INSTITUTIONAL_DRAFTS; }
}
