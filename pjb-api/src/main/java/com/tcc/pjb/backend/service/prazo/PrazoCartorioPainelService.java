package com.tcc.pjb.backend.service.prazo;

import com.tcc.pjb.backend.model.dto.prazo.PrazoCartorioItemResponse;
import com.tcc.pjb.backend.model.dto.prazo.PrazoCartorioPainelResponse;
import com.tcc.pjb.backend.model.entity.comunicacao.CienciaProcessual;
import com.tcc.pjb.backend.model.repository.CienciaProcessualRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PrazoCartorioPainelService {

    private static final int LIMITE_ITENS = 500;

    private final CienciaProcessualRepository cienciaProcessualRepository;

    public PrazoCartorioPainelService(CienciaProcessualRepository cienciaProcessualRepository) {
        this.cienciaProcessualRepository = Objects.requireNonNull(cienciaProcessualRepository);
    }

    @Transactional(readOnly = true)
    public PrazoCartorioPainelResponse painelPorVara(String vara, int diasJanela) {
        Objects.requireNonNull(vara, "vara");
        Instant agora = Instant.now();
        Instant ateData = agora.plus(Math.max(1, diasJanela), ChronoUnit.DAYS);
        List<CienciaProcessual> pendentes = cienciaProcessualRepository.findPendentesPorVaraAteData(
                vara, ateData, PageRequest.of(0, LIMITE_ITENS));

        long vencidos = 0;
        long vencendoEm7 = 0;
        long vencendoEm15 = 0;
        List<PrazoCartorioItemResponse> itens = pendentes.stream()
                .map(ciencia -> {
                    boolean vencido = ciencia.getDataExpiracao().isBefore(agora);
                    long diasRestantes = vencido ? 0 : ChronoUnit.DAYS.between(agora, ciencia.getDataExpiracao());
                    return new PrazoCartorioItemResponse(
                            ciencia.getId(),
                            ciencia.getProcesso() == null ? null : ciencia.getProcesso().getId(),
                            ciencia.getNumeroProcesso(),
                            ciencia.getTipoCiencia() == null ? null : ciencia.getTipoCiencia().name(),
                            ciencia.getDataExpiracao(),
                            diasRestantes,
                            vencido);
                })
                .toList();

        for (PrazoCartorioItemResponse item : itens) {
            if (item.vencido()) {
                vencidos++;
                vencendoEm7++;
                vencendoEm15++;
                continue;
            }
            if (item.diasRestantes() <= 7) {
                vencendoEm7++;
            }
            if (item.diasRestantes() <= 15) {
                vencendoEm15++;
            }
        }

        return new PrazoCartorioPainelResponse(vara, itens.size(), vencidos, vencendoEm7, vencendoEm15, itens);
    }
}
