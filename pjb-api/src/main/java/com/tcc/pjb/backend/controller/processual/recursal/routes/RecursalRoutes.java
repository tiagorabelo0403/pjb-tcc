package com.tcc.pjb.backend.controller.processual.recursal.routes;

public final class RecursalRoutes {

    public static final String BASE = "/api/v1/processual/recursal";
    public static final String FOUNDATION = "/foundation";
    public static final String EMBARGOS_DECLARACAO = "/embargos/declaracao";
    public static final String EMBARGOS_DECLARACAO_PREVIEW = EMBARGOS_DECLARACAO + "/preview";
    public static final String AUTOMATION_ADVISE = "/automation/advise";
    public static final String AUTOMATION_PLAYBOOK = "/automation/playbook";
    public static final String AUTOMATION_WORKSPACE = "/automation/workspace";
    public static final String SURFACES_OPERATIONAL = "/surfaces/operational";
    public static final String SURFACES_ATTORNEY = "/surfaces/attorney";
    public static final String SURFACES_INSTITUTIONAL = "/surfaces/institutional";
    public static final String SURFACES_DOCUMENTAL = "/surfaces/documental";
    public static final String SURFACES_INTELLIGENCE = "/surfaces/intelligence";
    public static final String DOCUMENT_VIEWER = "/document-viewer";
    public static final String DOCUMENT_AUTHENTICITY = "/document-authenticity";
    public static final String DOCUMENT_SIGNATURE_EVIDENCE = "/document-signature-evidence";
    public static final String NOTIFICATION_MOBILE_PREVIEW = "/analytics/mobile-acompanhamento";
    public static final String NOTIFICATION_GOVERNANCE = "/analytics/notifica-pendencias";
    public static final String NOTIFICATION_SCIENCE = "/notification/science";
    public static final String NOTIFICATION_PREFERENCES_FINE = "/notification/preferences/fine";
    public static final String NOTIFICATION_FEDERATED_DELIVERY = "/notification/federated-delivery";
    public static final String NOTIFICATION_MOBILE_POSTURE = "/notification/mobile/posture";
    public static final String NOTIFICATION_MOBILE_EXTERNAL_HARDENING = "/notification/mobile/external-delivery/hardened";

    private RecursalRoutes() {
    }
}
