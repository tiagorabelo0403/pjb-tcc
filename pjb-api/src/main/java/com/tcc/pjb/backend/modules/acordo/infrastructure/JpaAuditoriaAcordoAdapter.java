package com.tcc.pjb.backend.modules.acordo.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.modules.acordo.api.AcordoAuditEntry;
import com.tcc.pjb.backend.modules.acordo.api.AuditoriaAcordoPort;
import com.tcc.pjb.backend.modules.acordo.infrastructure.persistence.AcordoAuditoriaEntity;
import com.tcc.pjb.backend.modules.acordo.infrastructure.persistence.AcordoAuditoriaJpaRepository;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JpaAuditoriaAcordoAdapter implements AuditoriaAcordoPort {

    private final AcordoAuditoriaJpaRepository repository;
    private final ObjectMapper objectMapper;

    public JpaAuditoriaAcordoAdapter(AcordoAuditoriaJpaRepository repository,
                                     ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void registrarEvento(AcordoAuditEntry evento) {
        AcordoAuditoriaEntity entity = new AcordoAuditoriaEntity();
        entity.setSessaoId(evento.sessaoId());
        entity.setUsuarioId(evento.usuarioId());
        entity.setEvento(evento.evento());
        entity.setDetalhesJson(toJson(evento.detalhes() == null ? Map.of() : evento.detalhes()));
        entity.setIpHash(limit(evento.ipHash(), 64));
        entity.setUserAgentHash(limit(evento.userAgentHash(), 64));
        entity.setCreatedAt(evento.createdAt());
        repository.save(entity);
    }

    private String toJson(Map<String, Object> value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Falha ao serializar auditoria de acordo.", ex);
        }
    }

    private String limit(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
