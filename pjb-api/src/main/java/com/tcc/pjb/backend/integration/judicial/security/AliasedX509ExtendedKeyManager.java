package com.tcc.pjb.backend.integration.judicial.security;

import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.X509ExtendedKeyManager;

public final class AliasedX509ExtendedKeyManager extends X509ExtendedKeyManager {

    private final X509ExtendedKeyManager delegate;
    private final String preferredAlias;

    public AliasedX509ExtendedKeyManager(X509ExtendedKeyManager delegate, String preferredAlias) {
        this.delegate = delegate;
        this.preferredAlias = preferredAlias == null || preferredAlias.isBlank() ? null : preferredAlias.trim();
    }

    @Override
    public String[] getClientAliases(String keyType, Principal[] issuers) {
        return delegate.getClientAliases(keyType, issuers);
    }

    @Override
    public String chooseClientAlias(String[] keyTypes, Principal[] issuers, Socket socket) {
        return hasPreferredAlias() ? preferredAlias : delegate.chooseClientAlias(keyTypes, issuers, socket);
    }

    @Override
    public String[] getServerAliases(String keyType, Principal[] issuers) {
        return delegate.getServerAliases(keyType, issuers);
    }

    @Override
    public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
        return delegate.chooseServerAlias(keyType, issuers, socket);
    }

    @Override
    public X509Certificate[] getCertificateChain(String alias) {
        return delegate.getCertificateChain(alias);
    }

    @Override
    public PrivateKey getPrivateKey(String alias) {
        return delegate.getPrivateKey(alias);
    }

    @Override
    public String chooseEngineClientAlias(String[] keyTypes, Principal[] issuers, SSLEngine engine) {
        return hasPreferredAlias() ? preferredAlias : delegate.chooseEngineClientAlias(keyTypes, issuers, engine);
    }

    @Override
    public String chooseEngineServerAlias(String keyType, Principal[] issuers, SSLEngine engine) {
        return delegate.chooseEngineServerAlias(keyType, issuers, engine);
    }

    private boolean hasPreferredAlias() {
        return preferredAlias != null && delegate.getCertificateChain(preferredAlias) != null;
    }
}
