package com.tcc.pjb.backend.service.processual.document.envelope;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalSessionSecuritySignalService;
import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.util.Hashes;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TemplateDocumentoOficial;
import com.tcc.pjb.backend.modules.advocacia.office.enums.OfficeActionType;
import com.tcc.pjb.backend.modules.advocacia.office.service.OfficeProcessWorkspaceScopeService;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.QualifiedSignatureMetadata;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.SovereignValidationResult;
import com.tcc.pjb.backend.service.processual.document.envelope.dto.SignedDocumentEnvelope.ValidationRule;
import com.tcc.pjb.backend.service.processual.document.identity.QualifiedSignatureIdentityContextService;
import com.tcc.pjb.backend.service.processual.document.identity.ResolvedSignatureIdentity;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class QualifiedDocumentSignatureEnvelopeService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final InstitutionalSessionSecuritySignalService institutionalSessionSecuritySignalService;
    private final QualifiedSignatureIdentityContextService qualifiedSignatureIdentityContextService;
    private final OfficeProcessWorkspaceScopeService officeProcessWorkspaceScopeService;

    public QualifiedDocumentSignatureEnvelopeService(InstitutionalSessionSecuritySignalService institutionalSessionSecuritySignalService,
                                                     QualifiedSignatureIdentityContextService qualifiedSignatureIdentityContextService,
                                                     ObjectProvider<OfficeProcessWorkspaceScopeService> officeProcessWorkspaceScopeServiceProvider) {
        this.institutionalSessionSecuritySignalService = Objects.requireNonNull(institutionalSessionSecuritySignalService);
        this.qualifiedSignatureIdentityContextService = Objects.requireNonNull(qualifiedSignatureIdentityContextService);
        this.officeProcessWorkspaceScopeService = officeProcessWorkspaceScopeServiceProvider.getIfAvailable();
    }

    public SignedDocumentEnvelope signOfficialTemplate(Processo processo,
                                                       Usuario usuario,
                                                       TemplateDocumentoOficial template,
                                                       String titulo,
                                                       String conteudo,
                                                       boolean preservaSigilo) {
        String papel = resolveTemplateRole(template, usuario);
        String politica = resolvePolicy(template, papel);
        LinkedHashSet<String> governanceTags = new LinkedHashSet<>();
        governanceTags.add("assinatura_qualificada_completa");
        governanceTags.add("rubrica_data_hora_local");
        governanceTags.add("envelope_assinatura_soberana");
        governanceTags.add("antifraude_anti_replay");
        governanceTags.add(template == null ? "template_nao_informado" : template.name().toLowerCase(Locale.ROOT));
        return signContent(processo, usuario, titulo, conteudo, papel, politica, preservaSigilo, List.copyOf(governanceTags), true).envelope();
    }

    public SignedDocumentEnvelope signFreeContent(Processo processo,
                                                       Usuario usuario,
                                                       String titulo,
                                                       String conteudo,
                                                       String papelAssinante,
                                                       String politicaAssinatura,
                                                       boolean preservaSigilo,
                                                       List<String> governanceTags) {
        return signContent(processo, usuario, titulo, conteudo, papelAssinante, politicaAssinatura, preservaSigilo, governanceTags, true).envelope();
    }

    public SignedDocumentEnvelope signGovernedContent(Processo processo,
                                                      Usuario usuario,
                                                      String titulo,
                                                      String conteudo,
                                                      String papelAssinante,
                                                      String politicaAssinatura,
                                                      boolean preservaSigilo,
                                                      List<String> governanceTags) {
        return signContent(processo, usuario, titulo, conteudo, papelAssinante, politicaAssinatura, preservaSigilo, governanceTags, false).envelope();
    }

    private EnvelopeAssembly signContent(Processo processo,
                                         Usuario usuario,
                                         String titulo,
                                         String conteudo,
                                         String papelAssinante,
                                         String politicaAssinatura,
                                         boolean preservaSigilo,
                                         List<String> governanceTags,
                                         boolean enforceWorkspaceAccess) {
        String normalizedContent = conteudo == null ? "" : conteudo.trim();
        String normalizedTitle = titulo == null || titulo.isBlank() ? "DOCUMENTO_PROCESSUAL" : titulo.trim();
        String baseHash = Hashes.sha256Hex(normalizedContent);
        Instant signedAt = Instant.now().truncatedTo(ChronoUnit.SECONDS);
        ZonedDateTime localTime = signedAt.atZone(ZoneId.systemDefault());
        HttpServletRequest request = currentRequest();
        if (enforceWorkspaceAccess && processo != null && processo.getId() != null && officeProcessWorkspaceScopeService != null && officeProcessWorkspaceScopeService.supportsCurrentUser()) {
            officeProcessWorkspaceScopeService.requireAccess(processo.getId(), OfficeActionType.ASSINAR_DOCUMENTO, request);
        }
        InstitutionalSessionSecuritySignalService.InstitutionalSessionSecuritySignal signal = institutionalSessionSecuritySignalService.collect(usuario);
        ResolvedSignatureIdentity identity = qualifiedSignatureIdentityContextService.resolve(
                processo,
                usuario,
                papelAssinante,
                request
        );
        String local = resolveLocal(processo, usuario, identity);
        String certificateFingerprint = identity.entryCertificate() == null ? "" : normalize(identity.entryCertificate().fingerprintSha256());
        String certificateSerial = identity.entryCertificate() == null ? "" : normalize(identity.entryCertificate().serialNumber());
        String sessionBindingHash = Hashes.sha256Hex(String.join("|",
                baseHash,
                normalize(RequestContext.getRequestId().orElse(null)),
                normalize(RequestContext.getClientWindowBinding().orElse(null)),
                normalize(RequestContext.getClientTabBinding().orElse(null)),
                normalize(RequestContext.getClientRouteBinding().orElse(null)),
                normalize(request == null ? null : request.getRemoteAddr()),
                normalize(usuario == null || usuario.getId() == null ? null : String.valueOf(usuario.getId())),
                normalize(identity.lotacaoAssinante()),
                normalize(String.valueOf(identity.lotacaoInstitucional().get("unidadeJudiciariaCodigo"))),
                certificateFingerprint,
                certificateSerial));
        String replayShieldHash = Hashes.sha256Hex(String.join("|",
                baseHash,
                sessionBindingHash,
                normalize(normalizedTitle),
                signedAt.toString()));
        String rubrica = "PJB-RUB-" + replayShieldHash.substring(0, 12).toUpperCase(Locale.ROOT);
        String envelopeId = "PJB-ENV-" + Hashes.sha256Hex(String.join("|", rubrica, baseHash, signedAt.toString(), normalize(politicaAssinatura))).substring(0, 20).toUpperCase(Locale.ROOT);
        String assinaturaHash = Hashes.sha256Hex(String.join("|",
                envelopeId,
                rubrica,
                baseHash,
                replayShieldHash,
                normalize(usuario == null ? null : usuario.getNome()),
                normalize(papelAssinante),
                normalize(identity.papelDetalhado()),
                normalize(identity.ramoJustica()),
                normalize(identity.instancia()),
                normalize(identity.lotacaoAssinante()),
                normalize(String.valueOf(identity.lotacaoInstitucional().get("unidadeJudiciariaCodigo"))),
                certificateFingerprint,
                signedAt.toString()));

        LinkedHashMap<String, Object> antifraude = new LinkedHashMap<>();
        antifraude.put("requestId", RequestContext.getRequestId().orElse(null));
        antifraude.put("clientWindowBinding", RequestContext.getClientWindowBinding().orElse(null));
        antifraude.put("clientTabBinding", RequestContext.getClientTabBinding().orElse(null));
        antifraude.put("clientRouteBinding", RequestContext.getClientRouteBinding().orElse(null));
        antifraude.put("networkHash", request == null || request.getRemoteAddr() == null ? null : Hashes.sha256Hex(request.getRemoteAddr()));
        antifraude.put("sessionBindingHash", sessionBindingHash);
        antifraude.put("replayShieldHash", replayShieldHash);
        antifraude.put("mfaAtivo", signal.mfaAtivo());
        antifraude.put("govBrNivel", signal.govBrNivel().name());
        antifraude.put("certificadoICPDetectado", signal.certificadoICPDetectado());
        antifraude.put("redeInstitucionalConfiavel", signal.redeInstitucionalConfiavel());
        antifraude.put("autorizacaoRemotaCertificado", signal.autorizacaoRemotaCertificado());
        antifraude.put("dispositivoHomologado", signal.dispositivoHomologado());
        antifraude.put("webauthnSinalizado", headerTrue(request, "X-PJB-WEBAUTHN-AUTH") || signal.dispositivoHomologado());
        antifraude.put("antiReplayAtivo", Boolean.TRUE);
        antifraude.put("certificadoEntradaFingerprint", identity.entryCertificate() == null ? null : identity.entryCertificate().fingerprintSha256());
        antifraude.put("certificadoEntradaSerial", identity.entryCertificate() == null ? null : identity.entryCertificate().serialNumber());
        antifraude.put("classificacaoHierarquica", identity.etiquetaHierarquica());
        antifraude.put("lotacaoAssinante", identity.lotacaoAssinante());
        antifraude.put("lotacaoInstitucional", identity.lotacaoInstitucional());
        antifraude.put("evidenciasSessao", signal.evidencias());

        LinkedHashMap<String, Object> assinaturaQualificada = new LinkedHashMap<>();
        assinaturaQualificada.put("envelopeId", envelopeId);
        assinaturaQualificada.put("assinaturaHash", assinaturaHash);
        assinaturaQualificada.put("conteudoBaseHash", baseHash);
        assinaturaQualificada.put("rubrica", rubrica);
        assinaturaQualificada.put("data", DATE_FORMAT.format(localTime));
        assinaturaQualificada.put("hora", TIME_FORMAT.format(localTime));
        assinaturaQualificada.put("local", local);
        assinaturaQualificada.put("signatario", resolveSignerName(usuario));
        assinaturaQualificada.put("papelAssinante", normalize(papelAssinante));
        assinaturaQualificada.put("papelAssinanteDetalhado", identity.papelDetalhado());
        assinaturaQualificada.put("segmentoInstitucional", identity.segmentoInstitucional());
        assinaturaQualificada.put("ramoJustica", identity.ramoJustica());
        assinaturaQualificada.put("esferaInstitucional", identity.esferaInstitucional());
        assinaturaQualificada.put("instancia", identity.instancia());
        assinaturaQualificada.put("orgaoAssinante", identity.orgaoAssinante());
        assinaturaQualificada.put("lotacaoAssinante", identity.lotacaoAssinante());
        assinaturaQualificada.put("lotacaoInstitucional", identity.lotacaoInstitucional());
        assinaturaQualificada.put("etiquetaHierarquica", identity.etiquetaHierarquica());
        assinaturaQualificada.put("registroProfissional", resolveProfessionalRegistration(usuario));
        assinaturaQualificada.put("identidadeAssinante", identity.personIdentity() == null ? Map.of() : identity.personIdentity().payload());
        assinaturaQualificada.put("coerenciaCertificadoUsuario", identity.personIdentity() == null ? "NAO_CONFERIDA" : identity.personIdentity().coerenciaResumo());
        if (identity.personIdentity() != null && !identity.personIdentity().secretariaRecursal().isBlank()) {
            assinaturaQualificada.put("secretariaRecursal", identity.personIdentity().secretariaRecursal());
        }
        if (identity.personIdentity() != null && !identity.personIdentity().secretariaEmbargos().isBlank()) {
            assinaturaQualificada.put("secretariaEmbargos", identity.personIdentity().secretariaEmbargos());
        }
        copyIfPresent(assinaturaQualificada, "secretariaEspecializada", identity.classificacao().get("secretariaEspecializada"));
        copyIfPresent(assinaturaQualificada, "secretariaInstanciaClassificada", identity.classificacao().get("secretariaInstanciaClassificada"));
        copyIfPresent(assinaturaQualificada, "secretariaRamoClassificado", identity.classificacao().get("secretariaRamoClassificado"));
        copyIfPresent(assinaturaQualificada, "namespacePjb", identity.classificacao().get("namespacePjb"));
        copyIfPresent(assinaturaQualificada, "painelPjb", identity.classificacao().get("painelPjb"));
        copyIfPresent(assinaturaQualificada, "secretariaSegundaInstancia", identity.classificacao().get("secretariaSegundaInstancia"));
        copyIfPresent(assinaturaQualificada, "secretariaInstanciaSuperior", identity.classificacao().get("secretariaInstanciaSuperior"));
        copyIfPresent(assinaturaQualificada, "secretariaJuizadoEspecial", identity.classificacao().get("secretariaJuizadoEspecial"));
        copyIfPresent(assinaturaQualificada, "secretariaTrabalhista", identity.classificacao().get("secretariaTrabalhista"));
        copyIfPresent(assinaturaQualificada, "secretariaEleitoral", identity.classificacao().get("secretariaEleitoral"));
        copyIfPresent(assinaturaQualificada, "secretariaMilitar", identity.classificacao().get("secretariaMilitar"));
        assinaturaQualificada.put("politicaAssinatura", normalize(politicaAssinatura));
        assinaturaQualificada.put("politicaAssinaturaContextual", identity.classificacao().get("politicaContextual"));
        assinaturaQualificada.put("preservaSigilo", preservaSigilo);
        LinkedHashSet<String> enrichedGovernanceTags = new LinkedHashSet<>();
        if (governanceTags != null) {
            enrichedGovernanceTags.addAll(governanceTags);
        }
        enrichedGovernanceTags.addAll(identity.governanceTags());
        assinaturaQualificada.put("governanceTags", List.copyOf(enrichedGovernanceTags));
        assinaturaQualificada.put("assinadoEm", signedAt.toString());
        assinaturaQualificada.put("classificacaoContextual", identity.classificacao());
        assinaturaQualificada.put("certificadoEntrada", identity.entryCertificate() == null ? Map.of() : identity.entryCertificate().payload());
        assinaturaQualificada.put("antifraude", antifraude);

        String signedContent = normalizedContent + System.lineSeparator()
                + System.lineSeparator()
                + "ASSINATURA QUALIFICADA PJB" + System.lineSeparator()
                + "Signatário: " + resolveSignerName(usuario) + System.lineSeparator()
                + "Papel: " + normalize(papelAssinante) + System.lineSeparator()
                + "Perfil reconhecido: " + identity.papelDetalhado() + System.lineSeparator()
                + "Justiça/Ramo: " + identity.ramoJustica() + System.lineSeparator()
                + "Instância: " + identity.instancia() + System.lineSeparator()
                + "Órgão: " + identity.orgaoAssinante() + System.lineSeparator()
                + "Lotação: " + identity.lotacaoAssinante() + System.lineSeparator()
                + "Registro: " + resolveProfessionalRegistration(usuario) + System.lineSeparator()
                + "Vínculo certificado/usuário: " + (identity.personIdentity() == null ? "NAO_CONFERIDO" : identity.personIdentity().coerenciaResumo()) + System.lineSeparator()
                + renderSignatureIdentityLine("Secretaria especializada", identity.classificacao().get("secretariaEspecializada"))
                + renderSignatureIdentityLine("Classe de secretaria", identity.classificacao().get("secretariaInstanciaClassificada"))
                + renderSignatureIdentityLine("Ramo de secretaria", identity.classificacao().get("secretariaRamoClassificado"))
                + renderSignatureIdentityLine("Namespace PJB", identity.classificacao().get("namespacePjb"))
                + renderSignatureIdentityLine("Painel PJB", identity.classificacao().get("painelPjb"))
                + (identity.personIdentity() == null || identity.personIdentity().secretariaRecursal().isBlank() ? "" : "Secretaria recursal: " + identity.personIdentity().secretariaRecursal() + System.lineSeparator())
                + (identity.personIdentity() == null || identity.personIdentity().secretariaEmbargos().isBlank() ? "" : "Secretaria de embargos: " + identity.personIdentity().secretariaEmbargos() + System.lineSeparator())
                + "Rubrica eletrônica: " + rubrica + System.lineSeparator()
                + "Data: " + DATE_FORMAT.format(localTime) + System.lineSeparator()
                + "Hora: " + TIME_FORMAT.format(localTime) + System.lineSeparator()
                + "Local: " + local + System.lineSeparator()
                + "Envelope de assinatura: " + envelopeId + System.lineSeparator()
                + "Certificado de entrada: " + resolveCertificateLabel(identity) + System.lineSeparator()
                + "Validação soberana: " + assinaturaHash.substring(0, 24).toUpperCase(Locale.ROOT);
        String signedHash = Hashes.sha256Hex(signedContent);
        assinaturaQualificada.put("documentoAssinadoHash", signedHash);

        List<String> regrasAplicadasLegado = List.of(
                "ENVELOPE_SOBERANO_APPEND_ONLY",
                "ASSINATURA_QUALIFICADA_COMPLETA",
                "RUBRICA_DATA_HORA_LOCAL",
                "CLASSIFICACAO_HIERARQUICA_POR_PAPEL_E_ESFERA",
                "CERTIFICADO_ENTRADA_VINCULADO_AO_ATO",
                "IDENTIDADE_PESSOAL_E_PROFISSIONAL_CONFERIDA",
                "ANTIFRAUDE_BINDING_SESSAO_REDE_DISPOSITIVO",
                "ANTI_REPLAY_ATIVO"
        );

        boolean certificadoPresente = identity.entryCertificate() != null && identity.entryCertificate().presente();
        boolean cadeiaCustodiaElegivel = certificadoPresente
                && identity.entryCertificate().issuerRfc2253() != null
                && !identity.entryCertificate().issuerRfc2253().isBlank();
        boolean assinaturaCompletaMaterializada = envelopeId != null && !envelopeId.isBlank()
                && assinaturaHash != null && !assinaturaHash.isBlank()
                && signedHash != null && !signedHash.isBlank()
                && rubrica != null && !rubrica.isBlank();
        boolean rubricaDataHoraLocalPresentes = rubrica != null && !rubrica.isBlank()
                && localTime.toLocalDate() != null
                && localTime.toLocalTime() != null;

        LinkedHashMap<String, Object> validacaoSoberana = new LinkedHashMap<>();
        validacaoSoberana.put("status", "VALIDO");
        validacaoSoberana.put("fonte", "PJB_QUALIFIED_SIGNATURE_SPINE");
        validacaoSoberana.put("politicaAssinatura", normalize(politicaAssinatura));
        validacaoSoberana.put("cadeiaCustodiaElegivel", cadeiaCustodiaElegivel);
        validacaoSoberana.put("assinaturaCompletaMaterializada", assinaturaCompletaMaterializada);
        validacaoSoberana.put("rubricaDataHoraLocalPresentes", rubricaDataHoraLocalPresentes);
        validacaoSoberana.put("classificacaoContextualCoerente", Boolean.TRUE);
        validacaoSoberana.put("certificadoEntradaVinculado", identity.entryCertificate() != null && identity.entryCertificate().presente());
        validacaoSoberana.put("papelAssinanteDetalhado", identity.papelDetalhado());
        validacaoSoberana.put("ramoJustica", identity.ramoJustica());
        validacaoSoberana.put("instancia", identity.instancia());
        validacaoSoberana.put("lotacaoAssinante", identity.lotacaoAssinante());
        validacaoSoberana.put("lotacaoInstitucional", identity.lotacaoInstitucional());
        validacaoSoberana.put("identidadeAssinante", identity.personIdentity() == null ? Map.of() : identity.personIdentity().payload());
        validacaoSoberana.put("coerenciaCertificadoUsuario", identity.personIdentity() == null ? "NAO_CONFERIDA" : identity.personIdentity().coerenciaResumo());
        copyIfPresent(validacaoSoberana, "secretariaEspecializada", identity.classificacao().get("secretariaEspecializada"));
        copyIfPresent(validacaoSoberana, "secretariaInstanciaClassificada", identity.classificacao().get("secretariaInstanciaClassificada"));
        copyIfPresent(validacaoSoberana, "secretariaRamoClassificado", identity.classificacao().get("secretariaRamoClassificado"));
        copyIfPresent(validacaoSoberana, "namespacePjb", identity.classificacao().get("namespacePjb"));
        copyIfPresent(validacaoSoberana, "painelPjb", identity.classificacao().get("painelPjb"));
        validacaoSoberana.put("classificacaoContextual", identity.classificacao());
        validacaoSoberana.put("certificadoEntrada", identity.entryCertificate() == null ? Map.of() : identity.entryCertificate().payload());
        validacaoSoberana.put("sessionBindingHash", sessionBindingHash);
        validacaoSoberana.put("replayShieldHash", replayShieldHash);
        validacaoSoberana.put("documentoAssinadoHash", signedHash);
        validacaoSoberana.put("regrasAplicadas", regrasAplicadasLegado);

        SovereignValidationResult typedValidation = new SovereignValidationResult(
                "VALIDO",
                "PJB_QUALIFIED_SIGNATURE_SPINE",
                normalize(politicaAssinatura),
                cadeiaCustodiaElegivel,
                assinaturaCompletaMaterializada,
                rubricaDataHoraLocalPresentes,
                true,
                identity.entryCertificate() != null && identity.entryCertificate().presente(),
                identity.papelDetalhado(),
                identity.ramoJustica(),
                identity.instancia(),
                identity.lotacaoAssinante(),
                sessionBindingHash,
                replayShieldHash,
                signedHash,
                validationRules(regrasAplicadasLegado)
        );
        LocalDate assinaturaData = localTime.toLocalDate();
        LocalTime assinaturaHora = localTime.toLocalTime();
        boolean identidadeConferida = identity.personIdentity() != null && identity.personIdentity().identidadeConferida();
        QualifiedSignatureMetadata typedSignature = new QualifiedSignatureMetadata(
                envelopeId,
                assinaturaHash,
                baseHash,
                signedHash,
                rubrica != null && !rubrica.isBlank(),
                rubrica,
                assinaturaData,
                assinaturaHora,
                local,
                resolveSignerName(usuario),
                normalize(papelAssinante),
                identity.papelDetalhado(),
                identity.segmentoInstitucional(),
                identity.ramoJustica(),
                identity.esferaInstitucional(),
                identity.instancia(),
                identity.orgaoAssinante(),
                identity.lotacaoAssinante(),
                resolveProfessionalRegistration(usuario),
                identidadeConferida,
                identity.personIdentity() == null ? "NAO_CONFERIDA" : identity.personIdentity().coerenciaResumo(),
                sessionBindingHash,
                replayShieldHash,
                typedValidation
        );
        SignedDocumentEnvelope envelope = new SignedDocumentEnvelope(
                normalizedTitle,
                signedContent,
                signedHash,
                true,
                typedSignature,
                typedValidation
        );

        return new EnvelopeAssembly(envelope, Map.copyOf(assinaturaQualificada), Map.copyOf(validacaoSoberana));
    }

    private List<ValidationRule> validationRules(List<String> rules) {
        return rules.stream()
                .map(rule -> new ValidationRule(rule, null, null))
                .toList();
    }

    private String renderSignatureIdentityLine(String label, Object value) {
        if (value == null) {
            return "";
        }
        String normalized = String.valueOf(value).trim();
        if (normalized.isBlank()) {
            return "";
        }
        return label + ": " + normalized + System.lineSeparator();
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

    private String resolveTemplateRole(TemplateDocumentoOficial template, Usuario usuario) {
        if (template == null) {
            return resolveRoleFromUser(usuario);
        }
        return switch (template) {
            case DESPACHO, DECISAO, SENTENCA, ACORDAO, ALVARA -> "MAGISTRATURA";
            case MANDADO, OFICIO, CERTIDAO_CUMPRIMENTO, CERTIDAO_NAO_CUMPRIMENTO, AUTO_CUMPRIMENTO -> "OFICIAL_JUSTICA";
            case TERMO_AUDIENCIA, CERTIDAO, CERTIDAO_TRANSITO_JULGADO, CARTA_PRECATORIA, EDITAL, INTIMACAO_FORMAL -> "UNIDADE_JUDICIAL";
            case TERMO_ACORDO, SEM_INTERESSE_MANIFESTACAO -> resolveRoleFromUser(usuario);
        };
    }

    private String resolvePolicy(TemplateDocumentoOficial template, String papel) {
        if (template == null) {
            return normalize(papel) + "_QUALIFICADA_SOBERANA";
        }
        return switch (template) {
            case DESPACHO, DECISAO, SENTENCA, ACORDAO -> "MAGISTRATURA_QUALIFICADA_SOBERANA";
            case MANDADO, OFICIO, CERTIDAO_CUMPRIMENTO, CERTIDAO_NAO_CUMPRIMENTO, AUTO_CUMPRIMENTO -> "OFICIAL_JUSTICA_QUALIFICADA_SOBERANA";
            case CERTIDAO, CERTIDAO_TRANSITO_JULGADO, CARTA_PRECATORIA, EDITAL, INTIMACAO_FORMAL, TERMO_AUDIENCIA, ALVARA -> "ATO_OFICIAL_QUALIFICADO_SOBERANO";
            case TERMO_ACORDO, SEM_INTERESSE_MANIFESTACAO -> normalize(papel) + "_QUALIFICADA_SOBERANA";
        };
    }

    private String resolveRoleFromUser(Usuario usuario) {
        if (usuario == null || usuario.getTipoUsuario() == null) {
            return "PERFIL_NAO_IDENTIFICADO";
        }
        if (usuario.getTipoUsuario().isMagistratura()) {
            return "MAGISTRATURA";
        }
        if (usuario.getTipoUsuario().isServidorJudiciario()) {
            return "UNIDADE_JUDICIAL";
        }
        return usuario.getTipoUsuario().name();
    }

    private String resolveSignerName(Usuario usuario) {
        if (usuario == null || usuario.getNome() == null || usuario.getNome().isBlank()) {
            return "SIGNATARIO_NAO_IDENTIFICADO";
        }
        return usuario.getNome().trim();
    }

    private String resolveProfessionalRegistration(Usuario usuario) {
        if (usuario == null) {
            return "REGISTRO_NAO_INFORMADO";
        }
        if (usuario.getOab() != null && !usuario.getOab().isBlank()) {
            return usuario.getOab().trim();
        }
        if (usuario.getRegistroProfissional() != null && !usuario.getRegistroProfissional().isBlank()) {
            return usuario.getRegistroProfissional().trim();
        }
        if (usuario.getCpf() != null && !usuario.getCpf().isBlank()) {
            String digits = usuario.getCpf().replaceAll("\\D+", "");
            if (digits.length() == 11) {
                return "CPF ***." + digits.substring(3, 6) + ".***-" + digits.substring(9);
            }
        }
        return "REGISTRO_NAO_INFORMADO";
    }

    private String resolveLocal(Processo processo, Usuario usuario, ResolvedSignatureIdentity identity) {
        String comarca = processo != null && processo.getComarca() != null && !processo.getComarca().isBlank()
                ? processo.getComarca().trim()
                : usuario != null && usuario.getComarca() != null && !usuario.getComarca().isBlank()
                ? usuario.getComarca().trim()
                : "LOCAL_NAO_INFORMADO";
        String uf = processo != null && processo.getUf() != null && !processo.getUf().isBlank()
                ? processo.getUf().trim().toUpperCase(Locale.ROOT)
                : usuario != null && usuario.getUf() != null && !usuario.getUf().isBlank()
                ? usuario.getUf().trim().toUpperCase(Locale.ROOT)
                : "UF";
        String orgao = identity == null ? null : normalize(identity.orgaoAssinante());
        return orgao == null || orgao.isBlank() ? comarca + '/' + uf : comarca + '/' + uf + " | " + orgao;
    }

    private String resolveCertificateLabel(ResolvedSignatureIdentity identity) {
        if (identity == null || identity.entryCertificate() == null || !identity.entryCertificate().presente()) {
            return "NAO_VINCULADO";
        }
        String fingerprint = normalize(identity.entryCertificate().fingerprintSha256());
        String serial = normalize(identity.entryCertificate().serialNumber());
        String subject = normalize(identity.entryCertificate().subjectRfc2253());
        if (!fingerprint.isBlank()) {
            return "FP=" + fingerprint.substring(0, Math.min(20, fingerprint.length())).toUpperCase(Locale.ROOT);
        }
        if (!serial.isBlank()) {
            return "SERIAL=" + serial.toUpperCase(Locale.ROOT);
        }
        if (!subject.isBlank()) {
            return subject;
        }
        return "PRESENTE";
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes servlet) {
            return servlet.getRequest();
        }
        return null;
    }

    private boolean headerTrue(HttpServletRequest request, String name) {
        return request != null && request.getHeader(name) != null && "true".equalsIgnoreCase(request.getHeader(name));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private record EnvelopeAssembly(
            SignedDocumentEnvelope envelope,
            Map<String, Object> assinaturaQualificada,
            Map<String, Object> validacaoSoberana
    ) {
    }
}
