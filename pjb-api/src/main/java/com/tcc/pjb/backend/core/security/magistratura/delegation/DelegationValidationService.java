package com.tcc.pjb.backend.core.security.magistratura.delegation;

import java.time.Instant;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.enums.TipoUsuario;
import com.tcc.pjb.backend.model.entity.security.JudgeDelegationFlow;
import com.tcc.pjb.backend.model.entity.security.JudgeDelegationFlowStatus;
import com.tcc.pjb.backend.model.repository.JudgeDelegationFlowRepository;
import com.tcc.pjb.backend.model.repository.UsuarioRepository;

@Service
public class DelegationValidationService {

    private final UsuarioRepository usuarioRepository;
    private final JudgeDelegationFlowRepository judgeDelegationFlowRepository;

    public DelegationValidationService(UsuarioRepository usuarioRepository,
                                       JudgeDelegationFlowRepository judgeDelegationFlowRepository) {
        this.usuarioRepository = usuarioRepository;
        this.judgeDelegationFlowRepository = judgeDelegationFlowRepository;
    }

    public DelegationCredential validateForDelegate(DelegationTokenPayload payload, Usuario delegate, String providedDeviceBindingHash) {
        if (payload == null) throw new SecurityException("payload ausente");
        if (delegate == null) throw new SecurityException("usuário ausente");

        if (payload.delegateId() == null || !payload.delegateId().equals(delegate.getId())) {
            throw new SecurityException("token não pertence ao usuário autenticado");
        }

        long now = Instant.now().getEpochSecond();
        if (payload.isExpired(now)) {
            throw new SecurityException("token expirado");
        }
        if (payload.iat() > now + 60) {
            throw new SecurityException("iat inválido");
        }

        Usuario magistrado = usuarioRepository.findById(payload.magistrateId())
                .orElseThrow(() -> new SecurityException("magistrado inexistente"));
        if (!magistrado.isMagistrado()) {
            throw new SecurityException("issuer não é magistratura");
        }

        TipoUsuario issuerTipo = magistrado.getTipoUsuario();
        TipoUsuario delegateTipo = delegate.getTipoUsuario();
        if (issuerTipo == TipoUsuario.MINISTRO) {
            if (delegateTipo != TipoUsuario.JUIZ) {
                throw new SecurityException("delegado não é juiz auxiliar");
            }
        } else {
            if (delegateTipo != TipoUsuario.SERVIDOR && delegateTipo != TipoUsuario.SERVIDOR_FORUM) {
                throw new SecurityException("delegado não é servidor");
            }
        }

        String juizUf = normalize(magistrado.getUf());
        String juizComarca = normalize(magistrado.getComarca());
        String delUf = normalize(delegate.getUf());
        String delComarca = normalize(delegate.getComarca());
        if (juizUf == null || juizComarca == null || delUf == null || delComarca == null) {
            throw new SecurityException("perímetro indefinido");
        }
        if (!juizUf.equals(delUf) || !juizComarca.equals(delComarca)) {
            throw new SecurityException("perímetro divergente");
        }

        String expectedDevice = normalize(payload.deviceBindingHash());
        if (expectedDevice != null) {
            String provided = normalize(providedDeviceBindingHash);
            if (!expectedDevice.equals(provided)) {
                throw new SecurityException("device binding inválido");
            }
        }

        DelegationScope scope = payload.scopeEnum();
        if (scope == null) {
            throw new SecurityException("scope inválido");
        }

        validateFlowStatus(payload);

        return new DelegationCredential(
                payload.jti(),
                payload.magistrateId(),
                issuerTipo,
                payload.delegateId(),
                scope,
                Instant.ofEpochSecond(payload.exp()),
                payload.deviceBindingHash()
        );
    }

    private void validateFlowStatus(DelegationTokenPayload payload) {
        if (payload.jti() == null || payload.jti().isBlank()) {
            return;
        }
        JudgeDelegationFlow flow = judgeDelegationFlowRepository.findTop1ByTokenJtiOrderByApprovedAtDesc(payload.jti())
                .orElseThrow(() -> new SecurityException("delegação inexistente ou revogada"));
        if (flow.getStatus() != JudgeDelegationFlowStatus.APROVADA) {
            throw new SecurityException("delegação não está ativa");
        }
        if (flow.getExpiresAt() != null && flow.getExpiresAt().isBefore(LocalDateTime.now())) {
            flow.setStatus(JudgeDelegationFlowStatus.EXPIRADA);
            judgeDelegationFlowRepository.save(flow);
            throw new SecurityException("delegação expirada");
        }
    }

    private static String normalize(String v) {
        if (v == null) return null;
        String s = v.trim();
        return s.isBlank() ? null : s;
    }
}
