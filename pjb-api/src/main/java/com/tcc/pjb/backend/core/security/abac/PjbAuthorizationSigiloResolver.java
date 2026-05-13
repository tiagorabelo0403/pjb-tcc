package com.tcc.pjb.backend.core.security.abac;

import com.tcc.pjb.backend.core.observability.RequestContext;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.core.security.sigilo.SigiloCredential;
import com.tcc.pjb.backend.core.security.sigilo.service.SigiloAccessService;
import com.tcc.pjb.backend.core.security.stepup.JwtStepUpClaims;
import com.tcc.pjb.backend.model.entity.Processo;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.document.DocumentoProcessual;
import com.tcc.pjb.backend.model.entity.enums.DocumentoCategoria;
import com.tcc.pjb.backend.model.entity.enums.NivelSigilo;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

final class PjbAuthorizationSigiloResolver {

    private static final String STEP_UP_NONE = "NONE";
    private static final String STEP_UP_SIGILO_CREDENTIAL = "SIGILO_CREDENTIAL";
    private static final String STEP_UP_MFA = "MFA";
    private static final String STEP_UP_PRIVILEGED = "PRIVILEGED_BYPASS";

    private final CurrentUserService currentUserService;
    private final SigiloAccessService sigiloAccessService;

    PjbAuthorizationSigiloResolver(CurrentUserService currentUserService,
                                   SigiloAccessService sigiloAccessService) {
        this.currentUserService = currentUserService;
        this.sigiloAccessService = sigiloAccessService;
    }

    NivelSigilo computeProcessoSigiloEfetivo(Processo processo) {
        return processo != null && processo.getNivelSigilo() != null
                ? processo.getNivelSigilo()
                : NivelSigilo.PUBLICO;
    }

    NivelSigilo elevateProcessoSigilo(Processo processo, NivelSigilo sigiloEfetivo) {
        NivelSigilo base = computeProcessoSigiloEfetivo(processo);
        return maxSigilo(base, sigiloEfetivo);
    }

    Processo shadowProcessoAtSecrecy(Processo processo, NivelSigilo sigiloEfetivo) {
        if (processo == null) {
            return null;
        }
        NivelSigilo efetivo = elevateProcessoSigilo(processo, sigiloEfetivo);
        NivelSigilo atual = computeProcessoSigiloEfetivo(processo);
        return efetivo == atual ? processo : shadowProcesso(processo, efetivo);
    }

    Processo shadowProcesso(Processo base, NivelSigilo sigilo) {
        if (base == null) {
            return null;
        }
        Processo shadow = new Processo();
        shadow.setId(base.getId());
        shadow.setNumeroProcesso(base.getNumeroProcesso());
        shadow.setNumeroUnificado(base.getNumeroUnificado());
        shadow.setParteAutoraCpf(base.getParteAutoraCpf());
        shadow.setParteReuCpf(base.getParteReuCpf());
        shadow.setUsuario(base.getUsuario());
        shadow.setNivelSigilo(sigilo);
        return shadow;
    }

    NivelSigilo computeDocumentoSigiloEfetivo(Processo processo, DocumentoProcessual documento) {
        if (documento == null) {
            return NivelSigilo.PUBLICO;
        }
        DocumentoCategoria categoria = documento.getCategoria() == null
                ? DocumentoCategoria.PUBLICO
                : documento.getCategoria();
        NivelSigilo procSigilo = computeProcessoSigiloEfetivo(processo);
        NivelSigilo docSigilo = documento.getNivelSigilo() != null
                ? documento.getNivelSigilo()
                : NivelSigilo.PUBLICO;
        NivelSigilo minCategoria = categoria == DocumentoCategoria.PESSOAL
                ? NivelSigilo.SIGILO_N2
                : NivelSigilo.PUBLICO;
        return maxSigilo(procSigilo, maxSigilo(docSigilo, minCategoria));
    }

    PjbAuthorizationStepUpAssessment assessStepUpForSigiloEfetivo(Processo processo, NivelSigilo efetivo) {
        NivelSigilo sigilo = efetivo == null ? NivelSigilo.PUBLICO : efetivo;
        if (sigilo.getNivel() < NivelSigilo.SIGILO_N2.getNivel()) {
            return PjbAuthorizationStepUpAssessment.notRequired(STEP_UP_NONE, "sigilo_baixo");
        }
        Usuario usuario = currentUserService.getOrNull();
        TipoUsuario tipo = usuario != null ? usuario.getTipoUsuario() : null;
        if (tipo != null && (tipo.isAdmin()
                || tipo.isMagistratura()
                || tipo.isServidorJudiciario()
                || tipo.isMinisterioPublico()
                || tipo.isDefensoriaPublica()
                || tipo.isProcuradoria())) {
            return PjbAuthorizationStepUpAssessment.notRequired(STEP_UP_PRIVILEGED, "institucional_privilegiado");
        }
        if (tipo == TipoUsuario.ADVOGADO) {
            SigiloCredential credential = RequestContext.getSigiloCredential().orElse(null);
            Long processoId = processo != null ? processo.getId() : null;
            if (processoId != null && usuario != null && usuario.getId() != null && credential != null) {
                boolean valid = sigiloAccessService.validarCredencial(processoId, usuario.getId(), credential);
                if (valid) {
                    return PjbAuthorizationStepUpAssessment.satisfied(STEP_UP_SIGILO_CREDENTIAL, "sigilo_credential_valida");
                }
            }
            return PjbAuthorizationStepUpAssessment.requiredButMissing(
                    STEP_UP_SIGILO_CREDENTIAL,
                    "step_up_sigilo_credencial_requerida",
                    "Step-up obrigatório para sigilo alto (SIGILO_N2+): forneça credencial de sigilo aprovada."
            );
        }
        if (jwtHasMfaClaim()) {
            return PjbAuthorizationStepUpAssessment.satisfied(STEP_UP_MFA, "mfa_presente");
        }
        return PjbAuthorizationStepUpAssessment.requiredButMissing(
                STEP_UP_MFA,
                "step_up_mfa_requerido",
                "Step-up obrigatório para sigilo alto (SIGILO_N2+): autenticação forte (MFA/passkey) requerida."
        );
    }

    private NivelSigilo maxSigilo(NivelSigilo first, NivelSigilo second) {
        NivelSigilo left = first == null ? NivelSigilo.PUBLICO : first;
        NivelSigilo right = second == null ? NivelSigilo.PUBLICO : second;
        return left.getNivel() >= right.getNivel() ? left : right;
    }

    private boolean jwtHasMfaClaim() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return JwtStepUpClaims.hasMfa(authentication);
    }
}
