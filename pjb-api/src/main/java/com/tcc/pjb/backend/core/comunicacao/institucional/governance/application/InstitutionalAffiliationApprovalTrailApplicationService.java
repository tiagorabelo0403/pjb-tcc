package com.tcc.pjb.backend.core.comunicacao.institucional.governance.application;

import com.tcc.pjb.backend.core.comunicacao.institucional.affiliation.domain.InstitutionalAffiliationRequest;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.domain.InstitutionalAffiliationApprovalTrail;
import com.tcc.pjb.backend.core.comunicacao.institucional.governance.infrastructure.InstitutionalAffiliationApprovalTrailStateRepository;
import com.tcc.pjb.backend.model.entity.Usuario;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class InstitutionalAffiliationApprovalTrailApplicationService {

    private final InstitutionalAffiliationApprovalTrailStateRepository repository;

    public InstitutionalAffiliationApprovalTrailApplicationService(InstitutionalAffiliationApprovalTrailStateRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    public InstitutionalAffiliationApprovalTrail registrarSubmissao(InstitutionalAffiliationRequest request, Usuario representative) {
        Optional<InstitutionalAffiliationApprovalTrail> existing = repository.findLatestByRequestId(request.requestId());
        if (existing.isPresent()) {
            return existing.get();
        }
        Instant now = Instant.now();
        return repository.save(new InstitutionalAffiliationApprovalTrail(
                UUID.randomUUID().toString(),
                request.requestId(),
                representative == null ? request.representanteUsuarioId() : representative.getId(),
                representative == null ? request.representanteNome() : representative.getNome(),
                true,
                now,
                null,
                null,
                null,
                null,
                false,
                request.status().name(),
                merge(request.fundamentos(), List.of("assinatura_formal_do_orgao", "primeira_chave_de_confianca")),
                now,
                null
        ));
    }

    public InstitutionalAffiliationApprovalTrail registrarDecisao(InstitutionalAffiliationRequest request,
                                                                  Usuario approver,
                                                                  boolean approved,
                                                                  List<String> fundamentos) {
        InstitutionalAffiliationApprovalTrail current = repository.findLatestByRequestId(request.requestId())
                .orElseGet(() -> registrarSubmissao(request, null));
        return repository.save(current.withDecision(
                approver == null ? null : approver.getId(),
                approver == null ? null : approver.getNome(),
                approved,
                request.status().name(),
                merge(List.of("segunda_chave_pjb_homologacao", approved ? "homologacao_aprovada" : "homologacao_rejeitada"), fundamentos),
                Instant.now()
        ));
    }

    public Optional<InstitutionalAffiliationApprovalTrail> buscarUltima(String requestId) {
        return repository.findLatestByRequestId(requestId);
    }

    private List<String> merge(List<String> left, List<String> right) {
        ArrayList<String> out = new ArrayList<>();
        if (left != null) out.addAll(left);
        if (right != null) out.addAll(right);
        return List.copyOf(out.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isBlank()).distinct().toList());
    }
}
