package com.tcc.pjb.backend.core.security.crypto;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.Arrays;

public final class StringWiper {

    private static final VarHandle VALUE;
    private static final VarHandle CODER;

    static {
        VarHandle v = null;
        VarHandle c = null;
        try {
            MethodHandles.Lookup l = MethodHandles.privateLookupIn(String.class, MethodHandles.lookup());
            v = l.findVarHandle(String.class, "value", byte[].class);
            c = l.findVarHandle(String.class, "coder", byte.class);
        } catch (Throwable ignored) {
        }
        VALUE = v;
        CODER = c;
    }

    private StringWiper() {
    }

    public static void tryWipe(String s) {
        if (s == null) return;
        if (VALUE == null || CODER == null) return;
        try {
            byte[] bytes = (byte[]) VALUE.get(s);
            if (bytes == null) return;
            Arrays.fill(bytes, (byte) 0);
            CODER.set(s, (byte) 0);
        } catch (Throwable ignored) {
        }
    }
}
