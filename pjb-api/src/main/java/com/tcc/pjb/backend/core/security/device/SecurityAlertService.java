package com.tcc.pjb.backend.core.security.device;

import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.tcc.pjb.backend.model.entity.Usuario;
import com.tcc.pjb.backend.model.entity.security.SecurityAlert;
import com.tcc.pjb.backend.model.repository.security.SecurityAlertRepository;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class SecurityAlertService {

    private final SecurityAlertRepository repo;

    public SecurityAlertService(SecurityAlertRepository repo) {
        this.repo = Objects.requireNonNull(repo);
    }

    @Transactional
    public SecurityAlert create(Usuario usuario, String tipo, String titulo, String detalhes, String ip, int riskScore) {
        SecurityAlert a = new SecurityAlert();
        a.setUsuario(usuario);
        a.setTipo(tipo);
        a.setTitulo(titulo);
        a.setDetalhes(detalhes);
        a.setIp(ip);
        a.setRiskScore(Math.max(0, Math.min(100, riskScore)));
        a.setAcknowledged(false);
        SecurityAlert saved = repo.save(a);
        log.warn("[SECURITY_ALERT] userId={} tipo={} risk={} ip={} titulo={}",
                usuario != null ? usuario.getId() : null,
                tipo,
                riskScore,
                ip,
                titulo);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<SecurityAlert> myAlerts(Long userId) {
        return repo.findTop50ByUser(userId);
    }
}
