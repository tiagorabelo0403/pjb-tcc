package com.tcc.pjb.backend.core.security.webauthn;

import com.tcc.pjb.backend.model.repository.security.PasskeySessionRepository;
import java.time.LocalDateTime;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PasskeySessionActivityService {

    private final PasskeySessionRepository repository;

    public PasskeySessionActivityService(PasskeySessionRepository repository) {
        this.repository = Objects.requireNonNull(repository);
    }

    @Transactional
    public void touch(Long sessionId, LocalDateTime agora) {
        repository.touch(sessionId, agora);
    }
}
