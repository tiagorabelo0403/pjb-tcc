package com.tcc.pjb.backend.service.secretariat.query.queue;

import com.tcc.pjb.backend.core.forum.routing.ForumDeskPortfolioProfile;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatInstitutionalVisibilityService;
import com.tcc.pjb.backend.service.secretariat.query.reference.SecretariatDeskLoadProfile;
import java.util.List;
import java.util.Objects;

public record SecretariatQueueInboxContext(
    String inboxKey,
    List<String> statuses,
    SecretariatInstitutionalVisibilityService.SecretariatInboxInstitutionalProfile inboxProfile,
    ForumDeskPortfolioProfile portfolio,
    SecretariatQueueLoadProfile loadProfile,
    SecretariatDeskLoadProfile deskProfile,
    String inboxDescriptor,
    String dashboardBucket
) {

    public SecretariatQueueInboxContext {
        inboxKey = Objects.requireNonNull(inboxKey, "inboxKey");
        statuses = statuses == null ? List.of("PENDENTE", "EM_EXECUCAO") : List.copyOf(statuses);
        inboxProfile = Objects.requireNonNull(inboxProfile, "inboxProfile");
        portfolio = Objects.requireNonNull(portfolio, "portfolio");
        loadProfile = Objects.requireNonNull(loadProfile, "loadProfile");
        deskProfile = Objects.requireNonNull(deskProfile, "deskProfile");
        inboxDescriptor = Objects.requireNonNullElseGet(inboxDescriptor, portfolio::operationalDescriptor);
        dashboardBucket = Objects.requireNonNullElseGet(dashboardBucket, portfolio::dashboardBucket);
    }
}
