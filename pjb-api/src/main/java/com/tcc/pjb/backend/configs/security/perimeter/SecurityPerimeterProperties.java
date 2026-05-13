package com.tcc.pjb.backend.configs.security.perimeter;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import lombok.Data;

@Data
@SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
@ConfigurationProperties(prefix = "pjb.security.perimeter")
public class SecurityPerimeterProperties {

    
    private boolean enabled = true;

    
    private boolean trustProxyHeaders = false;

    private Proxy proxy = new Proxy();

    
    private List<String> bypassPaths = new ArrayList<>();


    private boolean corsEnabled = true;
    private List<String> corsAllowedOrigins = new ArrayList<>();
    private List<String> corsAllowedMethods = new ArrayList<>(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    private List<String> corsAllowedHeaders = new ArrayList<>(List.of("Authorization", "Content-Type", "X-Request-Id", "X-PJB-Justificativa", "X-PJB-Body-Hash", "X-PJB-Signature", "X-PJB-Timestamp"));
    private List<String> corsExposedHeaders = new ArrayList<>(List.of("RateLimit-Limit", "RateLimit-Remaining", "RateLimit-Reset", "Retry-After", "X-RateLimit-Limit", "X-RateLimit-Remaining", "X-Request-Id"));
    private boolean corsAllowCredentials = false;
    private Duration corsMaxAge = Duration.ofHours(1);

    private Blocklist blocklist = new Blocklist();
    private Ratelimit ratelimit = new Ratelimit();


    @Data
    public static class Proxy {
        private boolean rejectUntrustedForwardedHeaders = true;
        private int maxForwardedForHops = 8;
        private List<String> trustedCidrs = new ArrayList<>();
    }

    @Data
    public static class Blocklist {
        
        private String store = "memory";
        private String keyPrefix = "pjb:sec:block:";
    }

    @Data
    public static class Ratelimit {
        private boolean enabled = true;
        
        private String store = "memory";
        private Duration window = Duration.ofSeconds(60);
        private long maxRequests = 600;

        
        private Duration violationWindow = Duration.ofMinutes(10);
        
        private long banAfterViolations = 7;
        
        private Duration banTtl = Duration.ofHours(1);

        private String keyPrefix = "pjb:sec:rl:";

        
        private List<Rule> rules = new ArrayList<>();

        @Data
        public static class Rule {
            
            private String name;

            
            private List<String> paths = new ArrayList<>();

            
            private List<String> methods = new ArrayList<>();

            
            private Duration window;

            
            private Long maxRequests;

            
            private Duration violationWindow;
            private Long banAfterViolations;
            private Duration banTtl;

            
            private String keyStrategy = "ip";
        }
    }
}
