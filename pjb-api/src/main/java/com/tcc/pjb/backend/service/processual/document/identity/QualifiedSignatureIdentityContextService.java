package com.tcc.pjb.backend.service.processual.document.identity;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.EnteFederativo;
import com.tcc.pjb.backend.model.entity.enums.processual.RitoProcessual;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import jakarta.servlet.http.HttpServletRequest;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.security.auth.x500.X500Principal;
import org.springframework.stereotype.Service;

@Service
public class QualifiedSignatureIdentityContextService {

    static final DateTimeFormatter CERTIFICATE_TIME_FORMAT = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    static final String[] CERTIFICATE_ATTRIBUTE_NAMES = {
            "jakarta.servlet.request.X509Certificate",
            "javax.servlet.request.X509Certificate"
    };
    static final String[] CERTIFICATE_PEM_HEADERS = {
            "X-Client-Certificate",
            "X-SSL-CERT",
            "X-PJB-ENTRY-CERT-PEM"
    };
    static final String[] CERTIFICATE_SUBJECT_HEADERS = {
            "X-PJB-ENTRY-CERT-SUBJECT",
            "X-SSL-Client-S-DN",
            "X-SSL-Client-DN"
    };
    static final String[] CERTIFICATE_ISSUER_HEADERS = {
            "X-PJB-ENTRY-CERT-ISSUER",
            "X-SSL-Client-I-DN"
    };
    static final String[] CERTIFICATE_SERIAL_HEADERS = {
            "X-PJB-Client-Cert-Serial",
            "X-SSL-Client-Serial",
            "X-PJB-ENTRY-CERT-SERIAL"
    };
    static final String[] CERTIFICATE_FINGERPRINT_HEADERS = {
            "X-PJB-Client-Cert-Fingerprint",
            "X-SSL-Client-Fingerprint",
            "X-PJB-ENTRY-CERT-FINGERPRINT"
    };
    static final String[] CERTIFICATE_ROLE_HEADERS = {
            "X-PJB-ENTRY-CERT-ROLE",
            "X-PJB-ENTRY-CERT-PROFILE"
    };
    static final String[] CERTIFICATE_BRANCH_HEADERS = {
            "X-PJB-ENTRY-CERT-BRANCH",
            "X-PJB-ENTRY-CERT-JUSTICE"
    };
    static final String[] CERTIFICATE_INSTANCE_HEADERS = {
            "X-PJB-ENTRY-CERT-INSTANCE",
            "X-PJB-ENTRY-CERT-GRAU"
    };
    static final String[] CERTIFICATE_SCOPE_HEADERS = {
            "X-PJB-ENTRY-CERT-SCOPE",
            "X-PJB-ENTRY-CERT-ENTITY-SCOPE"
    };
    static final String[] CERTIFICATE_ORG_HEADERS = {
            "X-PJB-ENTRY-CERT-ORG",
            "X-PJB-ENTRY-CERT-UNIT",
            "X-SSL-Client-OU"
    };
    static final String[] CERTIFICATE_TRIBUNAL_HEADERS = {
            "X-PJB-ENTRY-CERT-TRIBUNAL",
            "X-PJB-ENTRY-CERT-COURT"
    };
    static final String[] CERTIFICATE_UNIT_CODE_HEADERS = {
            "X-PJB-ENTRY-CERT-UNIT-CODE",
            "X-PJB-ENTRY-CERT-UNIDADE-CODIGO",
            "X-PJB-ENTRY-CERT-JUDICIAL-UNIT-CODE"
    };
    static final String[] CERTIFICATE_VARA_HEADERS = {
            "X-PJB-ENTRY-CERT-VARA",
            "X-PJB-ENTRY-CERT-VARA-NOME"
    };
    static final String[] CERTIFICATE_ZONE_HEADERS = {
            "X-PJB-ENTRY-CERT-ZONA-ELEITORAL",
            "X-PJB-ENTRY-CERT-ZONA"
    };
    static final String[] CERTIFICATE_JUIZADO_HEADERS = {
            "X-PJB-ENTRY-CERT-JUIZADO",
            "X-PJB-ENTRY-CERT-JUIZADO-ESPECIAL"
    };
    static final String[] CERTIFICATE_TURMA_HEADERS = {
            "X-PJB-ENTRY-CERT-TURMA",
            "X-PJB-ENTRY-CERT-TURMA-RECURSAL"
    };
    static final String[] CERTIFICATE_CAMARA_HEADERS = {
            "X-PJB-ENTRY-CERT-CAMARA",
            "X-PJB-ENTRY-CERT-CAMARA-JULGADORA"
    };
    static final String[] CERTIFICATE_SECAO_HEADERS = {
            "X-PJB-ENTRY-CERT-SECAO",
            "X-PJB-ENTRY-CERT-SECAO-JUDICIARIA"
    };
    static final String[] CERTIFICATE_SUBSECAO_HEADERS = {
            "X-PJB-ENTRY-CERT-SUBSECAO",
            "X-PJB-ENTRY-CERT-SUBSECAO-JUDICIARIA"
    };
    static final String[] CERTIFICATE_AUDITORIA_HEADERS = {
            "X-PJB-ENTRY-CERT-AUDITORIA",
            "X-PJB-ENTRY-CERT-AUDITORIA-MILITAR"
    };
    static final String[] CERTIFICATE_ORGAO_FRACIONARIO_HEADERS = {
            "X-PJB-ENTRY-CERT-ORGAO-FRACIONARIO",
            "X-PJB-ENTRY-CERT-ORGAO-COLEGIADO"
    };
    static final String[] CERTIFICATE_GABINETE_HEADERS = {
            "X-PJB-ENTRY-CERT-GABINETE",
            "X-PJB-ENTRY-CERT-RELATORIA"
    };
    static final String[] CERTIFICATE_COMARCA_HEADERS = {
            "X-PJB-ENTRY-CERT-COMARCA",
            "X-PJB-ENTRY-CERT-CIDADE"
    };
    static final Pattern TRIBUNAL_HINT_PATTERN = Pattern.compile("\\b(TJ[A-Z]{2}|TRF[1-6]?|TRE[_-]?[A-Z]{2}|TRT\\d{1,2}|TJM[A-Z]{0,2}|STF|STJ|TST|TSE|STM|JF[A-Z]{2})\\b");
    static final String[] CERTIFICATE_UF_HEADERS = {
            "X-PJB-ENTRY-CERT-UF",
            "X-PJB-ENTRY-CERT-STATE"
    };
    static final String[] CERTIFICATE_FULLNAME_HEADERS = {
            "X-PJB-ENTRY-CERT-NAME",
            "X-PJB-ENTRY-CERT-FULLNAME",
            "X-PJB-ENTRY-CERT-CN"
    };
    static final String[] CERTIFICATE_CPF_HEADERS = {
            "X-PJB-ENTRY-CERT-CPF",
            "X-PJB-ENTRY-CERT-DOCUMENT",
            "X-PJB-ENTRY-CERT-ID-NUMBER"
    };
    static final String[] CERTIFICATE_EMAIL_HEADERS = {
            "X-PJB-ENTRY-CERT-EMAIL",
            "X-PJB-ENTRY-CERT-MAIL",
            "X-SSL-Client-Email"
    };
    static final String[] CERTIFICATE_OAB_HEADERS = {
            "X-PJB-ENTRY-CERT-OAB",
            "X-PJB-ENTRY-CERT-BAR-ID"
    };
    static final String[] CERTIFICATE_REGISTRATION_HEADERS = {
            "X-PJB-ENTRY-CERT-REGISTRATION",
            "X-PJB-ENTRY-CERT-PROFESSIONAL-REGISTRATION"
    };
    static final String[] CERTIFICATE_MATRICULA_HEADERS = {
            "X-PJB-ENTRY-CERT-MATRICULA",
            "X-PJB-ENTRY-CERT-FUNCTIONAL-ID"
    };
    static final String[] CERTIFICATE_CARGO_HEADERS = {
            "X-PJB-ENTRY-CERT-CARGO",
            "X-PJB-ENTRY-CERT-FUNCTION",
            "X-PJB-ENTRY-CERT-JOB-TITLE"
    };
    static final String[] CERTIFICATE_SECRETARIAT_HEADERS = {
            "X-PJB-ENTRY-CERT-SECRETARIA",
            "X-PJB-ENTRY-CERT-SECRETARIAT",
            "X-PJB-ENTRY-CERT-DESK"
    };
    static final String[] CERTIFICATE_RECURSAL_SECRETARIAT_HEADERS = {
            "X-PJB-ENTRY-CERT-SECRETARIA-RECURSAL",
            "X-PJB-ENTRY-CERT-RECURSAL-SECRETARIAT",
            "X-PJB-ENTRY-CERT-ADMISSIBILITY-DESK"
    };
    static final String[] CERTIFICATE_EMBARGOS_SECRETARIAT_HEADERS = {
            "X-PJB-ENTRY-CERT-SECRETARIA-EMBARGOS",
            "X-PJB-ENTRY-CERT-EMBARGOS-DESK",
            "X-PJB-ENTRY-CERT-REVIEW-DESK"
    };
    static final String[] CERTIFICATE_SECOND_INSTANCE_SECRETARIAT_HEADERS = {
            "X-PJB-ENTRY-CERT-SECRETARIA-SEGUNDA-INSTANCIA",
            "X-PJB-ENTRY-CERT-SECRETARIA-2G",
            "X-PJB-ENTRY-CERT-SECOND-INSTANCE-SECRETARIAT"
    };
    static final String[] CERTIFICATE_SUPERIOR_INSTANCE_SECRETARIAT_HEADERS = {
            "X-PJB-ENTRY-CERT-SECRETARIA-INSTANCIA-SUPERIOR",
            "X-PJB-ENTRY-CERT-SECRETARIA-ULTIMA-INSTANCIA",
            "X-PJB-ENTRY-CERT-SUPERIOR-INSTANCE-SECRETARIAT"
    };
    static final String[] CERTIFICATE_JUIZADO_SECRETARIAT_HEADERS = {
            "X-PJB-ENTRY-CERT-SECRETARIA-JUIZADO",
            "X-PJB-ENTRY-CERT-SECRETARIA-JUIZADO-ESPECIAL",
            "X-PJB-ENTRY-CERT-SMALL-CLAIMS-SECRETARIAT"
    };
    static final String[] CERTIFICATE_TRABALHISTA_SECRETARIAT_HEADERS = {
            "X-PJB-ENTRY-CERT-SECRETARIA-TRABALHISTA",
            "X-PJB-ENTRY-CERT-LABOR-SECRETARIAT"
    };
    static final String[] CERTIFICATE_ELEITORAL_SECRETARIAT_HEADERS = {
            "X-PJB-ENTRY-CERT-SECRETARIA-ELEITORAL",
            "X-PJB-ENTRY-CERT-ELECTORAL-SECRETARIAT"
    };
    static final String[] CERTIFICATE_MILITAR_SECRETARIAT_HEADERS = {
            "X-PJB-ENTRY-CERT-SECRETARIA-MILITAR",
            "X-PJB-ENTRY-CERT-MILITARY-SECRETARIAT"
    };

    public ResolvedSignatureIdentity resolve(Processo processo,
                                             Usuario usuario,
                                             String papelAssinante,
                                             HttpServletRequest request) {
        String papelBase = normalizeUpper(papelAssinante);
        TipoUsuario tipoUsuario = usuario == null ? null : usuario.getTipoUsuario();
        TipoJustica tipoJustica = resolveTipoJustica(processo, tipoUsuario);
        RitoProcessual rito = processo == null ? null : processo.getRito();
        String ramoJustica = resolveRamoJustica(tipoJustica, rito, tipoUsuario);
        String esferaInstitucional = resolveEsferaInstitucional(usuario, tipoUsuario, ramoJustica);
        String instancia = resolveInstancia(processo, tipoUsuario, ramoJustica, request);
        String papelDetalhado = resolvePapelDetalhado(usuario, tipoUsuario, ramoJustica, instancia, papelBase, request);
        InstitutionalAssignment lotacao = resolveInstitutionalAssignment(processo, usuario, tipoJustica, ramoJustica, instancia, request);
        papelDetalhado = refinePapelDetalhado(usuario, tipoUsuario, papelDetalhado, ramoJustica, instancia, lotacao.payload(), request);
        String segmentoInstitucional = resolveSegmentoInstitucional(tipoUsuario, papelBase, papelDetalhado);
        String orgaoAssinante = resolveOrgaoAssinante(processo, usuario, tipoJustica, ramoJustica, instancia, request, lotacao.payload(), lotacao.etiquetaLotacao());
        EntryCertificateContext certificate = resolveEntryCertificate(request, usuario, papelDetalhado, ramoJustica, instancia, orgaoAssinante, esferaInstitucional, lotacao.payload(), lotacao.etiquetaLotacao());
        ResolvedPersonIdentity personIdentity = resolvePersonIdentity(usuario, request, certificate, papelDetalhado, lotacao.payload());
        LinkedHashSet<String> governanceTags = new LinkedHashSet<>();
        governanceTags.add("papel_" + normalizeTag(papelDetalhado));
        governanceTags.add("segmento_" + normalizeTag(segmentoInstitucional));
        governanceTags.add("justica_" + normalizeTag(ramoJustica));
        governanceTags.add("instancia_" + normalizeTag(instancia));
        governanceTags.add("esfera_" + normalizeTag(esferaInstitucional));
        if (!lotacao.tipoLotacao().isBlank()) {
            governanceTags.add("lotacao_" + normalizeTag(lotacao.tipoLotacao()));
        }
        if (!lotacao.unidadeJudiciariaCodigo().isBlank()) {
            governanceTags.add("unidade_" + normalizeTag(lotacao.unidadeJudiciariaCodigo()));
        }
        if (certificate.presente()) {
            governanceTags.add("certificado_entrada_vinculado");
            if (!certificate.fingerprintSha256().isBlank()) {
                governanceTags.add("certificado_digital_identificado");
            }
        }
        if (personIdentity.identidadeConferida()) {
            governanceTags.add("identidade_pessoal_conferida");
        }
        if (!personIdentity.nivelConfianca().isBlank()) {
            governanceTags.add("identidade_" + normalizeTag(personIdentity.nivelConfianca()));
        }
        if (!personIdentity.secretariaRecursal().isBlank()) {
            governanceTags.add("secretaria_recursal_ativa");
        }
        if (!personIdentity.secretariaEmbargos().isBlank()) {
            governanceTags.add("secretaria_embargos_ativa");
        }
        addGovernanceTag(governanceTags, "secretaria_especializada", (String) lotacao.payload().get("secretariaEspecializada"));
        addGovernanceTag(governanceTags, "painel_pjb", (String) lotacao.payload().get("painelPjb"));
        addGovernanceTag(governanceTags, "namespace_pjb", (String) lotacao.payload().get("namespacePjb"));
        addGovernanceTag(governanceTags, "secretaria_instancia", (String) lotacao.payload().get("secretariaInstanciaClassificada"));
        addGovernanceTag(governanceTags, "secretaria_ramo", (String) lotacao.payload().get("secretariaRamoClassificado"));
        LinkedHashMap<String, Object> classificacao = new LinkedHashMap<>();
        classificacao.put("papelDetalhado", papelDetalhado);
        classificacao.put("segmentoInstitucional", segmentoInstitucional);
        classificacao.put("ramoJustica", ramoJustica);
        classificacao.put("esferaInstitucional", esferaInstitucional);
        classificacao.put("instancia", instancia);
        classificacao.put("orgaoAssinante", orgaoAssinante);
        classificacao.put("politicaContextual", resolveContextualPolicy(papelDetalhado, ramoJustica, instancia));
        classificacao.put("registroHierarquico", papelDetalhado + "|" + ramoJustica + "|" + instancia + "|" + esferaInstitucional);
        classificacao.put("lotacaoInstitucional", lotacao.payload());
        classificacao.put("lotacaoAssinante", lotacao.etiquetaLotacao());
        classificacao.put("tipoLotacao", lotacao.tipoLotacao());
        classificacao.put("unidadeJudiciariaCodigo", lotacao.unidadeJudiciariaCodigo().isBlank() ? null : lotacao.unidadeJudiciariaCodigo());
        classificacao.put("identidadePessoal", personIdentity.payload());
        classificacao.put("coerenciaIdentitaria", personIdentity.coerenciaResumo());
        if (!personIdentity.secretariaRecursal().isBlank()) {
            classificacao.put("secretariaRecursal", personIdentity.secretariaRecursal());
        }
        if (!personIdentity.secretariaEmbargos().isBlank()) {
            classificacao.put("secretariaEmbargos", personIdentity.secretariaEmbargos());
        }
        copyIfPresent(classificacao, "secretariaEspecializada", lotacao.payload().get("secretariaEspecializada"));
        copyIfPresent(classificacao, "secretariaInstanciaClassificada", lotacao.payload().get("secretariaInstanciaClassificada"));
        copyIfPresent(classificacao, "secretariaRamoClassificado", lotacao.payload().get("secretariaRamoClassificado"));
        copyIfPresent(classificacao, "namespacePjb", lotacao.payload().get("namespacePjb"));
        copyIfPresent(classificacao, "painelPjb", lotacao.payload().get("painelPjb"));
        copyIfPresent(classificacao, "secretariaSegundaInstancia", lotacao.payload().get("secretariaSegundaInstancia"));
        copyIfPresent(classificacao, "secretariaInstanciaSuperior", lotacao.payload().get("secretariaInstanciaSuperior"));
        copyIfPresent(classificacao, "secretariaJuizadoEspecial", lotacao.payload().get("secretariaJuizadoEspecial"));
        copyIfPresent(classificacao, "secretariaTrabalhista", lotacao.payload().get("secretariaTrabalhista"));
        copyIfPresent(classificacao, "secretariaEleitoral", lotacao.payload().get("secretariaEleitoral"));
        copyIfPresent(classificacao, "secretariaMilitar", lotacao.payload().get("secretariaMilitar"));
        classificacao.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return new ResolvedSignatureIdentity(
                papelDetalhado,
                segmentoInstitucional,
                ramoJustica,
                esferaInstitucional,
                instancia,
                orgaoAssinante,
                lotacao.payload(),
                lotacao.etiquetaLotacao(),
                papelDetalhado + "@" + ramoJustica + "@" + instancia + "@" + normalizeUpper(lotacao.etiquetaLotacao()),
                Map.copyOf(classificacao),
                personIdentity,
                certificate,
                List.copyOf(governanceTags)
        );
    }

    private TipoJustica resolveTipoJustica(Processo processo, TipoUsuario tipoUsuario) {
        if (processo != null && processo.getTipoJustica() != null) {
            return processo.getTipoJustica();
        }
        if (tipoUsuario == null) {
            return TipoJustica.ESTADUAL;
        }
        if (tipoUsuario == TipoUsuario.JUIZ_FEDERAL
                || tipoUsuario == TipoUsuario.DESEMBARGADOR_FEDERAL
                || tipoUsuario == TipoUsuario.DELEGADO_POLICIA_FEDERAL
                || tipoUsuario == TipoUsuario.DEFENSOR_PUBLICO_FEDERAL
                || tipoUsuario == TipoUsuario.PROCURADORIA_FEDERAL
                || tipoUsuario == TipoUsuario.PROCURADOR_GERAL_REPUBLICA) {
            return TipoJustica.FEDERAL;
        }
        if (tipoUsuario == TipoUsuario.JUIZ_ELEITORAL || tipoUsuario == TipoUsuario.PROMOTOR_ELEITORAL) {
            return TipoJustica.ELEITORAL;
        }
        if (tipoUsuario == TipoUsuario.JUIZ_TRABALHISTA || tipoUsuario == TipoUsuario.PROMOTOR_TRABALHISTA) {
            return TipoJustica.TRABALHO;
        }
        if (tipoUsuario == TipoUsuario.JUIZ_MILITAR) {
            return TipoJustica.MILITAR_ESTADUAL;
        }
        if (tipoUsuario == TipoUsuario.MINISTRO) {
            return TipoJustica.SUPERIOR;
        }
        return TipoJustica.ESTADUAL;
    }

    private String resolveRamoJustica(TipoJustica tipoJustica, RitoProcessual rito, TipoUsuario tipoUsuario) {
        if (tipoJustica != null) {
            return switch (tipoJustica) {
                case ESTADUAL -> rito != null && rito.isJuizado() ? "ESTADUAL_JUIZADO_ESPECIAL" : "ESTADUAL";
                case FEDERAL -> rito == RitoProcessual.JUIZADO_ESPECIAL_FEDERAL ? "FEDERAL_JUIZADO_ESPECIAL" : "FEDERAL";
                case ELEITORAL -> "ELEITORAL";
                case TRABALHO -> "TRABALHO";
                case MILITAR_ESTADUAL -> "MILITAR_ESTADUAL";
                case MILITAR_FEDERAL -> "MILITAR_FEDERAL";
                case SUPERIOR -> "SUPERIOR";
            };
        }
        if (tipoUsuario == null) {
            return "NAO_CLASSIFICADO";
        }
        if (tipoUsuario == TipoUsuario.JUIZ_ESPECIAL) {
            return "ESTADUAL_JUIZADO_ESPECIAL";
        }
        if (tipoUsuario == TipoUsuario.JUIZ_ELEITORAL || tipoUsuario == TipoUsuario.PROMOTOR_ELEITORAL) {
            return "ELEITORAL";
        }
        if (tipoUsuario == TipoUsuario.JUIZ_TRABALHISTA || tipoUsuario == TipoUsuario.PROMOTOR_TRABALHISTA) {
            return "TRABALHO";
        }
        if (tipoUsuario == TipoUsuario.JUIZ_MILITAR) {
            return "MILITAR_ESTADUAL";
        }
        if (tipoUsuario == TipoUsuario.JUIZ_FEDERAL || tipoUsuario == TipoUsuario.DESEMBARGADOR_FEDERAL || tipoUsuario == TipoUsuario.DELEGADO_POLICIA_FEDERAL || tipoUsuario == TipoUsuario.DEFENSOR_PUBLICO_FEDERAL || tipoUsuario == TipoUsuario.PROCURADORIA_FEDERAL || tipoUsuario == TipoUsuario.PROCURADOR_GERAL_REPUBLICA) {
            return "FEDERAL";
        }
        if (tipoUsuario == TipoUsuario.MINISTRO) {
            return "SUPERIOR";
        }
        return "ESTADUAL";
    }

    private String resolveEsferaInstitucional(Usuario usuario, TipoUsuario tipoUsuario, String ramoJustica) {
        if (usuario != null && usuario.getEnteFederativo() != null) {
            return switch (usuario.getEnteFederativo()) {
                case UNIAO -> "FEDERAL";
                case ESTADO -> "ESTADUAL";
                case MUNICIPIO -> "MUNICIPAL";
                default -> fallbackEsfera(tipoUsuario, ramoJustica);
            };
        }
        return fallbackEsfera(tipoUsuario, ramoJustica);
    }

    private String fallbackEsfera(TipoUsuario tipoUsuario, String ramoJustica) {
        if (tipoUsuario == TipoUsuario.PROCURADORIA_MUNICIPAL) {
            return "MUNICIPAL";
        }
        if (tipoUsuario == TipoUsuario.PROCURADORIA_ESTADUAL
                || tipoUsuario == TipoUsuario.PROMOTOR_ELEITORAL
                || tipoUsuario == TipoUsuario.PROMOTOR_TRABALHISTA
                || tipoUsuario == TipoUsuario.MEMBRO_MINISTERIO_PUBLICO
                || tipoUsuario == TipoUsuario.JUIZ_ESTADUAL
                || tipoUsuario == TipoUsuario.JUIZ_ESPECIAL
                || tipoUsuario == TipoUsuario.JUIZ_ELEITORAL
                || tipoUsuario == TipoUsuario.JUIZ_TRABALHISTA
                || tipoUsuario == TipoUsuario.JUIZ_MILITAR
                || tipoUsuario == TipoUsuario.DESEMBARGADOR) {
            return "ESTADUAL";
        }
        if (tipoUsuario == TipoUsuario.JUIZ_FEDERAL
                || tipoUsuario == TipoUsuario.DESEMBARGADOR_FEDERAL
                || tipoUsuario == TipoUsuario.DELEGADO_POLICIA_FEDERAL
                || tipoUsuario == TipoUsuario.DEFENSOR_PUBLICO_FEDERAL
                || tipoUsuario == TipoUsuario.PROCURADORIA_FEDERAL
                || tipoUsuario == TipoUsuario.PROCURADOR_GERAL_REPUBLICA
                || tipoUsuario == TipoUsuario.MINISTRO) {
            return "FEDERAL";
        }
        if (ramoJustica.startsWith("FEDERAL") || ramoJustica.startsWith("SUPERIOR")) {
            return "FEDERAL";
        }
        if (ramoJustica.startsWith("ELEITORAL") || ramoJustica.startsWith("TRABALHO") || ramoJustica.startsWith("MILITAR")) {
            return "ESTADUAL";
        }
        return "ESTADUAL";
    }

    private String resolveInstancia(Processo processo, TipoUsuario tipoUsuario, String ramoJustica, HttpServletRequest request) {
        String header = firstHeader(request, CERTIFICATE_INSTANCE_HEADERS);
        if (!header.isBlank()) {
            return normalizeInstancia(header);
        }
        if (tipoUsuario == TipoUsuario.DESEMBARGADOR || tipoUsuario == TipoUsuario.DESEMBARGADOR_FEDERAL) {
            return "SEGUNDO_GRAU";
        }
        if (tipoUsuario == TipoUsuario.MINISTRO) {
            String tribunal = normalizeUpper(processo == null ? null : processo.getTribunal());
            return tribunal.contains("STF") || tribunal.contains("SUPREMO") ? "CONSTITUCIONAL" : "SUPERIOR";
        }
        if (tipoUsuario != null && tipoUsuario.isMagistratura()) {
            return "PRIMEIRO_GRAU";
        }
        String tribunal = normalizeUpper(processo == null ? null : processo.getTribunal());
        if (tribunal.startsWith("TJ") || tribunal.startsWith("TRF") || tribunal.startsWith("TRE") || tribunal.startsWith("TRT") || tribunal.startsWith("TJM")) {
            return "SEGUNDO_GRAU";
        }
        if (tribunal.startsWith("STJ") || tribunal.startsWith("TST") || tribunal.startsWith("TSE") || tribunal.startsWith("STM")) {
            return "SUPERIOR";
        }
        if (tribunal.startsWith("STF")) {
            return "CONSTITUCIONAL";
        }
        if (ramoJustica.equals("SUPERIOR")) {
            return "SUPERIOR";
        }
        return "PRIMEIRO_GRAU";
    }

    private String resolvePapelDetalhado(Usuario usuario,
                                         TipoUsuario tipoUsuario,
                                         String ramoJustica,
                                         String instancia,
                                         String papelBase,
                                         HttpServletRequest request) {
        String headerRole = firstHeader(request, CERTIFICATE_ROLE_HEADERS);
        if (!headerRole.isBlank()) {
            return normalizeUpper(headerRole);
        }
        if (tipoUsuario == null) {
            return papelBase.isBlank() ? "PERFIL_NAO_IDENTIFICADO" : papelBase;
        }
        return switch (tipoUsuario) {
            case JUIZ, JUIZ_ESTADUAL -> "JUIZ_ESTADUAL_" + instancia;
            case JUIZ_FEDERAL -> "JUIZ_FEDERAL_" + instancia;
            case JUIZ_ESPECIAL -> "JUIZ_JUIZADO_ESPECIAL_" + instancia;
            case JUIZ_ELEITORAL -> "JUIZ_ELEITORAL_" + instancia;
            case JUIZ_TRABALHISTA -> "JUIZ_TRABALHISTA_" + instancia;
            case JUIZ_MILITAR -> "JUIZ_MILITAR_" + instancia;
            case DESEMBARGADOR -> "DESEMBARGADOR_ESTADUAL_SEGUNDO_GRAU";
            case DESEMBARGADOR_FEDERAL -> "DESEMBARGADOR_FEDERAL_SEGUNDO_GRAU";
            case MINISTRO -> instancia.equals("CONSTITUCIONAL") ? "MINISTRO_CONSTITUCIONAL" : "MINISTRO_TRIBUNAL_SUPERIOR";
            case MEMBRO_MINISTERIO_PUBLICO -> ramoJustica.startsWith("FEDERAL") ? "PROMOTOR_FEDERAL" : "PROMOTOR_ESTADUAL";
            case PROMOTOR_ELEITORAL -> "PROMOTOR_ELEITORAL";
            case PROMOTOR_TRABALHISTA -> "PROMOTOR_TRABALHISTA";
            case PROCURADOR_GERAL_REPUBLICA -> "PROCURADOR_GERAL_REPUBLICA";
            case DEFENSOR_PUBLICO -> "DEFENSOR_PUBLICO_ESTADUAL";
            case DEFENSOR_PUBLICO_FEDERAL -> "DEFENSOR_PUBLICO_FEDERAL";
            case PROCURADOR -> resolveProcuradorDetalhado(usuario);
            case PROCURADORIA_MUNICIPAL -> "PROCURADOR_GERAL_MUNICIPAL";
            case PROCURADORIA_ESTADUAL -> "PROCURADOR_GERAL_ESTADUAL";
            case PROCURADORIA_FEDERAL -> "PROCURADOR_GERAL_FEDERAL";
            case OFICIAL_JUSTICA -> "OFICIAL_JUSTICA_CUMPRIMENTO";
            case OFICIAL_JUSTICA_AVALIADOR -> "OFICIAL_JUSTICA_AVALIADOR";
            case DELEGADO_POLICIA -> "DELEGADO_POLICIA_ESTADUAL";
            case DELEGADO_POLICIA_FEDERAL -> "DELEGADO_POLICIA_FEDERAL";
            case ADVOGADO -> "ADVOGADO";
            case CONCILIADOR_CEJUSC -> "CONCILIADOR_CEJUSC";
            case MEDIADOR -> "MEDIADOR";
            default -> !papelBase.isBlank() ? papelBase : tipoUsuario.name();
        };
    }

    private String resolveProcuradorDetalhado(Usuario usuario) {
        EnteFederativo ente = usuario == null ? null : usuario.getEnteFederativo();
        if (ente == EnteFederativo.MUNICIPIO) {
            return "PROCURADOR_GERAL_MUNICIPAL";
        }
        if (ente == EnteFederativo.UNIAO) {
            return "PROCURADOR_GERAL_FEDERAL";
        }
        if (ente == EnteFederativo.ESTADO) {
            return "PROCURADOR_GERAL_ESTADUAL";
        }
        return "PROCURADOR";
    }

    private String resolveSegmentoInstitucional(TipoUsuario tipoUsuario, String papelBase, String papelDetalhado) {
        if (tipoUsuario != null) {
            if (tipoUsuario.isMagistratura()) {
                return "MAGISTRATURA";
            }
            if (tipoUsuario.isMinisterioPublico()) {
                return "MINISTERIO_PUBLICO";
            }
            if (tipoUsuario.isDefensoriaPublica()) {
                return "DEFENSORIA_PUBLICA";
            }
            if (tipoUsuario.isProcuradoria()) {
                return "PROCURADORIA";
            }
            if (tipoUsuario.isSegurancaPublica()) {
                return "POLICIA_JUDICIARIA";
            }
            if (tipoUsuario.isAdvocacia()) {
                return "ADVOCACIA";
            }
            if (tipoUsuario.isServidorJudiciario()) {
                return "UNIDADE_JUDICIAL";
            }
            if (tipoUsuario == TipoUsuario.OFICIAL_JUSTICA || tipoUsuario == TipoUsuario.OFICIAL_JUSTICA_AVALIADOR) {
                return "OFICIAL_JUSTICA";
            }
        }
        return "ASSINANTE_INSTITUCIONAL";
    }

    private String refinePapelDetalhado(Usuario usuario,
                                         TipoUsuario tipoUsuario,
                                         String papelDetalhado,
                                         String ramoJustica,
                                         String instancia,
                                         Map<String, Object> lotacaoInstitucional,
                                         HttpServletRequest request) {
        return QualifiedSignatureInstitutionalAssignmentSupport.refinePapelDetalhado(
                usuario,
                tipoUsuario,
                papelDetalhado,
                ramoJustica,
                instancia,
                lotacaoInstitucional,
                request
        );
    }

    private InstitutionalAssignment resolveInstitutionalAssignment(Processo processo,
                                                                   Usuario usuario,
                                                                   TipoJustica tipoJustica,
                                                                   String ramoJustica,
                                                                   String instancia,
                                                                   HttpServletRequest request) {
        return QualifiedSignatureInstitutionalAssignmentSupport.resolveInstitutionalAssignment(
                processo,
                usuario,
                tipoJustica,
                ramoJustica,
                instancia,
                request
        );
    }

    private String resolveOrgaoAssinante(Processo processo,
                                         Usuario usuario,
                                         TipoJustica tipoJustica,
                                         String ramoJustica,
                                         String instancia,
                                         HttpServletRequest request,
                                         Map<String, Object> lotacaoInstitucional,
                                         String lotacaoAssinante) {
        return QualifiedSignatureInstitutionalAssignmentSupport.resolveOrgaoAssinante(
                processo,
                usuario,
                tipoJustica,
                ramoJustica,
                instancia,
                request,
                lotacaoInstitucional,
                lotacaoAssinante
        );
    }

    private EntryCertificateContext resolveEntryCertificate(HttpServletRequest request,
                                                            Usuario usuario,
                                                            String papelDetalhado,
                                                            String ramoJustica,
                                                            String instancia,
                                                            String orgaoAssinante,
                                                            String esferaInstitucional,
                                                            Map<String, Object> lotacaoInstitucional,
                                                            String lotacaoAssinante) {
        return QualifiedSignatureCertificateContextSupport.resolveEntryCertificate(
                request,
                usuario,
                papelDetalhado,
                ramoJustica,
                instancia,
                orgaoAssinante,
                esferaInstitucional,
                lotacaoInstitucional,
                lotacaoAssinante
        );
    }

    private ResolvedPersonIdentity resolvePersonIdentity(Usuario usuario,
                                                         HttpServletRequest request,
                                                         EntryCertificateContext certificate,
                                                         String papelDetalhado,
                                                         Map<String, Object> lotacaoInstitucional) {
        return QualifiedSignaturePersonIdentitySupport.resolvePersonIdentity(
                usuario,
                request,
                certificate,
                papelDetalhado,
                lotacaoInstitucional
        );
    }

    private String resolveContextualPolicy(String papelDetalhado, String ramoJustica, String instancia) {
        return normalizeUpper(papelDetalhado) + "_" + normalizeUpper(ramoJustica) + "_" + normalizeUpper(instancia) + "_QUALIFICADA_SOBERANA";
    }

    private String normalizeInstancia(String raw) {
        String token = normalizeUpper(raw);
        if (token.contains("CONSTITUC")) {
            return "CONSTITUCIONAL";
        }
        if (token.contains("SUPERIOR") || token.contains("ULTIMA") || token.contains("ULTIMO")) {
            return "SUPERIOR";
        }
        if (token.contains("2") || token.contains("SEGUNDO")) {
            return "SEGUNDO_GRAU";
        }
        return "PRIMEIRO_GRAU";
    }

    static String firstHeader(HttpServletRequest request, String... names) {
        if (request == null || names == null) {
            return "";
        }
        for (String name : names) {
            String value = request.getHeader(name);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }

    static boolean hasAnyHeader(HttpServletRequest request, String[]... headerGroups) {
        if (request == null || headerGroups == null) {
            return false;
        }
        for (String[] group : headerGroups) {
            if (group == null) {
                continue;
            }
            for (String name : group) {
                if (request.getHeader(name) != null && !request.getHeader(name).isBlank()) {
                    return true;
                }
            }
        }
        return false;
    }

    static String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    static String normalizeFingerprint(String value) {
        String normalized = normalizeToNull(value);
        if (normalized == null) {
            return "";
        }
        return normalized.replace(":", "").replace(" ", "").toUpperCase(Locale.ROOT);
    }

    static String normalizeUpper(String value) {
        return value == null ? "" : value.trim().replace('º', 'O').replace('ª', 'A').replace('°', 'O').replace(' ', '_').replace('-', '_').toUpperCase(Locale.ROOT);
    }

    private void addGovernanceTag(Set<String> target, String prefix, String value) {
        if (target == null || prefix == null || value == null || value.isBlank()) {
            return;
        }
        target.add(prefix + '_' + normalizeTag(value));
    }

    private void copyIfPresent(Map<String, Object> target, String key, Object value) {
        if (target == null || key == null || value == null) {
            return;
        }
        if (value instanceof String s && s.isBlank()) {
            return;
        }
        target.put(key, value);
    }

    private String normalizeTag(String value) {
        return normalizeUpper(value).toLowerCase(Locale.ROOT);
    }

    static String normalizeToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    static String trimToNull(String value) {
        return normalizeToNull(value);
    }

    static String defaultString(String value) {
        return value == null ? "" : value;
    }

    static String resolveCertificateFingerprint(X509Certificate certificate, String fallbackFingerprint) {
        if (certificate == null) {
            return defaultString(normalizeFingerprint(fallbackFingerprint));
        }
        try {
            return sha256Hex(certificate.getEncoded());
        } catch (CertificateEncodingException e) {
            return defaultString(normalizeFingerprint(fallbackFingerprint));
        }
    }

    static String sha256Hex(byte[] input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input);
            StringBuilder out = new StringBuilder(bytes.length * 2);
            for (byte value : bytes) {
                out.append(String.format(Locale.ROOT, "%02x", value));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        } catch (Exception e) {
            return "";
        }
    }

}
