package com.tcc.pjb.backend.service.processual.document.identity;

import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_AUDITORIA_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_BRANCH_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_CAMARA_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_COMARCA_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_EMBARGOS_SECRETARIAT_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_ELEITORAL_SECRETARIAT_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_GABINETE_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_JUIZADO_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_JUIZADO_SECRETARIAT_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_MILITAR_SECRETARIAT_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_ORG_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_ORGAO_FRACIONARIO_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_RECURSAL_SECRETARIAT_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_ROLE_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_SCOPE_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_SECAO_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_SECOND_INSTANCE_SECRETARIAT_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_SECRETARIAT_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_SUBJECT_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_SUBSECAO_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_SUPERIOR_INSTANCE_SECRETARIAT_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_TRABALHISTA_SECRETARIAT_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_TRIBUNAL_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_TURMA_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_UF_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_UNIT_CODE_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_VARA_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_ZONE_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.TRIBUNAL_HINT_PATTERN;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.defaultString;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.firstHeader;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.firstNonBlank;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.hasAnyHeader;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.normalizeUpper;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.trimToNull;

import com.tcc.pjb.backend.domain.enums.TipoJustica;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import jakarta.servlet.http.HttpServletRequest;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import javax.security.auth.x500.X500Principal;

final class QualifiedSignatureInstitutionalAssignmentSupport {

    private QualifiedSignatureInstitutionalAssignmentSupport() {
    }

    static String refinePapelDetalhado(Usuario usuario,
                                       TipoUsuario tipoUsuario,
                                       String papelDetalhado,
                                       String ramoJustica,
                                       String instancia,
                                       Map<String, Object> lotacaoInstitucional,
                                       HttpServletRequest request) {
        String headerRole = firstHeader(request, CERTIFICATE_ROLE_HEADERS);
        if (!headerRole.isBlank()) {
            return normalizeUpper(headerRole);
        }
        if (tipoUsuario == null || !tipoUsuario.isServidorJudiciario()) {
            return papelDetalhado;
        }
        String instanciaSecretaria = trimToNull((String) lotacaoInstitucional.get("secretariaInstanciaClassificada"));
        String ramoSecretaria = trimToNull((String) lotacaoInstitucional.get("secretariaRamoClassificado"));
        String base = firstNonBlank((String) lotacaoInstitucional.get("secretariaEspecializada"), (String) lotacaoInstitucional.get("secretaria"));
        if (base != null && normalizeUpper(base).contains("EMBARGO")) {
            return "SECRETARIA_EMBARGOS_" + normalizeUpper(instanciaSecretaria == null ? instancia : instanciaSecretaria) + '_' + normalizeUpper(ramoSecretaria == null ? ramoJustica : ramoSecretaria);
        }
        if (base != null && (normalizeUpper(base).contains("RECURSAL") || normalizeUpper(base).contains("ADMISSIBILIDADE"))) {
            return "SECRETARIA_RECURSAL_" + normalizeUpper(instanciaSecretaria == null ? instancia : instanciaSecretaria) + '_' + normalizeUpper(ramoSecretaria == null ? ramoJustica : ramoSecretaria);
        }
        if (instanciaSecretaria != null || ramoSecretaria != null) {
            return "SECRETARIA_" + normalizeUpper(firstNonBlank(instanciaSecretaria, instancia)) + '_' + normalizeUpper(firstNonBlank(ramoSecretaria, ramoJustica));
        }
        return papelDetalhado;
    }

    static InstitutionalAssignment resolveInstitutionalAssignment(Processo processo,
                                                                  Usuario usuario,
                                                                  TipoJustica tipoJustica,
                                                                  String ramoJustica,
                                                                  String instancia,
                                                                  HttpServletRequest request) {
        List<String> clues = collectInstitutionalClues(request);
        String tribunal = firstNonBlank(
                firstHeader(request, CERTIFICATE_TRIBUNAL_HEADERS),
                processo == null ? null : processo.getTribunal(),
                extractTribunalHint(clues),
                defaultCourtLabel(tipoJustica, ramoJustica, instancia)
        );
        String unidadeJudiciariaCodigo = firstNonBlank(
                firstHeader(request, CERTIFICATE_UNIT_CODE_HEADERS),
                processo == null ? null : processo.getUnidadeJudiciariaCodigo()
        );
        String vara = firstNonBlank(
                firstHeader(request, CERTIFICATE_VARA_HEADERS),
                processo == null ? null : processo.getVara(),
                inferInstitutionalField(clues, "VARA")
        );
        String zonaEleitoral = firstNonBlank(
                firstHeader(request, CERTIFICATE_ZONE_HEADERS),
                inferInstitutionalField(clues, "ZONA ELEITORAL", "ZE ")
        );
        String juizado = firstNonBlank(
                firstHeader(request, CERTIFICATE_JUIZADO_HEADERS),
                inferInstitutionalField(clues, "JUIZADO")
        );
        if (juizado == null && ramoJustica.contains("JUIZADO") && vara != null && normalizeUpper(vara).contains("JUIZADO")) {
            juizado = vara;
        }
        String turma = firstNonBlank(
                firstHeader(request, CERTIFICATE_TURMA_HEADERS),
                inferInstitutionalField(clues, "TURMA")
        );
        String camara = firstNonBlank(
                firstHeader(request, CERTIFICATE_CAMARA_HEADERS),
                inferInstitutionalField(clues, "CAMARA", "CÂMARA")
        );
        String secao = firstNonBlank(
                firstHeader(request, CERTIFICATE_SECAO_HEADERS),
                inferInstitutionalField(clues, "SECAO", "SEÇÃO")
        );
        String subsecao = firstNonBlank(
                firstHeader(request, CERTIFICATE_SUBSECAO_HEADERS),
                inferInstitutionalField(clues, "SUBSECAO", "SUBSEÇÃO")
        );
        String auditoria = firstNonBlank(
                firstHeader(request, CERTIFICATE_AUDITORIA_HEADERS),
                inferInstitutionalField(clues, "AUDITORIA")
        );
        String orgaoFracionario = firstNonBlank(
                firstHeader(request, CERTIFICATE_ORGAO_FRACIONARIO_HEADERS),
                inferInstitutionalField(clues, "ORGAO ESPECIAL", "ÓRGÃO ESPECIAL", "CORTE ESPECIAL", "PLENARIO", "PLENÁRIO")
        );
        String gabinete = firstNonBlank(
                firstHeader(request, CERTIFICATE_GABINETE_HEADERS),
                inferInstitutionalField(clues, "GABINETE", "RELATORIA")
        );
        String comarca = firstNonBlank(
                firstHeader(request, CERTIFICATE_COMARCA_HEADERS),
                processo == null ? null : processo.getComarca(),
                usuario == null ? null : usuario.getComarca(),
                inferInstitutionalField(clues, "COMARCA")
        );
        String uf = firstNonBlank(
                firstHeader(request, CERTIFICATE_UF_HEADERS),
                processo == null ? null : processo.getUf(),
                usuario == null ? null : usuario.getUf()
        );
        String secretaria = firstNonBlank(
                firstHeader(request, CERTIFICATE_SECRETARIAT_HEADERS),
                inferInstitutionalField(clues, "SECRETARIA", "SECRETARIAT", "SECRETARIA JUDICIARIA", "SECRETARIA-GERAL JUDICIARIA")
        );
        String secretariaRecursal = firstNonBlank(
                firstHeader(request, CERTIFICATE_RECURSAL_SECRETARIAT_HEADERS),
                inferInstitutionalField(clues, "ADMISSIBILIDADE RECURSAL", "SECRETARIA RECURSAL", "MESA DE ADMISSIBILIDADE", "SEAR")
        );
        String secretariaEmbargos = firstNonBlank(
                firstHeader(request, CERTIFICATE_EMBARGOS_SECRETARIAT_HEADERS),
                inferInstitutionalField(clues, "EMBARGOS", "REVIEW DESK", "MESA DE REVISAO", "MESA DE REVISÃO")
        );
        SpecializedSecretariatProfile specializedSecretariat = resolveSpecializedSecretariatProfile(
                request,
                clues,
                ramoJustica,
                instancia,
                tribunal,
                juizado,
                zonaEleitoral,
                auditoria,
                secretaria,
                secretariaRecursal,
                secretariaEmbargos
        );
        String tipoLotacao = firstNonBlank(
                zonaEleitoral != null ? "ZONA_ELEITORAL" : null,
                auditoria != null ? "AUDITORIA" : null,
                vara != null ? "VARA" : null,
                juizado != null ? "JUIZADO" : null,
                camara != null ? "CAMARA" : null,
                turma != null ? "TURMA" : null,
                secao != null ? "SECAO" : null,
                subsecao != null ? "SUBSECAO" : null,
                gabinete != null ? "GABINETE" : null,
                orgaoFracionario != null ? "ORGAO_FRACIONARIO" : null,
                secretariaRecursal != null ? "SECRETARIA_RECURSAL" : null,
                secretariaEmbargos != null ? "SECRETARIA_EMBARGOS" : null,
                specializedSecretariat.tipoLotacao(),
                secretaria != null ? "SECRETARIA" : null,
                "TRIBUNAL"
        );
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("tribunal", trimToNull(tribunal));
        payload.put("unidadeJudiciariaCodigo", trimToNull(unidadeJudiciariaCodigo));
        payload.put("vara", trimToNull(vara));
        payload.put("zonaEleitoral", trimToNull(zonaEleitoral));
        payload.put("juizado", trimToNull(juizado));
        payload.put("turma", trimToNull(turma));
        payload.put("camara", trimToNull(camara));
        payload.put("secao", trimToNull(secao));
        payload.put("subsecao", trimToNull(subsecao));
        payload.put("auditoria", trimToNull(auditoria));
        payload.put("orgaoFracionario", trimToNull(orgaoFracionario));
        payload.put("gabinete", trimToNull(gabinete));
        payload.put("comarca", trimToNull(comarca));
        payload.put("uf", trimToNull(uf == null ? null : uf.toUpperCase(Locale.ROOT)));
        payload.put("secretaria", trimToNull(secretaria));
        payload.put("secretariaRecursal", trimToNull(secretariaRecursal));
        payload.put("secretariaEmbargos", trimToNull(secretariaEmbargos));
        payload.put("secretariaSegundaInstancia", trimToNull(specializedSecretariat.secretariaSegundaInstancia()));
        payload.put("secretariaInstanciaSuperior", trimToNull(specializedSecretariat.secretariaInstanciaSuperior()));
        payload.put("secretariaJuizadoEspecial", trimToNull(specializedSecretariat.secretariaJuizadoEspecial()));
        payload.put("secretariaTrabalhista", trimToNull(specializedSecretariat.secretariaTrabalhista()));
        payload.put("secretariaEleitoral", trimToNull(specializedSecretariat.secretariaEleitoral()));
        payload.put("secretariaMilitar", trimToNull(specializedSecretariat.secretariaMilitar()));
        payload.put("secretariaEspecializada", trimToNull(specializedSecretariat.secretariaEspecializada()));
        payload.put("secretariaInstanciaClassificada", trimToNull(specializedSecretariat.secretariaInstanciaClassificada()));
        payload.put("secretariaRamoClassificado", trimToNull(specializedSecretariat.secretariaRamoClassificado()));
        payload.put("namespacePjb", trimToNull(specializedSecretariat.namespacePjb()));
        payload.put("painelPjb", trimToNull(specializedSecretariat.painelPjb()));
        payload.put("tipoLotacao", tipoLotacao);
        payload.put("origemIdentificacao", detectInstitutionalAssignmentSource(request, clues, processo));
        String etiquetaLotacao = buildInstitutionalAssignmentLabel(payload);
        payload.put("etiquetaLotacao", etiquetaLotacao);
        payload.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return new InstitutionalAssignment(Collections.unmodifiableMap(payload), etiquetaLotacao, defaultString(unidadeJudiciariaCodigo), tipoLotacao);
    }

    static String resolveOrgaoAssinante(Processo processo,
                                        Usuario usuario,
                                        TipoJustica tipoJustica,
                                        String ramoJustica,
                                        String instancia,
                                        HttpServletRequest request,
                                        Map<String, Object> lotacaoInstitucional,
                                        String lotacaoAssinante) {
        String headerOrg = firstHeader(request, CERTIFICATE_ORG_HEADERS);
        if (!headerOrg.isBlank()) {
            return headerOrg;
        }
        String tribunal = trimToNull((String) lotacaoInstitucional.get("tribunal"));
        if (tribunal == null) {
            tribunal = trimToNull(processo == null ? null : processo.getTribunal());
        }
        String comarca = trimToNull((String) lotacaoInstitucional.get("comarca"));
        if (comarca == null && usuario != null) {
            comarca = trimToNull(usuario.getComarca());
        }
        String uf = normalizeUpper(firstNonBlank((String) lotacaoInstitucional.get("uf"), processo == null ? null : processo.getUf(), usuario == null ? null : usuario.getUf()));
        ArrayList<String> parts = new ArrayList<>();
        parts.add(firstNonBlank(tribunal, defaultCourtLabel(tipoJustica, ramoJustica, instancia)));
        if (lotacaoAssinante != null && !lotacaoAssinante.isBlank() && !normalizeUpper(lotacaoAssinante).contains(normalizeUpper(firstNonBlank(tribunal, "")))) {
            parts.add(lotacaoAssinante);
        }
        if (comarca != null) {
            parts.add(comarca + (uf.isBlank() ? "" : "/" + uf));
        } else if (!uf.isBlank()) {
            parts.add(uf);
        }
        return parts.stream().filter(Objects::nonNull).filter(v -> !v.isBlank()).distinct().reduce((a, b) -> a + " | " + b).orElse("ORGAO_NAO_IDENTIFICADO");
    }

    static String defaultCourtLabel(TipoJustica tipoJustica, String ramoJustica, String instancia) {
        if (instancia.equals("CONSTITUCIONAL")) {
            return "STF";
        }
        if (instancia.equals("SUPERIOR")) {
            return switch (ramoJustica) {
                case "TRABALHO" -> "TST";
                case "ELEITORAL" -> "TSE";
                case "MILITAR_FEDERAL", "MILITAR_ESTADUAL" -> "STM";
                default -> "STJ";
            };
        }
        if (instancia.equals("SEGUNDO_GRAU")) {
            if (tipoJustica == TipoJustica.FEDERAL) {
                return "TRF";
            }
            if (tipoJustica == TipoJustica.ELEITORAL) {
                return "TRE";
            }
            if (tipoJustica == TipoJustica.TRABALHO) {
                return "TRT";
            }
            if (tipoJustica == TipoJustica.MILITAR_ESTADUAL || tipoJustica == TipoJustica.MILITAR_FEDERAL) {
                return "TJM";
            }
            return "TJ";
        }
        if (tipoJustica == TipoJustica.FEDERAL) {
            return "JUSTICA FEDERAL";
        }
        if (tipoJustica == TipoJustica.ELEITORAL) {
            return "JUSTICA ELEITORAL";
        }
        if (tipoJustica == TipoJustica.TRABALHO) {
            return "JUSTICA DO TRABALHO";
        }
        if (tipoJustica == TipoJustica.MILITAR_ESTADUAL || tipoJustica == TipoJustica.MILITAR_FEDERAL) {
            return "JUSTICA MILITAR";
        }
        return "JUSTICA ESTADUAL";
    }

    private static SpecializedSecretariatProfile resolveSpecializedSecretariatProfile(HttpServletRequest request,
                                                                                      List<String> clues,
                                                                                      String ramoJustica,
                                                                                      String instancia,
                                                                                      String tribunal,
                                                                                      String juizado,
                                                                                      String zonaEleitoral,
                                                                                      String auditoria,
                                                                                      String secretariaBase,
                                                                                      String secretariaRecursal,
                                                                                      String secretariaEmbargos) {
        String segundaInstancia = firstNonBlank(
                firstHeader(request, CERTIFICATE_SECOND_INSTANCE_SECRETARIAT_HEADERS),
                "SEGUNDO_GRAU".equals(instancia) ? buildSpecializedSecretariatName(ramoJustica, instancia, tribunal) : null,
                inferInstitutionalField(clues, "SEGUNDA INSTANCIA", "2G", "CAMARA", "TURMA RECURSAL")
        );
        String instanciaSuperior = firstNonBlank(
                firstHeader(request, CERTIFICATE_SUPERIOR_INSTANCE_SECRETARIAT_HEADERS),
                ("SUPERIOR".equals(instancia) || "CONSTITUCIONAL".equals(instancia)) ? buildSpecializedSecretariatName(ramoJustica, instancia, tribunal) : null,
                inferInstitutionalField(clues, "INSTANCIA SUPERIOR", "ULTIMA INSTANCIA", "SECRETARIA JUDICIARIA", "SECRETARIA GERAL JUDICIARIA")
        );
        String juizadoEspecial = firstNonBlank(
                firstHeader(request, CERTIFICATE_JUIZADO_SECRETARIAT_HEADERS),
                juizado != null ? buildJuizadoSecretariatName(juizado) : null,
                ramoJustica.contains("JUIZADO") ? buildJuizadoSecretariatName(juizado) : null,
                inferInstitutionalField(clues, "JUIZADO", "TURMA RECURSAL")
        );
        String trabalhista = firstNonBlank(
                firstHeader(request, CERTIFICATE_TRABALHISTA_SECRETARIAT_HEADERS),
                ramoJustica.contains("TRABALHO") ? buildSpecializedSecretariatName("TRABALHO", instancia, tribunal) : null,
                inferInstitutionalField(clues, "TRABALHO", "TRT", "TST")
        );
        String eleitoral = firstNonBlank(
                firstHeader(request, CERTIFICATE_ELEITORAL_SECRETARIAT_HEADERS),
                ramoJustica.contains("ELEITORAL") ? buildElectoralSecretariatName(instancia, tribunal, zonaEleitoral) : null,
                inferInstitutionalField(clues, "ELEITORAL", "TRE", "TSE", "ZONA ELEITORAL")
        );
        String militar = firstNonBlank(
                firstHeader(request, CERTIFICATE_MILITAR_SECRETARIAT_HEADERS),
                ramoJustica.contains("MILITAR") ? buildMilitarySecretariatName(instancia, tribunal, auditoria) : null,
                inferInstitutionalField(clues, "AUDITORIA", "MILITAR", "STM", "TJM")
        );
        String secretariaInstanciaClassificada = firstNonBlank(
                "SEGUNDO_GRAU".equals(instancia) ? "SEGUNDA_INSTANCIA" : null,
                ("SUPERIOR".equals(instancia) || "CONSTITUCIONAL".equals(instancia)) ? "INSTANCIA_SUPERIOR" : null,
                ramoJustica.contains("JUIZADO") ? "JUIZADO_ESPECIAL" : null,
                "PRIMEIRO_GRAU"
        );
        String secretariaRamoClassificado = firstNonBlank(
                ramoJustica.contains("JUIZADO") ? "JUIZADO_ESPECIAL" : null,
                ramoJustica.contains("TRABALHO") ? "TRABALHISTA" : null,
                ramoJustica.contains("ELEITORAL") ? "ELEITORAL" : null,
                ramoJustica.contains("MILITAR") ? "MILITAR" : null,
                ramoJustica.contains("FEDERAL") ? "FEDERAL" : null,
                "ESTADUAL"
        );
        String secretariaEspecializada = firstNonBlank(
                secretariaEmbargos,
                secretariaRecursal,
                eleitoral,
                trabalhista,
                militar,
                juizadoEspecial,
                segundaInstancia,
                instanciaSuperior,
                secretariaBase,
                buildSpecializedSecretariatName(ramoJustica, instancia, tribunal)
        );
        String namespacePjb = resolvePjbNamespace(instancia, ramoJustica);
        String painelPjb = resolvePjbDisplayName(namespacePjb, secretariaInstanciaClassificada, secretariaRamoClassificado);
        return new SpecializedSecretariatProfile(
                defaultString(segundaInstancia),
                defaultString(instanciaSuperior),
                defaultString(juizadoEspecial),
                defaultString(trabalhista),
                defaultString(eleitoral),
                defaultString(militar),
                defaultString(secretariaEspecializada),
                defaultString(secretariaInstanciaClassificada),
                defaultString(secretariaRamoClassificado),
                defaultString(namespacePjb),
                defaultString(painelPjb),
                deriveSpecializedLotacaoType(secretariaEspecializada, secretariaInstanciaClassificada, secretariaRamoClassificado)
        );
    }

    private static String deriveSpecializedLotacaoType(String secretariaEspecializada, String secretariaInstanciaClassificada, String secretariaRamoClassificado) {
        String normalized = normalizeUpper(secretariaEspecializada);
        if (normalized.contains("EMBARGO")) {
            return "SECRETARIA_EMBARGOS";
        }
        if (normalized.contains("RECURSAL") || normalized.contains("ADMISSIBILIDADE")) {
            return "SECRETARIA_RECURSAL";
        }
        if (secretariaInstanciaClassificada == null && secretariaRamoClassificado == null) {
            return null;
        }
        return "SECRETARIA_" + normalizeUpper(defaultString(secretariaInstanciaClassificada)) + '_' + normalizeUpper(defaultString(secretariaRamoClassificado));
    }

    private static String resolvePjbNamespace(String instancia, String ramoJustica) {
        if ("SEGUNDO_GRAU".equals(instancia)) {
            return "PJB_SEGUNDA_INSTANCIA";
        }
        if ("SUPERIOR".equals(instancia) || "CONSTITUCIONAL".equals(instancia)) {
            return "PJB_INSTANCIA_SUPERIOR";
        }
        if (ramoJustica.contains("JUIZADO")) {
            return "PJB_JUIZADO_ESPECIAL";
        }
        if (ramoJustica.contains("TRABALHO")) {
            return "PJB_TRABALHISTA";
        }
        if (ramoJustica.contains("ELEITORAL")) {
            return "PJB_ELEITORAL";
        }
        if (ramoJustica.contains("MILITAR")) {
            return "PJB_MILITAR";
        }
        if (ramoJustica.contains("FEDERAL")) {
            return "PJB_FEDERAL";
        }
        return "PJB_ESTADUAL";
    }

    private static String resolvePjbDisplayName(String namespacePjb, String secretariaInstanciaClassificada, String secretariaRamoClassificado) {
        ArrayList<String> parts = new ArrayList<>();
        String base = switch (normalizeUpper(namespacePjb)) {
            case "PJB_SEGUNDA_INSTANCIA" -> "PJB Segunda Instância";
            case "PJB_INSTANCIA_SUPERIOR" -> "PJB Instância Superior";
            case "PJB_JUIZADO_ESPECIAL" -> "PJB Juizado Especial";
            case "PJB_TRABALHISTA" -> "PJB Trabalhista";
            case "PJB_ELEITORAL" -> "PJB Eleitoral";
            case "PJB_MILITAR" -> "PJB Militar";
            case "PJB_FEDERAL" -> "PJB Federal";
            default -> "PJB Estadual";
        };
        parts.add(base);
        if (secretariaRamoClassificado != null && !secretariaRamoClassificado.isBlank()) {
            parts.add(secretariaRamoClassificado.replace('_', ' '));
        }
        String instanciaClassificada = secretariaInstanciaClassificada == null ? null : secretariaInstanciaClassificada.replace('_', ' ');
        boolean instanciaJaRepresentadaNoNamespace = "PJB_SEGUNDA_INSTANCIA".equals(normalizeUpper(namespacePjb))
                || "PJB_INSTANCIA_SUPERIOR".equals(normalizeUpper(namespacePjb))
                || "PJB_JUIZADO_ESPECIAL".equals(normalizeUpper(namespacePjb));
        if (!instanciaJaRepresentadaNoNamespace && instanciaClassificada != null && !instanciaClassificada.isBlank()) {
            parts.add(instanciaClassificada);
        }
        return String.join(" | ", parts);
    }

    private static String buildSpecializedSecretariatName(String ramoJustica, String instancia, String tribunal) {
        String base = switch (normalizeUpper(instancia)) {
            case "SEGUNDO_GRAU" -> "Secretaria de Segundo Grau";
            case "SUPERIOR" -> "Secretaria de Instância Superior";
            case "CONSTITUCIONAL" -> "Secretaria Constitucional";
            default -> "Secretaria Judicial";
        };
        ArrayList<String> parts = new ArrayList<>();
        parts.add(base);
        if (ramoJustica != null && !ramoJustica.isBlank()) {
            parts.add(ramoJustica.replace('_', ' '));
        }
        if (tribunal != null && !tribunal.isBlank()) {
            parts.add(tribunal);
        }
        return String.join(" - ", parts);
    }

    private static String buildJuizadoSecretariatName(String juizado) {
        return juizado == null || juizado.isBlank() ? "Secretaria do Juizado Especial" : "Secretaria do " + juizado;
    }

    private static String buildElectoralSecretariatName(String instancia, String tribunal, String zonaEleitoral) {
        String zona = trimToNull(zonaEleitoral);
        if (zona != null) {
            return "Secretaria da " + zona;
        }
        if ("SEGUNDO_GRAU".equals(instancia) || "SUPERIOR".equals(instancia) || "CONSTITUCIONAL".equals(instancia)) {
            return firstNonBlank(trimToNull(tribunal), "Secretaria Eleitoral de Corte");
        }
        return "Secretaria Eleitoral";
    }

    private static String buildMilitarySecretariatName(String instancia, String tribunal, String auditoria) {
        String auditoriaValue = trimToNull(auditoria);
        if (auditoriaValue != null) {
            return auditoriaValue;
        }
        if ("SEGUNDO_GRAU".equals(instancia) || "SUPERIOR".equals(instancia) || "CONSTITUCIONAL".equals(instancia)) {
            return firstNonBlank(trimToNull(tribunal), "Secretaria Militar de Corte");
        }
        return "Secretaria Militar";
    }

    private static String buildInstitutionalAssignmentLabel(Map<String, Object> payload) {
        ArrayList<String> parts = new ArrayList<>();
        String tribunal = trimToNull((String) payload.get("tribunal"));
        String unidadeJudiciariaCodigo = trimToNull((String) payload.get("unidadeJudiciariaCodigo"));
        String principal = firstNonBlank(
                (String) payload.get("zonaEleitoral"),
                (String) payload.get("auditoria"),
                (String) payload.get("vara"),
                (String) payload.get("juizado"),
                (String) payload.get("camara"),
                (String) payload.get("turma"),
                (String) payload.get("secao"),
                (String) payload.get("subsecao"),
                (String) payload.get("gabinete"),
                (String) payload.get("orgaoFracionario"),
                (String) payload.get("secretariaRecursal"),
                (String) payload.get("secretariaEmbargos"),
                (String) payload.get("secretariaEspecializada"),
                (String) payload.get("secretaria")
        );
        if (tribunal != null) {
            parts.add(tribunal);
        }
        if (principal != null) {
            parts.add(principal);
        }
        if (unidadeJudiciariaCodigo != null) {
            parts.add("COD=" + unidadeJudiciariaCodigo);
        }
        String comarca = trimToNull((String) payload.get("comarca"));
        String uf = trimToNull((String) payload.get("uf"));
        if (comarca != null) {
            parts.add(uf == null ? comarca : comarca + "/" + uf);
        } else if (uf != null) {
            parts.add(uf);
        }
        return parts.isEmpty() ? "LOTACAO_NAO_IDENTIFICADA" : String.join(" | ", parts);
    }

    private static String detectInstitutionalAssignmentSource(HttpServletRequest request, List<String> clues, Processo processo) {
        if (hasAnyHeader(request,
                CERTIFICATE_TRIBUNAL_HEADERS,
                CERTIFICATE_UNIT_CODE_HEADERS,
                CERTIFICATE_VARA_HEADERS,
                CERTIFICATE_ZONE_HEADERS,
                CERTIFICATE_JUIZADO_HEADERS,
                CERTIFICATE_TURMA_HEADERS,
                CERTIFICATE_CAMARA_HEADERS,
                CERTIFICATE_SECAO_HEADERS,
                CERTIFICATE_SUBSECAO_HEADERS,
                CERTIFICATE_AUDITORIA_HEADERS,
                CERTIFICATE_ORGAO_FRACIONARIO_HEADERS,
                CERTIFICATE_GABINETE_HEADERS,
                CERTIFICATE_RECURSAL_SECRETARIAT_HEADERS,
                CERTIFICATE_EMBARGOS_SECRETARIAT_HEADERS,
                CERTIFICATE_SECOND_INSTANCE_SECRETARIAT_HEADERS,
                CERTIFICATE_SUPERIOR_INSTANCE_SECRETARIAT_HEADERS,
                CERTIFICATE_JUIZADO_SECRETARIAT_HEADERS,
                CERTIFICATE_TRABALHISTA_SECRETARIAT_HEADERS,
                CERTIFICATE_ELEITORAL_SECRETARIAT_HEADERS,
                CERTIFICATE_MILITAR_SECRETARIAT_HEADERS)) {
            return "HEADERS_CERTIFICADO_ENTRADA";
        }
        if (!clues.isEmpty()) {
            return "DN_CERTIFICADO_ENTRADA";
        }
        if (processo != null && (trimToNull(processo.getVara()) != null || trimToNull(processo.getUnidadeJudiciariaCodigo()) != null || trimToNull(processo.getTribunal()) != null)) {
            return "PROCESSO";
        }
        return "USUARIO_OU_FALLBACK";
    }

    private static List<String> collectInstitutionalClues(HttpServletRequest request) {
        LinkedHashSet<String> clues = new LinkedHashSet<>();
        String subject = firstHeader(request, CERTIFICATE_SUBJECT_HEADERS);
        if (subject.isBlank()) {
            subject = QualifiedSignatureCertificateContextSupport.extractServletCertificate(request)
                    .map(certificate -> certificate.getSubjectX500Principal().getName(X500Principal.RFC2253))
                    .orElseGet(() -> QualifiedSignatureCertificateContextSupport.extractPemCertificate(request)
                            .map(certificate -> certificate.getSubjectX500Principal().getName(X500Principal.RFC2253))
                            .orElse(""));
        }
        if (!subject.isBlank()) {
            clues.add(subject);
            clues.addAll(QualifiedSignatureCertificateContextSupport.parseDn(subject).values());
        }
        String org = firstHeader(request, CERTIFICATE_ORG_HEADERS);
        if (!org.isBlank()) {
            clues.add(org);
        }
        String branch = firstHeader(request, CERTIFICATE_BRANCH_HEADERS);
        if (!branch.isBlank()) {
            clues.add(branch);
        }
        String scope = firstHeader(request, CERTIFICATE_SCOPE_HEADERS);
        if (!scope.isBlank()) {
            clues.add(scope);
        }
        return List.copyOf(clues);
    }

    private static String inferInstitutionalField(List<String> clues, String... markers) {
        if (clues == null || clues.isEmpty() || markers == null || markers.length == 0) {
            return null;
        }
        for (String clue : clues) {
            if (clue == null || clue.isBlank()) {
                continue;
            }
            String token = normalizeUpper(clue).replace(',', '_');
            for (String marker : markers) {
                String normalizedMarker = normalizeUpper(marker);
                if (token.contains(normalizedMarker)) {
                    return clue.trim();
                }
            }
        }
        return null;
    }

    private static String extractTribunalHint(List<String> clues) {
        if (clues == null || clues.isEmpty()) {
            return null;
        }
        for (String clue : clues) {
            String token = normalizeUpper(clue);
            if (token != null && TRIBUNAL_HINT_PATTERN.matcher(token).find()) {
                return sanitizeInstitutionalValue(clue);
            }
        }
        return null;
    }

    private static String sanitizeInstitutionalValue(String value) {
        String sanitized = trimToNull(value);
        if (sanitized == null) {
            return null;
        }
        return sanitized.replaceAll("\\s+", " ").trim();
    }
}
