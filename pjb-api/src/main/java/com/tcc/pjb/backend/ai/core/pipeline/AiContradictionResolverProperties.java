package com.tcc.pjb.backend.ai.core.pipeline;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb.ai.contradiction")
public class AiContradictionResolverProperties {

    private final Map<String, DomainRules> domains = new HashMap<>();

    public Map<String, DomainRules> getDomains() {
        return domains;
    }

    public static final class DomainRules {

        private double proceedThresholdV1 = 0.55;
        private double proceedThresholdV2 = 0.58;
        private double proceedThresholdV3 = 0.62;

        private int temporalSpreadYearsHigh = 7;

        public double getProceedThresholdV1() {
            return proceedThresholdV1;
        }

        public void setProceedThresholdV1(double proceedThresholdV1) {
            this.proceedThresholdV1 = proceedThresholdV1;
        }

        public double getProceedThresholdV2() {
            return proceedThresholdV2;
        }

        public void setProceedThresholdV2(double proceedThresholdV2) {
            this.proceedThresholdV2 = proceedThresholdV2;
        }

        public double getProceedThresholdV3() {
            return proceedThresholdV3;
        }

        public void setProceedThresholdV3(double proceedThresholdV3) {
            this.proceedThresholdV3 = proceedThresholdV3;
        }

        public int getTemporalSpreadYearsHigh() {
            return temporalSpreadYearsHigh;
        }

        public void setTemporalSpreadYearsHigh(int temporalSpreadYearsHigh) {
            this.temporalSpreadYearsHigh = temporalSpreadYearsHigh;
        }
    }
}
