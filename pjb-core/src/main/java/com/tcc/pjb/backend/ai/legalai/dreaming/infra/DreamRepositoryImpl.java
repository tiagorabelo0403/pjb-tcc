package com.tcc.pjb.backend.ai.legalai.dreaming.infra;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.Dream;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamId;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamInput;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamRepository;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamResult;
import com.tcc.pjb.backend.ai.legalai.dreaming.domain.DreamStatus;
import com.tcc.pjb.backend.ai.legalai.memory.domain.MemoryStoreId;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class DreamRepositoryImpl implements DreamRepository {

    private final DreamJpaRepository jpaRepository;
    private final ObjectMapper objectMapper;

    public DreamRepositoryImpl(DreamJpaRepository jpaRepository, ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public Dream salvar(Dream dream) {
        DreamJpaEntity entity = toEntity(dream);
        DreamJpaEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Dream> buscarPorId(DreamId id) {
        return jpaRepository.findById(id.value()).map(this::toDomain);
    }

    @Override
    public Optional<Dream> buscarPorAnthropicDreamId(String anthropicDreamId) {
        return jpaRepository.findByAnthropicDreamId(anthropicDreamId).map(this::toDomain);
    }

    @Override
    public List<Dream> listarPorStatus(DreamStatus status) {
        return jpaRepository.findByStatus(status.name()).stream().map(this::toDomain).toList();
    }

    private DreamJpaEntity toEntity(Dream dream) {
        DreamJpaEntity e = new DreamJpaEntity();
        e.setId(dream.id().value());
        e.setAnthropicDreamId(dream.anthropicDreamId());
        e.setStatus(dream.status().name());
        e.setInputStoreId(dream.input().inputStoreId().value());
        e.setSessionIdsJson(serializeSessionIds(dream.input()));
        e.setInstrucoes(dream.instrucoes());
        e.setModelo(dream.modelo());
        e.setCriadoEm(dream.criadoEm());
        e.setEncerradoEm(dream.encerradoEm());
        e.setErro(dream.erro());
        if (dream.result() != null) {
            e.setOutputStoreId(dream.result().outputStoreId() != null ? dream.result().outputStoreId().value() : null);
            e.setAnthropicOutputStoreId(dream.result().anthropicOutputStoreId());
            e.setInputTokens(dream.result().inputTokens());
            e.setOutputTokens(dream.result().outputTokens());
        }
        return e;
    }

    private Dream toDomain(DreamJpaEntity e) {
        DreamInput input = buildInput(e);
        DreamResult result = e.getOutputStoreId() != null || e.getAnthropicOutputStoreId() != null
                ? new DreamResult(
                        e.getOutputStoreId() != null ? MemoryStoreId.de(e.getOutputStoreId()) : null,
                        e.getAnthropicOutputStoreId(),
                        e.getInputTokens() != null ? e.getInputTokens() : 0L,
                        e.getOutputTokens() != null ? e.getOutputTokens() : 0L)
                : null;
        return new Dream(
                DreamId.de(e.getId()),
                e.getAnthropicDreamId(),
                DreamStatus.valueOf(e.getStatus()),
                input,
                result,
                e.getInstrucoes(),
                e.getModelo(),
                e.getCriadoEm(),
                e.getEncerradoEm(),
                e.getErro()
        );
    }

    private DreamInput buildInput(DreamJpaEntity e) {
        MemoryStoreId storeId = MemoryStoreId.de(e.getInputStoreId());
        if (e.getSessionIdsJson() != null && !e.getSessionIdsJson().isBlank()) {
            try {
                List<String> sessionIds = objectMapper.readValue(e.getSessionIdsJson(), new TypeReference<>() {});
                return new DreamInput.SessionsInput(storeId, sessionIds);
            } catch (Exception ex) {
                return new DreamInput.MemoryStoreInput(storeId);
            }
        }
        return new DreamInput.MemoryStoreInput(storeId);
    }

    private String serializeSessionIds(DreamInput input) {
        if (input instanceof DreamInput.SessionsInput si) {
            try {
                return objectMapper.writeValueAsString(si.sessionIds());
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
