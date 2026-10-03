package com.tcc.pjb.backend.service.cidadao;

import com.tcc.pjb.backend.model.entity.Audiencia;
import com.tcc.pjb.backend.model.entity.julgamento.JulgamentoColegiado;
import com.tcc.pjb.backend.model.entity.workflow.MovimentacaoProcessual;
import com.tcc.pjb.backend.model.repository.AudienciaRepository;
import com.tcc.pjb.backend.model.repository.MovimentacaoProcessualRepository;
import com.tcc.pjb.backend.model.repository.julgamento.JulgamentoColegiadoRepository;
import com.tcc.pjb.backend.repository.document.DocumentoProcessualRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;

@Service
public class CidadaoProcessoDetalheLoaderService {

    private final MovimentacaoProcessualRepository movimentacaoRepository;
    private final DocumentoProcessualRepository documentoRepository;
    private final AudienciaRepository audienciaRepository;
    private final JulgamentoColegiadoRepository julgamentoRepository;

    public CidadaoProcessoDetalheLoaderService(MovimentacaoProcessualRepository movimentacaoRepository,
                                               DocumentoProcessualRepository documentoRepository,
                                               AudienciaRepository audienciaRepository,
                                               JulgamentoColegiadoRepository julgamentoRepository) {
        this.movimentacaoRepository = Objects.requireNonNull(movimentacaoRepository);
        this.documentoRepository = Objects.requireNonNull(documentoRepository);
        this.audienciaRepository = Objects.requireNonNull(audienciaRepository);
        this.julgamentoRepository = Objects.requireNonNull(julgamentoRepository);
    }

    public Map<Long, MovimentacaoProcessual> loadLatestMovements(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, MovimentacaoProcessual> out = new HashMap<>();
        for (MovimentacaoProcessual mov : movimentacaoRepository.findLatestByProcessoIds(new ArrayList<>(ids))) {
            if (mov != null && mov.getProcesso() != null && mov.getProcesso().getId() != null) {
                out.putIfAbsent(mov.getProcesso().getId(), mov);
            }
        }
        return out;
    }

    public Map<Long, Long> loadDocCounts(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        Map<Long, Long> out = new HashMap<>();
        for (DocumentoProcessualRepository.ProcessoDocCount row : documentoRepository.countDocsByProcessoIds(new ArrayList<>(ids))) {
            if (row != null && row.getProcessoId() != null) {
                out.put(row.getProcessoId(), row.getCnt());
            }
        }
        return out;
    }

    public Map<Long, Audiencia> loadNextAudiencias(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        long[] vector = ids.stream().mapToLong(Long::longValue).toArray();
        Map<Long, Audiencia> out = new HashMap<>();
        for (Audiencia audiencia : audienciaRepository.findNextUpcomingByProcessoIds(vector, LocalDateTime.now())) {
            if (audiencia != null && audiencia.getProcesso() != null && audiencia.getProcesso().getId() != null) {
                out.putIfAbsent(audiencia.getProcesso().getId(), audiencia);
            }
        }
        return out;
    }

    public Map<Long, JulgamentoColegiado> loadNextJulgamentos(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Map.of();
        }
        long[] vector = ids.stream().mapToLong(Long::longValue).toArray();
        Map<Long, JulgamentoColegiado> out = new HashMap<>();
        for (JulgamentoColegiado julgamento : julgamentoRepository.findNextPautaByProcessoIds(vector, LocalDateTime.now())) {
            if (julgamento != null && julgamento.getProcesso() != null && julgamento.getProcesso().getId() != null) {
                out.putIfAbsent(julgamento.getProcesso().getId(), julgamento);
            }
        }
        return out;
    }
}
