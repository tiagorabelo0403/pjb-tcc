package com.tcc.pjb.backend.core.security.crypto;

import java.security.Security;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

public final class BouncyCastleProviders {

    public static final String NAME = BouncyCastleProvider.PROVIDER_NAME;

    private BouncyCastleProviders() {
    }

    public static void ensureRegistered() {
        if (Security.getProvider(NAME) == null) {
            Security.addProvider(new BouncyCastleProvider());
        }
    }
}
