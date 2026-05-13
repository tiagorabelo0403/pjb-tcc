package com.tcc.pjb.backend.service.processual.comunicacao.institutional.surface;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NationalCommunicationInstitutionalHttpRoutes {

    public static final String CANONICAL_BASE = "/api/v1/institucional";
    public static final String PATH_PAINEL_EXECUTIVO = "/painel-executivo";
    public static final String PATH_INBOX = "/inbox";
    public static final String PATH_NOTIFICACOES = "/notificacoes";
    public static final String PATH_DLQ = "/dlq";
    public static final String PATH_SLA_PREDITIVO = "/sla-preditivo";
    public static final String PATH_GATES = "/gates";
    public static final String PATH_MINUTAS = "/minutas";
    public static final String PATH_PENDENTES_NAO_LEITURA = "/pendentes-nao-leitura";
    public static final String PATH_AFFILIATIONS = "/afiliacoes";
    public static final String PATH_OFFICIAL_SOURCE_CONNECTORS = "/fontes-oficiais/conectores";
    public static final String PATH_AFFILIATION_HOMOLOGATE = PATH_AFFILIATIONS + "/{affiliationId}/homologar";
    public static final String PATH_AFFILIATION_PUBLIC_RECOGNITION = PATH_AFFILIATIONS + "/{affiliationId}/reconhecimento-publico";
    public static final String PATH_AFFILIATION_OFFICIAL_SOURCE_DOSSIER = PATH_AFFILIATIONS + "/{affiliationId}/dossie-fontes-oficiais";
    public static final String PATH_AFFILIATION_OFFICIAL_IDENTIFIER_DOSSIER = PATH_AFFILIATIONS + "/{affiliationId}/identificadores-oficiais";
    public static final String PATH_AFFILIATION_OFFICIAL_SOURCE_ATTESTATION = PATH_AFFILIATIONS + "/{affiliationId}/atestacao-fontes-oficiais";
    public static final String PATH_AFFILIATION_OFFICIAL_SOURCE_REVALIDATE = PATH_AFFILIATIONS + "/{affiliationId}/revalidar-fontes-oficiais";
    public static final String PATH_AFFILIATION_ONBOARDING_PLAN = PATH_AFFILIATIONS + "/{affiliationId}/plano-onboarding";
    public static final String PATH_AFFILIATION_AUTHENTICATION_POLICY = PATH_AFFILIATIONS + "/{affiliationId}/politica-autenticacao";
    public static final String PATH_AFFILIATION_OPERATIONAL_PROVISIONING = PATH_AFFILIATIONS + "/{affiliationId}/provisionamento-operacional";
    public static final String PATH_AFFILIATION_OPERATIONAL_PROVISION = PATH_AFFILIATIONS + "/{affiliationId}/provisionar-operacional";
    public static final String PATH_AFFILIATION_MANAGED_CREDENTIALS = PATH_AFFILIATIONS + "/{affiliationId}/credenciais-gerenciadas";
    public static final String PATH_AFFILIATION_MANAGED_CREDENTIAL_REVOKE = PATH_AFFILIATION_MANAGED_CREDENTIALS + "/{credentialId}/revogar";
    public static final String PATH_AFFILIATION_ROOT_ADMIN_APPROVAL = PATH_AFFILIATIONS + "/{affiliationId}/aprovacao-administrador-raiz";
    public static final String PATH_AFFILIATION_STRONG_SIGNATURE_POLICY = PATH_AFFILIATIONS + "/{affiliationId}/assinatura-forte";
    public static final String PATH_AFFILIATION_UNIT_GOVERNANCE = PATH_AFFILIATIONS + "/{affiliationId}/governanca-unidades";
    public static final String PATH_AFFILIATION_UNIT_GOVERNANCE_UNITS = PATH_AFFILIATION_UNIT_GOVERNANCE + "/unidades";
    public static final String PATH_AFFILIATION_UNIT_GOVERNANCE_LOTACOES = PATH_AFFILIATION_UNIT_GOVERNANCE + "/lotacoes";
    public static final String PATH_AFFILIATION_WORKLOAD_IDENTITY = PATH_AFFILIATIONS + "/{affiliationId}/identidade-workload";
    public static final String PATH_AFFILIATION_COVERAGE_DELEGATIONS = PATH_AFFILIATIONS + "/{affiliationId}/delegacoes-cobertura";
    public static final String PATH_AFFILIATION_API_EDGE_PROFILE = PATH_AFFILIATIONS + "/{affiliationId}/perfil-seguranca-api";
    public static final String PATH_AFFILIATION_ACCESS_CONTEXT = PATH_AFFILIATIONS + "/{affiliationId}/contexto-acesso";
    public static final String PATH_NOMINATIONS = "/nomeacoes";
    public static final String PATH_SECURE_ENTRY = "/entrada-segura";
    public static final String PATH_ACCESS_CATALOG = "/catalogo-acessos";
    public static final String PATH_BLUEPRINTS = "/blueprints";
    public static final String PATH_RECERTIFICATIONS = "/recertificacoes";
    public static final String PATH_AFFILIATION_RECERTIFY = PATH_AFFILIATIONS + "/{affiliationId}/recertificar";
    public static final String PATH_AFFILIATION_REVOKE_ACCESS = PATH_AFFILIATIONS + "/{affiliationId}/revogar-acessos";
    public static final String PATH_GOVERNANCE_INTEGRATIONS = "/integracoes-governanca";
    public static final String PATH_FOUR_LEVELS = "/quatro-niveis";
    public static final String PATH_AFFILIATION_OPERATIONAL_CASES = PATH_AFFILIATIONS + "/{affiliationId}/casos-operacionais";
    public static final String PATH_STRUCTURAL_DIAGNOSTIC = "/diagnostico-estrutural";
    public static final String PATH_MODELO_OPERACIONAL = "/modelo-operacional";
    public static final String PATH_ENTRADA_INTELIGENTE = "/entrada-inteligente";
    public static final String PATH_CONTEXTOS_ENTRADA = "/contextos-entrada";
    public static final String PATH_PANEL_ORGAO = "/painel-orgao";
    public static final String PATH_FILAS_UNIDADE = "/filas-unidade";
    public static final String PATH_AVISOS_EXTERNOS = "/avisos-externos";
    public static final String PATH_CERTIFICAR_NAO_LEITURA = "/certificar-nao-leitura";
    public static final String PATH_CADASTROS_OPERACIONAIS = "/cadastros-operacionais";
    public static final String PATH_CADASTRO_OPERACIONAL_AFILIACAO = PATH_CADASTROS_OPERACIONAIS + "/afiliacao/{affiliationId}";
    public static final String PATH_CADASTRO_OPERACIONAL_SOLICITACAO = PATH_CADASTROS_OPERACIONAIS + "/solicitacao/{requestId}";
    public static final String PATH_GUARDIAO_ENTRADA = "/guardiao-entrada";
    public static final String PATH_DELEGATED_AFFILIATIONS = "/adesoes-delegadas";
    public static final String PATH_DELEGATED_AFFILIATION_HOMOLOGATE = PATH_DELEGATED_AFFILIATIONS + "/{requestId}/homologar";
    public static final String PATH_DELEGATED_AFFILIATION_PUBLIC_RECOGNITION = PATH_DELEGATED_AFFILIATIONS + "/{requestId}/reconhecimento-publico";
    public static final String PATH_DELEGATED_AFFILIATION_OFFICIAL_SOURCE_DOSSIER = PATH_DELEGATED_AFFILIATIONS + "/{requestId}/dossie-fontes-oficiais";
    public static final String PATH_DELEGATED_AFFILIATION_OFFICIAL_IDENTIFIER_DOSSIER = PATH_DELEGATED_AFFILIATIONS + "/{requestId}/identificadores-oficiais";
    public static final String PATH_DELEGATED_AFFILIATION_OFFICIAL_SOURCE_ATTESTATION = PATH_DELEGATED_AFFILIATIONS + "/{requestId}/atestacao-fontes-oficiais";
    public static final String PATH_DELEGATED_AFFILIATION_OFFICIAL_SOURCE_REVALIDATE = PATH_DELEGATED_AFFILIATIONS + "/{requestId}/revalidar-fontes-oficiais";
    public static final String PATH_OFFICIAL_SOURCE_CONNECTOR_PROBE_ALL = PATH_OFFICIAL_SOURCE_CONNECTORS + "/sondar";
    public static final String PATH_OFFICIAL_SOURCE_CONNECTOR_PROBE_ONE = PATH_OFFICIAL_SOURCE_CONNECTORS + "/{sourceCode}/sondar";
    public static final String PATH_DELEGATED_CLOSURE = "/fechamento-delegado";
    public static final String PATH_DELEGATED_CURRENT_ENTRY = PATH_DELEGATED_CLOSURE + "/entrada-atual";
    public static final String PATH_TRUST_MATRIX = "/matriz-confiabilidade";
    public static final String PATH_PANEL_BLUEPRINTS = "/painel-blueprints";
    public static final String PATH_DELEGATED_AFFILIATION_VALIDATION = PATH_DELEGATED_AFFILIATIONS + "/{requestId}/validacao";
    public static final String PATH_DELEGATED_AFFILIATION_APPROVAL_TRAIL = PATH_DELEGATED_AFFILIATIONS + "/{requestId}/trilha-aprovacao";
    public static final String PATH_REMOTE_CERTIFICATE_AUTHORIZATIONS = "/autorizacoes-certificado-remoto";
    public static final String PATH_REMOTE_CERTIFICATE_AUTHORIZATION_REVOKE = PATH_REMOTE_CERTIFICATE_AUTHORIZATIONS + "/{authorizationId}/revogar";
    public static final String PATH_SESSION_RISK = "/risco-sessao";
    public static final String PATH_SENSITIVE_ACT_AUTHORIZE = "/atos-sensiveis/autorizar";
    public static final String PATH_INTEGRATION_CREDENTIALS = "/credenciais-integracao";
    public static final String PATH_INTEGRATION_CREDENTIAL_ROTATE = PATH_INTEGRATION_CREDENTIALS + "/{credentialId}/rotacionar";
    public static final String PATH_INTEGRATION_CREDENTIAL_REVOKE = PATH_INTEGRATION_CREDENTIALS + "/{credentialId}/revogar";
    public static final String PATH_INTEGRATION_CREDENTIAL_REGISTER_CALL = PATH_INTEGRATION_CREDENTIALS + "/{credentialId}/registrar-chamada";
    public static final String PATH_INTEGRATION_CREDENTIAL_TRAIL = PATH_INTEGRATION_CREDENTIALS + "/{credentialId}/trilha";
    public static final String PATH_DIMENSIONAMENTO_USUARIOS_INTERNOS = "/dimensionamento-usuarios-internos";
    public static final String PERSONAL_PANEL_PATH = "/api/v1/painel/pessoal";
    public static final String PATH_GOVERNANCA_CONFIANCA = "/nomeacoes/{nominationId}/governanca-confianca";
    public static final String PATH_PLANO_DADOS_HORIZONTAL = "/nomeacoes/{nominationId}/plano-dados-horizontal";
    public static final String PATH_PERFIL_OPERACIONAL = "/nomeacoes/{nominationId}/perfil-operacional";
    public static final String PATH_GOVERNANCA_CONFIANCA_DECISOES = PATH_GOVERNANCA_CONFIANCA + "/decisoes";
    private static final Pattern NOMINATION_SEGMENT_PATTERN = Pattern.compile("(?:^|/)nomeacoes/([^/]+)(?:/|$)");
    private static final Pattern AFFILIATION_SEGMENT_PATTERN = Pattern.compile("(?:^|/)afiliacoes/([^/]+)(?:/|$)");

    public static final String PATH_CATALOGO_CANONICO = com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes.PATH_CATALOGO_CANONICO;
    public static final String PATH_CERTIFICAR_CIENCIA_LOTE = com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes.PATH_CERTIFICAR_CIENCIA_LOTE;
    public static final String PATH_COBERTURAS_OPERACIONAIS = com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes.PATH_COBERTURAS_OPERACIONAIS;
    public static final String PATH_COBERTURAS_OPERACIONAIS_APLICAR = com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes.PATH_COBERTURAS_OPERACIONAIS_APLICAR;
    public static final String PATH_COERENCIA_PROCESSUAL_ATO = com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes.PATH_COERENCIA_PROCESSUAL_ATO;
    public static final String PATH_COERENCIA_PROCESSUAL_DIAGNOSTICO = com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes.PATH_COERENCIA_PROCESSUAL_DIAGNOSTICO;
    public static final String PATH_COERENCIA_PROCESSUAL_PROFILE = com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes.PATH_COERENCIA_PROCESSUAL_PROFILE;
    public static final String PATH_CONTEXTOS_ATIVACAO = com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes.PATH_CONTEXTOS_ATIVACAO;
    public static final String PATH_CONTRATO_INTEGRACAO = com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes.PATH_CONTRATO_INTEGRACAO;
    public static final String PATH_FECHAMENTO_TEXTO = com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes.PATH_FECHAMENTO_TEXTO;
    public static final String PATH_IDENTIDADE_GUARDA = com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes.PATH_IDENTIDADE_GUARDA;
    public static final String PATH_PAPEIS_PROCESSUAIS = com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes.PATH_PAPEIS_PROCESSUAIS;
    public static final String PATH_PAPEIS_PROCESSUAIS_DIAGNOSTICO = com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes.PATH_PAPEIS_PROCESSUAIS_DIAGNOSTICO;
    public static final String PATH_PAPEIS_PROCESSUAIS_PROFILE = com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes.PATH_PAPEIS_PROCESSUAIS_PROFILE;
    public static final String PATH_POLITICA_STEP_UP = com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes.PATH_POLITICA_STEP_UP;
    public static final String PATH_RECEBER_LOTE = com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes.PATH_RECEBER_LOTE;
    public static final String PATH_REPRESENTANTE_VERIFICACAO = com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes.PATH_REPRESENTANTE_VERIFICACAO;
    public static final String PATH_TOPOLOGIA_DESTINATARIOS = com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes.PATH_TOPOLOGIA_DESTINATARIOS;
    public static final String PATH_TRIAGEM_SUGERIDA = com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes.PATH_TRIAGEM_SUGERIDA;
    public static final String PATH_VINCULOS_APROVACAO = com.tcc.pjb.backend.core.comunicacao.institucional.InstitutionalApiRoutes.PATH_VINCULOS_APROVACAO;

    private NationalCommunicationInstitutionalHttpRoutes() {
    }

    public static String painelExecutivo() {
        return CANONICAL_BASE + PATH_PAINEL_EXECUTIVO;
    }

    public static String inbox() {
        return CANONICAL_BASE + PATH_INBOX;
    }

    public static String notificacoes() {
        return CANONICAL_BASE + PATH_NOTIFICACOES;
    }

    public static String dlq() {
        return CANONICAL_BASE + PATH_DLQ;
    }

    public static String slaPreditivo() {
        return CANONICAL_BASE + PATH_SLA_PREDITIVO;
    }

    public static String gates() {
        return CANONICAL_BASE + PATH_GATES;
    }

    public static String minutas() {
        return CANONICAL_BASE + PATH_MINUTAS;
    }

    public static String pendentesNaoLeitura() {
        return CANONICAL_BASE + PATH_PENDENTES_NAO_LEITURA;
    }

    public static String afiliacoes() {
        return CANONICAL_BASE + PATH_AFFILIATIONS;
    }

    public static String conectoresFontesOficiais() {
        return CANONICAL_BASE + PATH_OFFICIAL_SOURCE_CONNECTORS;
    }

    public static String sondarConectoresFontesOficiais() {
        return CANONICAL_BASE + PATH_OFFICIAL_SOURCE_CONNECTOR_PROBE_ALL;
    }

    public static String sondarConectorFonteOficial() {
        return CANONICAL_BASE + PATH_OFFICIAL_SOURCE_CONNECTOR_PROBE_ONE;
    }

    public static String homologarAfiliacao() {
        return CANONICAL_BASE + PATH_AFFILIATION_HOMOLOGATE;
    }


    public static String reconhecimentoPublicoAfiliacao() {
        return CANONICAL_BASE + PATH_AFFILIATION_PUBLIC_RECOGNITION;
    }

    public static String dossieFontesOficiaisAfiliacao() {
        return CANONICAL_BASE + PATH_AFFILIATION_OFFICIAL_SOURCE_DOSSIER;
    }

    public static String identificadoresOficiaisAfiliacao() {
        return CANONICAL_BASE + PATH_AFFILIATION_OFFICIAL_IDENTIFIER_DOSSIER;
    }

    public static String atestacaoFontesOficiaisAfiliacao() {
        return CANONICAL_BASE + PATH_AFFILIATION_OFFICIAL_SOURCE_ATTESTATION;
    }

    public static String revalidarFontesOficiaisAfiliacao() {
        return CANONICAL_BASE + PATH_AFFILIATION_OFFICIAL_SOURCE_REVALIDATE;
    }

    public static String planoOnboardingAfiliacao() {
        return CANONICAL_BASE + PATH_AFFILIATION_ONBOARDING_PLAN;
    }

    public static String politicaAutenticacaoAfiliacao() {
        return CANONICAL_BASE + PATH_AFFILIATION_AUTHENTICATION_POLICY;
    }

    public static String provisionamentoOperacionalAfiliacao() {
        return CANONICAL_BASE + PATH_AFFILIATION_OPERATIONAL_PROVISIONING;
    }

    public static String provisionarOperacionalAfiliacao() {
        return CANONICAL_BASE + PATH_AFFILIATION_OPERATIONAL_PROVISION;
    }

    public static String credenciaisGerenciadasAfiliacao() {
        return CANONICAL_BASE + PATH_AFFILIATION_MANAGED_CREDENTIALS;
    }

    public static String revogarCredencialGerenciadaAfiliacao() {
        return CANONICAL_BASE + PATH_AFFILIATION_MANAGED_CREDENTIAL_REVOKE;
    }

    public static String aprovacaoAdministradorRaizAfiliacao() {
        return CANONICAL_BASE + PATH_AFFILIATION_ROOT_ADMIN_APPROVAL;
    }

    public static String assinaturaForteAfiliacao() {
        return CANONICAL_BASE + PATH_AFFILIATION_STRONG_SIGNATURE_POLICY;
    }

    public static String governancaUnidadesAfiliacao() {
        return CANONICAL_BASE + PATH_AFFILIATION_UNIT_GOVERNANCE;
    }

    public static String registrarUnidadeAfiliacao() {
        return CANONICAL_BASE + PATH_AFFILIATION_UNIT_GOVERNANCE_UNITS;
    }

    public static String registrarLotacaoAfiliacao() {
        return CANONICAL_BASE + PATH_AFFILIATION_UNIT_GOVERNANCE_LOTACOES;
    }

    public static String identidadeWorkloadAfiliacao() {
        return CANONICAL_BASE + PATH_AFFILIATION_WORKLOAD_IDENTITY;
    }

    public static String delegacoesCoberturaAfiliacao() {
        return CANONICAL_BASE + PATH_AFFILIATION_COVERAGE_DELEGATIONS;
    }

    public static String perfilSegurancaApiAfiliacao() {
        return CANONICAL_BASE + PATH_AFFILIATION_API_EDGE_PROFILE;
    }

    public static String contextoAcessoAfiliacao() {
        return CANONICAL_BASE + PATH_AFFILIATION_ACCESS_CONTEXT;
    }

    public static String nomeacoes() {
        return CANONICAL_BASE + PATH_NOMINATIONS;
    }

    public static String entradaSegura() {
        return CANONICAL_BASE + PATH_SECURE_ENTRY;
    }

    public static String catalogoAcessos() {
        return CANONICAL_BASE + PATH_ACCESS_CATALOG;
    }

    public static String blueprints() {
        return CANONICAL_BASE + PATH_BLUEPRINTS;
    }

    public static String recertificacoes() {
        return CANONICAL_BASE + PATH_RECERTIFICATIONS;
    }

    public static String recertificarAfiliacao() {
        return CANONICAL_BASE + PATH_AFFILIATION_RECERTIFY;
    }

    public static String revogarAcessosAfiliacao() {
        return CANONICAL_BASE + PATH_AFFILIATION_REVOKE_ACCESS;
    }

    public static String integracoesGovernanca() {
        return CANONICAL_BASE + PATH_GOVERNANCE_INTEGRATIONS;
    }

    public static String quatroNiveis() {
        return CANONICAL_BASE + PATH_FOUR_LEVELS;
    }

    public static String casosOperacionaisAfiliacao() {
        return CANONICAL_BASE + PATH_AFFILIATION_OPERATIONAL_CASES;
    }

    public static String diagnosticoEstrutural() {
        return CANONICAL_BASE + PATH_STRUCTURAL_DIAGNOSTIC;
    }

    public static String modeloOperacional() {
        return CANONICAL_BASE + PATH_MODELO_OPERACIONAL;
    }

    public static String entradaInteligente() {
        return CANONICAL_BASE + PATH_ENTRADA_INTELIGENTE;
    }

    public static String contextosEntrada() {
        return CANONICAL_BASE + PATH_CONTEXTOS_ENTRADA;
    }

    public static String painelPessoal() {
        return PERSONAL_PANEL_PATH;
    }

    public static String dimensionamentoUsuariosInternos() {
        return CANONICAL_BASE + PATH_DIMENSIONAMENTO_USUARIOS_INTERNOS;
    }

    public static String governancaConfianca() {
        return CANONICAL_BASE + PATH_GOVERNANCA_CONFIANCA;
    }

    public static String governancaConfiancaDecisoes() {
        return CANONICAL_BASE + PATH_GOVERNANCA_CONFIANCA_DECISOES;
    }

    public static String planoDadosHorizontal() {
        return CANONICAL_BASE + PATH_PLANO_DADOS_HORIZONTAL;
    }

    public static String perfilOperacional() {
        return CANONICAL_BASE + PATH_PERFIL_OPERACIONAL;
    }

    public static String homologarAfiliacao(String affiliationId) {
        return resolveAffiliationPath(homologarAfiliacao(), affiliationId);
    }

    public static String reconhecimentoPublicoAfiliacao(String affiliationId) {
        return resolveAffiliationPath(reconhecimentoPublicoAfiliacao(), affiliationId);
    }

    public static String dossieFontesOficiaisAfiliacao(String affiliationId) {
        return resolveAffiliationPath(dossieFontesOficiaisAfiliacao(), affiliationId);
    }

    public static String atestacaoFontesOficiaisAfiliacao(String affiliationId) {
        return resolveAffiliationPath(atestacaoFontesOficiaisAfiliacao(), affiliationId);
    }

    public static String revalidarFontesOficiaisAfiliacao(String affiliationId) {
        return resolveAffiliationPath(revalidarFontesOficiaisAfiliacao(), affiliationId);
    }

    public static String planoOnboardingAfiliacao(String affiliationId) {
        return resolveAffiliationPath(planoOnboardingAfiliacao(), affiliationId);
    }

    public static String politicaAutenticacaoAfiliacao(String affiliationId) {
        return resolveAffiliationPath(politicaAutenticacaoAfiliacao(), affiliationId);
    }

    public static String provisionamentoOperacionalAfiliacao(String affiliationId) {
        return resolveAffiliationPath(provisionamentoOperacionalAfiliacao(), affiliationId);
    }

    public static String provisionarOperacionalAfiliacao(String affiliationId) {
        return resolveAffiliationPath(provisionarOperacionalAfiliacao(), affiliationId);
    }

    public static String credenciaisGerenciadasAfiliacao(String affiliationId) {
        return resolveAffiliationPath(credenciaisGerenciadasAfiliacao(), affiliationId);
    }

    public static String aprovacaoAdministradorRaizAfiliacao(String affiliationId) {
        return resolveAffiliationPath(aprovacaoAdministradorRaizAfiliacao(), affiliationId);
    }

    public static String assinaturaForteAfiliacao(String affiliationId) {
        return resolveAffiliationPath(assinaturaForteAfiliacao(), affiliationId);
    }

    public static String recertificarAfiliacao(String affiliationId) {
        return resolveAffiliationPath(recertificarAfiliacao(), affiliationId);
    }

    public static String revogarAcessosAfiliacao(String affiliationId) {
        return resolveAffiliationPath(revogarAcessosAfiliacao(), affiliationId);
    }

    public static String casosOperacionaisAfiliacao(String affiliationId) {
        return resolveAffiliationPath(casosOperacionaisAfiliacao(), affiliationId);
    }

    public static String cadastroOperacionalAfiliacao(String affiliationId) {
        return resolveAffiliationPath(cadastroOperacionalAfiliacao(), affiliationId);
    }

    public static String cadastroOperacionalSolicitacao(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return cadastroOperacionalSolicitacao();
        }
        return Objects.requireNonNull(cadastroOperacionalSolicitacao()).replace("{requestId}", encodePathSegment(requestId.trim()));
    }

    public static String governancaConfianca(String nominationId) {
        return resolveNominationPath(governancaConfianca(), nominationId);
    }

    public static String governancaConfiancaDecisoes(String nominationId) {
        return resolveNominationPath(governancaConfiancaDecisoes(), nominationId);
    }

    public static String planoDadosHorizontal(String nominationId) {
        return resolveNominationPath(planoDadosHorizontal(), nominationId);
    }

    public static String perfilOperacional(String nominationId) {
        return resolveNominationPath(perfilOperacional(), nominationId);
    }

    public static String painelExecutivo(String scope) {
        return withParams(painelExecutivo(), orderedMap("scope", scope));
    }

    public static String painelExecutivo(String scope, String lane) {
        return withParams(painelExecutivo(), orderedMap("scope", scope, "lane", lane));
    }

    public static String painelExecutivoComUnidade(String unidadeCodigo) {
        return withParams(painelExecutivo(), orderedMap("unidadeCodigo", unidadeCodigo));
    }

    public static String painelExecutivoComUnidadeEScope(String unidadeCodigo, String scope) {
        return withParams(painelExecutivo(), orderedMap("unidadeCodigo", unidadeCodigo, "scope", scope));
    }

    public static String inbox(String unidadeCodigo, String caixaCodigo) {
        return withParams(inbox(), orderedMap("unidadeCodigo", unidadeCodigo, "caixaCodigo", caixaCodigo));
    }

    public static String painelOrgao() {
        return CANONICAL_BASE + PATH_PANEL_ORGAO;
    }

    public static String filasUnidade() {
        return CANONICAL_BASE + PATH_FILAS_UNIDADE;
    }

    public static String avisosExternos() {
        return CANONICAL_BASE + PATH_AVISOS_EXTERNOS;
    }

    public static String certificarNaoLeitura() {
        return CANONICAL_BASE + PATH_CERTIFICAR_NAO_LEITURA;
    }

    public static String cadastrosOperacionais() {
        return CANONICAL_BASE + PATH_CADASTROS_OPERACIONAIS;
    }

    public static String cadastroOperacionalAfiliacao() {
        return CANONICAL_BASE + PATH_CADASTRO_OPERACIONAL_AFILIACAO;
    }

    public static String cadastroOperacionalSolicitacao() {
        return CANONICAL_BASE + PATH_CADASTRO_OPERACIONAL_SOLICITACAO;
    }

    public static String guardiaoEntrada() {
        return CANONICAL_BASE + PATH_GUARDIAO_ENTRADA;
    }

    public static String adesoesDelegadas() {
        return CANONICAL_BASE + PATH_DELEGATED_AFFILIATIONS;
    }

    public static String homologarAdesaoDelegada() {
        return CANONICAL_BASE + PATH_DELEGATED_AFFILIATION_HOMOLOGATE;
    }

    public static String reconhecimentoPublicoAdesaoDelegada() {
        return CANONICAL_BASE + PATH_DELEGATED_AFFILIATION_PUBLIC_RECOGNITION;
    }

    public static String dossieFontesOficiaisAdesaoDelegada() {
        return CANONICAL_BASE + PATH_DELEGATED_AFFILIATION_OFFICIAL_SOURCE_DOSSIER;
    }

    public static String identificadoresOficiaisAdesaoDelegada() {
        return CANONICAL_BASE + PATH_DELEGATED_AFFILIATION_OFFICIAL_IDENTIFIER_DOSSIER;
    }

    public static String atestacaoFontesOficiaisAdesaoDelegada() {
        return CANONICAL_BASE + PATH_DELEGATED_AFFILIATION_OFFICIAL_SOURCE_ATTESTATION;
    }

    public static String revalidarFontesOficiaisAdesaoDelegada() {
        return CANONICAL_BASE + PATH_DELEGATED_AFFILIATION_OFFICIAL_SOURCE_REVALIDATE;
    }

    public static String dossieFontesOficiaisAdesaoDelegada(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return dossieFontesOficiaisAdesaoDelegada();
        }
        return dossieFontesOficiaisAdesaoDelegada().replace("{requestId}", encodePathSegment(requestId.trim()));
    }

    public static String atestacaoFontesOficiaisAdesaoDelegada(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return atestacaoFontesOficiaisAdesaoDelegada();
        }
        return atestacaoFontesOficiaisAdesaoDelegada().replace("{requestId}", encodePathSegment(requestId.trim()));
    }

    public static String revalidarFontesOficiaisAdesaoDelegada(String requestId) {
        if (requestId == null || requestId.isBlank()) {
            return revalidarFontesOficiaisAdesaoDelegada();
        }
        return revalidarFontesOficiaisAdesaoDelegada().replace("{requestId}", encodePathSegment(requestId.trim()));
    }

    public static String fechamentoDelegado() {
        return CANONICAL_BASE + PATH_DELEGATED_CLOSURE;
    }

    public static String entradaAtualDelegada() {
        return CANONICAL_BASE + PATH_DELEGATED_CURRENT_ENTRY;
    }

    public static String matrizConfiabilidade() {
        return CANONICAL_BASE + PATH_TRUST_MATRIX;
    }

    public static String painelBlueprints() {
        return CANONICAL_BASE + PATH_PANEL_BLUEPRINTS;
    }

    public static String validacaoAdesaoDelegada() {
        return CANONICAL_BASE + PATH_DELEGATED_AFFILIATION_VALIDATION;
    }

    public static String trilhaAprovacaoAdesaoDelegada() {
        return CANONICAL_BASE + PATH_DELEGATED_AFFILIATION_APPROVAL_TRAIL;
    }

    public static String autorizacoesCertificadoRemoto() {
        return CANONICAL_BASE + PATH_REMOTE_CERTIFICATE_AUTHORIZATIONS;
    }

    public static String revogarAutorizacaoCertificadoRemoto() {
        return CANONICAL_BASE + PATH_REMOTE_CERTIFICATE_AUTHORIZATION_REVOKE;
    }

    public static String riscoSessao() {
        return CANONICAL_BASE + PATH_SESSION_RISK;
    }

    public static String autorizarAtoSensivel() {
        return CANONICAL_BASE + PATH_SENSITIVE_ACT_AUTHORIZE;
    }

    public static String credenciaisIntegracao() {
        return CANONICAL_BASE + PATH_INTEGRATION_CREDENTIALS;
    }

    public static String rotacionarCredencialIntegracao() {
        return CANONICAL_BASE + PATH_INTEGRATION_CREDENTIAL_ROTATE;
    }

    public static String revogarCredencialIntegracao() {
        return CANONICAL_BASE + PATH_INTEGRATION_CREDENTIAL_REVOKE;
    }

    public static String registrarChamadaCredencialIntegracao() {
        return CANONICAL_BASE + PATH_INTEGRATION_CREDENTIAL_REGISTER_CALL;
    }

    public static String trilhaCredencialIntegracao() {
        return CANONICAL_BASE + PATH_INTEGRATION_CREDENTIAL_TRAIL;
    }

    public static String modeloOperacional(String affiliationId, String destinatarioKind, String municipio, String uf) {
        return withParams(modeloOperacional(), orderedMap(
                "affiliationId", affiliationId,
                "destinatarioKind", destinatarioKind,
                "municipio", municipio,
                "uf", uf));
    }

    public static boolean isInstitutionalPath(String path) {
        String normalized = normalizePath(path);
        return normalized != null && (normalized.equals(CANONICAL_BASE) || normalized.startsWith(CANONICAL_BASE + '/'));
    }

    public static String extractNominationId(String path) {
        String normalized = normalizePath(path);
        if (normalized == null) {
            return null;
        }
        Matcher matcher = NOMINATION_SEGMENT_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }
        String candidate = matcher.group(1);
        return candidate == null || candidate.isBlank() ? null : candidate.trim();
    }

    public static String extractAffiliationId(String path) {
        String normalized = normalizePath(path);
        if (normalized == null) {
            return null;
        }
        Matcher matcher = AFFILIATION_SEGMENT_PATTERN.matcher(normalized);
        if (!matcher.find()) {
            return null;
        }
        String candidate = matcher.group(1);
        return candidate == null || candidate.isBlank() ? null : candidate.trim();
    }

    public static String normalizePath(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String normalized = path.trim();
        int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }
        if (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String resolveNominationPath(String template, String nominationId) {
        if (nominationId == null || nominationId.isBlank()) {
            return template;
        }
        return Objects.requireNonNull(template).replace("{nominationId}", encodePathSegment(nominationId.trim()));
    }

    private static String resolveAffiliationPath(String template, String affiliationId) {
        if (affiliationId == null || affiliationId.isBlank()) {
            return template;
        }
        return Objects.requireNonNull(template).replace("{affiliationId}", encodePathSegment(affiliationId.trim()));
    }

    private static LinkedHashMap<String, String> orderedMap(String key, String value) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        out.put(key, value);
        return out;
    }

    private static LinkedHashMap<String, String> orderedMap(String key1, String value1, String key2, String value2) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        out.put(key1, value1);
        out.put(key2, value2);
        return out;
    }

    private static LinkedHashMap<String, String> orderedMap(String key1, String value1,
                                                             String key2, String value2,
                                                             String key3, String value3,
                                                             String key4, String value4) {
        LinkedHashMap<String, String> out = new LinkedHashMap<>();
        out.put(key1, value1);
        out.put(key2, value2);
        out.put(key3, value3);
        out.put(key4, value4);
        return out;
    }

    private static String withParams(String base, Map<String, String> params) {
        StringBuilder out = new StringBuilder(Objects.requireNonNull(base));
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) {
                continue;
            }
            out.append(first ? '?' : '&')
                    .append(encodeQueryComponent(entry.getKey()))
                    .append('=')
                    .append(encodeQueryComponent(entry.getValue().trim()));
            first = false;
        }
        return out.toString();
    }

    private static String encodeQueryComponent(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String encodePathSegment(String value) {
        return encodeQueryComponent(value).replace("+", "%20");
    }

    public static final class InstitutionalApiRoutes {

        public static final String CANONICAL_BASE = NationalCommunicationInstitutionalHttpRoutes.CANONICAL_BASE;
        public static final String PATH_SLA_PREDITIVO = NationalCommunicationInstitutionalHttpRoutes.PATH_SLA_PREDITIVO;
        public static final String PATH_PENDENTES_NAO_LEITURA = NationalCommunicationInstitutionalHttpRoutes.PATH_PENDENTES_NAO_LEITURA;
        public static final String PATH_PANEL_ORGAO = NationalCommunicationInstitutionalHttpRoutes.PATH_PANEL_ORGAO;
        public static final String PATH_FILAS_UNIDADE = NationalCommunicationInstitutionalHttpRoutes.PATH_FILAS_UNIDADE;
        public static final String PATH_AVISOS_EXTERNOS = NationalCommunicationInstitutionalHttpRoutes.PATH_AVISOS_EXTERNOS;
        public static final String PATH_CERTIFICAR_NAO_LEITURA = NationalCommunicationInstitutionalHttpRoutes.PATH_CERTIFICAR_NAO_LEITURA;
        public static final String PATH_ENTRADA_INTELIGENTE = NationalCommunicationInstitutionalHttpRoutes.PATH_ENTRADA_INTELIGENTE;
        public static final String PATH_CONTEXTOS_ENTRADA = NationalCommunicationInstitutionalHttpRoutes.PATH_CONTEXTOS_ENTRADA;
        public static final String PATH_CADASTROS_OPERACIONAIS = NationalCommunicationInstitutionalHttpRoutes.PATH_CADASTROS_OPERACIONAIS;
        public static final String PATH_CADASTRO_OPERACIONAL_AFILIACAO = NationalCommunicationInstitutionalHttpRoutes.PATH_CADASTRO_OPERACIONAL_AFILIACAO;
        public static final String PATH_CADASTRO_OPERACIONAL_SOLICITACAO = NationalCommunicationInstitutionalHttpRoutes.PATH_CADASTRO_OPERACIONAL_SOLICITACAO;
        public static final String PATH_GUARDIAO_ENTRADA = NationalCommunicationInstitutionalHttpRoutes.PATH_GUARDIAO_ENTRADA;
        public static final String PATH_FOUR_LEVELS = NationalCommunicationInstitutionalHttpRoutes.PATH_FOUR_LEVELS;
        public static final String PATH_AFFILIATION_OPERATIONAL_CASES = NationalCommunicationInstitutionalHttpRoutes.PATH_AFFILIATION_OPERATIONAL_CASES;
        public static final String PATH_STRUCTURAL_DIAGNOSTIC = NationalCommunicationInstitutionalHttpRoutes.PATH_STRUCTURAL_DIAGNOSTIC;
        public static final String PATH_MODELO_OPERACIONAL = NationalCommunicationInstitutionalHttpRoutes.PATH_MODELO_OPERACIONAL;
        public static final String PATH_DELEGATED_CLOSURE = NationalCommunicationInstitutionalHttpRoutes.PATH_DELEGATED_CLOSURE;
        public static final String PATH_DELEGATED_CURRENT_ENTRY = NationalCommunicationInstitutionalHttpRoutes.PATH_DELEGATED_CURRENT_ENTRY;
        public static final String PATH_TRUST_MATRIX = NationalCommunicationInstitutionalHttpRoutes.PATH_TRUST_MATRIX;
        public static final String PATH_PANEL_BLUEPRINTS = NationalCommunicationInstitutionalHttpRoutes.PATH_PANEL_BLUEPRINTS;
        public static final String PATH_DELEGATED_AFFILIATIONS = NationalCommunicationInstitutionalHttpRoutes.PATH_DELEGATED_AFFILIATIONS;
        public static final String PATH_DELEGATED_AFFILIATION_HOMOLOGATE = NationalCommunicationInstitutionalHttpRoutes.PATH_DELEGATED_AFFILIATION_HOMOLOGATE;
        public static final String PATH_DELEGATED_AFFILIATION_PUBLIC_RECOGNITION = NationalCommunicationInstitutionalHttpRoutes.PATH_DELEGATED_AFFILIATION_PUBLIC_RECOGNITION;
        public static final String PATH_DELEGATED_AFFILIATION_OFFICIAL_SOURCE_DOSSIER = NationalCommunicationInstitutionalHttpRoutes.PATH_DELEGATED_AFFILIATION_OFFICIAL_SOURCE_DOSSIER;
        public static final String PATH_DELEGATED_AFFILIATION_OFFICIAL_IDENTIFIER_DOSSIER = NationalCommunicationInstitutionalHttpRoutes.PATH_DELEGATED_AFFILIATION_OFFICIAL_IDENTIFIER_DOSSIER;
        public static final String PATH_DELEGATED_AFFILIATION_OFFICIAL_SOURCE_ATTESTATION = NationalCommunicationInstitutionalHttpRoutes.PATH_DELEGATED_AFFILIATION_OFFICIAL_SOURCE_ATTESTATION;
        public static final String PATH_DELEGATED_AFFILIATION_OFFICIAL_SOURCE_REVALIDATE = NationalCommunicationInstitutionalHttpRoutes.PATH_DELEGATED_AFFILIATION_OFFICIAL_SOURCE_REVALIDATE;
        public static final String PATH_CATALOGO_CANONICO = NationalCommunicationInstitutionalHttpRoutes.PATH_CATALOGO_CANONICO;
        public static final String PATH_CERTIFICAR_CIENCIA_LOTE = NationalCommunicationInstitutionalHttpRoutes.PATH_CERTIFICAR_CIENCIA_LOTE;
        public static final String PATH_COBERTURAS_OPERACIONAIS = NationalCommunicationInstitutionalHttpRoutes.PATH_COBERTURAS_OPERACIONAIS;
        public static final String PATH_COBERTURAS_OPERACIONAIS_APLICAR = NationalCommunicationInstitutionalHttpRoutes.PATH_COBERTURAS_OPERACIONAIS_APLICAR;
        public static final String PATH_COERENCIA_PROCESSUAL_ATO = NationalCommunicationInstitutionalHttpRoutes.PATH_COERENCIA_PROCESSUAL_ATO;
        public static final String PATH_COERENCIA_PROCESSUAL_DIAGNOSTICO = NationalCommunicationInstitutionalHttpRoutes.PATH_COERENCIA_PROCESSUAL_DIAGNOSTICO;
        public static final String PATH_COERENCIA_PROCESSUAL_PROFILE = NationalCommunicationInstitutionalHttpRoutes.PATH_COERENCIA_PROCESSUAL_PROFILE;
        public static final String PATH_CONTEXTOS_ATIVACAO = NationalCommunicationInstitutionalHttpRoutes.PATH_CONTEXTOS_ATIVACAO;
        public static final String PATH_CONTRATO_INTEGRACAO = NationalCommunicationInstitutionalHttpRoutes.PATH_CONTRATO_INTEGRACAO;
        public static final String PATH_FECHAMENTO_TEXTO = NationalCommunicationInstitutionalHttpRoutes.PATH_FECHAMENTO_TEXTO;
        public static final String PATH_IDENTIDADE_GUARDA = NationalCommunicationInstitutionalHttpRoutes.PATH_IDENTIDADE_GUARDA;
        public static final String PATH_PAPEIS_PROCESSUAIS = NationalCommunicationInstitutionalHttpRoutes.PATH_PAPEIS_PROCESSUAIS;
        public static final String PATH_PAPEIS_PROCESSUAIS_DIAGNOSTICO = NationalCommunicationInstitutionalHttpRoutes.PATH_PAPEIS_PROCESSUAIS_DIAGNOSTICO;
        public static final String PATH_PAPEIS_PROCESSUAIS_PROFILE = NationalCommunicationInstitutionalHttpRoutes.PATH_PAPEIS_PROCESSUAIS_PROFILE;
        public static final String PATH_POLITICA_STEP_UP = NationalCommunicationInstitutionalHttpRoutes.PATH_POLITICA_STEP_UP;
        public static final String PATH_RECEBER_LOTE = NationalCommunicationInstitutionalHttpRoutes.PATH_RECEBER_LOTE;
        public static final String PATH_REPRESENTANTE_VERIFICACAO = NationalCommunicationInstitutionalHttpRoutes.PATH_REPRESENTANTE_VERIFICACAO;
        public static final String PATH_TOPOLOGIA_DESTINATARIOS = NationalCommunicationInstitutionalHttpRoutes.PATH_TOPOLOGIA_DESTINATARIOS;
        public static final String PATH_TRIAGEM_SUGERIDA = NationalCommunicationInstitutionalHttpRoutes.PATH_TRIAGEM_SUGERIDA;
        public static final String PATH_VINCULOS_APROVACAO = NationalCommunicationInstitutionalHttpRoutes.PATH_VINCULOS_APROVACAO;

        private InstitutionalApiRoutes() {
        }
    }
}
