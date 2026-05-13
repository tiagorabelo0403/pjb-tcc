package com.tcc.pjb.backend.modules.atendimento.service;

import com.tcc.pjb.backend.core.security.CurrentUserService;
import com.tcc.pjb.backend.modules.atendimento.config.AtendimentoTosProperties;
import com.tcc.pjb.backend.modules.atendimento.entity.AtendimentoTosAcceptance;
import com.tcc.pjb.backend.modules.atendimento.repository.AtendimentoTosAcceptanceRepository;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AtendimentoTosService {

    private final AtendimentoTosAcceptanceRepository repo;
    private final AtendimentoTosProperties props;
    private final CurrentUserService currentUser;

    public AtendimentoTosService(AtendimentoTosAcceptanceRepository repo, AtendimentoTosProperties props, CurrentUserService currentUser) {
        this.repo = repo;
        this.props = props;
        this.currentUser = currentUser;
    }

    public TosInfo info() {
        int v = props.getVersion();
        String url = props.getUrl();
        long userId = currentUser.getRequired().getId();
        Optional<AtendimentoTosAcceptance> acc = repo.findById(userId);
        boolean accepted = acc.map(a -> a.getVersion() >= v).orElse(false);
        int acceptedVersion = acc.map(AtendimentoTosAcceptance::getVersion).orElse(0);
        return new TosInfo(v, url, accepted, acceptedVersion);
    }

    public void requireAccepted() {
        int required = props.getVersion();
        AtendimentoTosAcceptance acc = repo.findById(currentUser.getRequired().getId()).orElse(null);
        if (acc == null || acc.getVersion() < required) {
            throw new AtendimentoTosNotAcceptedException(required, props.getUrl());
        }
    }

    @Transactional
    public void accept(int version) {
        int required = props.getVersion();
        if (version < required) {
            throw new IllegalArgumentException("invalid_version");
        }
        long userId = currentUser.getRequired().getId();
        AtendimentoTosAcceptance a = repo.findById(userId).orElseGet(AtendimentoTosAcceptance::new);
        a.setUsuarioId(userId);
        a.setVersion(version);
        a.setAcceptedAt(Instant.now());
        repo.save(a);
    }

    public record TosInfo(int requiredVersion, String tosUrl, boolean accepted, int acceptedVersion) {
    }
}
