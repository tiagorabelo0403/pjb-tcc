package com.tcc.pjb.backend.core.kernel.casefile;


public enum CaseFileEventType {

    CASEFILE_CREATED,
    ROOT_PROCEEDING_ASSURED,

    FACT_INGESTED,

    PROCEEDING_UPSERTED,
    EDGE_UPSERTED,
    CASE_CONTINUITY_SYNCED,
    CASE_FILES_MERGED,

    SYNC_DIRECTIVE_EMITTED,
    WORK_ITEM_EMITTED
}
