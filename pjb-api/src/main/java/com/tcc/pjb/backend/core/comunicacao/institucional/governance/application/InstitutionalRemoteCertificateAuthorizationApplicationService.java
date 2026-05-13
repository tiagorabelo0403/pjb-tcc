package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliation;
import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.infrastructure.InstitutionalAffiliationStateRepository;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalRemoteCertificateAuthorization;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalRemoteCertificateAuthorizationStateRepository;
import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.model.entity.Usuario;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalRemoteCertificateAuthorizationApplicationService {

    private final InstitutionalRemoteCertificateAuthorizationStateRepository repository;
    private final InstitutionalAffiliationStateRepository affiliationRepository;
    private final CurrentUserService currentUserService;

    public InstitutionalRemoteCertificateAuthorizationApplicationService(InstitutionalRemoteCertificateAuthorizationStateRepository repository,
                                                                        InstitutionalAffiliationStateRepository affiliationRepository,
                                                                        CurrentUserService currentUserService) {
        this.repository = Objects.requireNonNull(repository);
        this.affiliationRepository = Objects.requireNonNull(affiliationRepository);
        this.currentUserService = Objects.requireNonNull(currentUserService);
    }

    public InstitutionalRemoteCertificateAuthorization emitir(String affiliationId,
                                                              Long nominatedUserId,
                                                              String reason,
                                                              List<String> allowedNetworks,
                                                              List<String> allowedDevices,
                                                              Integer validForHours,
                                                              List<String> fundamentos) {
        InstitutionalAffiliation affiliation = affiliationRepository.findByAffiliationId(affiliationId)
                .orElseThrow(() -> new IllegalArgumentException("Afiliação institucional não encontrada."));
        if (!affiliation.permiteUsoRemotoComAutorizacao()) {
            throw new IllegalStateException("Afiliação institucional não autoriza uso remoto de certificado fora do ambiente institucional.");
        }
        Usuario issuer = currentUserService.getRequired();
        Instant now = Instant.now();
        int hours = validForHours == null || validForHours <= 0 ? 8 : Math.min(validForHours, 72);
        InstitutionalRemoteCertificateAuthorization authorization = new InstitutionalRemoteCertificateAuthorization(
                UUID.randomUUID().toString(),
                affiliationId,
                Objects.requireNonNull(nominatedUserId),
                issuer.getId(),
                issuer.getNome(),
                reason == null || reason.isBlank() ? "uso_remoto_autorizado_pela_diretoria" : reason.trim(),
                sanitize(allowedNetworks),
                sanitize(allowedDevices),
                now,
                now.plusSeconds(hours * 3600L),
                com.tcc.pjb.backend.model.entity.enums.InstitutionalRemoteCertificateAuthorizationStatus.ATIVA,
                mergeFundamentos(fundamentos, List.of(
                        "autorizacao_remota_de_certificado",
                        "emissor_user_id=" + issuer.getId(),
                        "validade_horas=" + hours)),
                now,
                now,
                null
        );
        return repository.save(authorization);
    }

    public InstitutionalRemoteCertificateAuthorization revogar(String authorizationId, List<String> fundamentos) {
        InstitutionalRemoteCertificateAuthorization current = repository.findByAuthorizationId(authorizationId)
                .orElseThrow(() -> new IllegalArgumentException("Autorização remota não encontrada."));
        return repository.save(current.revoke(mergeFundamentos(fundamentos, List.of("revogacao_imediata")), Instant.now()));
    }

    public List<InstitutionalRemoteCertificateAuthorization> listar(String affiliationId, Long userId) {
        List<InstitutionalRemoteCertificateAuthorization> source = affiliationId == null || affiliationId.isBlank()
                ? repository.findAll()
                : repository.findByAffiliationId(affiliationId.trim());
        return source.stream()
                .filter(item -> userId == null || Objects.equals(item.nominatedUserId(), userId))
                .sorted(Comparator.comparing(InstitutionalRemoteCertificateAuthorization::updatedAt).reversed())
                .toList();
    }

    public boolean possuiAutorizacaoAtiva(Long userId, String affiliationId) {
        if (userId == null || affiliationId == null || affiliationId.isBlank()) {
            return false;
        }
        Instant now = Instant.now();
        return listar(affiliationId, userId).stream().anyMatch(item -> item.ativaEm(now));
    }

    public Optional<InstitutionalRemoteCertificateAuthorization> buscar(String authorizationId) {
        return repository.findByAuthorizationId(authorizationId);
    }

    private List<String> sanitize(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream().filter(Objects::nonNull).map(String::trim).filter(item -> !item.isBlank()).distinct().toList();
    }

    private List<String> mergeFundamentos(List<String> left, List<String> right) {
        ArrayList<String> out = new ArrayList<>();
        if (left != null) out.addAll(left);
        if (right != null) out.addAll(right);
        return List.copyOf(out.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).distinct().toList());
    }
}
