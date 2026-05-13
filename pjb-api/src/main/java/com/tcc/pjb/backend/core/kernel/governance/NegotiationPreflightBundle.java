package com.tcc.pjb.backend.core.kernel.governance;

import com.tcc.pjb.backend.core.kernel.advisory.InstitutionalGovernanceContextReport;
import com.tcc.pjb.backend.core.kernel.advisory.KernelOperationalGovernanceReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationApprovalMatrixReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChannelGovernanceReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationChatDigestReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationExplainabilityReport;
import com.tcc.pjb.backend.core.kernel.advisory.NegotiationMemoryReport;
import com.tcc.pjb.backend.core.kernel.advisory.SettlementAdvisoryReport;

public record NegotiationPreflightBundle(
        SettlementAdvisoryReport settlementAdvisory,
        InstitutionalGovernanceContextReport institutionalGovernanceContext,
        NegotiationMemoryReport negotiationMemory,
        NegotiationExplainabilityReport negotiationExplainability,
        KernelOperationalGovernanceReport kernelOperationalGovernance,
        NegotiationChatDigestReport negotiationChatDigest,
        NegotiationApprovalMatrixReport negotiationApprovalMatrix,
        NegotiationChannelGovernanceReport negotiationChannelGovernance,
        InstitutionalPolicySnapshotReport institutionalPolicySnapshot,
        KernelDecisionMetricsReport kernelDecisionMetrics,
        KernelRiskEscalationReport kernelRiskEscalation,
        NegotiationMessageDecision decision
) {
}
