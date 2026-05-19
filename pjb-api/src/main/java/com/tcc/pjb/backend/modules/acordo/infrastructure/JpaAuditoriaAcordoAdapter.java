package com.tcc.pjb.backend.modules.acordo.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tcc.pjb.backend.modules.acordo.api.AuditoriaAcordoCommand;
import com.tcc.pjb.backend.modules.acordo.api.AuditoriaAcordoPort;
import com.tcc.pjb.backend.modules.acordo.infrastructure.persistence.AcordoAuditoriaEntity;
import com.tcc.pjb.backend.modules.acordo.infrastructure.persistence.AcordoAuditoriaJpaRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
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
    public void registrarEvento(AuditoriaAcordoCommand command) {
        persist(command, false, false);
    }

    @Override
    public void registrarEventoSensivel(AuditoriaAcordoCommand command) {
        persist(command, true, false);
    }

    @Override
    public void registrarTentativaNegada(AuditoriaAcordoCommand command) {
        persist(command, true, true);
    }

    private void persist(AuditoriaAcordoCommand command, boolean sensivel, boolean tentativaNegada) {
        if (command == null || command.evento() == null) {
            throw new IllegalArgumentException("Comando de auditoria de acordo invalido");
        }
        AcordoAuditoriaEntity entity = new AcordoAuditoriaEntity();
        entity.setSessaoId(command.sessaoId());
        entity.setUsuarioId(command.usuarioId());
        entity.setEvento(command.evento());
        entity.setDetalhesJson(toJson(detalhes(command, sensivel, tentativaNegada)));
        entity.setIpHash(limit(command.ipHash(), 64));
        entity.setUserAgentHash(limit(command.userAgentHash(), 64));
        entity.setCreatedAt(command.createdAt() != null ? command.createdAt() : Instant.now());
        repository.save(entity);
    }

    private Map<String, Object> detalhes(AuditoriaAcordoCommand command, boolean sensivel, boolean tentativaNegada) {
        LinkedHashMap<String, Object> out = new LinkedHashMap<>();
        if (command.detalhes() != null) {
            out.putAll(command.detalhes());
        }
        out.put("origem", command.origem() == null || command.origem().isBlank() ? "ACORDO_PROCESSUAL" : command.origem().trim());
        out.put("sensivel", sensivel);
        out.put("tentativaNegada", tentativaNegada);
        return out;
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
