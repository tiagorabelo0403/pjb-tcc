package com.tcc.pjb.backend.service.admin.surface;

import java.util.Objects;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.dto.admin.backfill.AdminDuplicateClienteResponse;
import com.tcc.pjb.backend.modules.advocacia.enums.StatusCliente;
import com.tcc.pjb.backend.modules.advocacia.repository.ClienteRepository;

@Service
public class AdminBackfillReviewFacadeService {

    private final ClienteRepository clienteRepository;

    public AdminBackfillReviewFacadeService(ClienteRepository clienteRepository) {
        this.clienteRepository = Objects.requireNonNull(clienteRepository);
    }

    public Page<AdminDuplicateClienteResponse> duplicates(Long advogadoId, Pageable pageable) {
        return clienteRepository
                .findDuplicatesForReview(StatusCliente.EM_ANALISE, advogadoId, pageable)
                .map(c -> new AdminDuplicateClienteResponse(
                        c.getId(),
                        c.getNomeCompleto(),
                        c.getAdvogado() != null ? c.getAdvogado().getId() : null,
                        c.getEquipe() != null ? c.getEquipe().getId() : null,
                        c.getStatus() != null ? c.getStatus().name() : null,
                        c.getDataCriacao() != null ? c.getDataCriacao().toString() : null
                ));
    }
}
