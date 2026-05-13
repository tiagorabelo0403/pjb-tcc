package com.tcc.pjb.backend.configs.security.perimeter;

import java.math.BigInteger;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class IpCidrMatcher {

    private final List<Cidr> cidrs;

    IpCidrMatcher(List<String> cidrValues) {
        this.cidrs = new ArrayList<>();
        if (cidrValues == null) {
            return;
        }
        for (String value : cidrValues) {
            if (value == null || value.isBlank()) {
                continue;
            }
            Cidr parsed = Cidr.parse(value.trim());
            if (parsed != null) {
                cidrs.add(parsed);
            }
        }
    }

    boolean matches(String ip) {
        if (ip == null || ip.isBlank() || cidrs.isEmpty()) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(ip.trim());
            for (Cidr cidr : cidrs) {
                if (cidr.matches(address)) {
                    return true;
                }
            }
            return false;
        } catch (Exception ex) {
            return false;
        }
    }

    private record Cidr(byte[] network, int prefix) {
        static Cidr parse(String value) {
            try {
                String[] parts = value.split("/");
                InetAddress address = InetAddress.getByName(parts[0]);
                int max = address.getAddress().length * 8;
                int prefix = parts.length > 1 ? Integer.parseInt(parts[1]) : max;
                if (prefix < 0 || prefix > max) {
                    return null;
                }
                return new Cidr(address.getAddress(), prefix);
            } catch (Exception ex) {
                return null;
            }
        }

        boolean matches(InetAddress address) {
            byte[] candidate = address.getAddress();
            if (candidate.length != network.length) {
                return false;
            }
            BigInteger mask = prefix == 0
                    ? BigInteger.ZERO
                    : BigInteger.ONE.shiftLeft(network.length * 8).subtract(BigInteger.ONE)
                    .shiftRight(network.length * 8 - prefix)
                    .shiftLeft(network.length * 8 - prefix);
            BigInteger left = new BigInteger(1, network).and(mask);
            BigInteger right = new BigInteger(1, candidate).and(mask);
            return Objects.equals(left, right);
        }
    }
}
