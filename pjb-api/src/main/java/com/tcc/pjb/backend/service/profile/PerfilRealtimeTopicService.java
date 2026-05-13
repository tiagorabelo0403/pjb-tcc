package com.tcc.pjb.backend.service.profile;

import java.util.Locale;
import org.springframework.stereotype.Service;
import com.tcc.pjb.backend.model.entity.Usuario;

@Service
public class PerfilRealtimeTopicService {

    public String inboxTopic(Usuario usuario, String channel) {
        String suffix = normalize(channel, "DEFAULT");
        long userId = usuario != null && usuario.getId() != null ? usuario.getId() : 0L;
        return "HIST:INBOX:" + suffix + ":USR:" + userId;
    }

    public String territoryTopic(Usuario usuario, String channel) {
        String suffix = normalize(channel, "DEFAULT");
        String uf = normalize(usuario != null ? usuario.getUf() : null, "BR");
        String comarca = normalize(usuario != null ? usuario.getComarca() : null, "GERAL");
        return "HIST:INBOX:" + suffix + ":UF:" + uf + ":COMARCA:" + comarca;
    }

    public String processTopic(Long processoId, String channel) {
        String suffix = normalize(channel, "PROCESSO");
        return "HIST:INBOX:" + suffix + ":PROCESSO:" + (processoId == null ? 0L : processoId);
    }

    private static String normalize(String raw, String fallback) {
        String value = raw == null || raw.isBlank() ? fallback : raw.trim();
        return value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9_:-]", "_");
    }
}
