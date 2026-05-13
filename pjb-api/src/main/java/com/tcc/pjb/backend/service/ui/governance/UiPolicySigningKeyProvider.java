package com.tcc.pjb.backend.service.ui.governance;

import javax.crypto.SecretKey;

public interface UiPolicySigningKeyProvider {
    Handle acquire();
    interface Handle extends AutoCloseable {
        SecretKey key();
        @Override
        void close();
    }
}
