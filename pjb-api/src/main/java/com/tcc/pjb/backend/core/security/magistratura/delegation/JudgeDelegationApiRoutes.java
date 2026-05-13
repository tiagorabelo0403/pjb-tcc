package com.tcc.pjb.backend.core.security.magistratura.delegation;

public final class JudgeDelegationApiRoutes {

    public static final String CANONICAL_BASE = "/api/v1/judge/delegation";
    public static final String PATH_ISSUE = "/issue";
    public static final String PATH_REQUESTS = "/requests";
    public static final String PATH_REQUEST_APPROVE = PATH_REQUESTS + "/{requestId}/approve";
    public static final String PATH_REQUEST_REJECT = PATH_REQUESTS + "/{requestId}/reject";
    public static final String PATH_REQUEST_REVOKE = PATH_REQUESTS + "/{requestId}/revoke";
    public static final String PATH_REQUESTS_PENDING = PATH_REQUESTS + "/pending";
    public static final String PATH_REQUESTS_MINE = PATH_REQUESTS + "/mine";
    public static final String PATH_ACTIVE = "/active";
    public static final String PATH_VERIFY = "/verify";
    public static final String PATH_ME = "/me";

    private JudgeDelegationApiRoutes() {
    }
}
