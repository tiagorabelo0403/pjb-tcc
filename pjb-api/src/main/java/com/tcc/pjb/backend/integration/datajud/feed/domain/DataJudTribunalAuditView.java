package com.tcc.pjb.backend.integration.datajud.feed.domain;
public record DataJudTribunalAuditView(String tribunalCodigo, DataJudCheckpointAuditSnapshot checkpointAudit, DataJudTribunalProgressSnapshot progress) {}
