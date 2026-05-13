package com.tcc.pjb.backend.core.security.abac;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

final class PjbAuthorizationTrailCsvExporter {

    String export(List<PjbAuthorizationTrailSnapshot> snapshots) {
        StringBuilder out = new StringBuilder(4096);
        out.append("occurredAt,auditEventCode,action,resourceType,resourceId,allowed,reason,riskLevel,riskScore,actorType,actorId,requestId,integrationCode,institutionalUnitCode,institutionalBoxCode,institutionalCapabilityCode,governanceRequired,governanceSatisfied,stepUpRequired,stepUpSatisfied,payloadHash\n");
        if (snapshots == null) {
            return out.toString();
        }
        for (PjbAuthorizationTrailSnapshot snapshot : snapshots) {
            out.append(csv(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(snapshot.occurredAt().atOffset(ZoneOffset.UTC)))).append(',')
                    .append(csv(snapshot.auditEventCode())).append(',')
                    .append(csv(snapshot.action())).append(',')
                    .append(csv(snapshot.resourceType())).append(',')
                    .append(csv(snapshot.resourceId())).append(',')
                    .append(snapshot.allowed()).append(',')
                    .append(csv(snapshot.reason())).append(',')
                    .append(csv(snapshot.riskLevel().name())).append(',')
                    .append(snapshot.riskScore()).append(',')
                    .append(csv(snapshot.actorType())).append(',')
                    .append(snapshot.actorId() == null ? "" : snapshot.actorId()).append(',')
                    .append(csv(snapshot.requestId())).append(',')
                    .append(csv(snapshot.integrationCode())).append(',')
                    .append(csv(snapshot.institutionalUnitCode())).append(',')
                    .append(csv(snapshot.institutionalBoxCode())).append(',')
                    .append(csv(snapshot.institutionalCapabilityCode())).append(',')
                    .append(snapshot.governanceRequired()).append(',')
                    .append(snapshot.governanceSatisfied()).append(',')
                    .append(snapshot.stepUpRequired()).append(',')
                    .append(snapshot.stepUpSatisfied()).append(',')
                    .append(csv(snapshot.payloadHash()))
                    .append('\n');
        }
        return out.toString();
    }

    private String csv(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace("\"", "\"\"");
        return '\"' + normalized + '\"';
    }
}
