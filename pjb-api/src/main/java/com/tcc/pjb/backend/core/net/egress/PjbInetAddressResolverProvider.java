package com.tcc.pjb.backend.core.net.egress;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.net.spi.InetAddressResolver;
import java.net.spi.InetAddressResolverProvider;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

public final class PjbInetAddressResolverProvider extends InetAddressResolverProvider {

    @Override
    public InetAddressResolver get(Configuration configuration) {
        InetAddressResolver delegate = configuration.builtinResolver();
        return new InetAddressResolver() {
            @Override
            public Stream<InetAddress> lookupByName(String host, LookupPolicy lookupPolicy) throws UnknownHostException {
                if (!DnsEgressPolicy.isAllowed(host)) throw new UnknownHostException(host);
                return delegate.lookupByName(host, lookupPolicy);
            }

            @Override
            public String lookupByAddress(byte[] addr) throws UnknownHostException {
                return delegate.lookupByAddress(addr);
            }
        };
    }

    @Override
    public String name() {
        return "PJB-EGRESS-DNS";
    }

    static final class DnsEgressPolicy {

        static boolean isAllowed(String host) {
            if (host == null || host.isBlank()) return false;
            if (!isEnforced()) return true;
            String h = host.trim();
            if (isIpLiteral(h)) return true;
            String n = h.toLowerCase(Locale.ROOT);
            Set<String> allow = AllowlistHolder.allow();
            return allow.contains(n);
        }

        private static boolean isEnforced() {
            String v = System.getenv("PJB_EGRESS_DNS_ENFORCE");
            if (v == null || v.isBlank()) return false;
            String t = v.trim().toLowerCase(Locale.ROOT);
            return t.equals("1") || t.equals("true") || t.equals("yes") || t.equals("on");
        }

        private static boolean isIpLiteral(String h) {
            if (h.indexOf(':') >= 0) return true;
            int dots = 0;
            for (int i = 0; i < h.length(); i++) {
                char c = h.charAt(i);
                if (c == '.') dots++;
                else if (c < '0' || c > '9') return false;
            }
            return dots == 3;
        }

        static final class AllowlistHolder {
            static Set<String> allow() {
                String raw = System.getenv("PJB_EGRESS_DNS_ALLOWLIST");
                if (raw == null || raw.isBlank()) return Set.of("localhost");
                String[] parts = raw.split(",");
                return Arrays.stream(parts)
                        .map(String::trim)
                        .filter(s -> !s.isBlank())
                        .map(s -> s.toLowerCase(Locale.ROOT))
                        .collect(java.util.stream.Collectors.toUnmodifiableSet());
            }
        }
    }
}
