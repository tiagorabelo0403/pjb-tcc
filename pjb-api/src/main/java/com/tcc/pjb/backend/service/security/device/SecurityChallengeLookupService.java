package com.tcc.pjb.backend.service.security.device;

import com.tcc.pjb.backend.model.entity.security.SecurityChallenge;
import com.tcc.pjb.backend.model.repository.security.SecurityChallengeRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;

@Service
public class SecurityChallengeLookupService {

    private final SecurityChallengeRepository securityChallengeRepository;

    public SecurityChallengeLookupService(SecurityChallengeRepository securityChallengeRepository) {
        this.securityChallengeRepository = securityChallengeRepository;
    }

    public Optional<SecurityChallenge> findSafe(Long challengeId) {
        return securityChallengeRepository.findByIdSafe(challengeId);
    }
}
