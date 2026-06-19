package com.tcc.pjb.backend.core.security.abac;

import org.springframework.data.jpa.repository.JpaRepository;

interface PjbAuthzTrailDedupRepository extends JpaRepository<PjbAuthzTrailDedup, String> {
}
