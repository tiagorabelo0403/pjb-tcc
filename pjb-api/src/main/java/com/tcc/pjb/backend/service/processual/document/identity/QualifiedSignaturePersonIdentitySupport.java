package com.tcc.pjb.backend.service.processual.document.identity;

import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_CARGO_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_CPF_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_EMAIL_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_EMBARGOS_SECRETARIAT_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_FULLNAME_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_MATRICULA_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_OAB_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_RECURSAL_SECRETARIAT_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_REGISTRATION_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.CERTIFICATE_SECRETARIAT_HEADERS;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.defaultString;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.firstHeader;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.firstNonBlank;
import static com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService.trimToNull;

import com.tcc.pjb.backend.model.entity.Usuario;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

final class QualifiedSignaturePersonIdentitySupport {

    private QualifiedSignaturePersonIdentitySupport() {
    }

    static ResolvedPersonIdentity resolvePersonIdentity(Usuario usuario,
                                                        HttpServletRequest request,
                                                        EntryCertificateContext certificate,
                                                        String papelDetalhado,
                                                        Map<String, Object> lotacaoInstitucional) {
        String certName = firstNonBlank(
                firstHeader(request, CERTIFICATE_FULLNAME_HEADERS),
                certificate == null ? null : certificate.subjectAttributes().get("CN"),
                certificate == null ? null : certificate.subjectAttributes().get("GN"),
                certificate == null ? null : certificate.subjectAttributes().get("NAME")
        );
        String certCpf = normalizeDigits(firstNonBlank(
                firstHeader(request, CERTIFICATE_CPF_HEADERS),
                certificate == null ? null : certificate.subjectAttributes().get("SERIALNUMBER"),
                certificate == null ? null : certificate.subjectAttributes().get("UID")
        ));
        String certEmail = firstNonBlank(
                firstHeader(request, CERTIFICATE_EMAIL_HEADERS),
                certificate == null ? null : certificate.subjectAttributes().get("EMAILADDRESS"),
                certificate == null ? null : certificate.subjectAttributes().get("E")
        );
        String certOab = normalizeRegistration(firstNonBlank(
                firstHeader(request, CERTIFICATE_OAB_HEADERS),
                certificate == null ? null : certificate.subjectAttributes().get("OAB")
        ));
        String certRegistro = normalizeRegistration(firstNonBlank(
                firstHeader(request, CERTIFICATE_REGISTRATION_HEADERS),
                certificate == null ? null : certificate.subjectAttributes().get("REGISTRATIONNUMBER")
        ));
        String certMatricula = normalizeRegistration(firstNonBlank(
                firstHeader(request, CERTIFICATE_MATRICULA_HEADERS),
                certificate == null ? null : certificate.subjectAttributes().get("EMPLOYEENUMBER")
        ));
        String certCargo = firstNonBlank(
                firstHeader(request, CERTIFICATE_CARGO_HEADERS),
                certificate == null ? null : certificate.subjectAttributes().get("TITLE"),
                certificate == null ? null : certificate.subjectAttributes().get("OU")
        );
        String userName = usuario == null ? null : usuario.getNome();
        String userCpf = usuario == null ? null : usuario.getCpf();
        String userEmail = usuario == null ? null : usuario.getEmail();
        String userOab = usuario == null ? null : firstNonBlank(usuario.getOabNormalizada(), usuario.getOab(), usuario.getOabNumero());
        String userRegistro = usuario == null ? null : usuario.getRegistroProfissional();
        boolean nomeCoerente = isNameCoherent(userName, certName);
        boolean cpfCoerente = isDigitsCoherent(userCpf, certCpf);
        boolean emailCoerente = isTextCoherent(userEmail, certEmail);
        boolean oabCoerente = isRegistrationCoherent(userOab, certOab);
        boolean registroCoerente = isRegistrationCoherent(userRegistro, certRegistro) || isRegistrationCoherent(userRegistro, certMatricula);
        int score = 0;
        if (nomeCoerente) score += 2;
        if (cpfCoerente) score += 4;
        if (emailCoerente) score += 2;
        if (oabCoerente) score += 3;
        if (registroCoerente) score += 3;
        String nivelConfianca = score >= 8 ? "ALTA" : score >= 4 ? "MEDIA" : score > 0 ? "BAIXA" : "NAO_CONFERIDA";
        String secretariaRecursal = defaultString(firstNonBlank(
                request == null ? null : firstHeader(request, CERTIFICATE_RECURSAL_SECRETARIAT_HEADERS),
                lotacaoInstitucional == null ? null : (String) lotacaoInstitucional.get("secretariaRecursal")
        ));
        String secretariaEmbargos = defaultString(firstNonBlank(
                request == null ? null : firstHeader(request, CERTIFICATE_EMBARGOS_SECRETARIAT_HEADERS),
                lotacaoInstitucional == null ? null : (String) lotacaoInstitucional.get("secretariaEmbargos")
        ));
        String secretariaEspecializada = defaultString(firstNonBlank(
                request == null ? null : firstHeader(request, CERTIFICATE_SECRETARIAT_HEADERS),
                lotacaoInstitucional == null ? null : (String) lotacaoInstitucional.get("secretariaEspecializada")
        ));
        String namespacePjb = defaultString(lotacaoInstitucional == null ? null : (String) lotacaoInstitucional.get("namespacePjb"));
        String painelPjb = defaultString(lotacaoInstitucional == null ? null : (String) lotacaoInstitucional.get("painelPjb"));
        LinkedHashMap<String, Object> payload = new LinkedHashMap<>();
        payload.put("tipoUsuarioReal", usuario == null || usuario.getTipoUsuario() == null ? null : usuario.getTipoUsuario().name());
        payload.put("papelDetalhado", papelDetalhado);
        payload.put("nomeUsuario", trimToNull(userName));
        payload.put("nomeCertificado", trimToNull(certName));
        payload.put("cpfUsuario", maskCpf(userCpf));
        payload.put("cpfCertificado", maskCpf(certCpf));
        payload.put("emailUsuario", trimToNull(userEmail));
        payload.put("emailCertificado", trimToNull(certEmail));
        payload.put("oabUsuario", trimToNull(userOab));
        payload.put("oabCertificado", trimToNull(certOab));
        payload.put("registroUsuario", trimToNull(userRegistro));
        payload.put("registroCertificado", trimToNull(firstNonBlank(certRegistro, certMatricula)));
        payload.put("cargoCertificado", trimToNull(certCargo));
        payload.put("nomeCoerente", nomeCoerente);
        payload.put("cpfCoerente", cpfCoerente);
        payload.put("emailCoerente", emailCoerente);
        payload.put("oabCoerente", oabCoerente);
        payload.put("registroCoerente", registroCoerente);
        payload.put("secretariaRecursal", secretariaRecursal.isBlank() ? null : secretariaRecursal);
        payload.put("secretariaEmbargos", secretariaEmbargos.isBlank() ? null : secretariaEmbargos);
        payload.put("secretariaEspecializada", secretariaEspecializada.isBlank() ? null : secretariaEspecializada);
        payload.put("namespacePjb", namespacePjb.isBlank() ? null : namespacePjb);
        payload.put("painelPjb", painelPjb.isBlank() ? null : painelPjb);
        payload.put("nivelConfianca", nivelConfianca);
        payload.put("identidadeConferida", score > 0);
        payload.entrySet().removeIf(entry -> entry.getKey() == null || entry.getValue() == null);
        return new ResolvedPersonIdentity(
                score > 0,
                nivelConfianca,
                score,
                defaultString(certName),
                defaultString(certCpf),
                defaultString(certEmail),
                defaultString(firstNonBlank(certRegistro, certMatricula)),
                defaultString(certOab),
                secretariaRecursal,
                secretariaEmbargos,
                Collections.unmodifiableMap(payload),
                (score > 0 ? "USUARIO_CERTIFICADO_" + nivelConfianca : "USUARIO_CERTIFICADO_NAO_CONFERIDO")
        );
    }

    private static boolean isNameCoherent(String userName, String certName) {
        if (userName == null || userName.isBlank() || certName == null || certName.isBlank()) {
            return false;
        }
        String left = normalizeComparableText(userName);
        String right = normalizeComparableText(certName);
        return left.equals(right) || left.contains(right) || right.contains(left);
    }

    private static boolean isDigitsCoherent(String userValue, String certValue) {
        String left = normalizeDigits(userValue);
        String right = normalizeDigits(certValue);
        if (left == null || right == null) {
            return false;
        }
        return left.equals(right) || left.endsWith(right) || right.endsWith(left);
    }

    private static boolean isTextCoherent(String userValue, String certValue) {
        if (userValue == null || userValue.isBlank() || certValue == null || certValue.isBlank()) {
            return false;
        }
        return normalizeComparableText(userValue).equals(normalizeComparableText(certValue));
    }

    private static boolean isRegistrationCoherent(String userValue, String certValue) {
        String left = normalizeRegistration(userValue);
        String right = normalizeRegistration(certValue);
        if (left == null || right == null) {
            return false;
        }
        return left.equals(right);
    }

    private static String normalizeComparableText(String value) {
        return QualifiedSignatureIdentityContextService.normalizeUpper(value).replace("_", "");
    }

    private static String normalizeDigits(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("\\D+", "");
        return digits.isBlank() ? null : digits;
    }

    private static String normalizeRegistration(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.replaceAll("[^A-Za-z0-9]+", "").toUpperCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private static String maskCpf(String value) {
        String digits = normalizeDigits(value);
        if (digits == null || digits.length() != 11) {
            return trimToNull(value);
        }
        return "***." + digits.substring(3, 6) + ".***-" + digits.substring(9);
    }
}
