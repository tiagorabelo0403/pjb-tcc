package com.tcc.pjb.backend.controller.system;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
public class PjbLocalRequestGuard {

    public boolean isAllowed(HttpServletRequest request) {
        if (request == null) {
            return false;
        }
        return isLoopback(request.getRemoteAddr());
    }

    public ResponseEntity<Map<String, Object>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.<String, Object>of("status", "FORBIDDEN"));
    }

    private boolean isLoopback(String remoteAddress) {
        if (remoteAddress == null || remoteAddress.isBlank()) {
            return false;
        }
        try {
            InetAddress address = InetAddress.getByName(remoteAddress);
            return address.isLoopbackAddress() || address.isAnyLocalAddress();
        } catch (UnknownHostException ex) {
            return false;
        }
    }
}
