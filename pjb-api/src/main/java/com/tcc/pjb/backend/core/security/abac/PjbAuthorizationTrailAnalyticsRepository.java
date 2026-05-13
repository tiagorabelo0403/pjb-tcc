package com.tcc.pjb.backend.core.security.abac;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PjbAuthorizationTrailAnalyticsRepository extends JpaRepository<PjbAuthorizationTrailAnalyticsEntry, Long> {

    boolean existsByAnalyticsKey(String analyticsKey);

    Optional<PjbAuthorizationTrailAnalyticsEntry> findByAnalyticsKey(String analyticsKey);

    void deleteByGranularityAndBucketStartedAtGreaterThanEqualAndBucketStartedAtLessThan(String granularity,
                                                                                         LocalDateTime bucketStartedAt,
                                                                                         LocalDateTime bucketStartedBefore);

    List<PjbAuthorizationTrailAnalyticsEntry> findByGranularityAndBucketStartedAtGreaterThanEqualAndBucketStartedAtLessThanOrderByBucketStartedAtAscDimensionTypeAscTotalCountDesc(
            String granularity,
            LocalDateTime bucketStartedAt,
            LocalDateTime bucketStartedBefore
    );

    long countByGranularityAndBucketStartedAtGreaterThanEqualAndBucketStartedAtLessThan(String granularity,
                                                                                        LocalDateTime bucketStartedAt,
                                                                                        LocalDateTime bucketStartedBefore);

    long countByGranularity(String granularity);

    PjbAuthorizationTrailAnalyticsEntry findFirstByGranularityOrderByBucketStartedAtAscDimensionTypeAsc(String granularity);

    PjbAuthorizationTrailAnalyticsEntry findFirstByGranularityOrderByBucketStartedAtDescDimensionTypeAsc(String granularity);
}
