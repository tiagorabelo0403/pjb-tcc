package com.tcc.pjb.backend.service.offline.domain;

import java.util.List;

public record OfflineBundleActionResult(Long bundleId, List<OfflineActionView> actions) {}
