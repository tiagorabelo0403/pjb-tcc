package com.tcc.pjb.backend.configs.security.perimeter;

final class ApiRequestOriginGovernanceMessages {

    static final String TYPE_PREFIX = "https://pjb.local/problems/";
    static final String TITLE = "Origin Governance Rejected";
    static final String CODE_REQUIRED = "origin_attestation_required";
    static final String DETAIL_REQUIRED = "Requisicao mutavel governada exige origem conhecida na borda ou atestacao assinada.";
    static final String CODE_BROWSER_ORIGIN = "browser_origin_not_allowed";
    static final String DETAIL_BROWSER_ORIGIN = "Origin/Referer nao pertence a uma origem confiavel configurada na borda.";
    static final String CODE_SIGNED_ORIGIN_ID = "signed_origin_id_required";
    static final String DETAIL_SIGNED_ORIGIN_ID = "Cabecalho X-PJB-Origin-Id eh obrigatorio para atestacao assinada.";
    static final String CODE_SIGNED_TIMESTAMP = "signed_origin_timestamp_invalid";
    static final String DETAIL_SIGNED_TIMESTAMP = "Cabecalho X-PJB-Timestamp ausente, invalido ou fora da janela soberana permitida.";
    static final String CODE_SIGNED_SIGNATURE = "signed_origin_signature_invalid";
    static final String DETAIL_SIGNED_SIGNATURE = "Assinatura de origem da borda nao confere com o material atestado.";
    static final String CODE_SIGNED_ALGORITHM = "signed_origin_algorithm_not_allowed";
    static final String DETAIL_SIGNED_ALGORITHM = "Algoritmo de assinatura da borda nao permitido para esta governanca.";
    static final String CODE_SIGNED_BODY_HASH = "signed_origin_body_hash_required";
    static final String DETAIL_SIGNED_BODY_HASH = "Body hash governado eh obrigatorio para payload JSON assinado.";
    static final String CODE_SIGNED_BODY_HASH_MISMATCH = "signed_origin_body_hash_mismatch";
    static final String DETAIL_SIGNED_BODY_HASH_MISMATCH = "Body hash informado nao confere com o hash canonico validado na borda.";
    static final String CODE_SIGNED_ORIGIN_UNKNOWN = "signed_origin_unknown";
    static final String DETAIL_SIGNED_ORIGIN_UNKNOWN = "Origem assinada nao pertence ao registry de origens confiaveis.";
    static final String CODE_SIGNED_IP = "signed_origin_ip_forbidden";
    static final String DETAIL_SIGNED_IP = "Endereco IP nao pertence ao perimetro permitido para a origem assinada.";
    static final String CODE_SIGNED_PATH = "signed_origin_path_forbidden";
    static final String DETAIL_SIGNED_PATH = "Caminho nao pertence a faixa permitida para a origem assinada.";
    static final String CODE_SIGNED_METHOD = "signed_origin_method_forbidden";
    static final String DETAIL_SIGNED_METHOD = "Metodo HTTP nao pertence a faixa permitida para a origem assinada.";
    static final String CODE_SIGNED_BROWSER_ORIGIN = "signed_browser_origin_forbidden";
    static final String DETAIL_SIGNED_BROWSER_ORIGIN = "Origin/Referer nao pertence ao conjunto permitido para a origem assinada.";
    static final String CODE_SIGNED_CAPABILITY = "signed_attestation_required_for_capability";
    static final String DETAIL_SIGNED_CAPABILITY = "Capability juridica sensivel exige atestacao assinada de origem, mesmo sob browser governado na borda.";

    private ApiRequestOriginGovernanceMessages() {
    }
}
