package com.tcc.pjb.backend.platform.security.rbac;


public interface CapabilityCatalog {

    
    boolean isAllowed(String canonicalToken);
}
