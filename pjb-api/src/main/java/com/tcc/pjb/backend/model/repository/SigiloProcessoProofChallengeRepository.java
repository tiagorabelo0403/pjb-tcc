package com.tcc.pjb.backend.model.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import com.tcc.pjb.backend.model.entity.security.SigiloProcessoProofChallenge;

public interface SigiloProcessoProofChallengeRepository extends JpaRepository<SigiloProcessoProofChallenge, Long> {

    Optional<SigiloProcessoProofChallenge> findByChallengeId(String challengeId);
}
