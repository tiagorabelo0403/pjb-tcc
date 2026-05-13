package com.tcc.pjb.backend.configs;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "pjb")
public class PjbFeatureFlagsProperties {

    private final Toggle kafka = new Toggle();
    private final Toggle workflow = new Toggle();
    private final Toggle search = new Toggle();
    private final Gov gov = new Gov();

    public Toggle getKafka() { return kafka; }
    public Toggle getWorkflow() { return workflow; }
    public Toggle getSearch() { return search; }
    public Gov getGov() { return gov; }

    public static class Toggle {
        
        private boolean enabled = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }

    public static class Gov {
        private final Toggle vitalMonitor = new Toggle();

        public Toggle getVitalMonitor() { return vitalMonitor; }

        
        public void setVitalMonitor(Toggle vitalMonitor) {
            
            if (vitalMonitor != null) {
                this.vitalMonitor.setEnabled(vitalMonitor.isEnabled());
            }
        }
    }
}
