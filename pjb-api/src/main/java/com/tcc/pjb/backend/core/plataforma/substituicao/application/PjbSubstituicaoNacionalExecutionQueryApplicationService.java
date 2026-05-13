package com.tcc.pjb.backend.core.plataforma.substituicao.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoAcao;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoExecucaoSituacao;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoNacionalExecucaoAggregate;
import com.tcc.pjb.backend.core.plataforma.substituicao.domain.PjbSubstituicaoNacionalExecucaoEvento;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity.PjbSubstituicaoNacionalExecucaoEntity;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.entity.PjbSubstituicaoNacionalExecucaoEventoEntity;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoNacionalExecucaoEventoRepository;
import com.tcc.pjb.backend.core.plataforma.substituicao.persistence.repo.PjbSubstituicaoNacionalExecucaoRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@ConditionalOnProperty(prefix = "pjb.runtime.barrier.features", name = "substituicao-nacional", havingValue = "true", matchIfMissing = true)
public class PjbSubstituicaoNacionalExecutionQueryApplicationService {

    private static final TypeReference<LinkedHashMap<String, Object>> MAP_TYPE = new TypeReference<>() {};

    private final PjbSubstituicaoNacionalExecucaoRepository repository;
    private final PjbSubstituicaoNacionalExecucaoEventoRepository eventoRepository;
    private final ObjectMapper objectMapper;

    public PjbSubstituicaoNacionalExecutionQueryApplicationService(PjbSubstituicaoNacionalExecucaoRepository repository,
                                                                   PjbSubstituicaoNacionalExecucaoEventoRepository eventoRepository,
                                                                   ObjectMapper objectMapper) {
        this.repository = Objects.requireNonNull(repository);
        this.eventoRepository = Objects.requireNonNull(eventoRepository);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Transactional(readOnly = true)
    public PjbSubstituicaoNacionalExecucaoAggregate detalhar(Long execucaoId) {
        PjbSubstituicaoNacionalExecucaoEntity entity = repository.findById(execucaoId)
                .orElseThrow(() -> new IllegalArgumentException("Execução de substituição nacional não encontrada: " + execucaoId));
        return map(entity);
    }

    @Transactional(readOnly = true)
    public List<PjbSubstituicaoNacionalExecucaoAggregate> listar(String tribunalCodigo,
                                                                 PjbSubstituicaoExecucaoAcao acao,
                                                                 PjbSubstituicaoExecucaoSituacao situacao) {
        String normalizedTribunal = tribunalCodigo == null || tribunalCodigo.isBlank() ? null : tribunalCodigo.trim().toUpperCase();
        return repository.list(normalizedTribunal, acao, situacao).stream().limit(100).map(this::map).toList();
    }

    @Transactional(readOnly = true)
    public PjbSubstituicaoNacionalExecucaoAggregate porRequestHash(String requestHash) {
        PjbSubstituicaoNacionalExecucaoEntity entity = repository.findByRequestHash(requestHash)
                .orElseThrow(() -> new IllegalArgumentException("Execução de substituição nacional não encontrada para o hash informado."));
        return map(entity);
    }

    public PjbSubstituicaoNacionalExecucaoAggregate map(PjbSubstituicaoNacionalExecucaoEntity entity) {
        List<PjbSubstituicaoNacionalExecucaoEvento> eventos = eventoRepository.findByExecucaoIdOrderByCreatedAtAsc(entity.getId()).stream()
                .map(this::mapEvento)
                .toList();
        return new PjbSubstituicaoNacionalExecucaoAggregate(
                entity.getId(),
                entity.getTribunalCodigo(),
                entity.getTribunalNome(),
                entity.getRamoJustica(),
                entity.getAcao(),
                entity.getSituacao(),
                entity.getFaseAtual(),
                entity.getModoExecucao(),
                entity.isDryRun(),
                entity.isGateAprovado(),
                entity.isRollbackReversivel(),
                entity.getGateScore(),
                entity.getJobId(),
                entity.getCorrelationId(),
                entity.getRequestHash(),
                entity.getRequestedBy(),
                entity.getJustificativa(),
                entity.getOndaAlvo(),
                decodeMap(entity.getPayloadJson()),
                decodeMap(entity.getResultadoJson()),
                eventos,
                entity.getCreatedAt(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getUpdatedAt()
        );
    }

    private PjbSubstituicaoNacionalExecucaoEvento mapEvento(PjbSubstituicaoNacionalExecucaoEventoEntity entity) {
        return new PjbSubstituicaoNacionalExecucaoEvento(
                entity.getId(),
                entity.getCodigo(),
                entity.getSeveridade(),
                entity.getFase(),
                entity.getDescricao(),
                decodeMap(entity.getDetalhesJson()),
                entity.getCreatedAt()
        );
    }

    private Map<String, Object> decodeMap(String json) {
        try {
            if (json == null || json.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(json, MAP_TYPE);
        } catch (Exception ex) {
            return Map.of("raw", json);
        }
    }
}
