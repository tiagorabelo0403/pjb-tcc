package com.tcc.pjb.backend.core.security.professional;

import com.tcc.pjb.backend.core.security.abac.AuthzDecision;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class ProfessionalDocumentScopePolicyService {

    private final ProfessionalProcessAccessVectorService accessVectorService;

    public ProfessionalDocumentScopePolicyService(ProfessionalProcessAccessVectorService accessVectorService) {
        this.accessVectorService = Objects.requireNonNull(accessVectorService);
    }

    public AuthzDecision refineDocumentReadDecision(Usuario usuario,
                                                    Processo processo,
                                                    DocumentoProcessual documento,
                                                    AuthzDecision baseDecision) {
        if (baseDecision == null || !baseDecision.allowed()) {
            return baseDecision == null ? AuthzDecision.deny("documento_base_negado", "abac-v1.0") : baseDecision;
        }
        ProfessionalDocumentAccessDecision decision = decide(usuario, processo, documento);
        if (decision.allowed()) {
            return baseDecision;
        }
        return AuthzDecision.deny(decision.reason(), baseDecision.policyVersion());
    }

    public ProfessionalDocumentAccessDecision decide(Usuario usuario,
                                                     Processo processo,
                                                     DocumentoProcessual documento) {
        ProfessionalDocumentVisibilityScope scope = resolveScope(processo, documento);
        if (scope == ProfessionalDocumentVisibilityScope.PUBLIC_ACT || scope == ProfessionalDocumentVisibilityScope.PUBLIC_DOCUMENT) {
            return new ProfessionalDocumentAccessDecision(true, scope, "document_scope_public");
        }
        if (usuario == null || documento == null) {
            return new ProfessionalDocumentAccessDecision(false, scope, "documento_sem_usuario_ou_nulo");
        }
        ProfessionalProcessAccessVector vector = accessVectorService.resolve(usuario, processo);
        if (!vector.allowed()) {
            return new ProfessionalDocumentAccessDecision(false, scope, "document_vector_denied");
        }
        if (vector.allowsScope(scope)) {
            return new ProfessionalDocumentAccessDecision(true, scope, "document_scope_allowed_" + scope.name().toLowerCase(Locale.ROOT));
        }
        if (scope == ProfessionalDocumentVisibilityScope.PROFESSIONAL_NON_MANDATE_VIEW
                && vector.hasCapability(ProfessionalCapability.VIEW_PROFESSIONAL_DOCUMENTS)) {
            return new ProfessionalDocumentAccessDecision(true, scope, "document_scope_professional_non_mandate");
        }
        if ((scope == ProfessionalDocumentVisibilityScope.COUNSEL_REPRESENTED_PARTY || scope == ProfessionalDocumentVisibilityScope.EVIDENCE_RESTRICTED)
                && vector.hasCapability(ProfessionalCapability.VIEW_RESTRICTED_PARTY_DOCUMENTS)) {
            return new ProfessionalDocumentAccessDecision(true, scope, "document_scope_represented_party");
        }
        if ((scope == ProfessionalDocumentVisibilityScope.COURT_INTERNAL || scope == ProfessionalDocumentVisibilityScope.CHAMBER_INTERNAL)
                && vector.actorClass() == ProfessionalActorClass.MAGISTRATURA
                && vector.hasCapability(ProfessionalCapability.VIEW_CONFIDENTIAL_CASE)) {
            return new ProfessionalDocumentAccessDecision(true, scope, "document_scope_magistratura_internal");
        }
        return new ProfessionalDocumentAccessDecision(false, scope, "document_scope_denied_" + scope.name().toLowerCase(Locale.ROOT));
    }

    public ProfessionalDocumentVisibilityScope resolveScope(Processo processo, DocumentoProcessual documento) {
        if (documento == null) {
            return ProfessionalDocumentVisibilityScope.PUBLIC_DOCUMENT;
        }
        String explicit = documento.getVisibilityScope();
        if (explicit != null && !explicit.isBlank()) {
            try {
                return ProfessionalDocumentVisibilityScope.valueOf(explicit.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
            }
        }
        String title = (safe(documento.getTitulo()) + " " + safe(documento.getNomeOriginal())).toUpperCase(Locale.ROOT);
        if (containsAny(title, "SENTEN", "DECIS", "DESPACH", "ACÓRD", "ACORDA", "VOTO", "PAUTA", "PROCLAMA")) {
            return ProfessionalDocumentVisibilityScope.PUBLIC_ACT;
        }
        if (containsAny(title, "MINUTA", "RASCUNHO", "DRAFT", "VERSAO INTERNA", "VERSÃO INTERNA")) {
            return ProfessionalDocumentVisibilityScope.PRIVATE_DRAFT;
        }
        if (containsAny(title, "GABINETE", "NOTA INTERNA", "MEMORANDO INTERNO")) {
            return ProfessionalDocumentVisibilityScope.COURT_INTERNAL;
        }
        if (containsAny(title, "VOTO RESERVADO", "MINUTA DE ACORDAO", "MINUTA DE ACÓRDÃO", "COLEGIADO INTERNO", "CAMARA INTERNA", "CÂMARA INTERNA")) {
            return ProfessionalDocumentVisibilityScope.CHAMBER_INTERNAL;
        }
        if (containsAny(title, "LAUDO", "PRONTUARIO", "PRONTUÁRIO", "EXAME", "PERICIA", "PERÍCIA", "QUEBRA", "DADOS BANCARIOS", "DADOS BANCÁRIOS")) {
            return ProfessionalDocumentVisibilityScope.EVIDENCE_RESTRICTED;
        }
        if (documento.getCategoria() == DocumentoCategoria.PESSOAL) {
            return ProfessionalDocumentVisibilityScope.COUNSEL_REPRESENTED_PARTY;
        }
        NivelSigilo docSigilo = documento.getNivelSigilo() == null ? NivelSigilo.PUBLICO : documento.getNivelSigilo();
        NivelSigilo procSigilo = processo == null || processo.getNivelSigilo() == null ? NivelSigilo.PUBLICO : processo.getNivelSigilo();
        if (docSigilo.getNivel() >= NivelSigilo.SIGILO_N2.getNivel() || procSigilo.getNivel() >= NivelSigilo.SIGILO_N2.getNivel()) {
            return ProfessionalDocumentVisibilityScope.COUNSEL_REPRESENTED_PARTY;
        }
        if (containsAny(title, "CERTIDAO", "CERTIDÃO", "COMPROVANTE", "GUIA", "TERMO", "ATA", "MANDADO")) {
            return ProfessionalDocumentVisibilityScope.PROFESSIONAL_NON_MANDATE_VIEW;
        }
        return ProfessionalDocumentVisibilityScope.PUBLIC_DOCUMENT;
    }

    private boolean containsAny(String text, String... tokens) {
        for (String token : tokens) {
            if (text.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
